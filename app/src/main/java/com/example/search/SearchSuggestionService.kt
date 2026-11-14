package com.example.search

import com.example.data.db.BookmarkEntity
import com.example.data.db.HistoryDao
import com.example.data.model.SearchEngineOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class OmniboxSuggestion(
    val text: String,
    val subtitle: String? = null,
    val url: String? = null,
    val type: SuggestionType
)

enum class SuggestionType {
    BOOKMARK,
    HISTORY,
    AUTOCOMPLETE,
    DIRECT_URL
}

class SearchSuggestionService(
    private val historyDao: HistoryDao
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()

    suspend fun fetchSuggestions(
        query: String,
        searchEngine: SearchEngineOption,
        bookmarks: List<BookmarkEntity>,
        allowRemoteSuggestions: Boolean
    ): List<OmniboxSuggestion> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        val results = mutableListOf<OmniboxSuggestion>()
        val seenUrls = HashSet<String>()

        // 1. Direct URL hint if input looks like a domain
        if (UrlUtils.looksLikeUrl(trimmed) && !trimmed.contains("://")) {
            results.add(
                OmniboxSuggestion(
                    text = "https://$trimmed",
                    subtitle = "Go to website",
                    url = "https://$trimmed",
                    type = SuggestionType.DIRECT_URL
                )
            )
            seenUrls.add("https://$trimmed")
        }

        // 2. Local Bookmark matches
        val lower = trimmed.lowercase()
        bookmarks.filter {
            it.title.lowercase().contains(lower) || it.url.lowercase().contains(lower)
        }.take(3).forEach { b ->
            if (seenUrls.add(b.url)) {
                results.add(
                    OmniboxSuggestion(
                        text = b.title,
                        subtitle = b.url,
                        url = b.url,
                        type = SuggestionType.BOOKMARK
                    )
                )
            }
        }

        // 3. Local History matches
        val historyMatches = runCatching { historyDao.searchHistorySuggestions(trimmed) }.getOrDefault(emptyList())
        historyMatches.take(4).forEach { h ->
            if (seenUrls.add(h.url)) {
                results.add(
                    OmniboxSuggestion(
                        text = h.title,
                        subtitle = h.url,
                        url = h.url,
                        type = SuggestionType.HISTORY
                    )
                )
            }
        }

        // 4. Remote search engine autocomplete (fast timeout, never blocks UI)
        if (allowRemoteSuggestions && trimmed.length >= 2) {
            val remote = fetchRemoteAutocomplete(trimmed, searchEngine)
            remote.take(5).forEach { suggestion ->
                results.add(
                    OmniboxSuggestion(
                        text = suggestion,
                        subtitle = "Search with ${searchEngine.displayName}",
                        url = null,
                        type = SuggestionType.AUTOCOMPLETE
                    )
                )
            }
        }

        results.take(10)
    }

    private fun fetchRemoteAutocomplete(query: String, engine: SearchEngineOption): List<String> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val endpoint = when (engine) {
            SearchEngineOption.DUCKDUCKGO -> "https://duckduckgo.com/ac/?q=$encoded&type=list"
            else -> "https://suggestqueries.google.com/complete/search?client=firefox&q=$encoded"
        }
        return runCatching {
            val req = Request.Builder()
                .url(endpoint)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val body = resp.body?.string() ?: return emptyList()
                val root = JSONArray(body)
                if (root.length() < 2) return emptyList()
                val arr = root.getJSONArray(1)
                List(arr.length()) { idx -> arr.getString(idx) }
            }
        }.getOrDefault(emptyList())
    }
}
