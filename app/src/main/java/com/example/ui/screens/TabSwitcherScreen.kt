package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.db.ClosedTabEntity
import com.example.search.UrlUtils
import com.example.tabs.BrowserTab
import com.example.ui.theme.CrumiaCyan
import com.example.ui.theme.CrumiaSubtleBrush
import com.example.ui.theme.CrumiaViolet
import com.example.ui.theme.DangerCrimson
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabSwitcherScreen(
    tabs: List<BrowserTab>,
    activeTabId: String,
    closedTabs: List<ClosedTabEntity>,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onDuplicateTab: (String) -> Unit,
    onNewTab: (isIncognito: Boolean) -> Unit,
    onCloseAllTabs: (incognitoOnly: Boolean) -> Unit,
    onRestoreClosedTab: (ClosedTabEntity) -> Unit,
    onRestoreLastClosed: () -> Unit,
    onBack: () -> Unit
) {
    val activeTab = tabs.firstOrNull { it.id == activeTabId }
    var viewingPrivate by remember { mutableStateOf(activeTab?.isIncognito == true) }
    var confirmCloseAll by remember { mutableStateOf(false) }
    var overflowExpanded by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val normalTabs = tabs.filter { !it.isIncognito }
    val privateTabs = tabs.filter { it.isIncognito }
    val shownTabs = if (viewingPrivate) privateTabs else normalTabs

    val handleTabClose: (BrowserTab) -> Unit = { closingTab ->
        onCloseTab(closingTab.id)
        if (!closingTab.isIncognito && !closingTab.isHomepage && closingTab.url.isNotBlank()) {
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val res = snackbarHostState.showSnackbar(
                    message = "Closed ${closingTab.title.ifBlank { "tab" }}",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short
                )
                if (res == SnackbarResult.ActionPerformed) {
                    onRestoreLastClosed()
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("tabs_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text(
                        text = if (viewingPrivate) "Private Tabs" else "Tabs",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { onNewTab(viewingPrivate) },
                        modifier = Modifier.testTag("tabs_header_new_tab")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "New tab")
                    }
                    if (shownTabs.isNotEmpty()) {
                        IconButton(
                            onClick = { confirmCloseAll = true },
                            modifier = Modifier.testTag("tabs_close_all_button")
                        ) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = "Close all tabs")
                        }
                    }
                    Box {
                        IconButton(onClick = { overflowExpanded = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "More tab options")
                        }
                        DropdownMenu(
                            expanded = overflowExpanded,
                            onDismissRequest = { overflowExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("New tab") },
                                leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                                onClick = {
                                    overflowExpanded = false
                                    onNewTab(false)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("New Private tab") },
                                leadingIcon = { Icon(Icons.Rounded.VisibilityOff, contentDescription = null) },
                                onClick = {
                                    overflowExpanded = false
                                    onNewTab(true)
                                }
                            )
                            if (closedTabs.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Reopen closed tab") },
                                    leadingIcon = { Icon(Icons.Rounded.Restore, contentDescription = null) },
                                    onClick = {
                                        overflowExpanded = false
                                        onRestoreLastClosed()
                                    }
                                )
                            }
                            if (shownTabs.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(if (viewingPrivate) "Close all Private tabs" else "Close all tabs") },
                                    leadingIcon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null) },
                                    onClick = {
                                        overflowExpanded = false
                                        confirmCloseAll = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onNewTab(false) },
                        modifier = Modifier.weight(1f).testTag("new_normal_tab_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Tab")
                    }
                    FilledTonalButton(
                        onClick = { onNewTab(true) },
                        modifier = Modifier.weight(1f).testTag("new_private_tab_button")
                    ) {
                        Icon(Icons.Rounded.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Private Tab")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp)
        ) {
            // Chrome-style Normal vs Private Segmented Switcher
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !viewingPrivate,
                    onClick = { viewingPrivate = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("segment_normal_tabs")
                ) {
                    Text("Tabs (${normalTabs.size})")
                }
                SegmentedButton(
                    selected = viewingPrivate,
                    onClick = { viewingPrivate = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("segment_private_tabs")
                ) {
                    Text("Private (${privateTabs.size})")
                }
            }

            if (!viewingPrivate && closedTabs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(closedTabs.take(5), key = { it.id }) { closed ->
                        AssistChip(
                            onClick = { onRestoreClosedTab(closed) },
                            label = {
                                Text(
                                    text = "Restore: ${closed.title}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (shownTabs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (viewingPrivate) Icons.Rounded.VisibilityOff else Icons.Rounded.Public,
                            contentDescription = null,
                            tint = CrumiaViolet,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (viewingPrivate) "No Private Tabs Open" else "No Tabs Open",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (viewingPrivate) {
                                "Private tabs never save history or cookies after closing."
                            } else {
                                "Tap New Tab below to start browsing."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 156.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(shownTabs, key = { it.id }) { tab ->
                        val isActive = tab.id == activeTabId
                        val dismissState = rememberSwipeToDismissBoxState()

                        LaunchedEffect(dismissState.currentValue) {
                            if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                                handleTabClose(tab)
                            }
                        }

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(DangerCrimson.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Swipe to close tab",
                                        tint = DangerCrimson
                                    )
                                }
                            }
                        ) {
                            TabGridCard(
                                tab = tab,
                                isActive = isActive,
                                onSelect = { onSelectTab(tab.id) },
                                onDuplicate = { onDuplicateTab(tab.id) },
                                onClose = { handleTabClose(tab) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmCloseAll) {
        AlertDialog(
            onDismissRequest = { confirmCloseAll = false },
            title = { Text(if (viewingPrivate) "Close All Private Tabs?" else "Close All Tabs?") },
            text = {
                Text(
                    if (viewingPrivate) {
                        "All private tabs and private browsing cookies will be wiped immediately."
                    } else {
                        "All open tabs will be closed and added to Recently Closed."
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { confirmCloseAll = false }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmCloseAll = false
                        onCloseAllTabs(viewingPrivate)
                    }
                ) { Text("Close All") }
            }
        )
    }
}

@Composable
private fun TabGridCard(
    tab: BrowserTab,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDuplicate: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isActive) 6.dp else 2.dp,
        border = BorderStroke(
            width = if (isActive) 2.dp else 1.dp,
            color = if (isActive) CrumiaCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onSelect)
            .testTag("tab_card_${tab.id}")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (tab.isIncognito) CrumiaViolet.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    )
                    .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (tab.isIncognito) {
                    Icon(
                        Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                        tint = CrumiaCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = tab.title.ifBlank { "New Tab" },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDuplicate,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Rounded.ContentCopy,
                        contentDescription = "Duplicate tab",
                        modifier = Modifier.size(14.dp)
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close tab",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.85f)
                    .background(CrumiaSubtleBrush),
                contentAlignment = Alignment.Center
            ) {
                val bmp = tab.thumbnail
                if (bmp != null && !tab.isHomepage) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = tab.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (tab.isHomepage) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.crumia_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (tab.isIncognito) "Private Home" else "Homepage",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = UrlUtils.extractDisplayHost(tab.url).take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = UrlUtils.extractDisplayHost(tab.url),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
