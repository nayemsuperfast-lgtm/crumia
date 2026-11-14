package com.example.data.model

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    DARK("Dark Mode"),
    LIGHT("Light Mode")
}

enum class SearchEngineOption(
    val displayName: String,
    val searchUrlTemplate: String,
    val homeUrl: String
) {
    GOOGLE("Google", "https://www.google.com/search?q={query}", "https://www.google.com"),
    BING("Bing", "https://www.bing.com/search?q={query}", "https://www.bing.com"),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q={query}", "https://duckduckgo.com"),
    YAHOO("Yahoo", "https://search.yahoo.com/search?p={query}", "https://www.yahoo.com"),
    CUSTOM("Custom", "https://www.google.com/search?q={query}", "https://www.google.com")
}

enum class StartupBehavior(val label: String) {
    HOMEPAGE("Open Homepage"),
    RESTORE_TABS("Restore Last Session"),
    CUSTOM_PAGE("Open Custom Page")
}

enum class HomepageBackgroundStyle(val label: String) {
    CHROME_WARM("Chrome Warm"),
    CHROME_CLEAN("Chrome Clean"),
    COSMIC_GRADIENT("Cosmic Gradient"),
    AURORA_CYAN("Aurora Cyan"),
    VIOLET_NEBULA("Violet Nebula"),
    MIDNIGHT_OLED("Midnight OLED"),
    MINIMAL_CLEAN("Minimal Clean")
}

enum class ToolbarPosition(val label: String) {
    TOP("Top Toolbar"),
    BOTTOM("Bottom Toolbar")
}

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class FilterCategory {
    AD,
    TRACKER,
    POPUP
}

enum class FilterListSource(val displayName: String, val remoteUrl: String) {
    EASYLIST("EasyList", "https://easylist.to/easylist/easylist.txt"),
    EASYPRIVACY("EasyPrivacy", "https://easylist.to/easylist/easyprivacy.txt"),
    ADGUARD("AdGuard Base & Mobile", "https://filters.adtidy.org/android/filters/2_optimized.txt"),
    CUSTOM("Custom Rules", "")
}

enum class HistoryRange(val label: String, val millisBack: Long?) {
    LAST_HOUR("Last hour", 60 * 60 * 1000L),
    LAST_24_HOURS("Last 24 hours", 24 * 60 * 60 * 1000L),
    LAST_7_DAYS("Last 7 days", 7 * 24 * 60 * 60 * 1000L),
    ALL_TIME("All time", null)
}
