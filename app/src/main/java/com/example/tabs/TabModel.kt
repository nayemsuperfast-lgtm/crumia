package com.example.tabs

import android.graphics.Bitmap
import java.util.UUID

sealed interface BrowserError {
    val url: String

    data class NoInternet(override val url: String) : BrowserError
    data class WebsiteUnavailable(override val url: String, val message: String) : BrowserError
    data class SslError(override val url: String, val reason: String) : BrowserError
    data class PageLoadError(override val url: String, val code: Int, val message: String) : BrowserError
}

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Tab",
    val url: String = "",
    val isIncognito: Boolean = false,
    val isHomepage: Boolean = true,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isSecure: Boolean = true,
    val isDesktopMode: Boolean = false,
    val errorState: BrowserError? = null,
    val blockedOnPage: Int = 0,
    val thumbnail: Bitmap? = null,
    val lastAccessedAt: Long = System.currentTimeMillis()
)

data class PendingSitePermission(
    val host: String,
    val origin: String,
    val needsCamera: Boolean = false,
    val needsMic: Boolean = false,
    val needsLocation: Boolean = false,
    val onDecision: (granted: Boolean) -> Unit
)

data class WebContextMenuRequest(
    val url: String,
    val isImage: Boolean = false
)
