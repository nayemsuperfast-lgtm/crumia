package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.db.SiteSettingEntity
import com.example.data.model.HistoryRange
import com.example.data.model.HomepageBackgroundStyle
import com.example.data.model.SearchEngineOption
import com.example.data.model.StartupBehavior
import com.example.data.model.ThemeMode
import com.example.data.model.ToolbarPosition
import com.example.data.prefs.CrumiaPreferences
import com.example.ui.components.ClearBrowsingDataDialog
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.ShieldEmerald
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: CrumiaPreferences,
    siteSettings: List<SiteSettingEntity>,
    openAboutInitially: Boolean = false,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetSearchEngine: (SearchEngineOption) -> Unit,
    onSetCustomSearchUrl: (String) -> Unit,
    onSetStartupBehavior: (StartupBehavior) -> Unit,
    onSetCustomHomepageUrl: (String) -> Unit,
    onSetHomepageBackground: (HomepageBackgroundStyle) -> Unit,
    onSetShowShortcuts: (Boolean) -> Unit,
    onSetShowRecentSites: (Boolean) -> Unit,
    onSetShowBookmarks: (Boolean) -> Unit,
    onSetToolbarPosition: (ToolbarPosition) -> Unit,
    onSetTextZoomPercent: (Int) -> Unit,
    onSetDownloadNotifications: (Boolean) -> Unit,
    onSetCookiesEnabled: (Boolean) -> Unit,
    onSetThirdPartyCookies: (Boolean) -> Unit,
    onSetDoNotTrack: (Boolean) -> Unit,
    onSetHttpsUpgrade: (Boolean) -> Unit,
    onSetSafeBrowsing: (Boolean) -> Unit,
    onSetJavascriptEnabled: (Boolean) -> Unit,
    onSetDesktopModeDefault: (Boolean) -> Unit,
    onClearBrowsingData: (HistoryRange, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onResetSiteSettings: () -> Unit,
    onResetApp: () -> Unit,
    onOpenAdShield: () -> Unit,
    onOpenDownloads: () -> Unit,
    onBack: () -> Unit
) {
    var showSearchEngineDialog by remember { mutableStateOf(false) }
    var showCustomSearchDialog by remember { mutableStateOf(false) }
    var showStartupDialog by remember { mutableStateOf(false) }
    var showHomepageUrlDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showBackgroundDialog by remember { mutableStateOf(false) }
    var showToolbarDialog by remember { mutableStateOf(false) }
    var showTextZoomDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showSiteSettingsDialog by remember { mutableStateOf(false) }
    var showResetAppDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(openAboutInitially) }

    Scaffold(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // GENERAL
            item { SectionHeader("GENERAL") }
            item {
                SettingsGroup {
                    SettingNavItem(
                        icon = Icons.Rounded.Search,
                        title = "Default Search Engine",
                        subtitle = prefs.searchEngine.displayName,
                        onClick = { showSearchEngineDialog = true },
                        tag = "setting_search_engine"
                    )
                    if (prefs.searchEngine == SearchEngineOption.CUSTOM) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingNavItem(
                            icon = Icons.Rounded.Tune,
                            title = "Custom Search URL",
                            subtitle = prefs.customSearchUrl,
                            onClick = { showCustomSearchDialog = true },
                            tag = "setting_custom_search_url"
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingNavItem(
                        icon = Icons.Rounded.Home,
                        title = "Startup Behavior",
                        subtitle = prefs.startupBehavior.label,
                        onClick = { showStartupDialog = true },
                        tag = "setting_startup"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingNavItem(
                        icon = Icons.Rounded.Home,
                        title = "Custom Startup / Home URL",
                        subtitle = prefs.customHomepageUrl,
                        onClick = { showHomepageUrlDialog = true },
                        tag = "setting_custom_homepage"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingSwitchItem(
                        icon = Icons.Rounded.Notifications,
                        title = "Download Notifications",
                        subtitle = "Show progress and completion alerts",
                        checked = prefs.downloadNotificationsEnabled,
                        onCheckedChange = onSetDownloadNotifications,
                        tag = "setting_download_notifs"
                    )
                }
            }

            // APPEARANCE
            item { SectionHeader("APPEARANCE") }
            item {
                SettingsGroup {
                    SettingNavItem(
                        icon = Icons.Rounded.Palette,
                        title = "Theme",
                        subtitle = prefs.themeMode.label,
                        onClick = { showThemeDialog = true },
                        tag = "setting_theme"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingNavItem(
                        icon = Icons.Rounded.Palette,
                        title = "Homepage Background",
                        subtitle = prefs.homepageBackground.label,
                        onClick = { showBackgroundDialog = true },
                        tag = "setting_homepage_bg"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingNavItem(
                        icon = Icons.Rounded.ViewAgenda,
                        title = "Address Bar Position",
                        subtitle = prefs.toolbarPosition.label,
                        onClick = { showToolbarDialog = true },
                        tag = "setting_toolbar_position"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingNavItem(
                        icon = Icons.Rounded.FormatSize,
                        title = "Text Scaling & Accessibility",
                        subtitle = "${prefs.textZoomPercent}% page text size",
                        onClick = { showTextZoomDialog = true },
                        tag = "setting_text_zoom"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingSwitchItem(
                        icon = Icons.Rounded.Tune,
                        title = "Show Shortcuts on Homepage",
                        subtitle = "Display customizable quick-launch grid",
                        checked = prefs.showShortcutsOnHome,
                        onCheckedChange = onSetShowShortcuts,
                        tag = "setting_show_shortcuts"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingSwitchItem(
                        icon = Icons.Rounded.History,
                        title = "Show Recently Visited",
                        subtitle = "Show recent websites on homepage",
                        checked = prefs.showRecentSitesOnHome,
                        onCheckedChange = onSetShowRecentSites,
                        tag = "setting_show_recent"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingSwitchItem(
                        icon = Icons.Rounded.Tune,
                        title = "Show Bookmarks on Homepage",
                        subtitle = "Show quick bookmark strip on homepage",
                        checked = prefs.showBookmarksOnHome,
                        onCheckedChange = onSetShowBookmarks,
                        tag = "setting_show_bookmarks"
                    )
                }
            }

            // PRIVACY
            item { SectionHeader("PRIVACY") }
            item {
                SettingsGroup {
                    SettingSwitchItem(
                        icon = Icons.Rounded.Cookie,
                        title = "Allow Cookies",
                        subtitle = "Sites use cookies to keep you signed in",
                        checked = prefs.cookiesEnabled,
                        onCheckedChange = onSetCookiesEnabled,
                        tag = "setting_cookies"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingSwitchItem(
                        icon = Icons.Rounded.Cookie,
                        title = "Third-Party Cookies",
                        subtitle = "Disabled by default for stronger tracking protection",
                        checked = prefs.thirdPartyCookiesEnabled,
                        onCheckedChange = onSetThirdPartyCookies,
                        tag = "setting_third_party_cookies"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingSwitchItem(
                        icon = Icons.Rounded.Security,
                        title = "Do Not Track",
                        subtitle = "Prefer privacy-preserving browsing defaults",
                        checked = prefs.doNotTrackEnabled,
                        onCheckedChange = onSetDoNotTrack,
                        tag = "setting_dnt"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingNavItem(
                        icon = Icons.Rounded.DeleteForever,
                        title = "Delete Browsing Data",
                        subtitle = "History, cookies, site data & cached files",
                        onClick = { showClearDataDialog = true },
                        tag = "setting_clear_browsing_data"
                    )
                }
            }

            // SECURITY
            item { SectionHeader("SECURITY") }
            item {
                SettingsGroup {
                    SettingSwitchItem(
                        icon = Icons.Rounded.Lock,
                        title = "Automatic HTTPS Upgrade",
                        subtitle = "Prefer encrypted HTTPS when entering domains",
                        checked = prefs.httpsUpgradeEnabled,
                        onCheckedChange = onSetHttpsUpgrade,
                        tag = "setting_https_upgrade"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingSwitchItem(
                        icon = Icons.Rounded.Security,
                        title = "Safe Browsing",
                        subtitle = "Warn against known phishing & malware pages",
                        checked = prefs.safeBrowsingEnabled,
                        onCheckedChange = onSetSafeBrowsing,
                        tag = "setting_safe_browsing"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingNavItem(
                        icon = Icons.Rounded.Tune,
                        title = "Website Permissions & Overrides",
                        subtitle = "${siteSettings.size} site records stored locally",
                        onClick = { showSiteSettingsDialog = true },
                        tag = "setting_site_permissions"
                    )
                }
            }

            // AD SHIELD
            item { SectionHeader("AD SHIELD") }
            item {
                SettingsGroup {
                    SettingNavItem(
                        icon = Icons.Rounded.Shield,
                        title = "Crumia Ad Shield Dashboard",
                        subtitle = if (prefs.adShieldEnabled) {
                            "Active • ${prefs.totalAdsBlocked + prefs.totalTrackersBlocked} blocked"
                        } else {
                            "Paused • Tap to configure"
                        },
                        iconTint = if (prefs.adShieldEnabled) ShieldEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onOpenAdShield,
                        tag = "setting_open_ad_shield"
                    )
                }
            }

            // ADVANCED
            item { SectionHeader("ADVANCED") }
            item {
                SettingsGroup {
                    SettingSwitchItem(
                        icon = Icons.Rounded.DesktopWindows,
                        title = "Desktop Mode by Default",
                        subtitle = "Request desktop layout on new tabs",
                        checked = prefs.desktopModeDefault,
                        onCheckedChange = onSetDesktopModeDefault,
                        tag = "setting_desktop_default"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingSwitchItem(
                        icon = Icons.Rounded.Code,
                        title = "Enable JavaScript",
                        subtitle = "Required by most modern interactive websites",
                        checked = prefs.javascriptEnabled,
                        onCheckedChange = onSetJavascriptEnabled,
                        tag = "setting_javascript"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingNavItem(
                        icon = Icons.Rounded.Download,
                        title = "Downloads Manager",
                        subtitle = "View active and completed downloads",
                        onClick = onOpenDownloads,
                        tag = "setting_downloads"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingNavItem(
                        icon = Icons.Rounded.RestartAlt,
                        title = "Reset Application",
                        subtitle = "Restore default preferences, clear history & cookies",
                        iconTint = DangerCrimson,
                        onClick = { showResetAppDialog = true },
                        tag = "setting_reset_app"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingNavItem(
                        icon = Icons.Rounded.Info,
                        title = "About Crumia",
                        subtitle = "Version 1.0.0 • Android System WebView",
                        onClick = { showAboutDialog = true },
                        tag = "setting_about"
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }

    if (showSearchEngineDialog) {
        SingleChoiceDialog(
            title = "Default Search Engine",
            options = SearchEngineOption.entries,
            selected = prefs.searchEngine,
            labelOf = { it.displayName },
            onSelect = {
                onSetSearchEngine(it)
                showSearchEngineDialog = false
                if (it == SearchEngineOption.CUSTOM) showCustomSearchDialog = true
            },
            onDismiss = { showSearchEngineDialog = false }
        )
    }

    if (showCustomSearchDialog) {
        TextEditDialog(
            title = "Custom Search URL",
            label = "Use {query} placeholder",
            initial = prefs.customSearchUrl,
            onSave = {
                onSetCustomSearchUrl(it)
                showCustomSearchDialog = false
            },
            onDismiss = { showCustomSearchDialog = false }
        )
    }

    if (showStartupDialog) {
        SingleChoiceDialog(
            title = "Startup Behavior",
            options = StartupBehavior.entries,
            selected = prefs.startupBehavior,
            labelOf = { it.label },
            onSelect = {
                onSetStartupBehavior(it)
                showStartupDialog = false
            },
            onDismiss = { showStartupDialog = false }
        )
    }

    if (showHomepageUrlDialog) {
        TextEditDialog(
            title = "Custom Startup URL",
            label = "Website URL",
            initial = prefs.customHomepageUrl,
            onSave = {
                onSetCustomHomepageUrl(it)
                showHomepageUrlDialog = false
            },
            onDismiss = { showHomepageUrlDialog = false }
        )
    }

    if (showThemeDialog) {
        SingleChoiceDialog(
            title = "Theme",
            options = ThemeMode.entries,
            selected = prefs.themeMode,
            labelOf = { it.label },
            onSelect = {
                onSetThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showBackgroundDialog) {
        SingleChoiceDialog(
            title = "Homepage Background",
            options = HomepageBackgroundStyle.entries,
            selected = prefs.homepageBackground,
            labelOf = { it.label },
            onSelect = {
                onSetHomepageBackground(it)
                showBackgroundDialog = false
            },
            onDismiss = { showBackgroundDialog = false }
        )
    }

    if (showToolbarDialog) {
        SingleChoiceDialog(
            title = "Address Bar Position",
            options = ToolbarPosition.entries,
            selected = prefs.toolbarPosition,
            labelOf = { it.label },
            onSelect = {
                onSetToolbarPosition(it)
                showToolbarDialog = false
            },
            onDismiss = { showToolbarDialog = false }
        )
    }

    if (showTextZoomDialog) {
        var sliderPercent by remember { mutableFloatStateOf(prefs.textZoomPercent.toFloat()) }
        AlertDialog(
            onDismissRequest = { showTextZoomDialog = false },
            title = { Text("Text Scaling (${sliderPercent.roundToInt()}%)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "The quick brown fox jumps over the lazy dog.",
                        fontSize = (14f * (sliderPercent / 100f)).sp
                    )
                    Slider(
                        value = sliderPercent,
                        onValueChange = { sliderPercent = it },
                        valueRange = 75f..150f,
                        steps = 14
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onSetTextZoomPercent(100)
                    showTextZoomDialog = false
                }) { Text("Reset 100%") }
            },
            confirmButton = {
                Button(onClick = {
                    onSetTextZoomPercent(sliderPercent.roundToInt())
                    showTextZoomDialog = false
                }) { Text("Apply") }
            }
        )
    }

    if (showClearDataDialog) {
        ClearBrowsingDataDialog(
            onConfirm = onClearBrowsingData,
            onDismiss = { showClearDataDialog = false }
        )
    }

    if (showSiteSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSiteSettingsDialog = false },
            title = { Text("Site Settings") },
            text = {
                if (siteSettings.isEmpty()) {
                    Text("No custom per-site overrides saved.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        siteSettings.take(12).forEach { s ->
                            Text(
                                text = "${s.host}: " + buildList {
                                    if (s.adShieldAllowlisted) add("Allowlisted")
                                    if (s.desktopMode == true) add("Desktop")
                                    if (s.javascriptEnabled == false) add("JS Off")
                                }.ifEmpty { listOf("Default") }.joinToString(", "),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            dismissButton = {
                if (siteSettings.isNotEmpty()) {
                    TextButton(onClick = {
                        onResetSiteSettings()
                        showSiteSettingsDialog = false
                    }) { Text("Clear All", color = DangerCrimson) }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSiteSettingsDialog = false }) { Text("Close") }
            }
        )
    }

    if (showResetAppDialog) {
        AlertDialog(
            onDismissRequest = { showResetAppDialog = false },
            title = { Text("Reset Crumia?") },
            text = { Text("This clears browsing history, cookies, cache, site permissions, and resets all preferences.") },
            dismissButton = { TextButton(onClick = { showResetAppDialog = false }) { Text("Cancel") } },
            confirmButton = {
                Button(onClick = {
                    showResetAppDialog = false
                    onResetApp()
                }) { Text("Reset") }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.crumia_logo),
                        contentDescription = stringResource(id = R.string.cd_crumia_logo),
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(id = R.string.app_name), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Version 1.0.0", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "Engine: Android System WebView (Chromium-based)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Crumia is a fast, local-first web browser with zero ads, zero monetization SDKs, zero analytics, and built-in Ad Shield protection.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Note on Android System WebView: Private tabs erase history, cookies, and storage when closed; however, Android System WebView shares a single process profile across tabs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Done") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SettingNavItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun <T> SingleChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelect(option) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = option == selected, onClick = { onSelect(option) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(labelOf(option), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun TextEditDialog(
    title: String,
    label: String,
    initial: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            Button(onClick = { if (value.isNotBlank()) onSave(value.trim()) }) { Text("Save") }
        }
    )
}
