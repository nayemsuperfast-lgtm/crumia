package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.db.DownloadEntity
import com.example.data.model.DownloadStatus
import com.example.downloads.CrumiaDownloadManager
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.ShieldEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    downloads: List<DownloadEntity>,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onCancel: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onOpen: (DownloadEntity) -> Unit,
    onShare: (DownloadEntity) -> Unit,
    onDelete: (DownloadEntity) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("downloads_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No downloads yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(downloads, key = { it.id }) { item ->
                    val status = runCatching { DownloadStatus.valueOf(item.status) }.getOrDefault(DownloadStatus.FAILED)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.fileName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val sizeLabel = if (item.totalBytes > 0) {
                                        "${CrumiaDownloadManager.formatBytes(item.downloadedBytes)} / ${CrumiaDownloadManager.formatBytes(item.totalBytes)}"
                                    } else {
                                        CrumiaDownloadManager.formatBytes(item.downloadedBytes)
                                    }
                                    Text(
                                        text = "${status.name.lowercase().replaceFirstChar { it.uppercase() }} • $sizeLabel",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = when (status) {
                                            DownloadStatus.COMPLETED -> ShieldEmerald
                                            DownloadStatus.FAILED -> DangerCrimson
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    if (!item.errorMessage.isNullOrBlank() && status == DownloadStatus.FAILED) {
                                        Text(
                                            text = item.errorMessage,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DangerCrimson
                                        )
                                    }
                                }

                                when (status) {
                                    DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> {
                                        IconButton(onClick = { onPause(item.id) }) {
                                            Icon(Icons.Rounded.Pause, contentDescription = "Pause download")
                                        }
                                        IconButton(onClick = { onCancel(item.id) }) {
                                            Icon(Icons.Rounded.Cancel, contentDescription = "Cancel download")
                                        }
                                    }
                                    DownloadStatus.PAUSED -> {
                                        IconButton(onClick = { onResume(item.id) }) {
                                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Resume download")
                                        }
                                        IconButton(onClick = { onCancel(item.id) }) {
                                            Icon(Icons.Rounded.Cancel, contentDescription = "Cancel download")
                                        }
                                    }
                                    DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                                        IconButton(onClick = { onRetry(item.id) }) {
                                            Icon(Icons.Rounded.Refresh, contentDescription = "Retry download")
                                        }
                                        IconButton(onClick = { onDelete(item) }) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Delete record")
                                        }
                                    }
                                    DownloadStatus.COMPLETED -> {
                                        IconButton(onClick = { onOpen(item) }) {
                                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Open file")
                                        }
                                        IconButton(onClick = { onShare(item) }) {
                                            Icon(Icons.Rounded.Share, contentDescription = "Share file")
                                        }
                                        IconButton(onClick = { onDelete(item) }) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Delete file")
                                        }
                                    }
                                }
                            }

                            if (status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED) {
                                Spacer(modifier = Modifier.height(10.dp))
                                if (item.totalBytes > 0L) {
                                    val progress = (item.downloadedBytes.toFloat() / item.totalBytes.toFloat()).coerceIn(0f, 1f)
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
