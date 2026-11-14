package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.HomepageBackgroundStyle
import com.example.data.model.SearchEngineOption
import com.example.data.model.StartupBehavior
import com.example.data.model.ThemeMode
import com.example.data.model.ToolbarPosition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.crumiaDataStore: DataStore<Preferences> by preferencesDataStore(name = "crumia_settings")

data class CrumiaPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val searchEngine: SearchEngineOption = SearchEngineOption.GOOGLE,
    val customSearchUrl: String = "https://www.google.com/search?q={query}",
    val startupBehavior: StartupBehavior = StartupBehavior.HOMEPAGE,
    val customHomepageUrl: String = "https://www.wikipedia.org",
    val homepageBackground: HomepageBackgroundStyle = HomepageBackgroundStyle.CHROME_WARM,
    val showShortcutsOnHome: Boolean = true,
    val showRecentSitesOnHome: Boolean = true,
    val showBookmarksOnHome: Boolean = true,
    val toolbarPosition: ToolbarPosition = ToolbarPosition.TOP,
    val textZoomPercent: Int = 100,
    val adShieldEnabled: Boolean = true,
    val blockAds: Boolean = true,
    val blockTrackers: Boolean = true,
    val blockPopups: Boolean = true,
    val easyListEnabled: Boolean = true,
    val easyPrivacyEnabled: Boolean = true,
    val adGuardEnabled: Boolean = true,
    val lastFilterUpdateMillis: Long = 0L,
    val totalAdsBlocked: Long = 0L,
    val totalTrackersBlocked: Long = 0L,
    val javascriptEnabled: Boolean = true,
    val safeBrowsingEnabled: Boolean = true,
    val cookiesEnabled: Boolean = true,
    val thirdPartyCookiesEnabled: Boolean = false,
    val doNotTrackEnabled: Boolean = true,
    val httpsUpgradeEnabled: Boolean = true,
    val desktopModeDefault: Boolean = false,
    val downloadNotificationsEnabled: Boolean = true,
    val confirmBeforeClearingIncognito: Boolean = false
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val CUSTOM_SEARCH_URL = stringPreferencesKey("custom_search_url")
        val STARTUP_BEHAVIOR = stringPreferencesKey("startup_behavior")
        val CUSTOM_HOMEPAGE_URL = stringPreferencesKey("custom_homepage_url")
        val HOMEPAGE_BACKGROUND = stringPreferencesKey("homepage_background")
        val SHOW_SHORTCUTS = booleanPreferencesKey("show_shortcuts")
        val SHOW_RECENT = booleanPreferencesKey("show_recent")
        val SHOW_BOOKMARKS = booleanPreferencesKey("show_bookmarks")
        val TOOLBAR_POSITION = stringPreferencesKey("toolbar_position")
        val TEXT_ZOOM = androidx.datastore.preferences.core.intPreferencesKey("text_zoom_percent")
        val AD_SHIELD_ENABLED = booleanPreferencesKey("ad_shield_enabled")
        val BLOCK_ADS = booleanPreferencesKey("block_ads")
        val BLOCK_TRACKERS = booleanPreferencesKey("block_trackers")
        val BLOCK_POPUPS = booleanPreferencesKey("block_popups")
        val EASYLIST_ENABLED = booleanPreferencesKey("easylist_enabled")
        val EASYPRIVACY_ENABLED = booleanPreferencesKey("easyprivacy_enabled")
        val ADGUARD_ENABLED = booleanPreferencesKey("adguard_enabled")
        val LAST_FILTER_UPDATE = longPreferencesKey("last_filter_update")
        val TOTAL_ADS_BLOCKED = longPreferencesKey("total_ads_blocked")
        val TOTAL_TRACKERS_BLOCKED = longPreferencesKey("total_trackers_blocked")
        val JAVASCRIPT_ENABLED = booleanPreferencesKey("javascript_enabled")
        val SAFE_BROWSING_ENABLED = booleanPreferencesKey("safe_browsing_enabled")
        val COOKIES_ENABLED = booleanPreferencesKey("cookies_enabled")
        val THIRD_PARTY_COOKIES = booleanPreferencesKey("third_party_cookies")
        val DO_NOT_TRACK = booleanPreferencesKey("do_not_track")
        val HTTPS_UPGRADE = booleanPreferencesKey("https_upgrade")
        val DESKTOP_MODE_DEFAULT = booleanPreferencesKey("desktop_mode_default")
        val DOWNLOAD_NOTIFICATIONS = booleanPreferencesKey("download_notifications")
    }

    val preferencesFlow: Flow<CrumiaPreferences> = context.crumiaDataStore.data.map { prefs ->
        CrumiaPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            searchEngine = prefs[Keys.SEARCH_ENGINE]?.let { runCatching { SearchEngineOption.valueOf(it) }.getOrNull() }
                ?: SearchEngineOption.GOOGLE,
            customSearchUrl = prefs[Keys.CUSTOM_SEARCH_URL] ?: "https://www.google.com/search?q={query}",
            startupBehavior = prefs[Keys.STARTUP_BEHAVIOR]?.let { runCatching { StartupBehavior.valueOf(it) }.getOrNull() }
                ?: StartupBehavior.HOMEPAGE,
            customHomepageUrl = prefs[Keys.CUSTOM_HOMEPAGE_URL] ?: "https://www.wikipedia.org",
            homepageBackground = prefs[Keys.HOMEPAGE_BACKGROUND]?.let { runCatching { HomepageBackgroundStyle.valueOf(it) }.getOrNull() }
                ?: HomepageBackgroundStyle.CHROME_WARM,
            showShortcutsOnHome = prefs[Keys.SHOW_SHORTCUTS] ?: true,
            showRecentSitesOnHome = prefs[Keys.SHOW_RECENT] ?: true,
            showBookmarksOnHome = prefs[Keys.SHOW_BOOKMARKS] ?: true,
            toolbarPosition = prefs[Keys.TOOLBAR_POSITION]?.let { runCatching { ToolbarPosition.valueOf(it) }.getOrNull() }
                ?: ToolbarPosition.TOP,
            textZoomPercent = prefs[Keys.TEXT_ZOOM] ?: 100,
            adShieldEnabled = prefs[Keys.AD_SHIELD_ENABLED] ?: true,
            blockAds = prefs[Keys.BLOCK_ADS] ?: true,
            blockTrackers = prefs[Keys.BLOCK_TRACKERS] ?: true,
            blockPopups = prefs[Keys.BLOCK_POPUPS] ?: true,
            easyListEnabled = prefs[Keys.EASYLIST_ENABLED] ?: true,
            easyPrivacyEnabled = prefs[Keys.EASYPRIVACY_ENABLED] ?: true,
            adGuardEnabled = prefs[Keys.ADGUARD_ENABLED] ?: true,
            lastFilterUpdateMillis = prefs[Keys.LAST_FILTER_UPDATE] ?: 0L,
            totalAdsBlocked = prefs[Keys.TOTAL_ADS_BLOCKED] ?: 0L,
            totalTrackersBlocked = prefs[Keys.TOTAL_TRACKERS_BLOCKED] ?: 0L,
            javascriptEnabled = prefs[Keys.JAVASCRIPT_ENABLED] ?: true,
            safeBrowsingEnabled = prefs[Keys.SAFE_BROWSING_ENABLED] ?: true,
            cookiesEnabled = prefs[Keys.COOKIES_ENABLED] ?: true,
            thirdPartyCookiesEnabled = prefs[Keys.THIRD_PARTY_COOKIES] ?: false,
            doNotTrackEnabled = prefs[Keys.DO_NOT_TRACK] ?: true,
            httpsUpgradeEnabled = prefs[Keys.HTTPS_UPGRADE] ?: true,
            desktopModeDefault = prefs[Keys.DESKTOP_MODE_DEFAULT] ?: false,
            downloadNotificationsEnabled = prefs[Keys.DOWNLOAD_NOTIFICATIONS] ?: true
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = update { it[Keys.THEME_MODE] = mode.name }
    suspend fun setSearchEngine(engine: SearchEngineOption) = update { it[Keys.SEARCH_ENGINE] = engine.name }
    suspend fun setCustomSearchUrl(url: String) = update { it[Keys.CUSTOM_SEARCH_URL] = url }
    suspend fun setStartupBehavior(behavior: StartupBehavior) = update { it[Keys.STARTUP_BEHAVIOR] = behavior.name }
    suspend fun setCustomHomepageUrl(url: String) = update { it[Keys.CUSTOM_HOMEPAGE_URL] = url }
    suspend fun setHomepageBackground(style: HomepageBackgroundStyle) = update { it[Keys.HOMEPAGE_BACKGROUND] = style.name }
    suspend fun setShowShortcuts(show: Boolean) = update { it[Keys.SHOW_SHORTCUTS] = show }
    suspend fun setShowRecentSites(show: Boolean) = update { it[Keys.SHOW_RECENT] = show }
    suspend fun setShowBookmarks(show: Boolean) = update { it[Keys.SHOW_BOOKMARKS] = show }
    suspend fun setToolbarPosition(pos: ToolbarPosition) = update { it[Keys.TOOLBAR_POSITION] = pos.name }
    suspend fun setTextZoomPercent(percent: Int) = update { it[Keys.TEXT_ZOOM] = percent.coerceIn(75, 160) }

    suspend fun setAdShieldEnabled(enabled: Boolean) = update { it[Keys.AD_SHIELD_ENABLED] = enabled }
    suspend fun setBlockAds(enabled: Boolean) = update { it[Keys.BLOCK_ADS] = enabled }
    suspend fun setBlockTrackers(enabled: Boolean) = update { it[Keys.BLOCK_TRACKERS] = enabled }
    suspend fun setBlockPopups(enabled: Boolean) = update { it[Keys.BLOCK_POPUPS] = enabled }
    suspend fun setFilterListEnabled(easyList: Boolean? = null, easyPrivacy: Boolean? = null, adGuard: Boolean? = null) = update {
        easyList?.let { v -> it[Keys.EASYLIST_ENABLED] = v }
        easyPrivacy?.let { v -> it[Keys.EASYPRIVACY_ENABLED] = v }
        adGuard?.let { v -> it[Keys.ADGUARD_ENABLED] = v }
    }
    suspend fun setLastFilterUpdate(millis: Long) = update { it[Keys.LAST_FILTER_UPDATE] = millis }

    suspend fun incrementBlockedCounters(adsDelta: Long, trackersDelta: Long) {
        if (adsDelta <= 0L && trackersDelta <= 0L) return
        context.crumiaDataStore.edit { prefs ->
            if (adsDelta > 0L) {
                prefs[Keys.TOTAL_ADS_BLOCKED] = (prefs[Keys.TOTAL_ADS_BLOCKED] ?: 0L) + adsDelta
            }
            if (trackersDelta > 0L) {
                prefs[Keys.TOTAL_TRACKERS_BLOCKED] = (prefs[Keys.TOTAL_TRACKERS_BLOCKED] ?: 0L) + trackersDelta
            }
        }
    }

    suspend fun resetAdShieldSettings() {
        context.crumiaDataStore.edit { prefs ->
            prefs[Keys.AD_SHIELD_ENABLED] = true
            prefs[Keys.BLOCK_ADS] = true
            prefs[Keys.BLOCK_TRACKERS] = true
            prefs[Keys.BLOCK_POPUPS] = true
            prefs[Keys.EASYLIST_ENABLED] = true
            prefs[Keys.EASYPRIVACY_ENABLED] = true
            prefs[Keys.ADGUARD_ENABLED] = true
            prefs[Keys.TOTAL_ADS_BLOCKED] = 0L
            prefs[Keys.TOTAL_TRACKERS_BLOCKED] = 0L
        }
    }

    suspend fun setJavascriptEnabled(enabled: Boolean) = update { it[Keys.JAVASCRIPT_ENABLED] = enabled }
    suspend fun setSafeBrowsingEnabled(enabled: Boolean) = update { it[Keys.SAFE_BROWSING_ENABLED] = enabled }
    suspend fun setCookiesEnabled(enabled: Boolean) = update { it[Keys.COOKIES_ENABLED] = enabled }
    suspend fun setThirdPartyCookiesEnabled(enabled: Boolean) = update { it[Keys.THIRD_PARTY_COOKIES] = enabled }
    suspend fun setDoNotTrackEnabled(enabled: Boolean) = update { it[Keys.DO_NOT_TRACK] = enabled }
    suspend fun setHttpsUpgradeEnabled(enabled: Boolean) = update { it[Keys.HTTPS_UPGRADE] = enabled }
    suspend fun setDesktopModeDefault(enabled: Boolean) = update { it[Keys.DESKTOP_MODE_DEFAULT] = enabled }
    suspend fun setDownloadNotificationsEnabled(enabled: Boolean) = update { it[Keys.DOWNLOAD_NOTIFICATIONS] = enabled }

    suspend fun resetAllSettings() {
        context.crumiaDataStore.edit { it.clear() }
    }

    private suspend inline fun update(crossinline block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.crumiaDataStore.edit { block(it) }
    }
}
