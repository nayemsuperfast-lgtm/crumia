package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.speech.RecognizerIntent
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.adshield.AdShieldEngine
import com.example.data.model.ToolbarPosition
import com.example.tabs.PendingSitePermission
import com.example.ui.components.BrowserToolbar
import com.example.ui.components.ChromeShareSheet
import com.example.ui.components.ErrorPageView
import com.example.ui.components.FindInPageBar
import com.example.ui.components.HomepageChromeTopBar
import com.example.ui.components.MainMenuSheet
import com.example.ui.components.OmniboxSearchOverlay
import com.example.ui.components.PageInfoBottomSheet
import com.example.ui.components.SplashOverlay
import com.example.ui.screens.AdShieldScreen
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.HomepageScreen
import com.example.ui.screens.TabSwitcherScreen
import com.example.ui.theme.CrumiaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private object CrumiaRoutes {
    const val BROWSER = "browser"
    const val TABS = "tabs"
    const val BOOKMARKS = "bookmarks"
    const val HISTORY = "history"
    const val DOWNLOADS = "downloads"
    const val AD_SHIELD = "ad_shield"
    const val SETTINGS_PATTERN = "settings?openAbout={openAbout}"
    fun settings(openAbout: Boolean = false) = "settings?openAbout=$openAbout"
}

@Composable
fun CrumiaApp(
    viewModel: BrowserViewModel,
    isInPictureInPicture: Boolean,
    onEnterPictureInPicture: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()

    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val findActive by viewModel.findInPageActive.collectAsStateWithLifecycle()
    val findQuery by viewModel.findInPageQuery.collectAsStateWithLifecycle()
    val findMatches by viewModel.findMatches.collectAsStateWithLifecycle()
    val fullscreenView by viewModel.fullscreenView.collectAsStateWithLifecycle()
    val pendingPermission by viewModel.pendingPermission.collectAsStateWithLifecycle()
    val contextMenuRequest by viewModel.contextMenuRequest.collectAsStateWithLifecycle()
    val isUpdatingFilters by viewModel.isUpdatingFilters.collectAsStateWithLifecycle()

    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val readingList by viewModel.readingList.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val recentSites by viewModel.recentSites.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val shortcuts by viewModel.shortcuts.collectAsStateWithLifecycle()
    val allowlistedSites by viewModel.allowlistedSites.collectAsStateWithLifecycle()
    val allSiteSettings by viewModel.allSiteSettings.collectAsStateWithLifecycle()
    val filterRules by viewModel.filterRules.collectAsStateWithLifecycle()
    val closedTabs by viewModel.closedTabs.collectAsStateWithLifecycle()

    val activeTab = remember(tabs, activeTabId) {
        tabs.firstOrNull { it.id == activeTabId } ?: tabs.first()
    }
    val visibleTabCount = remember(tabs, activeTab.isIncognito) {
        tabs.count { it.isIncognito == activeTab.isIncognito }
    }
    val isBookmarked by viewModel.repo.isBookmarked(activeTab.url)
        .collectAsStateWithLifecycle(initialValue = false)

    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(620)
        showSplash = false
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Voice Search Availability & Launcher
    val voiceIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Search with Crumia")
        }
    }
    val voiceSearchAvailable = remember {
        voiceIntent.resolveActivity(context.packageManager) != null
    }
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.navigateActiveTab(spoken)
            }
        }
    }

    // WebView File Upload Picker (zero broad storage permissions!)
    var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val singleFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingFileCallback?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
        pendingFileCallback = null
    }
    val multiFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        pendingFileCallback?.onReceiveValue(if (uris.isNotEmpty()) uris.toTypedArray() else null)
        pendingFileCallback = null
    }

    LaunchedEffect(Unit) {
        viewModel.fileChooserEvents.collect { req ->
            pendingFileCallback?.onReceiveValue(null)
            pendingFileCallback = req.callback
            val mimeTypes = req.acceptTypes.filter { it.isNotBlank() }.toTypedArray().ifEmpty { arrayOf("*/*") }
            if (req.allowMultiple) {
                multiFilePicker.launch(mimeTypes)
            } else {
                singleFilePicker.launch(mimeTypes)
            }
        }
    }

    // Bookmarks HTML Import / Export SAF Launchers
    val exportBookmarksLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                viewModel.repo.exportBookmarksHtml(uri)
                    .onSuccess { count -> viewModel.showToastMessage("Exported $count bookmarks") }
                    .onFailure { viewModel.showToastMessage("Failed to export bookmarks") }
            }
        }
    }
    val importBookmarksLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                viewModel.repo.importBookmarksHtml(uri)
                    .onSuccess { count -> viewModel.showToastMessage("Imported $count bookmarks") }
                    .onFailure { viewModel.showToastMessage("Failed to import bookmarks") }
            }
        }
    }

    // Android Runtime Permissions Launcher (for Website Camera/Mic/Location requests & Notifications)
    var awaitingPermissionPrompt by remember { mutableStateOf<PendingSitePermission?>(null) }
    val runtimePermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val req = awaitingPermissionPrompt
        awaitingPermissionPrompt = null
        if (req != null) {
            val allGranted = grants.values.all { it }
            req.onDecision(allGranted)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(prefs.downloadNotificationsEnabled) {
        if (prefs.downloadNotificationsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    var menuSheetVisible by remember { mutableStateOf(false) }
    var omniboxOverlayVisible by remember { mutableStateOf(false) }
    var pageInfoVisible by remember { mutableStateOf(false) }
    var shareSheetVisible by remember { mutableStateOf(false) }

    CrumiaTheme(
        themeMode = prefs.themeMode,
        isIncognito = activeTab.isIncognito
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = CrumiaRoutes.BROWSER
            ) {
                composable(CrumiaRoutes.BROWSER) {
                    BackHandler(enabled = omniboxOverlayVisible || findActive || (!activeTab.isHomepage)) {
                        when {
                            omniboxOverlayVisible -> {
                                omniboxOverlayVisible = false
                                viewModel.clearSuggestions()
                            }
                            findActive -> viewModel.closeFindInPage()
                            else -> viewModel.goBack()
                        }
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { _ ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .imePadding()
                        ) {
                            if (activeTab.isHomepage) {
                                // Chrome-style clean minimal top bar on Homepage
                                if (!isInPictureInPicture) {
                                    HomepageChromeTopBar(
                                        tabCount = visibleTabCount,
                                        isIncognito = activeTab.isIncognito,
                                        adShieldEnabled = prefs.adShieldEnabled,
                                        totalBlockedCount = prefs.totalAdsBlocked + prefs.totalTrackersBlocked,
                                        onHome = { viewModel.goHomeActiveTab() },
                                        onOpenAdShield = { navController.navigate(CrumiaRoutes.AD_SHIELD) },
                                        onOpenTabs = {
                                            viewModel.captureCurrentTabThumbnail()
                                            navController.navigate(CrumiaRoutes.TABS)
                                        },
                                        onOpenMenu = { menuSheetVisible = true }
                                    )
                                }

                                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    HomepageScreen(
                                        isIncognito = activeTab.isIncognito,
                                        prefs = prefs,
                                        shortcuts = shortcuts,
                                        recentSites = recentSites,
                                        bookmarks = bookmarks,
                                        closedTabs = closedTabs,
                                        voiceSearchAvailable = voiceSearchAvailable,
                                        onOpenOmniboxSearch = { omniboxOverlayVisible = true },
                                        onNavigate = { viewModel.navigateActiveTab(it) },
                                        onSelectSearchEngine = { viewModel.setSearchEngine(it) },
                                        onSelectBackground = { viewModel.setHomepageBackground(it) },
                                        onAddShortcut = { t, u, a ->
                                            scope.launch { viewModel.repo.addShortcut(t, u, a) }
                                        },
                                        onUpdateShortcut = { s ->
                                            scope.launch { viewModel.repo.updateShortcut(s) }
                                        },
                                        onDeleteShortcut = { id ->
                                            scope.launch { viewModel.repo.deleteShortcut(id) }
                                        },
                                        onRestoreClosedTab = { viewModel.restoreClosedTab(it) },
                                        onOpenIncognitoTab = { viewModel.openNewTab(isIncognito = true) },
                                        onVoiceSearch = {
                                            if (voiceSearchAvailable) voiceLauncher.launch(voiceIntent)
                                        },
                                        onOpenBookmarks = { navController.navigate(CrumiaRoutes.BOOKMARKS) },
                                        onOpenHistory = { navController.navigate(CrumiaRoutes.HISTORY) },
                                        onOpenDownloads = { navController.navigate(CrumiaRoutes.DOWNLOADS) },
                                        onOpenAdShield = { navController.navigate(CrumiaRoutes.AD_SHIELD) },
                                        onOpenSettings = { navController.navigate(CrumiaRoutes.settings(openAbout = false)) }
                                    )
                                }
                            } else {
                                val toolbarContent: @Composable () -> Unit = {
                                    if (!isInPictureInPicture) {
                                        BrowserToolbar(
                                            tab = activeTab,
                                            tabCount = visibleTabCount,
                                            adShieldEnabled = prefs.adShieldEnabled,
                                            suggestions = suggestions,
                                            voiceSearchAvailable = voiceSearchAvailable,
                                            onNavigate = { viewModel.navigateActiveTab(it) },
                                            onAddressInputChanged = { viewModel.onAddressInputChanged(it) },
                                            onClearSuggestions = { viewModel.clearSuggestions() },
                                            onHome = { viewModel.goHomeActiveTab() },
                                            onNewTab = { viewModel.openNewTab(isIncognito = activeTab.isIncognito) },
                                            onOpenTabs = {
                                                viewModel.captureCurrentTabThumbnail()
                                                navController.navigate(CrumiaRoutes.TABS)
                                            },
                                            onOpenMenu = { menuSheetVisible = true },
                                            onOpenAdShield = { navController.navigate(CrumiaRoutes.AD_SHIELD) },
                                            onOpenPageInfo = { pageInfoVisible = true },
                                            onSwipeTabNext = { viewModel.switchRelativeTab(1) },
                                            onSwipeTabPrev = { viewModel.switchRelativeTab(-1) },
                                            onVoiceSearch = {
                                                if (voiceSearchAvailable) voiceLauncher.launch(voiceIntent)
                                            }
                                        )
                                    }
                                }

                                if (prefs.toolbarPosition == ToolbarPosition.TOP) {
                                    toolbarContent()
                                }

                                if (findActive && !isInPictureInPicture) {
                                    FindInPageBar(
                                        query = findQuery,
                                        matches = findMatches,
                                        onQueryChange = { viewModel.updateFindQuery(it) },
                                        onPrev = { viewModel.findNext(false) },
                                        onNext = { viewModel.findNext(true) },
                                        onClose = { viewModel.closeFindInPage() }
                                    )
                                }

                                // Real Android System WebView container
                                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    val wv = remember(activeTab.id, activeTab.isIncognito) {
                                        viewModel.webViewController.getOrCreateWebView(
                                            tabId = activeTab.id,
                                            isIncognito = activeTab.isIncognito,
                                            isDesktopMode = activeTab.isDesktopMode,
                                            prefs = prefs
                                        )
                                    }

                                    AndroidView(
                                        factory = {
                                            (wv.parent as? ViewGroup)?.removeView(wv)
                                            wv
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    activeTab.errorState?.let { err ->
                                        ErrorPageView(
                                            error = err,
                                            onRetry = { viewModel.reloadOrStop() },
                                            onReturnHome = { viewModel.goHomeActiveTab() },
                                            onGoBack = { viewModel.goBack() }
                                        )
                                    }
                                }

                                if (!isInPictureInPicture && prefs.toolbarPosition == ToolbarPosition.BOTTOM) {
                                    toolbarContent()
                                }
                            }
                        }

                        if (omniboxOverlayVisible) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .navigationBarsPadding()
                                    .imePadding()
                            ) {
                                OmniboxSearchOverlay(
                                    searchEngineName = prefs.searchEngine.displayName,
                                    suggestions = suggestions,
                                    voiceSearchAvailable = voiceSearchAvailable,
                                    onQueryChanged = { viewModel.onAddressInputChanged(it) },
                                    onSubmit = { query ->
                                        omniboxOverlayVisible = false
                                        viewModel.clearSuggestions()
                                        viewModel.navigateActiveTab(query)
                                    },
                                    onVoiceSearch = {
                                        omniboxOverlayVisible = false
                                        if (voiceSearchAvailable) voiceLauncher.launch(voiceIntent)
                                    },
                                    onDismiss = {
                                        omniboxOverlayVisible = false
                                        viewModel.clearSuggestions()
                                    }
                                )
                            }
                        }
                    }
                }

                composable(CrumiaRoutes.TABS) {
                    TabSwitcherScreen(
                        tabs = tabs,
                        activeTabId = activeTabId,
                        closedTabs = closedTabs,
                        onSelectTab = { id ->
                            viewModel.selectTab(id)
                            navController.popBackStack()
                        },
                        onCloseTab = { viewModel.closeTab(it) },
                        onDuplicateTab = { viewModel.duplicateTab(it) },
                        onNewTab = { incognito ->
                            viewModel.openNewTab(isIncognito = incognito, selectImmediately = true)
                            navController.popBackStack()
                        },
                        onCloseAllTabs = { incognitoOnly -> viewModel.closeAllTabs(incognitoOnly) },
                        onRestoreClosedTab = { closed ->
                            viewModel.restoreClosedTab(closed)
                            navController.popBackStack()
                        },
                        onRestoreLastClosed = {
                            viewModel.restoreLastClosedTab()
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(CrumiaRoutes.BOOKMARKS) {
                    BookmarksScreen(
                        bookmarks = bookmarks,
                        readingList = readingList,
                        onOpenUrl = { url ->
                            viewModel.loadUrlInTab(activeTabId, url)
                            navController.popBackStack()
                        },
                        onOpenOffline = { bm ->
                            viewModel.openOfflineArchive(bm)
                            navController.popBackStack()
                        },
                        onSaveBookmark = { bm ->
                            scope.launch { viewModel.repo.saveBookmark(bm) }
                        },
                        onDeleteBookmark = { bm ->
                            scope.launch { viewModel.repo.deleteBookmark(bm.id, bm.offlineArchivePath) }
                        },
                        onImportHtml = {
                            importBookmarksLauncher.launch(arrayOf("text/html", "text/plain"))
                        },
                        onExportHtml = {
                            exportBookmarksLauncher.launch("crumia_bookmarks.html")
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(CrumiaRoutes.HISTORY) {
                    HistoryScreen(
                        history = history,
                        closedTabs = closedTabs,
                        onOpenUrl = { url ->
                            viewModel.loadUrlInTab(activeTabId, url)
                            navController.popBackStack()
                        },
                        onRestoreClosedTab = { closed ->
                            viewModel.restoreClosedTab(closed)
                            navController.popBackStack()
                        },
                        onDeleteItem = { id ->
                            scope.launch { viewModel.repo.deleteHistoryItem(id) }
                        },
                        onClearRange = { range -> viewModel.clearHistory(range) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(CrumiaRoutes.DOWNLOADS) {
                    DownloadsScreen(
                        downloads = downloads,
                        onPause = { viewModel.downloadManager.pauseDownload(it) },
                        onResume = {
                            viewModel.downloadManager.resumeDownload(it, prefs.downloadNotificationsEnabled)
                        },
                        onCancel = { viewModel.downloadManager.cancelDownload(it) },
                        onRetry = {
                            viewModel.downloadManager.retryDownload(it, prefs.downloadNotificationsEnabled)
                        },
                        onOpen = { entity ->
                            val intent = viewModel.downloadManager.openDownloadedFile(entity)
                            if (intent != null) {
                                runCatching { context.startActivity(intent) }
                                    .onFailure { viewModel.showToastMessage("No app available to open this file") }
                            } else {
                                viewModel.showToastMessage("File not found")
                            }
                        },
                        onShare = { entity ->
                            val intent = viewModel.downloadManager.shareDownloadedFile(entity)
                            if (intent != null) {
                                context.startActivity(Intent.createChooser(intent, "Share file"))
                            }
                        },
                        onDelete = { viewModel.downloadManager.deleteDownloadRecord(it) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(CrumiaRoutes.AD_SHIELD) {
                    val currentHost = remember(activeTab.url) {
                        AdShieldEngine.normalizeHost(AdShieldEngine.safeHost(activeTab.url))
                    }
                    val siteAllowlisted = remember(currentHost, allowlistedSites) {
                        currentHost != null && allowlistedSites.any { it.host.equals(currentHost, ignoreCase = true) }
                    }
                    AdShieldScreen(
                        prefs = prefs,
                        currentHost = currentHost,
                        isCurrentSiteAllowlisted = siteAllowlisted,
                        allowlistedSites = allowlistedSites,
                        filterRules = filterRules,
                        isUpdatingFilters = isUpdatingFilters,
                        onToggleMaster = { viewModel.setAdShieldEnabled(it) },
                        onToggleAds = { viewModel.setBlockAds(it) },
                        onToggleTrackers = { viewModel.setBlockTrackers(it) },
                        onTogglePopups = { viewModel.setBlockPopups(it) },
                        onToggleCurrentSiteAllowlist = { viewModel.toggleCurrentSiteAllowlist() },
                        onRemoveAllowlistHost = { host ->
                            scope.launch { viewModel.adShield.setSiteAllowlisted(host, false) }
                        },
                        onAddAllowlistHost = { host ->
                            scope.launch { viewModel.adShield.setSiteAllowlisted(host, true) }
                        },
                        onToggleFilterSource = { src, enabled ->
                            when (src) {
                                com.example.data.model.FilterListSource.EASYLIST -> viewModel.setFilterList(easyList = enabled)
                                com.example.data.model.FilterListSource.EASYPRIVACY -> viewModel.setFilterList(easyPrivacy = enabled)
                                com.example.data.model.FilterListSource.ADGUARD -> viewModel.setFilterList(adGuard = enabled)
                                com.example.data.model.FilterListSource.CUSTOM -> Unit
                            }
                        },
                        onManualUpdate = { viewModel.manualUpdateFilterLists() },
                        onAddCustomRule = { pattern, cat -> viewModel.addCustomFilterRule(pattern, cat) },
                        onDeleteRule = { viewModel.deleteFilterRule(it) },
                        onResetSettings = { viewModel.resetAdShield() },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = CrumiaRoutes.SETTINGS_PATTERN,
                    arguments = listOf(
                        navArgument("openAbout") {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    )
                ) { backStackEntry ->
                    val openAbout = backStackEntry.arguments?.getBoolean("openAbout") ?: false
                    SettingsScreen(
                        prefs = prefs,
                        siteSettings = allSiteSettings,
                        openAboutInitially = openAbout,
                        onSetThemeMode = { viewModel.setThemeMode(it) },
                        onSetSearchEngine = { viewModel.setSearchEngine(it) },
                        onSetCustomSearchUrl = { viewModel.setCustomSearchUrl(it) },
                        onSetStartupBehavior = { viewModel.setStartupBehavior(it) },
                        onSetCustomHomepageUrl = { viewModel.setCustomHomepageUrl(it) },
                        onSetHomepageBackground = { viewModel.setHomepageBackground(it) },
                        onSetShowShortcuts = { viewModel.setShowShortcuts(it) },
                        onSetShowRecentSites = { viewModel.setShowRecentSites(it) },
                        onSetShowBookmarks = { viewModel.setShowBookmarks(it) },
                        onSetToolbarPosition = { viewModel.setToolbarPosition(it) },
                        onSetTextZoomPercent = { viewModel.setTextZoomPercent(it) },
                        onSetDownloadNotifications = { viewModel.setDownloadNotificationsEnabled(it) },
                        onSetCookiesEnabled = { viewModel.setCookiesEnabled(it) },
                        onSetThirdPartyCookies = { viewModel.setThirdPartyCookiesEnabled(it) },
                        onSetDoNotTrack = { viewModel.setDoNotTrackEnabled(it) },
                        onSetHttpsUpgrade = { viewModel.setHttpsUpgradeEnabled(it) },
                        onSetSafeBrowsing = { viewModel.setSafeBrowsingEnabled(it) },
                        onSetJavascriptEnabled = { viewModel.setJavascriptEnabled(it) },
                        onSetDesktopModeDefault = { viewModel.setDesktopModeDefault(it) },
                        onClearBrowsingData = { range, hist, cookies, cache, closed ->
                            viewModel.clearBrowsingData(range, hist, cookies, cache, closed)
                        },
                        onResetSiteSettings = {
                            scope.launch {
                                viewModel.repo.resetAllSiteSettings()
                                viewModel.showToastMessage("Site permissions cleared")
                            }
                        },
                        onResetApp = { viewModel.resetApplication() },
                        onOpenAdShield = { navController.navigate(CrumiaRoutes.AD_SHIELD) },
                        onOpenDownloads = { navController.navigate(CrumiaRoutes.DOWNLOADS) },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // Fullscreen Video Overlay
            fullscreenView?.let { (view, _) ->
                BackHandler { viewModel.exitFullscreen() }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = {
                            FrameLayout(it).apply {
                                (view.parent as? ViewGroup)?.removeView(view)
                                addView(
                                    view,
                                    FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (!isInPictureInPicture) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .statusBarsPadding()
                                .padding(16.dp)
                                .clickable { onEnterPictureInPicture() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.PictureInPictureAlt,
                                    contentDescription = "Picture-in-Picture",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PiP", color = Color.White, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // Chrome-style Floating Overflow Menu
            if (menuSheetVisible) {
                MainMenuSheet(
                    tab = activeTab,
                    isBookmarked = isBookmarked,
                    adShieldEnabled = prefs.adShieldEnabled,
                    anchorAtBottom = prefs.toolbarPosition == ToolbarPosition.BOTTOM && !activeTab.isHomepage,
                    onDismiss = { menuSheetVisible = false },
                    onForward = { viewModel.goForward() },
                    onToggleBookmark = { viewModel.toggleBookmarkForActiveTab() },
                    onSaveOffline = { viewModel.saveActivePageForOffline() },
                    onOpenPageInfo = { pageInfoVisible = true },
                    onReloadOrStop = { viewModel.reloadOrStop() },
                    onNewTab = { viewModel.openNewTab(isIncognito = false) },
                    onNewIncognitoTab = { viewModel.openNewTab(isIncognito = true) },
                    onOpenBookmarks = { navController.navigate(CrumiaRoutes.BOOKMARKS) },
                    onOpenRecentTabs = { viewModel.restoreLastClosedTab() },
                    onOpenHistory = { navController.navigate(CrumiaRoutes.HISTORY) },
                    onOpenDownloads = { navController.navigate(CrumiaRoutes.DOWNLOADS) },
                    onShare = {
                        if (activeTab.url.isNotBlank()) shareSheetVisible = true
                    },
                    onFindInPage = { viewModel.openFindInPage() },
                    onTranslatePage = { viewModel.translateActivePage() },
                    onAddToHomepage = { viewModel.addActivePageAsShortcut() },
                    onToggleDesktopSite = { viewModel.toggleDesktopMode() },
                    onPrint = { viewModel.printActivePage() },
                    onOpenExternal = {
                        if (activeTab.url.isNotBlank()) {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(activeTab.url)))
                            }
                        }
                    },
                    onOpenAdShield = { navController.navigate(CrumiaRoutes.AD_SHIELD) },
                    onOpenSettings = { navController.navigate(CrumiaRoutes.settings(openAbout = false)) },
                    onOpenAbout = { navController.navigate(CrumiaRoutes.settings(openAbout = true)) }
                )
            }

            // Chrome-style Page Info / Security Controls Sheet
            if (pageInfoVisible && !activeTab.isHomepage && activeTab.url.isNotBlank()) {
                val currentHost = remember(activeTab.url) {
                    AdShieldEngine.normalizeHost(AdShieldEngine.safeHost(activeTab.url)).orEmpty()
                }
                val siteAllowlisted = remember(currentHost, allowlistedSites) {
                    currentHost.isNotBlank() && allowlistedSites.any { it.host.equals(currentHost, ignoreCase = true) }
                }
                val siteSetting = remember(currentHost, allSiteSettings) {
                    allSiteSettings.firstOrNull { it.host.equals(currentHost, ignoreCase = true) }
                }
                val effectiveJs = siteSetting?.javascriptEnabled ?: prefs.javascriptEnabled

                PageInfoBottomSheet(
                    tab = activeTab,
                    host = currentHost,
                    isAdShieldAllowlisted = siteAllowlisted,
                    isJavascriptEnabled = effectiveJs,
                    onToggleAdShieldSite = { viewModel.toggleCurrentSiteAllowlist() },
                    onToggleDesktopMode = { viewModel.toggleDesktopMode() },
                    onToggleJavascript = { enabled ->
                        if (currentHost.isNotBlank()) viewModel.toggleSiteJavascript(currentHost, enabled)
                    },
                    onResetSiteSettings = {
                        if (currentHost.isNotBlank()) viewModel.clearSiteSettingsForHost(currentHost)
                    },
                    onDismiss = { pageInfoVisible = false }
                )
            }

            // Chrome-style Share Sheet
            if (shareSheetVisible && activeTab.url.isNotBlank()) {
                ChromeShareSheet(
                    title = activeTab.title,
                    url = activeTab.url,
                    onCopyLink = {
                        clipboard.setText(AnnotatedString(activeTab.url))
                        viewModel.showToastMessage("Link copied")
                    },
                    onSaveOffline = { viewModel.saveActivePageForOffline() },
                    onPrintPdf = { viewModel.printActivePage() },
                    onSendToApps = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, activeTab.title)
                            putExtra(Intent.EXTRA_TEXT, activeTab.url)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share link"))
                    },
                    onDismiss = { shareSheetVisible = false }
                )
            }

            // Site Permission Request Dialog (Never auto-grants sensitive permissions!)
            pendingPermission?.let { req ->
                val permSummary = buildList {
                    if (req.needsCamera) add("Camera")
                    if (req.needsMic) add("Microphone")
                    if (req.needsLocation) add("Location")
                }.joinToString(", ")

                AlertDialog(
                    onDismissRequest = { viewModel.resolvePermissionPrompt(false) },
                    title = { Text("Permission Request") },
                    text = {
                        Text("${req.host} is requesting access to: $permSummary")
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.resolvePermissionPrompt(false) }) {
                            Text("Block")
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            val androidPerms = buildList {
                                if (req.needsCamera) add(Manifest.permission.CAMERA)
                                if (req.needsMic) add(Manifest.permission.RECORD_AUDIO)
                                if (req.needsLocation) add(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                            val missing = androidPerms.filter {
                                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                            }
                            if (missing.isEmpty()) {
                                viewModel.resolvePermissionPrompt(true)
                            } else {
                                awaitingPermissionPrompt = req
                                viewModel.deferPermissionPromptToRuntime()
                            }
                        }) {
                            Text("Allow")
                        }
                    }
                )
            }

            // Properly handle runtime permissions if missing without dropping callback
            awaitingPermissionPrompt?.let { req ->
                LaunchedEffect(req) {
                    val androidPerms = buildList {
                        if (req.needsCamera) add(Manifest.permission.CAMERA)
                        if (req.needsMic) add(Manifest.permission.RECORD_AUDIO)
                        if (req.needsLocation) add(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    runtimePermissionsLauncher.launch(androidPerms.toTypedArray())
                }
            }

            // Long-Press Link / Image Context Menu
            contextMenuRequest?.let { req ->
                AlertDialog(
                    onDismissRequest = { viewModel.dismissContextMenu() },
                    title = {
                        Text(
                            text = req.url,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 2
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ContextMenuItem(Icons.Rounded.Add, "Open in new tab") {
                                viewModel.openNewTab(url = req.url, isIncognito = activeTab.isIncognito, selectImmediately = true)
                                viewModel.dismissContextMenu()
                            }
                            ContextMenuItem(Icons.Rounded.Add, "Open in background tab") {
                                viewModel.openNewTab(url = req.url, isIncognito = activeTab.isIncognito, selectImmediately = false)
                                viewModel.showToastMessage("Opened in background tab")
                                viewModel.dismissContextMenu()
                            }
                            ContextMenuItem(Icons.Rounded.VisibilityOff, "Open in Private tab") {
                                viewModel.openNewTab(url = req.url, isIncognito = true, selectImmediately = true)
                                viewModel.dismissContextMenu()
                            }
                            ContextMenuItem(Icons.Rounded.ContentCopy, "Copy link address") {
                                clipboard.setText(AnnotatedString(req.url))
                                viewModel.showToastMessage("Link copied")
                                viewModel.dismissContextMenu()
                            }
                            ContextMenuItem(
                                Icons.Rounded.Download,
                                if (req.isImage) "Download image" else "Download link"
                            ) {
                                viewModel.downloadManager.startDownload(
                                    url = req.url,
                                    showNotifications = prefs.downloadNotificationsEnabled
                                )
                                viewModel.showToastMessage("Downloading…")
                                viewModel.dismissContextMenu()
                            }
                            ContextMenuItem(Icons.Rounded.Share, "Share link") {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, req.url)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share"))
                                viewModel.dismissContextMenu()
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.dismissContextMenu() }) { Text("Cancel") }
                    }
                )
            }

            // Branded Launch Splash Overlay
            SplashOverlay(visible = showSplash)
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
