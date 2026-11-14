package com.example.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.graphics.createBitmap
import com.example.adshield.AdShieldEngine
import com.example.adshield.BlockDecision
import com.example.data.prefs.CrumiaPreferences
import com.example.tabs.BrowserError
import com.example.tabs.PendingSitePermission
import com.example.tabs.WebContextMenuRequest
import java.io.File
import java.util.concurrent.ConcurrentHashMap

interface WebViewEventListener {
    fun onPageStarted(tabId: String, url: String)
    fun onPageFinished(tabId: String, url: String, title: String, canGoBack: Boolean, canGoForward: Boolean)
    fun onProgressChanged(tabId: String, progress: Int)
    fun onTitleReceived(tabId: String, title: String)
    fun onNavigationStateChanged(tabId: String, canGoBack: Boolean, canGoForward: Boolean)
    fun onBlockedRequestOnPage(tabId: String)
    fun onPopupBlocked(tabId: String, blockedUrl: String?)
    fun onError(tabId: String, error: BrowserError)
    fun onDownloadRequested(url: String, userAgent: String, contentDisposition: String, mimeType: String)
    fun onFileChooserRequested(callback: ValueCallback<Array<Uri>>, acceptTypes: Array<String>, allowMultiple: Boolean)
    fun onFullscreenShow(view: View, callback: WebChromeClient.CustomViewCallback)
    fun onFullscreenHide()
    fun onSitePermissionRequested(request: PendingSitePermission)
    fun onOpenNewTabRequested(url: String, isIncognito: Boolean)
    fun onContextMenuRequested(request: WebContextMenuRequest)
}

class WebViewController(
    private val context: Context,
    private val adShieldEngine: AdShieldEngine,
    private val listener: WebViewEventListener
) {
    private val webViews = ConcurrentHashMap<String, WebView>()
    private val currentUrls = ConcurrentHashMap<String, String>()
    private var defaultUserAgent: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(
        tabId: String,
        isIncognito: Boolean,
        isDesktopMode: Boolean,
        prefs: CrumiaPreferences
    ): WebView {
        webViews[tabId]?.let { existing ->
            applyPreferences(existing, isIncognito, isDesktopMode, prefs)
            return existing
        }

        val wv = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            isFocusable = true
            isFocusableInTouchMode = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS

            if (defaultUserAgent == null) {
                defaultUserAgent = settings.userAgentString
            }

            setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                listener.onDownloadRequested(url, userAgent, contentDisposition, mimeType)
            }

            setOnLongClickListener {
                val hit = hitTestResult
                when (hit.type) {
                    WebView.HitTestResult.SRC_ANCHOR_TYPE,
                    WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                        val extra = hit.extra
                        if (!extra.isNullOrBlank()) {
                            listener.onContextMenuRequested(
                                WebContextMenuRequest(
                                    url = extra,
                                    isImage = hit.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
                                )
                            )
                            true
                        } else false
                    }
                    WebView.HitTestResult.IMAGE_TYPE -> {
                        val extra = hit.extra
                        if (!extra.isNullOrBlank()) {
                            listener.onContextMenuRequested(WebContextMenuRequest(url = extra, isImage = true))
                            true
                        } else false
                    }
                    else -> false
                }
            }

            webViewClient = createWebViewClient(tabId, isIncognito)
            webChromeClient = createWebChromeClient(tabId, isIncognito)
        }

        applyPreferences(wv, isIncognito, isDesktopMode, prefs)
        webViews[tabId] = wv
        return wv
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun applyPreferences(
        webView: WebView,
        isIncognito: Boolean,
        isDesktopMode: Boolean,
        prefs: CrumiaPreferences
    ) {
        val s = webView.settings
        s.javaScriptEnabled = prefs.javascriptEnabled
        s.domStorageEnabled = true
        s.databaseEnabled = !isIncognito
        s.loadsImagesAutomatically = true
        s.useWideViewPort = true
        s.loadWithOverviewMode = true
        s.builtInZoomControls = true
        s.displayZoomControls = false
        s.mediaPlaybackRequiresUserGesture = false
        s.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        s.cacheMode = if (isIncognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
        s.setSupportMultipleWindows(true)
        s.javaScriptCanOpenWindowsAutomatically = false
        s.allowFileAccess = false
        s.allowContentAccess = true
        s.setGeolocationEnabled(!isIncognito)
        s.textZoom = prefs.textZoomPercent

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            s.safeBrowsingEnabled = prefs.safeBrowsingEnabled
        }

        val mobileUa = defaultUserAgent ?: s.userAgentString
        s.userAgentString = if (isDesktopMode) DESKTOP_USER_AGENT else mobileUa

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(if (isIncognito) true else prefs.cookiesEnabled)
        cookieManager.setAcceptThirdPartyCookies(
            webView,
            if (isIncognito) false else (prefs.cookiesEnabled && prefs.thirdPartyCookiesEnabled)
        )
    }

    private fun createWebViewClient(tabId: String, isIncognito: Boolean): WebViewClient {
        return object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val reqUrl = request?.url?.toString() ?: return null
                val pageUrl = currentUrls[tabId]
                val decision = adShieldEngine.evaluateRequest(
                    requestUrl = reqUrl,
                    pageUrl = pageUrl,
                    isMainFrame = request.isForMainFrame
                )
                return when (decision) {
                    is BlockDecision.Block -> {
                        listener.onBlockedRequestOnPage(tabId)
                        adShieldEngine.emptyBlockedResponse()
                    }
                    BlockDecision.Allow -> super.shouldInterceptRequest(view, request)
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false
                val scheme = uri.scheme?.lowercase() ?: return false
                if (scheme == "http" || scheme == "https" || scheme == "file" || scheme == "about") {
                    return false
                }
                // Handle tel:, mailto:, intent:, etc. safely
                runCatching {
                    val intent = if (scheme == "intent") {
                        Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                    } else {
                        Intent(Intent.ACTION_VIEW, uri)
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (!url.isNullOrBlank()) {
                    currentUrls[tabId] = url
                    listener.onPageStarted(tabId, url)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val finalUrl = url ?: currentUrls[tabId] ?: ""
                currentUrls[tabId] = finalUrl
                val title = view?.title?.takeIf { it.isNotBlank() } ?: finalUrl
                listener.onPageFinished(
                    tabId = tabId,
                    url = finalUrl,
                    title = title,
                    canGoBack = view?.canGoBack() == true,
                    canGoForward = view?.canGoForward() == true
                )
                adShieldEngine.buildCosmeticHideScript(finalUrl)?.let { js ->
                    view?.evaluateJavascript(js, null)
                }
                adShieldEngine.flushCountersAsync()
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                if (!url.isNullOrBlank()) {
                    currentUrls[tabId] = url
                }
                listener.onNavigationStateChanged(
                    tabId = tabId,
                    canGoBack = view?.canGoBack() == true,
                    canGoForward = view?.canGoForward() == true
                )
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    val failUrl = request.url?.toString().orEmpty()
                    val code = error?.errorCode ?: -1
                    val desc = error?.description?.toString().orEmpty()
                    val mapped = if (!isNetworkAvailable()) {
                        BrowserError.NoInternet(failUrl)
                    } else if (code == ERROR_HOST_LOOKUP || code == ERROR_CONNECT || code == ERROR_TIMEOUT) {
                        BrowserError.WebsiteUnavailable(failUrl, desc.ifBlank { "Unable to reach server." })
                    } else {
                        BrowserError.PageLoadError(failUrl, code, desc.ifBlank { "Page could not be loaded." })
                    }
                    listener.onError(tabId, mapped)
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                // Never silently ignore SSL errors
                handler?.cancel()
                val failUrl = error?.url ?: currentUrls[tabId].orEmpty()
                val reason = when (error?.primaryError) {
                    SslError.SSL_EXPIRED -> "The security certificate has expired."
                    SslError.SSL_IDMISMATCH -> "The certificate hostname does not match this site."
                    SslError.SSL_UNTRUSTED -> "The certificate authority is not trusted."
                    SslError.SSL_DATE_INVALID -> "The certificate date is invalid."
                    else -> "The site's SSL certificate failed validation."
                }
                listener.onError(tabId, BrowserError.SslError(failUrl, reason))
            }
        }
    }

    private fun createWebChromeClient(tabId: String, isIncognito: Boolean): WebChromeClient {
        return object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                listener.onProgressChanged(tabId, newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) {
                    listener.onTitleReceived(tabId, title)
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                if (filePathCallback == null) return false
                val accepts = fileChooserParams?.acceptTypes ?: emptyArray()
                val multiple = fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                listener.onFileChooserRequested(filePathCallback, accepts, multiple)
                return true
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (view != null && callback != null) {
                    listener.onFullscreenShow(view, callback)
                }
            }

            override fun onHideCustomView() {
                listener.onFullscreenHide()
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                if (request == null) return
                val resources = request.resources ?: emptyArray()
                val needsCamera = resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                val needsMic = resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                if (!needsCamera && !needsMic) {
                    request.deny()
                    return
                }
                val origin = request.origin?.toString().orEmpty()
                val host = AdShieldEngine.normalizeHost(request.origin?.host) ?: origin
                listener.onSitePermissionRequested(
                    PendingSitePermission(
                        host = host,
                        origin = origin,
                        needsCamera = needsCamera,
                        needsMic = needsMic,
                        onDecision = { granted ->
                            if (granted) request.grant(resources) else request.deny()
                        }
                    )
                )
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                if (origin == null || callback == null) return
                if (isIncognito) {
                    callback.invoke(origin, false, false)
                    return
                }
                val host = AdShieldEngine.normalizeHost(AdShieldEngine.safeHost(origin)) ?: origin
                listener.onSitePermissionRequested(
                    PendingSitePermission(
                        host = host,
                        origin = origin,
                        needsLocation = true,
                        onDecision = { granted ->
                            callback.invoke(origin, granted, false)
                        }
                    )
                )
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                val openerUrl = currentUrls[tabId]
                val hitUrl = view?.hitTestResult?.extra
                if (!isUserGesture && adShieldEngine.shouldBlockPopupUrl(hitUrl, openerUrl)) {
                    listener.onPopupBlocked(tabId, hitUrl)
                    return false
                }
                if (!hitUrl.isNullOrBlank()) {
                    listener.onOpenNewTabRequested(hitUrl, isIncognito)
                    return false
                }
                // Create a lightweight temporary WebView to capture target URL cleanly or block popup
                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                val tempWebView = WebView(context)
                tempWebView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(v: WebView?, req: WebResourceRequest?): Boolean {
                        val target = req?.url?.toString()
                        if (adShieldEngine.shouldBlockPopupUrl(target, openerUrl) && !isUserGesture) {
                            listener.onPopupBlocked(tabId, target)
                        } else if (!target.isNullOrBlank()) {
                            listener.onOpenNewTabRequested(target, isIncognito)
                        }
                        tempWebView.destroy()
                        return true
                    }
                }
                transport.webView = tempWebView
                resultMsg.sendToTarget()
                return true
            }
        }
    }

    fun getWebView(tabId: String): WebView? = webViews[tabId]

    fun pauseOtherTabs(activeTabId: String) {
        webViews.forEach { (id, wv) ->
            if (id == activeTabId) wv.onResume() else wv.onPause()
        }
    }

    fun captureThumbnail(tabId: String): Bitmap? {
        val wv = webViews[tabId] ?: return null
        if (wv.width <= 0 || wv.height <= 0) return null
        return runCatching {
            val targetW = 240
            val targetH = ((wv.height.toFloat() / wv.width.toFloat()) * targetW).toInt().coerceIn(160, 420)
            val bmp = createBitmap(targetW, targetH)
            val canvas = Canvas(bmp)
            val scale = targetW.toFloat() / wv.width.toFloat()
            canvas.scale(scale, scale)
            wv.draw(canvas)
            bmp
        }.getOrNull()
    }

    fun destroyTab(tabId: String) {
        currentUrls.remove(tabId)
        val wv = webViews.remove(tabId) ?: return
        (wv.parent as? ViewGroup)?.removeView(wv)
        wv.stopLoading()
        wv.loadUrl("about:blank")
        wv.clearHistory()
        wv.removeAllViews()
        wv.destroy()
    }

    fun clearIncognitoData() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
    }

    fun clearAllCacheAndCookies(clearCookies: Boolean, clearCache: Boolean) {
        if (clearCookies) {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
        }
        if (clearCache) {
            webViews.values.forEach { it.clearCache(true) }
        }
    }

    fun printActivePage(tabId: String, title: String) {
        val wv = webViews[tabId] ?: return
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = wv.createPrintDocumentAdapter(title.ifBlank { "Crumia Page" })
        printManager.print("Crumia - ${title.ifBlank { "Document" }}", adapter, PrintAttributes.Builder().build())
    }

    fun savePageArchive(tabId: String, onSaved: (String?) -> Unit) {
        val wv = webViews[tabId]
        if (wv == null) {
            onSaved(null)
            return
        }
        val dir = File(context.filesDir, "offline_pages").apply { mkdirs() }
        val file = File(dir, "page_${System.currentTimeMillis()}.mht")
        wv.saveWebArchive(file.absolutePath, false) { filename ->
            onSaved(filename)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    companion object {
        const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
    }
}
