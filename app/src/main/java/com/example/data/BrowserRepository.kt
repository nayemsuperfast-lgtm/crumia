package com.example.data

import android.content.Context
import android.net.Uri
import com.example.adshield.AdShieldEngine
import com.example.data.db.BookmarkEntity
import com.example.data.db.ClosedTabEntity
import com.example.data.db.CrumiaDatabase
import com.example.data.db.DownloadEntity
import com.example.data.db.FilterRuleEntity
import com.example.data.db.HistoryEntity
import com.example.data.db.SavedTabEntity
import com.example.data.db.ShortcutEntity
import com.example.data.db.SiteSettingEntity
import com.example.data.model.HistoryRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class BrowserRepository(
    private val context: Context,
    private val db: CrumiaDatabase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val bookmarks: Flow<List<BookmarkEntity>> = db.bookmarkDao().observeBookmarks()
    val readingList: Flow<List<BookmarkEntity>> = db.bookmarkDao().observeReadingList()
    val history: Flow<List<HistoryEntity>> = db.historyDao().observeHistory()
    val recentUniqueHistory: Flow<List<HistoryEntity>> = db.historyDao().observeRecentUnique()
    val downloads: Flow<List<DownloadEntity>> = db.downloadDao().observeDownloads()
    val shortcuts: Flow<List<ShortcutEntity>> = db.shortcutDao().observeShortcuts()
    val siteSettings: Flow<List<SiteSettingEntity>> = db.siteSettingDao().observeAll()
    val allowlistedSites: Flow<List<SiteSettingEntity>> = db.siteSettingDao().observeAllowlisted()
    val filterRules: Flow<List<FilterRuleEntity>> = db.filterRuleDao().observeAll()
    val closedTabs: Flow<List<ClosedTabEntity>> = db.closedTabDao().observeClosedTabs()

    init {
        scope.launch {
            seedDefaultShortcutsIfEmpty()
        }
    }

    private suspend fun seedDefaultShortcutsIfEmpty() {
        if (db.shortcutDao().count() == 0) {
            db.shortcutDao().insertAll(
                listOf(
                    ShortcutEntity(title = "Google", url = "https://www.google.com", sortOrder = 0, accentHex = 0xFF3B82F6),
                    ShortcutEntity(title = "Wikipedia", url = "https://www.wikipedia.org", sortOrder = 1, accentHex = 0xFF6366F1),
                    ShortcutEntity(title = "GitHub", url = "https://github.com", sortOrder = 2, accentHex = 0xFF8B5CF6),
                    ShortcutEntity(title = "YouTube", url = "https://m.youtube.com", sortOrder = 3, accentHex = 0xFFEC4899),
                    ShortcutEntity(title = "DuckDuckGo", url = "https://duckduckgo.com", sortOrder = 4, accentHex = 0xFF38BDF8),
                    ShortcutEntity(title = "Reddit", url = "https://www.reddit.com", sortOrder = 5, accentHex = 0xFFF97316),
                    ShortcutEntity(title = "Hacker News", url = "https://news.ycombinator.com", sortOrder = 6, accentHex = 0xFF10B981),
                    ShortcutEntity(title = "MDN Web Docs", url = "https://developer.mozilla.org", sortOrder = 7, accentHex = 0xFF0EA5E9)
                )
            )
        }
    }

    // Bookmarks & Reading List
    fun isBookmarked(url: String): Flow<Boolean> = db.bookmarkDao().isBookmarked(url)

    suspend fun toggleBookmark(title: String, url: String, folder: String = "General"): Boolean {
        val existing = db.bookmarkDao().findByUrl(url)
        return if (existing != null) {
            db.bookmarkDao().deleteById(existing.id)
            false
        } else {
            db.bookmarkDao().insert(
                BookmarkEntity(
                    title = title.ifBlank { url },
                    url = url,
                    folder = folder,
                    isReadingList = false
                )
            )
            true
        }
    }

    suspend fun saveBookmark(bookmark: BookmarkEntity) {
        if (bookmark.id == 0L) {
            db.bookmarkDao().insert(bookmark)
        } else {
            db.bookmarkDao().update(bookmark)
        }
    }

    suspend fun addToReadingList(title: String, url: String, offlineArchivePath: String? = null) {
        db.bookmarkDao().insert(
            BookmarkEntity(
                title = title.ifBlank { url },
                url = url,
                folder = "Reading List",
                isReadingList = true,
                offlineArchivePath = offlineArchivePath
            )
        )
    }

    suspend fun deleteBookmark(id: Long, offlineArchivePath: String? = null) {
        if (!offlineArchivePath.isNullOrBlank()) {
            runCatching { File(offlineArchivePath).delete() }
        }
        db.bookmarkDao().deleteById(id)
    }

    suspend fun exportBookmarksHtml(targetUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val all = db.bookmarkDao().getAllBookmarksOnce()
            val sb = StringBuilder()
            sb.appendLine("<!DOCTYPE NETSCAPE-Bookmark-file-1>")
            sb.appendLine("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">")
            sb.appendLine("<TITLE>Crumia Bookmarks</TITLE>")
            sb.appendLine("<H1>Crumia Bookmarks</H1>")
            sb.appendLine("<DL><p>")
            val byFolder = all.groupBy { it.folder.ifBlank { "General" } }
            for ((folder, items) in byFolder) {
                sb.appendLine("    <DT><H3>${escapeHtml(folder)}</H3>")
                sb.appendLine("    <DL><p>")
                for (b in items) {
                    val addDate = b.createdAt / 1000L
                    sb.appendLine("        <DT><A HREF=\"${escapeHtml(b.url)}\" ADD_DATE=\"$addDate\">${escapeHtml(b.title)}</A>")
                }
                sb.appendLine("    </DL><p>")
            }
            sb.appendLine("</DL><p>")
            context.contentResolver.openOutputStream(targetUri)?.bufferedWriter()?.use { writer ->
                writer.write(sb.toString())
            } ?: error("Unable to open destination file")
            all.size
        }
    }

    suspend fun importBookmarksHtml(sourceUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val content = context.contentResolver.openInputStream(sourceUri)?.bufferedReader()?.use { it.readText() }
                ?: error("Unable to read bookmark file")
            val linkRegex = Regex("""<A[^>]+HREF=["']([^"']+)["'][^>]*>(.*?)</A>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            var imported = 0
            for (match in linkRegex.findAll(content)) {
                val url = match.groupValues[1].trim()
                val title = match.groupValues[2].replace(Regex("<[^>]+>"), "").trim().ifBlank { url }
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    if (db.bookmarkDao().findByUrl(url) == null) {
                        db.bookmarkDao().insert(
                            BookmarkEntity(
                                title = title,
                                url = url,
                                folder = "Imported",
                                isReadingList = false
                            )
                        )
                        imported++
                    }
                }
            }
            imported
        }
    }

    // History (never called for Incognito!)
    suspend fun recordVisit(title: String, url: String) {
        if (url.isBlank() || url == "about:blank" || url.startsWith("file://")) return
        db.historyDao().insert(
            HistoryEntity(
                title = title.ifBlank { url },
                url = url,
                visitedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteHistoryItem(id: Long) = db.historyDao().deleteById(id)

    suspend fun clearHistory(range: HistoryRange) {
        val back = range.millisBack
        if (back == null) {
            db.historyDao().clearAll()
        } else {
            db.historyDao().deleteSince(System.currentTimeMillis() - back)
        }
    }

    // Shortcuts
    suspend fun addShortcut(title: String, url: String, accentHex: Long = 0xFF6366F1) {
        val normalizedUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        val currentCount = db.shortcutDao().count()
        db.shortcutDao().insert(
            ShortcutEntity(
                title = title.ifBlank { normalizedUrl },
                url = normalizedUrl,
                sortOrder = currentCount,
                accentHex = accentHex
            )
        )
    }

    suspend fun updateShortcut(shortcut: ShortcutEntity) = db.shortcutDao().update(shortcut)
    suspend fun deleteShortcut(id: Long) = db.shortcutDao().deleteById(id)

    // Closed tabs
    suspend fun recordClosedTab(title: String, url: String) {
        if (url.isBlank() || url == "about:blank") return
        db.closedTabDao().insert(ClosedTabEntity(title = title.ifBlank { url }, url = url))
    }

    suspend fun popClosedTab(): ClosedTabEntity? {
        val latest = db.closedTabDao().getMostRecent()
        if (latest != null) db.closedTabDao().deleteById(latest.id)
        return latest
    }

    suspend fun deleteClosedTab(id: Long) = db.closedTabDao().deleteById(id)
    suspend fun clearClosedTabs() = db.closedTabDao().clearAll()

    // Session persistence (non-incognito only)
    suspend fun saveSessionTabs(tabs: List<SavedTabEntity>) {
        db.savedTabDao().clearAll()
        if (tabs.isNotEmpty()) {
            db.savedTabDao().insertAll(tabs)
        }
    }

    suspend fun getSavedSessionTabs(): List<SavedTabEntity> = db.savedTabDao().getAllOnce()

    // Site settings
    suspend fun getSiteSetting(urlOrHost: String): SiteSettingEntity? {
        val host = AdShieldEngine.normalizeHost(
            if (urlOrHost.contains("://")) AdShieldEngine.safeHost(urlOrHost) else urlOrHost
        ) ?: return null
        return db.siteSettingDao().getByHost(host)
    }

    suspend fun upsertSiteSetting(setting: SiteSettingEntity) {
        db.siteSettingDao().upsert(setting.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun resetAllSiteSettings() = db.siteSettingDao().clearAll()

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
