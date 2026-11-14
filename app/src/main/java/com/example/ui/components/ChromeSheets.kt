package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.HistoryRange
import com.example.tabs.BrowserTab
import com.example.ui.theme.SecurityAmber
import com.example.ui.theme.ShieldEmerald

/**
 * Chrome-style Page Info & Site Controls sheet triggered by tapping the Lock/Warning icon in the address bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageInfoBottomSheet(
    tab: BrowserTab,
    host: String,
    isAdShieldAllowlisted: Boolean,
    isJavascriptEnabled: Boolean,
    onToggleAdShieldSite: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onToggleJavascript: (Boolean) -> Unit,
    onResetSiteSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("page_info_sheet")
        ) {
            Text(
                text = host.ifBlank { tab.url },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Connection security status row
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (tab.isSecure) {
                    ShieldEmerald.copy(alpha = 0.12f)
                } else {
                    SecurityAmber.copy(alpha = 0.14f)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (tab.isSecure) Icons.Rounded.Lock else Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = if (tab.isSecure) ShieldEmerald else SecurityAmber
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (tab.isSecure) "Connection is secure" else "Connection is not secure",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (tab.isSecure) ShieldEmerald else SecurityAmber
                        )
                        Text(
                            text = if (tab.isSecure) {
                                "Information you send or receive on this site is encrypted via HTTPS."
                            } else {
                                "Do not enter sensitive information (passwords or cards) on this page."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()

            // Site Ad Shield Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Shield, contentDescription = null, tint = ShieldEmerald)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Crumia Ad Shield", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(
                        text = if (isAdShieldAllowlisted) "Paused for $host" else "Active • ${tab.blockedOnPage} blocked",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = !isAdShieldAllowlisted,
                    onCheckedChange = { onToggleAdShieldSite() },
                    modifier = Modifier.testTag("page_info_ad_shield_switch")
                )
            }

            HorizontalDivider()

            // Site JavaScript Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Code, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("JavaScript", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(
                        text = if (isJavascriptEnabled) "Allowed on this site" else "Blocked on this site",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isJavascriptEnabled,
                    onCheckedChange = onToggleJavascript,
                    modifier = Modifier.testTag("page_info_js_switch")
                )
            }

            HorizontalDivider()

            // Site Desktop Mode Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.DesktopWindows, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Desktop site", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(
                        text = if (tab.isDesktopMode) "Using desktop layout" else "Using mobile layout",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = tab.isDesktopMode,
                    onCheckedChange = { onToggleDesktopMode() },
                    modifier = Modifier.testTag("page_info_desktop_switch")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        onResetSiteSettings()
                        onDismiss()
                    },
                    modifier = Modifier.testTag("page_info_reset_button")
                ) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset permissions")
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

/**
 * Chrome-style Share Sheet offering Copy link, Save Offline, Print / PDF, and Android System Share.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChromeShareSheet(
    title: String,
    url: String,
    onCopyLink: () -> Unit,
    onSaveOffline: () -> Unit,
    onPrintPdf: () -> Unit,
    onSendToApps: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .testTag("chrome_share_sheet")
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = title.ifBlank { "Webpage" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShareActionTile(Icons.Rounded.ContentCopy, "Copy link") {
                    onCopyLink(); onDismiss()
                }
                ShareActionTile(Icons.Rounded.OfflinePin, "Save offline") {
                    onSaveOffline(); onDismiss()
                }
                ShareActionTile(Icons.Rounded.Print, "Print / PDF") {
                    onPrintPdf(); onDismiss()
                }
                ShareActionTile(Icons.Rounded.Share, "More apps") {
                    onSendToApps(); onDismiss()
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun ShareActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(50.dp)
        ) {
            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

/**
 * Chrome-style Clear Browsing Data dialog with time range + multi-item checkboxes.
 */
@Composable
fun ClearBrowsingDataDialog(
    onConfirm: (range: HistoryRange, history: Boolean, cookies: Boolean, cache: Boolean, closedTabs: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRange by remember { mutableStateOf(HistoryRange.ALL_TIME) }
    var clearHistory by remember { mutableStateOf(true) }
    var clearCookies by remember { mutableStateOf(true) }
    var clearCache by remember { mutableStateOf(true) }
    var clearClosedTabs by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete browsing data") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Time range", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(HistoryRange.entries) { range ->
                        FilterChip(
                            selected = selectedRange == range,
                            onClick = { selectedRange = range },
                            label = { Text(range.label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                ClearCheckRow("Browsing history", clearHistory) { clearHistory = it }
                ClearCheckRow("Cookies and site data", clearCookies) { clearCookies = it }
                ClearCheckRow("Cached images and files", clearCache) { clearCache = it }
                ClearCheckRow("Recently closed tabs", clearClosedTabs) { clearClosedTabs = it }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        confirmButton = {
            Button(
                enabled = clearHistory || clearCookies || clearCache || clearClosedTabs,
                onClick = {
                    onConfirm(selectedRange, clearHistory, clearCookies, clearCache, clearClosedTabs)
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_clear_browsing_data")
            ) {
                Text("Delete data")
            }
        }
    )
}

@Composable
private fun ClearCheckRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
