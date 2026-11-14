package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AddToHomeScreen
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FindInPage
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.tabs.BrowserTab
import com.example.ui.theme.ShieldEmerald

/**
 * Chrome-exact floating Overflow Menu anchored to top-right (or bottom-right when toolbar is at bottom),
 * featuring the 5-icon top action strip (Forward, Bookmark, Save Offline, Page Info, Reload/Stop)
 * followed by Chrome's standard menu list.
 */
@Composable
fun MainMenuSheet(
    tab: BrowserTab,
    isBookmarked: Boolean,
    adShieldEnabled: Boolean,
    anchorAtBottom: Boolean,
    onDismiss: () -> Unit,
    onForward: () -> Unit,
    onToggleBookmark: () -> Unit,
    onSaveOffline: () -> Unit,
    onOpenPageInfo: () -> Unit,
    onReloadOrStop: () -> Unit,
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenRecentTabs: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
    onShare: () -> Unit,
    onFindInPage: () -> Unit,
    onTranslatePage: () -> Unit,
    onAddToHomepage: () -> Unit,
    onToggleDesktopSite: () -> Unit,
    onPrint: () -> Unit,
    onOpenExternal: () -> Unit,
    onOpenAdShield: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .padding(horizontal = 10.dp, vertical = 10.dp),
            contentAlignment = if (anchorAtBottom) Alignment.BottomEnd else Alignment.TopEnd
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .width(284.dp)
                    .heightIn(max = 620.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .testTag("chrome_overflow_menu")
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    // Chrome 5-icon top action row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onForward(); onDismiss() },
                            enabled = tab.canGoForward,
                            modifier = Modifier.size(44.dp).testTag("menu_forward_action")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "Forward"
                            )
                        }
                        IconButton(
                            onClick = { onToggleBookmark(); onDismiss() },
                            enabled = !tab.isHomepage && tab.url.isNotBlank(),
                            modifier = Modifier.size(44.dp).testTag("menu_bookmark_action")
                        ) {
                            Icon(
                                if (isBookmarked) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { onSaveOffline(); onDismiss() },
                            enabled = !tab.isHomepage && tab.url.isNotBlank(),
                            modifier = Modifier.size(44.dp).testTag("menu_save_offline_action")
                        ) {
                            Icon(
                                Icons.Rounded.OfflinePin,
                                contentDescription = "Save Offline"
                            )
                        }
                        IconButton(
                            onClick = { onOpenPageInfo(); onDismiss() },
                            enabled = !tab.isHomepage && tab.url.isNotBlank(),
                            modifier = Modifier.size(44.dp).testTag("menu_page_info_action")
                        ) {
                            Icon(
                                Icons.Rounded.Info,
                                contentDescription = "Site Info"
                            )
                        }
                        IconButton(
                            onClick = { onReloadOrStop(); onDismiss() },
                            enabled = !tab.isHomepage,
                            modifier = Modifier.size(44.dp).testTag("menu_reload_action")
                        ) {
                            Icon(
                                if (tab.isLoading) Icons.Rounded.Close else Icons.Rounded.Refresh,
                                contentDescription = if (tab.isLoading) "Stop" else "Reload"
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    MenuRow(Icons.Rounded.Add, "New tab", tag = "menu_new_tab") {
                        onNewTab(); onDismiss()
                    }
                    MenuRow(Icons.Rounded.VisibilityOff, "New Incognito tab", tag = "menu_new_incognito") {
                        onNewIncognitoTab(); onDismiss()
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    MenuRow(Icons.Rounded.Bookmarks, "Bookmarks & Reading list", tag = "menu_bookmarks") {
                        onOpenBookmarks(); onDismiss()
                    }
                    MenuRow(Icons.Rounded.Restore, "Recent tabs", tag = "menu_recent_tabs") {
                        onOpenRecentTabs(); onDismiss()
                    }
                    MenuRow(Icons.Rounded.History, "History", tag = "menu_history") {
                        onOpenHistory(); onDismiss()
                    }
                    MenuRow(Icons.Rounded.Download, "Downloads", tag = "menu_downloads") {
                        onOpenDownloads(); onDismiss()
                    }

                    if (!tab.isHomepage && tab.url.isNotBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        MenuRow(Icons.Rounded.Share, "Share...", tag = "menu_share_action") {
                            onShare(); onDismiss()
                        }
                        MenuRow(Icons.Rounded.FindInPage, "Find in page", tag = "menu_find_in_page") {
                            onFindInPage(); onDismiss()
                        }
                        MenuRow(Icons.Rounded.Translate, "Translate...", tag = "menu_translate") {
                            onTranslatePage(); onDismiss()
                        }
                        MenuRow(Icons.AutoMirrored.Rounded.AddToHomeScreen, "Add to Homepage", tag = "menu_add_homepage") {
                            onAddToHomepage(); onDismiss()
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleDesktopSite(); onDismiss() }
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .testTag("menu_desktop_site"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.DesktopWindows,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(21.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Desktop site",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Checkbox(
                                checked = tab.isDesktopMode,
                                onCheckedChange = { onToggleDesktopSite(); onDismiss() }
                            )
                        }
                        MenuRow(Icons.Rounded.Print, "Print / Save PDF", tag = "menu_print_action") {
                            onPrint(); onDismiss()
                        }
                        MenuRow(Icons.AutoMirrored.Rounded.OpenInNew, "Open in external app", tag = "menu_open_external") {
                            onOpenExternal(); onDismiss()
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    MenuRow(
                        icon = Icons.Rounded.Shield,
                        title = "Ad Shield",
                        subtitle = if (adShieldEnabled) "Active (${tab.blockedOnPage} on page)" else "Paused",
                        iconTint = if (adShieldEnabled) ShieldEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        tag = "menu_ad_shield"
                    ) {
                        onOpenAdShield(); onDismiss()
                    }
                    MenuRow(Icons.Rounded.Settings, "Settings", tag = "menu_settings") {
                        onOpenSettings(); onDismiss()
                    }
                    MenuRow(Icons.Rounded.Info, "About Crumia", tag = "menu_about") {
                        onOpenAbout(); onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(21.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
