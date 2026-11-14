package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.db.BookmarkEntity
import com.example.data.db.ClosedTabEntity
import com.example.data.db.HistoryEntity
import com.example.data.db.ShortcutEntity
import com.example.data.model.HomepageBackgroundStyle
import com.example.data.model.SearchEngineOption
import com.example.data.prefs.CrumiaPreferences
import com.example.search.UrlUtils
import com.example.ui.theme.ChromeWarmBackground
import com.example.ui.theme.CrumiaBrandBrush
import com.example.ui.theme.CrumiaCyan
import com.example.ui.theme.CrumiaIndigo
import com.example.ui.theme.ShieldEmerald
import com.example.ui.theme.SpaceGroteskFontFamily

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomepageScreen(
    isIncognito: Boolean,
    prefs: CrumiaPreferences,
    shortcuts: List<ShortcutEntity>,
    recentSites: List<HistoryEntity>,
    bookmarks: List<BookmarkEntity>,
    closedTabs: List<ClosedTabEntity>,
    voiceSearchAvailable: Boolean,
    onOpenOmniboxSearch: () -> Unit,
    onNavigate: (String) -> Unit,
    onSelectSearchEngine: (SearchEngineOption) -> Unit,
    onSelectBackground: (HomepageBackgroundStyle) -> Unit,
    onAddShortcut: (title: String, url: String, accentHex: Long) -> Unit,
    onUpdateShortcut: (ShortcutEntity) -> Unit,
    onDeleteShortcut: (Long) -> Unit,
    onRestoreClosedTab: (ClosedTabEntity) -> Unit,
    onOpenIncognitoTab: () -> Unit,
    onVoiceSearch: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenAdShield: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.4f
    val clipboard = LocalClipboardManager.current
    val backgroundBrush = remember(prefs.homepageBackground, isIncognito, isDark) {
        homepageBackgroundBrush(prefs.homepageBackground, isIncognito, isDark)
    }

    var engineMenuExpanded by remember { mutableStateOf(false) }
    var backgroundDialogVisible by remember { mutableStateOf(false) }
    var editingShortcut by remember { mutableStateOf<ShortcutEntity?>(null) }
    var addShortcutDialogVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .testTag("homepage_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 580.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(22.dp))

                // Centered Crumia Brand Header (Chrome-style centered wordmark + Crumia emblem)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.crumia_logo),
                        contentDescription = stringResource(id = R.string.cd_crumia_logo),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = TextStyle(
                            brush = CrumiaBrandBrush,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 38.sp,
                            letterSpacing = (-0.8).sp
                        )
                    )
                }

                if (isIncognito) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You've gone Private — history is not saved",
                        style = MaterialTheme.typography.labelMedium,
                        color = CrumiaCyan
                    )
                }

                Spacer(modifier = Modifier.height(26.dp))

                // Large Chrome-style Floating Search Pill
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .clickable { onOpenOmniboxSearch() }
                        .testTag("home_search_input"),
                    shape = RoundedCornerShape(30.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tappable Search Engine Badge (G / B / D / Y / C)
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { engineMenuExpanded = true }
                                    .testTag("home_search_engine_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = prefs.searchEngine.displayName.take(1),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            DropdownMenu(
                                expanded = engineMenuExpanded,
                                onDismissRequest = { engineMenuExpanded = false }
                            ) {
                                SearchEngineOption.entries.forEach { engine ->
                                    DropdownMenuItem(
                                        text = { Text(engine.displayName) },
                                        onClick = {
                                            onSelectSearchEngine(engine)
                                            engineMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "Search ${prefs.searchEngine.displayName} or type URL",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (voiceSearchAvailable) {
                            IconButton(
                                onClick = onVoiceSearch,
                                modifier = Modifier.size(40.dp).testTag("home_voice_search_button")
                            ) {
                                Icon(
                                    Icons.Rounded.Mic,
                                    contentDescription = stringResource(id = R.string.cd_voice_search),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        val clipText = clipboard.getText()?.text
                        if (!clipText.isNullOrBlank()) {
                            IconButton(
                                onClick = { onNavigate(clipText) },
                                modifier = Modifier.size(40.dp).testTag("home_paste_go_button")
                            ) {
                                Icon(
                                    Icons.Rounded.ContentPaste,
                                    contentDescription = "Paste and Go",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { backgroundDialogVisible = true },
                                modifier = Modifier.size(40.dp).testTag("home_quick_background")
                            ) {
                                Icon(
                                    Icons.Rounded.Palette,
                                    contentDescription = "Customize background",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Two Chrome-style Tonal Pill Buttons (Ad Shield & Incognito — matching screenshot)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ChromeTonalPillButton(
                        icon = Icons.Rounded.Shield,
                        label = "Ad Shield",
                        iconTint = if (prefs.adShieldEnabled) ShieldEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onOpenAdShield,
                        modifier = Modifier.weight(1f).testTag("home_quick_ad_shield")
                    )
                    ChromeTonalPillButton(
                        icon = Icons.Rounded.VisibilityOff,
                        label = "Incognito",
                        iconTint = MaterialTheme.colorScheme.onSurface,
                        onClick = onOpenIncognitoTab,
                        modifier = Modifier.weight(1f).testTag("home_quick_incognito")
                    )
                }

                // Horizontal Shortcuts Card (Exact match to screenshot's rounded card with horizontal site icons)
                if (prefs.showShortcutsOnHome) {
                    Spacer(modifier = Modifier.height(22.dp))
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(shortcuts, key = { it.id }) { shortcut ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(68.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .combinedClickable(
                                            onClick = { onNavigate(shortcut.url) },
                                            onLongClick = { editingShortcut = shortcut }
                                        )
                                        .padding(vertical = 4.dp)
                                        .testTag("shortcut_item_${shortcut.id}")
                                ) {
                                    val accent = Color(shortcut.accentHex)
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(accent.copy(alpha = 0.18f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = shortcut.title.take(1).uppercase(),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = accent
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = shortcut.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            item {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(68.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { addShortcutDialogVisible = true }
                                        .padding(vertical = 4.dp)
                                        .testTag("add_shortcut_button")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Add,
                                            contentDescription = "Add Shortcut",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Add",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Recently Closed Tabs Quick Restore
                if (!isIncognito && closedTabs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(closedTabs.take(5), key = { it.id }) { closed ->
                            AssistChip(
                                onClick = { onRestoreClosedTab(closed) },
                                label = {
                                    Text(
                                        text = closed.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 130.dp)
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }

                // Chrome-style Rounded Feed Cards for Bookmarks & Recently Visited
                if (prefs.showBookmarksOnHome && bookmarks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.Bookmarks,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Bookmarks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                }
                                TextButton(onClick = onOpenBookmarks) { Text("See all") }
                            }
                            bookmarks.take(3).forEach { bm ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onNavigate(bm.url) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.Bookmark,
                                        contentDescription = null,
                                        tint = CrumiaIndigo,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(bm.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            UrlUtils.extractDisplayHost(bm.url),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (!isIncognito && prefs.showRecentSitesOnHome && recentSites.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.History,
                                        contentDescription = null,
                                        tint = CrumiaCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Recently Visited", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                }
                                TextButton(onClick = onOpenHistory) { Text("History") }
                            }
                            recentSites.take(4).forEach { entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onNavigate(entry.url) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.Public,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(entry.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    if (backgroundDialogVisible) {
        AlertDialog(
            onDismissRequest = { backgroundDialogVisible = false },
            title = { Text("Homepage Background") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomepageBackgroundStyle.entries.forEach { style ->
                        val selected = prefs.homepageBackground == style
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onSelectBackground(style)
                                    backgroundDialogVisible = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(homepageBackgroundBrush(style, false, isDark))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = style.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { backgroundDialogVisible = false }) { Text("Done") }
            }
        )
    }

    if (addShortcutDialogVisible) {
        ShortcutEditorDialog(
            initial = null,
            onDismiss = { addShortcutDialogVisible = false },
            onSave = { title, url, accent ->
                onAddShortcut(title, url, accent)
                addShortcutDialogVisible = false
            },
            onDelete = null
        )
    }

    editingShortcut?.let { existing ->
        ShortcutEditorDialog(
            initial = existing,
            onDismiss = { editingShortcut = null },
            onSave = { title, url, accent ->
                onUpdateShortcut(existing.copy(title = title, url = url, accentHex = accent))
                editingShortcut = null
            },
            onDelete = {
                onDeleteShortcut(existing.id)
                editingShortcut = null
            }
        )
    }
}

@Composable
private fun ChromeTonalPillButton(
    icon: ImageVector,
    label: String,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(23.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(19.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ShortcutEditorDialog(
    initial: ShortcutEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, url: String, accentHex: Long) -> Unit,
    onDelete: (() -> Unit)?
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var url by remember { mutableStateOf(initial?.url ?: "https://") }
    var selectedAccent by remember { mutableLongStateOf(initial?.accentHex ?: 0xFF6366F1) }
    val palette = listOf(0xFF38BDF8L, 0xFF3B82F6L, 0xFF6366F1L, 0xFF8B5CF6L, 0xFFEC4899L, 0xFF10B981L)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (initial == null) "Add Shortcut" else "Edit Shortcut")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("shortcut_title_input")
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Website URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("shortcut_url_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    palette.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    width = if (selectedAccent == hex) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { selectedAccent = hex }
                        )
                    }
                }
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (url.isNotBlank()) {
                        onSave(title.ifBlank { url }, url.trim(), selectedAccent)
                    }
                },
                modifier = Modifier.testTag("save_shortcut_button")
            ) {
                Text("Save")
            }
        }
    )
}

fun homepageBackgroundBrush(
    style: HomepageBackgroundStyle,
    isIncognito: Boolean,
    isDark: Boolean
): Brush {
    if (isIncognito) {
        return Brush.verticalGradient(
            listOf(Color(0xFF191820), Color(0xFF141318))
        )
    }
    return when (style) {
        HomepageBackgroundStyle.CHROME_WARM -> if (isDark) {
            Brush.verticalGradient(listOf(Color(0xFF1A1A1E), Color(0xFF1A1A1E)))
        } else {
            Brush.verticalGradient(listOf(ChromeWarmBackground, ChromeWarmBackground))
        }
        HomepageBackgroundStyle.CHROME_CLEAN -> if (isDark) {
            Brush.verticalGradient(listOf(Color(0xFF1F1F24), Color(0xFF18181C)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFF6F8FC), Color(0xFFF1F3F4)))
        }
        HomepageBackgroundStyle.COSMIC_GRADIENT -> if (isDark) {
            Brush.verticalGradient(listOf(Color(0xFF171C3A), Color(0xFF0F1328), Color(0xFF0B0D17)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFE0F2FE), Color(0xFFEEF2FF), Color(0xFFF6F8FC)))
        }
        HomepageBackgroundStyle.AURORA_CYAN -> if (isDark) {
            Brush.verticalGradient(listOf(Color(0xFF0B2A3A), Color(0xFF0D1B2A), Color(0xFF0B0D17)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFCFFAFE), Color(0xFFE0F2FE), Color(0xFFF8FAFC)))
        }
        HomepageBackgroundStyle.VIOLET_NEBULA -> if (isDark) {
            Brush.verticalGradient(listOf(Color(0xFF28174A), Color(0xFF15122B), Color(0xFF0B0D17)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFF3E8FF), Color(0xFFEDE9FE), Color(0xFFF8FAFC)))
        }
        HomepageBackgroundStyle.MIDNIGHT_OLED -> {
            Brush.verticalGradient(listOf(Color(0xFF05060A), Color(0xFF000000)))
        }
        HomepageBackgroundStyle.MINIMAL_CLEAN -> if (isDark) {
            Brush.verticalGradient(listOf(Color(0xFF121526), Color(0xFF121526)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9)))
        }
    }
}
