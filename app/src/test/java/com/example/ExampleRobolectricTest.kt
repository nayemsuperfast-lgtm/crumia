package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.adshield.BlockDecision
import com.example.data.db.BookmarkEntity
import com.example.data.db.CrumiaDatabase
import com.example.data.db.HistoryEntity
import com.example.data.model.FilterCategory
import com.example.data.model.SearchEngineOption
import com.example.search.UrlUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name is strictly Crumia and package id is com_crumia_browser`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Crumia", context.getString(R.string.app_name))
        assertEquals("com.crumia.browser", context.packageName)
    }

    @Test
    fun `url utils resolves urls, localhost, and search queries accurately`() {
        assertEquals(
            "https://example.com",
            UrlUtils.resolveInput("example.com", SearchEngineOption.GOOGLE, "", httpsUpgrade = true)
        )
        assertEquals(
            "https://example.com",
            UrlUtils.resolveInput("http://example.com", SearchEngineOption.GOOGLE, "", httpsUpgrade = true)
        )
        assertEquals(
            "http://192.168.1.1",
            UrlUtils.resolveInput("http://192.168.1.1", SearchEngineOption.GOOGLE, "", httpsUpgrade = true)
        )
        assertEquals(
            "https://duckduckgo.com/?q=kotlin+compose",
            UrlUtils.resolveInput("kotlin compose", SearchEngineOption.DUCKDUCKGO, "", httpsUpgrade = true)
        )
        assertEquals(
            "https://kagi.com/search?q=privacy",
            UrlUtils.resolveInput("privacy", SearchEngineOption.CUSTOM, "https://kagi.com/search?q={query}")
        )
    }

    @Test
    fun `ad shield blocks known third-party ads and trackers and respects allowlist`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, CrumiaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val prefs = com.example.data.prefs.PreferencesRepository(context)
            val engine = com.example.adshield.AdShieldEngine(
                db.filterRuleDao(),
                db.siteSettingDao(),
                prefs
            )

            val adDecision = engine.evaluateRequest(
                requestUrl = "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js",
                pageUrl = "https://news.ycombinator.com",
                isMainFrame = false
            )
            assertTrue(adDecision is BlockDecision.Block && adDecision.category == FilterCategory.AD)

            val trackerDecision = engine.evaluateRequest(
                requestUrl = "https://www.google-analytics.com/analytics.js",
                pageUrl = "https://wikipedia.org",
                isMainFrame = false
            )
            assertTrue(trackerDecision is BlockDecision.Block && trackerDecision.category == FilterCategory.TRACKER)

            val normalDecision = engine.evaluateRequest(
                requestUrl = "https://en.wikipedia.org/wiki/Kotlin",
                pageUrl = "https://en.wikipedia.org",
                isMainFrame = false
            )
            assertTrue(normalDecision is BlockDecision.Allow)

            engine.setSiteAllowlisted("news.ycombinator.com", true)
            val allowlistedDecision = engine.evaluateRequest(
                requestUrl = "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js",
                pageUrl = "https://news.ycombinator.com",
                isMainFrame = false
            )
            assertTrue(allowlistedDecision is BlockDecision.Allow)
        } finally {
            db.close()
        }
    }

    @Test
    fun `room database persists bookmarks and history locally`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, CrumiaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            db.bookmarkDao().insert(BookmarkEntity(title = "Kotlin", url = "https://kotlinlang.org", folder = "Dev"))
            val bookmarks = db.bookmarkDao().observeBookmarks().first()
            assertEquals(1, bookmarks.size)
            assertEquals("Kotlin", bookmarks.first().title)

            db.historyDao().insert(HistoryEntity(title = "MDN", url = "https://developer.mozilla.org"))
            val history = db.historyDao().observeHistory().first()
            assertEquals(1, history.size)
        } finally {
            db.close()
        }
    }
}
