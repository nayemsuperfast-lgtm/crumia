package com.example.ui

import android.app.Application
import android.net.Uri
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.CrumiaApplication
import com.example.adshield.AdShieldEngine
import com.example.browser.WebViewController
import com.example.browser.WebViewEventListener
import com.example.data.db.BookmarkEntity
import com.example.data.db.ClosedTabEntity
import com.example.data.db.DownloadEntity
import com.example.data.db.FilterRuleEntity
import com.example.data.db.HistoryEntity
import com.example.data.db.SavedTabEntity
import com.example.data.db.ShortcutEntity
import com.example.data.db.SiteSettingEntity
import com.example.data.model.FilterCategory
import com.example.data.model.HistoryRange
import com.example.data.model.HomepageBackgroundStyle
import com.example.data.model.SearchEngineOption
import com.example.data.model.StartupBehavior
import com.example.data.model.ThemeMode
import com.example.data.model.ToolbarPosition
import com.example.data.prefs.CrumiaPreferences
import com.example.downloads.CrumiaDownloadManager
import com.example.search.OmniboxSuggestion
import com.example.search.UrlUtils
import com.example.tabs.BrowserError
import com.example.tabs.BrowserTab
import com.example.tabs.PendingSitePermission
import com.example.tabs.WebContextMenuRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FileChooserRequest(
    val callback: ValueCallback<Array<Uri>>,
    val acceptTypes: Array<String>,
    val allowMultiple: Boolean
)

class BrowserViewModel(application: Application) : AndroidViewModel(application), WebViewEventListener {

    private val app = application as CrumiaApplication
    val prefsRepo = app.preferencesRepository
    val repo = app.browserRepository
    val adShield = app.adShieldEngine
    val downloadManager = app.downloadManager
    private val suggestionService = app.searchSuggestionService

    val webViewController = WebViewController(application.applicationContext, adShield, this)

    val preferences: StateFlow<CrumiaPreferences> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, CrumiaPreferences())

    val bookmarks: StateFlow<List<BookmarkEntity>> = repo.bookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val readingList: StateFlow<List<BookmarkEntity>> = repo.readingList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntity>> = repo.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSites: StateFlow<List<HistoryEntity>> = repo.recentUniqueHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloads: StateFlow<List<DownloadEntity>> = repo.downloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shortcuts: StateFlow<List<ShortcutEntity>> = repo.shortcuts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allowlistedSites: StateFlow<List<SiteSettingEntity>> = repo.allowlistedSites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSiteSettings: StateFlow<List<SiteSettingEntity>> = repo.siteSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filterRules: StateFlow<List<FilterRuleEntity>> = repo.filterRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val closedTabs: StateFlow<List<ClosedTabEntity>> = repo.closedTabs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val initialTab = BrowserTab(title = "Homepage", isHomepage = true)
    private val _tabs = MutableStateFlow(listOf(initialTab))
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(initialTab.id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    private val _suggestions = MutableStateFlow<List<OmniboxSuggestion>>(emptyList())
    val suggestions: StateFlow<List<OmniboxSuggestion>> = _suggestions.asStateFlow()

    private val _findInPageActive = MutableStateFlow(false)
    val findInPageActive: StateFlow<Boolean> = _findInPageActive.asStateFlow()

    private val _findInPageQuery = MutableStateFlow("")
    val findInPageQuery: StateFlow<String> = _findInPageQuery.asStateFlow()

    private val _findMatches = MutableStateFlow(0 to 0)
    val findMatches: StateFlow<Pair<Int, Int>> = _findMatches.asStateFlow()

    private val _fullscreenView = MutableStateFlow<Pair<View, WebChromeClient.CustomViewCallback>?>(null)
    val fullscreenView: StateFlow<Pair<View, WebChromeClient.CustomViewCallback>?> = _fullscreenView.asStateFlow()

    private val _pendingPermission = MutableStateFlow<PendingSitePermission?>(null)
    val pendingPermission: StateFlow<PendingSitePermission?> = _pendingPermission.asStateFlow()

    private val _contextMenuRequest = MutableStateFlow<WebContextMenuRequest?>(null)
    val contextMenuRequest: StateFlow<WebContextMenuRequest?> = _contextMenuRequest.asStateFlow()

    private val _isUpdatingFilters = MutableStateFlow(false)
    val isUpdatingFilters: StateFlow<Boolean> = _isUpdatingFilters.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    private val _fileChooserEvents = MutableSharedFlow<FileChooserRequest>(extraBufferCapacity = 4)
    val fileChooserEvents: SharedFlow<FileChooserRequest> = _fileChooserEvents.asSharedFlow()

    private var suggestionJob: Job? = null
    private var startupInitialized = false

    init {
        initializeStartupSession()
    }

    private fun initializeStartupSession() {
        if (startupInitialized) return
        startupInitialized = true
        viewModelScope.launch {
            val prefs = prefsRepo.preferencesFlow.first()
            when (prefs.startupBehavior) {
                StartupBehavior.RESTORE_TABS -> {
                    val saved = repo.getSavedSessionTabs()
                    if (saved.isNotEmpty()) {
                        val restored = saved.map { s ->
                            BrowserTab(
                                id = s.id,
                                title = s.title,
                                url = s.url,
                                isHomepage = s.isHomepage,
                                isDesktopMode = s.isDesktopMode,
                                isSecure = UrlUtils.isHttps(s.url)
                            )
                        }
                        val active = saved.firstOrNull { it.isActive }?.id ?: restored.first().id
                        _tabs.value = restored
                        _activeTabId.value = active
                        val activeTab = restored.firstOrNull { it.id == active }
                        if (activeTab != null && !activeTab.isHomepage && activeTab.url.isNotBlank()) {
                            loadUrlInTab(activeTab.id, activeTab.url)
                        }
                    }
                }
                StartupBehavior.CUSTOM_PAGE -> {
                    val customUrl = prefs.customHomepageUrl.trim()
                    if (customUrl.isNotBlank()) {
                        navigateActiveTab(customUrl)
                    }
                }
                StartupBehavior.HOMEPAGE -> {
                    // Default initial homepage tab is already active
                }
            }
        }
    }

    fun handleIncomingExternalUrl(url: String) {
        if (url.isBlank()) return
        openNewTab(url = url, isIncognito = false, selectImmediately = true)
    }

    fun activeTab(): BrowserTab {
        val id = _activeTabId.value
        return _tabs.value.firstOrNull { it.id == id } ?: _tabs.value.first()
    }

    fun navigateActiveTab(rawInput: String) {
        val prefs = preferences.value
        val resolved = UrlUtils.resolveInput(
            rawInput = rawInput,
            searchEngine = prefs.searchEngine,
            customTemplate = prefs.customSearchUrl,
            httpsUpgrade = prefs.httpsUpgradeEnabled
        )
        if (resolved.isBlank()) return
        val tab = activeTab()
        loadUrlInTab(tab.id, resolved)
    }

    fun loadUrlInTab(tabId: String, url: String) {
        val currentTab = _tabs.value.firstOrNull { it.id == tabId } ?: return
        val prefs = preferences.value
        updateTab(tabId) {
            it.copy(
                url = url,
                isHomepage = false,
                isLoading = true,
                progress = 10,
                errorState = null,
                blockedOnPage = 0,
                isSecure = UrlUtils.isHttps(url)
            )
        }
        viewModelScope.launch {
            val host = AdShieldEngine.safeHost(url)
            val siteSetting = host?.let { repo.getSiteSetting(it) }
            val desktop = siteSetting?.desktopMode ?: currentTab.isDesktopMode
            if (desktop != currentTab.isDesktopMode) {
                updateTab(tabId) { it.copy(isDesktopMode = desktop) }
            }
            val effectivePrefs = if (siteSetting?.javascriptEnabled != null) {
                prefs.copy(javascriptEnabled = siteSetting.javascriptEnabled)
            } else {
                prefs
            }
            val wv = webViewController.getOrCreateWebView(tabId, currentTab.isIncognito, desktop, effectivePrefs)
            wv.loadUrl(url)
        }
        persistSessionAsync()
    }

    fun openOfflineArchive(bookmark: BookmarkEntity) {
        val path = bookmark.offlineArchivePath
        if (!path.isNullOrBlank()) {
            loadUrlInTab(_activeTabId.value, "file://$path")
        } else {
            loadUrlInTab(_activeTabId.value, bookmark.url)
        }
    }

    fun goHomeActiveTab() {
        val tab = activeTab()
        webViewController.getWebView(tab.id)?.stopLoading()
        updateTab(tab.id) {
            it.copy(
                isHomepage = true,
                url = "",
                title = if (it.isIncognito) "Private Tab" else "Homepage",
                isLoading = false,
                progress = 0,
                errorState = null
            )
        }
        persistSessionAsync()
    }

    fun goBack(): Boolean {
        val tab = activeTab()
        if (tab.isHomepage) return false
        val wv = webViewController.getWebView(tab.id)
        return if (wv != null && wv.canGoBack()) {
            updateTab(tab.id) { it.copy(errorState = null) }
            wv.goBack()
            true
        } else {
            goHomeActiveTab()
            true
        }
    }

    fun goForward() {
        val tab = activeTab()
        val wv = webViewController.getWebView(tab.id) ?: return
        if (tab.isHomepage && wv.url != null && wv.url != "about:blank") {
            updateTab(tab.id) { it.copy(isHomepage = false, errorState = null) }
            return
        }
        if (wv.canGoForward()) {
            updateTab(tab.id) { it.copy(errorState = null) }
            wv.goForward()
        }
    }

    fun reloadOrStop() {
        val tab = activeTab()
        if (tab.isHomepage) return
        val wv = webViewController.getWebView(tab.id) ?: return
        if (tab.isLoading) {
            wv.stopLoading()
            updateTab(tab.id) { it.copy(isLoading = false) }
        } else {
            updateTab(tab.id) { it.copy(errorState = null, isLoading = true) }
            if (wv.url.isNullOrBlank() || wv.url == "about:blank") {
                wv.loadUrl(tab.url)
            } else {
                wv.reload()
            }
        }
    }

    fun openNewTab(url: String? = null, isIncognito: Boolean = false, selectImmediately: Boolean = true) {
        val currentActive = activeTab()
        if (!currentActive.isHomepage) {
            val thumb = webViewController.captureThumbnail(currentActive.id)
            if (thumb != null) updateTab(currentActive.id) { it.copy(thumbnail = thumb) }
        }
        val prefs = preferences.value
        val newTab = BrowserTab(
            title = if (isIncognito) "Private Tab" else "Homepage",
            url = url.orEmpty(),
            isIncognito = isIncognito,
            isHomepage = url.isNullOrBlank(),
            isDesktopMode = prefs.desktopModeDefault
        )
        _tabs.update { it + newTab }
        if (selectImmediately) {
            _activeTabId.value = newTab.id
            webViewController.pauseOtherTabs(newTab.id)
        }
        if (!url.isNullOrBlank()) {
            loadUrlInTab(newTab.id, url)
        }
        persistSessionAsync()
    }

    fun selectTab(tabId: String) {
        val currentId = _activeTabId.value
        if (currentId == tabId) return
        val current = _tabs.value.firstOrNull { it.id == currentId }
        if (current != null && !current.isHomepage) {
            val thumb = webViewController.captureThumbnail(current.id)
            if (thumb != null) updateTab(current.id) { it.copy(thumbnail = thumb) }
        }
        _activeTabId.value = tabId
        updateTab(tabId) { it.copy(lastAccessedAt = System.currentTimeMillis()) }
        webViewController.pauseOtherTabs(tabId)
        val target = _tabs.value.firstOrNull { it.id == tabId }
        if (target != null && !target.isHomepage && target.url.isNotBlank() && webViewController.getWebView(tabId) == null) {
            loadUrlInTab(tabId, target.url)
        }
        persistSessionAsync()
    }

    fun switchRelativeTab(delta: Int) {
        val current = activeTab()
        val sameModeTabs = _tabs.value.filter { it.isIncognito == current.isIncognito }
        if (sameModeTabs.size <= 1) return
        val idx = sameModeTabs.indexOfFirst { it.id == current.id }
        if (idx == -1) return
        val targetIdx = (idx + delta).coerceIn(0, sameModeTabs.lastIndex)
        if (targetIdx != idx) {
            selectTab(sameModeTabs[targetIdx].id)
        }
    }

    fun addActivePageAsShortcut() {
        val tab = activeTab()
        if (tab.isHomepage || tab.url.isBlank()) return
        val title = tab.title.ifBlank { UrlUtils.extractDisplayHost(tab.url) }
        viewModelScope.launch {
            repo.addShortcut(title, tab.url)
            _snackbarMessages.tryEmit("Added $title to Homepage")
        }
    }

    fun captureCurrentTabThumbnail() {
        val tab = activeTab()
        if (!tab.isHomepage) {
            val bmp = webViewController.captureThumbnail(tab.id)
            if (bmp != null) updateTab(tab.id) { it.copy(thumbnail = bmp) }
        }
    }

    fun duplicateTab(tabId: String) {
        val source = _tabs.value.firstOrNull { it.id == tabId } ?: return
        openNewTab(
            url = if (source.isHomepage) null else source.url,
            isIncognito = source.isIncognito,
            selectImmediately = true
        )
    }

    fun closeTab(tabId: String) {
        val closing = _tabs.value.firstOrNull { it.id == tabId } ?: return
        if (!closing.isIncognito && !closing.isHomepage && closing.url.isNotBlank()) {
            viewModelScope.launch {
                repo.recordClosedTab(closing.title, closing.url)
            }
        }
        webViewController.destroyTab(tabId)
        closing.thumbnail?.recycle()

        val remaining = _tabs.value.filterNot { it.id == tabId }
        if (closing.isIncognito && remaining.none { it.isIncognito }) {
            webViewController.clearIncognitoData()
        }

        if (remaining.isEmpty()) {
            val fresh = BrowserTab(title = "Homepage", isHomepage = true)
            _tabs.value = listOf(fresh)
            _activeTabId.value = fresh.id
        } else {
            _tabs.value = remaining
            if (_activeTabId.value == tabId) {
                val next = remaining.last()
                _activeTabId.value = next.id
                webViewController.pauseOtherTabs(next.id)
                if (!next.isHomepage && next.url.isNotBlank() && webViewController.getWebView(next.id) == null) {
                    loadUrlInTab(next.id, next.url)
                }
            }
        }
        persistSessionAsync()
    }

    fun closeAllTabs(incognitoOnly: Boolean? = null) {
        val toClose = _tabs.value.filter { tab ->
            when (incognitoOnly) {
                true -> tab.isIncognito
                false -> !tab.isIncognito
                null -> true
            }
        }
        toClose.forEach { tab ->
            if (!tab.isIncognito && !tab.isHomepage && tab.url.isNotBlank()) {
                viewModelScope.launch { repo.recordClosedTab(tab.title, tab.url) }
            }
            webViewController.destroyTab(tab.id)
            tab.thumbnail?.recycle()
        }
        if (incognitoOnly != false) {
            webViewController.clearIncognitoData()
        }

        val remaining = _tabs.value - toClose.toSet()
        if (remaining.isEmpty()) {
            val fresh = BrowserTab(title = "Homepage", isHomepage = true)
            _tabs.value = listOf(fresh)
            _activeTabId.value = fresh.id
        } else {
            _tabs.value = remaining
            if (remaining.none { it.id == _activeTabId.value }) {
                _activeTabId.value = remaining.last().id
            }
        }
        persistSessionAsync()
    }

    fun restoreLastClosedTab() {
        viewModelScope.launch {
            val popped = repo.popClosedTab()
            if (popped != null) {
                openNewTab(url = popped.url, isIncognito = false, selectImmediately = true)
                _snackbarMessages.tryEmit("Restored ${popped.title}")
            } else {
                _snackbarMessages.tryEmit("No recently closed tabs")
            }
        }
    }

    fun restoreClosedTab(entity: ClosedTabEntity) {
        viewModelScope.launch {
            repo.deleteClosedTab(entity.id)
            openNewTab(url = entity.url, isIncognito = false, selectImmediately = true)
        }
    }

    fun toggleDesktopMode() {
        val tab = activeTab()
        val newDesktop = !tab.isDesktopMode
        updateTab(tab.id) { it.copy(isDesktopMode = newDesktop) }
        if (!tab.isHomepage && tab.url.isNotBlank()) {
            val wv = webViewController.getWebView(tab.id)
            if (wv != null) {
                webViewController.applyPreferences(wv, tab.isIncognito, newDesktop, preferences.value)
                wv.reload()
            }
            val host = AdShieldEngine.normalizeHost(AdShieldEngine.safeHost(tab.url))
            if (host != null && !tab.isIncognito) {
                viewModelScope.launch {
                    val existing = repo.getSiteSetting(host) ?: SiteSettingEntity(host = host)
                    repo.upsertSiteSetting(existing.copy(desktopMode = newDesktop))
                }
            }
        }
        _snackbarMessages.tryEmit(if (newDesktop) "Desktop site enabled" else "Mobile site enabled")
    }

    fun translateActivePage() {
        val tab = activeTab()
        if (tab.isHomepage || tab.url.isBlank()) return
        val encoded = Uri.encode(tab.url)
        loadUrlInTab(tab.id, "https://translate.google.com/translate?sl=auto&tl=en&u=$encoded")
    }

    // Find in Page
    fun openFindInPage() {
        _findInPageActive.value = true
    }

    fun closeFindInPage() {
        _findInPageActive.value = false
        _findInPageQuery.value = ""
        _findMatches.value = 0 to 0
        webViewController.getWebView(_activeTabId.value)?.clearMatches()
    }

    fun updateFindQuery(query: String) {
        _findInPageQuery.value = query
        val wv = webViewController.getWebView(_activeTabId.value) ?: return
        if (query.isBlank()) {
            wv.clearMatches()
            _findMatches.value = 0 to 0
            return
        }
        wv.setFindListener { activeOrdinal, numberOfMatches, _ ->
            val current = if (numberOfMatches > 0) activeOrdinal + 1 else 0
            _findMatches.value = current to numberOfMatches
        }
        wv.findAllAsync(query)
    }

    fun findNext(forward: Boolean) {
        webViewController.getWebView(_activeTabId.value)?.findNext(forward)
    }

    // Omnibox Suggestions
    fun onAddressInputChanged(input: String) {
        suggestionJob?.cancel()
        if (input.isBlank()) {
            _suggestions.value = emptyList()
            return
        }
        suggestionJob = viewModelScope.launch {
            delay(140)
            val list = suggestionService.fetchSuggestions(
                query = input,
                searchEngine = preferences.value.searchEngine,
                bookmarks = bookmarks.value,
                allowRemoteSuggestions = !activeTab().isIncognito
            )
            _suggestions.value = list
        }
    }

    fun clearSuggestions() {
        suggestionJob?.cancel()
        _suggestions.value = emptyList()
    }

    // Bookmarks & Offline Reading
    fun toggleBookmarkForActiveTab() {
        val tab = activeTab()
        if (tab.isHomepage || tab.url.isBlank()) return
        viewModelScope.launch {
            val added = repo.toggleBookmark(tab.title, tab.url)
            _snackbarMessages.tryEmit(if (added) "Bookmarked ${tab.title}" else "Bookmark removed")
        }
    }

    fun saveActivePageForOffline() {
        val tab = activeTab()
        if (tab.isHomepage || tab.url.isBlank()) return
        webViewController.savePageArchive(tab.id) { savedPath ->
            viewModelScope.launch {
                repo.addToReadingList(tab.title, tab.url, savedPath)
                _snackbarMessages.tryEmit(
                    if (savedPath != null) "Saved to Reading List for offline viewing"
                    else "Added to Reading List"
                )
            }
        }
    }

    fun printActivePage() {
        val tab = activeTab()
        if (tab.isHomepage || tab.url.isBlank()) return
        webViewController.printActivePage(tab.id, tab.title)
    }

    // Ad Shield controls
    fun toggleCurrentSiteAllowlist() {
        val tab = activeTab()
        val host = AdShieldEngine.normalizeHost(AdShieldEngine.safeHost(tab.url)) ?: return
        viewModelScope.launch {
            val currentlyAllowlisted = adShield.isSiteAllowlisted(host)
            adShield.setSiteAllowlisted(host, !currentlyAllowlisted)
            _snackbarMessages.tryEmit(
                if (!currentlyAllowlisted) "Ad Shield paused on $host"
                else "Ad Shield enabled on $host"
            )
            webViewController.getWebView(tab.id)?.reload()
        }
    }

    fun manualUpdateFilterLists() {
        if (_isUpdatingFilters.value) return
        viewModelScope.launch {
            _isUpdatingFilters.value = true
            val res = adShield.updateFilterLists()
            _isUpdatingFilters.value = false
            res.onSuccess { count ->
                _snackbarMessages.tryEmit("Filter lists updated ($count rules synced)")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "Filter update failed")
            }
        }
    }

    fun addCustomFilterRule(pattern: String, category: FilterCategory) {
        viewModelScope.launch {
            adShield.addCustomRule(pattern, category)
            _snackbarMessages.tryEmit("Rule added")
        }
    }

    fun deleteFilterRule(id: Long) {
        viewModelScope.launch { adShield.deleteRule(id) }
    }

    fun resetAdShield() {
        viewModelScope.launch {
            adShield.resetToDefaults()
            _snackbarMessages.tryEmit("Ad Shield reset to defaults")
        }
    }

    // Settings updates
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    fun setSearchEngine(engine: SearchEngineOption) = viewModelScope.launch { prefsRepo.setSearchEngine(engine) }
    fun setCustomSearchUrl(url: String) = viewModelScope.launch { prefsRepo.setCustomSearchUrl(url) }
    fun setStartupBehavior(behavior: StartupBehavior) = viewModelScope.launch { prefsRepo.setStartupBehavior(behavior) }
    fun setCustomHomepageUrl(url: String) = viewModelScope.launch { prefsRepo.setCustomHomepageUrl(url) }
    fun setHomepageBackground(style: HomepageBackgroundStyle) = viewModelScope.launch { prefsRepo.setHomepageBackground(style) }
    fun setShowShortcuts(show: Boolean) = viewModelScope.launch { prefsRepo.setShowShortcuts(show) }
    fun setShowRecentSites(show: Boolean) = viewModelScope.launch { prefsRepo.setShowRecentSites(show) }
    fun setShowBookmarks(show: Boolean) = viewModelScope.launch { prefsRepo.setShowBookmarks(show) }
    fun setToolbarPosition(pos: ToolbarPosition) = viewModelScope.launch { prefsRepo.setToolbarPosition(pos) }
    fun setTextZoomPercent(percent: Int) = viewModelScope.launch {
        prefsRepo.setTextZoomPercent(percent)
        reapplyPreferencesToActiveWebView()
    }
    fun setAdShieldEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setAdShieldEnabled(enabled) }
    fun setBlockAds(enabled: Boolean) = viewModelScope.launch { prefsRepo.setBlockAds(enabled) }
    fun setBlockTrackers(enabled: Boolean) = viewModelScope.launch { prefsRepo.setBlockTrackers(enabled) }
    fun setBlockPopups(enabled: Boolean) = viewModelScope.launch { prefsRepo.setBlockPopups(enabled) }
    fun setFilterList(easyList: Boolean? = null, easyPrivacy: Boolean? = null, adGuard: Boolean? = null) =
        viewModelScope.launch {
            prefsRepo.setFilterListEnabled(easyList, easyPrivacy, adGuard)
            adShield.reloadRulesCache()
        }
    fun setJavascriptEnabled(enabled: Boolean) = viewModelScope.launch {
        prefsRepo.setJavascriptEnabled(enabled)
        reapplyPreferencesToActiveWebView()
    }
    fun setSafeBrowsingEnabled(enabled: Boolean) = viewModelScope.launch {
        prefsRepo.setSafeBrowsingEnabled(enabled)
        reapplyPreferencesToActiveWebView()
    }
    fun setCookiesEnabled(enabled: Boolean) = viewModelScope.launch {
        prefsRepo.setCookiesEnabled(enabled)
        reapplyPreferencesToActiveWebView()
    }
    fun setThirdPartyCookiesEnabled(enabled: Boolean) = viewModelScope.launch {
        prefsRepo.setThirdPartyCookiesEnabled(enabled)
        reapplyPreferencesToActiveWebView()
    }
    fun setDoNotTrackEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setDoNotTrackEnabled(enabled) }
    fun setHttpsUpgradeEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setHttpsUpgradeEnabled(enabled) }
    fun setDesktopModeDefault(enabled: Boolean) = viewModelScope.launch { prefsRepo.setDesktopModeDefault(enabled) }
    fun setDownloadNotificationsEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setDownloadNotificationsEnabled(enabled) }

    fun clearHistory(range: HistoryRange) = viewModelScope.launch {
        repo.clearHistory(range)
        _snackbarMessages.tryEmit("History cleared")
    }

    fun clearCacheAndCookies(clearCookies: Boolean, clearCache: Boolean) {
        webViewController.clearAllCacheAndCookies(clearCookies, clearCache)
        _snackbarMessages.tryEmit(
            when {
                clearCookies && clearCache -> "Cookies and cache cleared"
                clearCookies -> "Cookies cleared"
                else -> "Cache cleared"
            }
        )
    }

    fun clearBrowsingData(
        range: HistoryRange,
        clearHistory: Boolean,
        clearCookies: Boolean,
        clearCache: Boolean,
        clearClosedTabs: Boolean
    ) {
        viewModelScope.launch {
            if (clearHistory) repo.clearHistory(range)
            if (clearClosedTabs) repo.clearClosedTabs()
            if (clearCookies || clearCache) {
                webViewController.clearAllCacheAndCookies(clearCookies, clearCache)
            }
            _snackbarMessages.tryEmit("Browsing data cleared")
        }
    }

    fun toggleSiteJavascript(host: String, enabled: Boolean) {
        viewModelScope.launch {
            val existing = repo.getSiteSetting(host) ?: SiteSettingEntity(host = host)
            repo.upsertSiteSetting(existing.copy(javascriptEnabled = enabled))
            val tab = activeTab()
            webViewController.getWebView(tab.id)?.let { wv ->
                webViewController.applyPreferences(
                    wv,
                    tab.isIncognito,
                    tab.isDesktopMode,
                    preferences.value.copy(javascriptEnabled = enabled)
                )
                wv.reload()
            }
        }
    }

    fun clearSiteSettingsForHost(host: String) {
        viewModelScope.launch {
            adShield.setSiteAllowlisted(host, false)
            val existing = repo.getSiteSetting(host)
            if (existing != null) {
                repo.upsertSiteSetting(
                    SiteSettingEntity(host = host)
                )
            }
            webViewController.getWebView(activeTab().id)?.reload()
            _snackbarMessages.tryEmit("Reset settings for $host")
        }
    }

    fun resetApplication() {
        viewModelScope.launch {
            prefsRepo.resetAllSettings()
            repo.clearHistory(HistoryRange.ALL_TIME)
            repo.clearClosedTabs()
            repo.resetAllSiteSettings()
            adShield.resetToDefaults()
            webViewController.clearAllCacheAndCookies(clearCookies = true, clearCache = true)
            _snackbarMessages.tryEmit("Crumia reset to factory defaults")
        }
    }

    private fun reapplyPreferencesToActiveWebView() {
        val tab = activeTab()
        webViewController.getWebView(tab.id)?.let { wv ->
            webViewController.applyPreferences(wv, tab.isIncognito, tab.isDesktopMode, preferences.value)
        }
    }

    fun resolvePermissionPrompt(granted: Boolean) {
        val current = _pendingPermission.value ?: return
        _pendingPermission.value = null
        current.onDecision(granted)
    }

    fun deferPermissionPromptToRuntime() {
        _pendingPermission.value = null
    }

    fun dismissContextMenu() {
        _contextMenuRequest.value = null
    }

    fun showToastMessage(msg: String) {
        _snackbarMessages.tryEmit(msg)
    }

    fun exitFullscreen() {
        val pair = _fullscreenView.value ?: return
        _fullscreenView.value = null
        runCatching { pair.second.onCustomViewHidden() }
    }

    // WebViewEventListener callbacks
    override fun onPageStarted(tabId: String, url: String) {
        updateTab(tabId) {
            it.copy(
                url = url,
                isHomepage = false,
                isLoading = true,
                progress = 12,
                errorState = null,
                isSecure = UrlUtils.isHttps(url)
            )
        }
    }

    override fun onPageFinished(
        tabId: String,
        url: String,
        title: String,
        canGoBack: Boolean,
        canGoForward: Boolean
    ) {
        val tab = _tabs.value.firstOrNull { it.id == tabId }
        updateTab(tabId) {
            it.copy(
                url = url,
                title = title,
                isLoading = false,
                progress = 100,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                isSecure = UrlUtils.isHttps(url)
            )
        }
        if (tab != null && !tab.isIncognito && url.startsWith("http")) {
            viewModelScope.launch {
                repo.recordVisit(title, url)
            }
        }
        persistSessionAsync()
    }

    override fun onProgressChanged(tabId: String, progress: Int) {
        updateTab(tabId) { it.copy(progress = progress, isLoading = progress < 100) }
    }

    override fun onTitleReceived(tabId: String, title: String) {
        updateTab(tabId) { it.copy(title = title) }
    }

    override fun onNavigationStateChanged(tabId: String, canGoBack: Boolean, canGoForward: Boolean) {
        updateTab(tabId) { it.copy(canGoBack = canGoBack, canGoForward = canGoForward) }
    }

    override fun onBlockedRequestOnPage(tabId: String) {
        updateTab(tabId) { it.copy(blockedOnPage = it.blockedOnPage + 1) }
    }

    override fun onPopupBlocked(tabId: String, blockedUrl: String?) {
        updateTab(tabId) { it.copy(blockedOnPage = it.blockedOnPage + 1) }
        _snackbarMessages.tryEmit("Ad Shield blocked a pop-up")
    }

    override fun onError(tabId: String, error: BrowserError) {
        updateTab(tabId) { it.copy(isLoading = false, errorState = error) }
    }

    override fun onDownloadRequested(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String
    ) {
        downloadManager.startDownload(
            url = url,
            userAgent = userAgent,
            contentDisposition = contentDisposition,
            mimeType = mimeType,
            showNotifications = preferences.value.downloadNotificationsEnabled
        )
        _snackbarMessages.tryEmit("Downloading file…")
    }

    override fun onFileChooserRequested(
        callback: ValueCallback<Array<Uri>>,
        acceptTypes: Array<String>,
        allowMultiple: Boolean
    ) {
        _fileChooserEvents.tryEmit(FileChooserRequest(callback, acceptTypes, allowMultiple))
    }

    override fun onFullscreenShow(view: View, callback: WebChromeClient.CustomViewCallback) {
        _fullscreenView.value = view to callback
    }

    override fun onFullscreenHide() {
        _fullscreenView.value = null
    }

    override fun onSitePermissionRequested(request: PendingSitePermission) {
        _pendingPermission.value = request
    }

    override fun onOpenNewTabRequested(url: String, isIncognito: Boolean) {
        openNewTab(url = url, isIncognito = isIncognito, selectImmediately = true)
    }

    override fun onContextMenuRequested(request: WebContextMenuRequest) {
        _contextMenuRequest.value = request
    }

    private fun updateTab(tabId: String, transform: (BrowserTab) -> BrowserTab) {
        _tabs.update { list ->
            list.map { if (it.id == tabId) transform(it) else it }
        }
    }

    private fun persistSessionAsync() {
        val currentTabs = _tabs.value.filter { !it.isIncognito }
        val active = _activeTabId.value
        viewModelScope.launch {
            val saved = currentTabs.mapIndexed { idx, tab ->
                SavedTabEntity(
                    id = tab.id,
                    title = tab.title,
                    url = tab.url,
                    isHomepage = tab.isHomepage,
                    isDesktopMode = tab.isDesktopMode,
                    sortOrder = idx,
                    isActive = tab.id == active
                )
            }
            repo.saveSessionTabs(saved)
        }
    }

    override fun onCleared() {
        super.onCleared()
        adShield.flushCountersAsync()
        _tabs.value.forEach {
            webViewController.destroyTab(it.id)
            it.thumbnail?.recycle()
        }
    }
}
