package com.example.downloads

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.db.DownloadDao
import com.example.data.db.DownloadEntity
import com.example.data.model.DownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class CrumiaDownloadManager(
    private val context: Context,
    private val downloadDao: DownloadDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    init {
        ensureNotificationChannel()
    }

    private fun ensureNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Crumia Downloads",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "File download progress and completion notifications"
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun startDownload(
        url: String,
        userAgent: String? = null,
        contentDisposition: String? = null,
        mimeType: String? = null,
        showNotifications: Boolean = true
    ) {
        val resolvedMime = mimeType?.takeIf { it.isNotBlank() && it != "application/octet-stream" }
            ?: guessMimeType(url)
        val rawName = URLUtil.guessFileName(url, contentDisposition, resolvedMime)
        val safeName = ensureUniqueFilename(sanitizeFileName(rawName))
        val targetDir = getDownloadsDir()
        val destFile = File(targetDir, safeName)

        scope.launch {
            val id = downloadDao.insert(
                DownloadEntity(
                    fileName = safeName,
                    url = url,
                    mimeType = resolvedMime,
                    totalBytes = -1L,
                    downloadedBytes = 0L,
                    status = DownloadStatus.QUEUED.name,
                    filePath = destFile.absolutePath
                )
            )
            launchDownloadJob(id, userAgent, showNotifications)
        }
    }

    fun pauseDownload(id: Long) {
        activeJobs.remove(id)?.cancel()
        scope.launch {
            val item = downloadDao.getById(id) ?: return@launch
            downloadDao.update(item.copy(status = DownloadStatus.PAUSED.name))
            notificationManager.cancel(id.toInt())
        }
    }

    fun resumeDownload(id: Long, showNotifications: Boolean = true) {
        scope.launch {
            val item = downloadDao.getById(id) ?: return@launch
            if (item.status == DownloadStatus.COMPLETED.name) return@launch
            launchDownloadJob(id, null, showNotifications)
        }
    }

    fun retryDownload(id: Long, showNotifications: Boolean = true) {
        scope.launch {
            val item = downloadDao.getById(id) ?: return@launch
            downloadDao.update(
                item.copy(
                    status = DownloadStatus.QUEUED.name,
                    errorMessage = null
                )
            )
            launchDownloadJob(id, null, showNotifications)
        }
    }

    fun cancelDownload(id: Long) {
        activeJobs.remove(id)?.cancel()
        scope.launch {
            val item = downloadDao.getById(id) ?: return@launch
            runCatching { File(item.filePath).delete() }
            downloadDao.update(item.copy(status = DownloadStatus.CANCELLED.name))
            notificationManager.cancel(id.toInt())
        }
    }

    fun deleteDownloadRecord(entity: DownloadEntity) {
        activeJobs.remove(entity.id)?.cancel()
        scope.launch {
            runCatching { File(entity.filePath).delete() }
            downloadDao.deleteById(entity.id)
            notificationManager.cancel(entity.id.toInt())
        }
    }

    private fun launchDownloadJob(id: Long, userAgent: String?, showNotifications: Boolean) {
        activeJobs[id]?.cancel()
        val job = scope.launch {
            val entity = downloadDao.getById(id) ?: return@launch
            val destFile = File(entity.filePath)
            destFile.parentFile?.mkdirs()

            var existingBytes = if (destFile.exists()) destFile.length() else 0L
            downloadDao.update(entity.copy(status = DownloadStatus.DOWNLOADING.name, errorMessage = null))

            try {
                val requestBuilder = Request.Builder().url(entity.url)
                if (!userAgent.isNullOrBlank()) {
                    requestBuilder.header("User-Agent", userAgent)
                }
                if (existingBytes > 0L) {
                    requestBuilder.header("Range", "bytes=$existingBytes-")
                }

                client.newCall(requestBuilder.build()).execute().use { response ->
                    val code = response.code
                    val appendMode = (code == 206 && existingBytes > 0L)
                    if (!response.isSuccessful && code != 206) {
                        if (code == 416) {
                            // Range unsatisfiable: restart from 0
                            destFile.delete()
                            existingBytes = 0L
                        } else {
                            throw IllegalStateException("HTTP $code")
                        }
                    }

                    val body = response.body ?: throw IllegalStateException("Empty response")
                    val contentLength = body.contentLength()
                    val totalBytes = if (appendMode && contentLength > 0) {
                        existingBytes + contentLength
                    } else if (!appendMode && contentLength > 0) {
                        contentLength
                    } else {
                        entity.totalBytes
                    }

                    val raf = RandomAccessFile(destFile, "rw")
                    try {
                        if (appendMode) {
                            raf.seek(existingBytes)
                        } else {
                            raf.setLength(0L)
                            existingBytes = 0L
                        }

                        val buffer = ByteArray(16 * 1024)
                        var downloaded = existingBytes
                        var lastUiUpdate = 0L

                        body.byteStream().use { input ->
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                raf.write(buffer, 0, read)
                                downloaded += read

                                val now = System.currentTimeMillis()
                                if (now - lastUiUpdate >= 350L) {
                                    lastUiUpdate = now
                                    downloadDao.update(
                                        entity.copy(
                                            status = DownloadStatus.DOWNLOADING.name,
                                            downloadedBytes = downloaded,
                                            totalBytes = totalBytes
                                        )
                                    )
                                    if (showNotifications) {
                                        postProgressNotification(id.toInt(), entity.fileName, downloaded, totalBytes)
                                    }
                                }
                            }
                        }

                        val finalTotal = if (totalBytes > 0) totalBytes else downloaded
                        downloadDao.update(
                            entity.copy(
                                status = DownloadStatus.COMPLETED.name,
                                downloadedBytes = downloaded,
                                totalBytes = finalTotal,
                                errorMessage = null
                            )
                        )
                        exportToPublicDownloads(destFile, entity.fileName, entity.mimeType)
                        if (showNotifications) {
                            postCompleteNotification(id.toInt(), entity.fileName)
                        }
                    } finally {
                        raf.close()
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                val current = downloadDao.getById(id)
                if (current != null && current.status != DownloadStatus.PAUSED.name && current.status != DownloadStatus.CANCELLED.name) {
                    downloadDao.update(
                        current.copy(
                            status = DownloadStatus.FAILED.name,
                            errorMessage = e.message ?: "Download failed"
                        )
                    )
                    if (showNotifications) {
                        postFailedNotification(id.toInt(), current.fileName)
                    }
                }
            } finally {
                activeJobs.remove(id)
            }
        }
        activeJobs[id] = job
    }

    private fun exportToPublicDownloads(sourceFile: File, fileName: String, mimeType: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || !sourceFile.exists()) return
        runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Crumia")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
            resolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
    }

    fun openDownloadedFile(entity: DownloadEntity): Intent? {
        val file = File(entity.filePath)
        if (!file.exists()) return null
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, entity.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun shareDownloadedFile(entity: DownloadEntity): Intent? {
        val file = File(entity.filePath)
        if (!file.exists()) return null
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = entity.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun canPostNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun postProgressNotification(notifId: Int, fileName: String, downloaded: Long, total: Long) {
        if (!canPostNotifications()) return
        val progress = if (total > 0L) ((downloaded * 100L) / total).toInt().coerceIn(0, 100) else 0
        val indeterminate = total <= 0L
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.crumia_logo)
            .setContentTitle(fileName)
            .setContentText(if (indeterminate) formatBytes(downloaded) else "$progress% • ${formatBytes(downloaded)} / ${formatBytes(total)}")
            .setProgress(100, progress, indeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        notificationManager.notify(notifId, notif)
    }

    private fun postCompleteNotification(notifId: Int, fileName: String) {
        if (!canPostNotifications()) return
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.crumia_logo)
            .setContentTitle("Download complete")
            .setContentText(fileName)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notifId, notif)
    }

    private fun postFailedNotification(notifId: Int, fileName: String) {
        if (!canPostNotifications()) return
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.crumia_logo)
            .setContentTitle("Download failed")
            .setContentText(fileName)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notifId, notif)
    }

    private fun getDownloadsDir(): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: File(context.filesDir, "Download")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(100).ifBlank { "download_${System.currentTimeMillis()}" }
    }

    private fun ensureUniqueFilename(fileName: String): String {
        val dir = getDownloadsDir()
        var candidate = File(dir, fileName)
        if (!candidate.exists()) return fileName
        val dot = fileName.lastIndexOf('.')
        val base = if (dot > 0) fileName.substring(0, dot) else fileName
        val ext = if (dot > 0) fileName.substring(dot) else ""
        var counter = 1
        while (candidate.exists()) {
            val nextName = "${base}_($counter)$ext"
            candidate = File(dir, nextName)
            counter++
        }
        return candidate.name
    }

    private fun guessMimeType(url: String): String {
        val ext = MimeTypeMap.getFileExtensionFromUrl(url)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase()) ?: "application/octet-stream"
    }

    companion object {
        private const val CHANNEL_ID = "crumia_downloads_channel"

        fun formatBytes(bytes: Long): String {
            if (bytes < 0L) return "Unknown size"
            if (bytes < 1024L) return "$bytes B"
            val kb = bytes / 1024.0
            if (kb < 1024.0) return String.format("%.1f KB", kb)
            val mb = kb / 1024.0
            if (mb < 1024.0) return String.format("%.1f MB", mb)
            val gb = mb / 1024.0
            return String.format("%.2f GB", gb)
        }
    }
}
