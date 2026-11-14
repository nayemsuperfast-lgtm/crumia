package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.db.BookmarkEntity
import com.example.ui.theme.ShieldEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    bookmarks: List<BookmarkEntity>,
    readingList: List<BookmarkEntity>,
    onOpenUrl: (String) -> Unit,
    onOpenOffline: (BookmarkEntity) -> Unit,
    onSaveBookmark: (BookmarkEntity) -> Unit,
    onDeleteBookmark: (BookmarkEntity) -> Unit,
    onImportHtml: () -> Unit,
    onExportHtml: () -> Unit,
    onBack: () -> Unit
) {
    var showReadingList by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf("All") }
    var editingBookmark by remember { mutableStateOf<BookmarkEntity?>(null) }
    var addingNew by remember { mutableStateOf(false) }

    val folders = remember(bookmarks) {
        listOf("All") + bookmarks.map { it.folder.ifBlank { "General" } }.distinct().sorted()
    }

    val displayed = remember(bookmarks, readingList, showReadingList, searchQuery, selectedFolder) {
        val base = if (showReadingList) readingList else bookmarks
        base.filter { item ->
            val folderOk = showReadingList || selectedFolder == "All" || item.folder == selectedFolder
            val queryOk = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.url.contains(searchQuery, ignoreCase = true)
            folderOk && queryOk
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(if (showReadingList) "Reading List" else "Bookmarks") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("bookmarks_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onImportHtml, modifier = Modifier.testTag("import_bookmarks_button")) {
                        Icon(Icons.Rounded.FileUpload, contentDescription = "Import Bookmarks HTML")
                    }
                    IconButton(onClick = onExportHtml, modifier = Modifier.testTag("export_bookmarks_button")) {
                        Icon(Icons.Rounded.FileDownload, contentDescription = "Export Bookmarks HTML")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showReadingList) {
                FloatingActionButton(
                    onClick = { addingNew = true },
                    modifier = Modifier.testTag("add_bookmark_fab")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Bookmark")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !showReadingList,
                    onClick = { showReadingList = false },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Bookmarks (${bookmarks.size})") }
                SegmentedButton(
                    selected = showReadingList,
                    onClick = { showReadingList = true },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Reading List (${readingList.size})") }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = { Text("Search saved pages") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("bookmarks_search_input")
            )

            if (!showReadingList && folders.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(folders) { folder ->
                        FilterChip(
                            selected = selectedFolder == folder,
                            onClick = { selectedFolder = folder },
                            label = { Text(folder) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (displayed.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (showReadingList) {
                            "No saved pages yet. Use 'Save Offline' from the menu while browsing."
                        } else {
                            "No bookmarks found."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(displayed, key = { it.id }) { item ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (showReadingList && !item.offlineArchivePath.isNullOrBlank()) {
                                        onOpenOffline(item)
                                    } else {
                                        onOpenUrl(item.url)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (!item.offlineArchivePath.isNullOrBlank()) Icons.Rounded.OfflinePin else Icons.Rounded.Bookmark,
                                    contentDescription = null,
                                    tint = if (!item.offlineArchivePath.isNullOrBlank()) ShieldEmerald else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.url,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!showReadingList) {
                                        Text(
                                            text = item.folder,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else if (!item.offlineArchivePath.isNullOrBlank()) {
                                        Text(
                                            text = "Available offline",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ShieldEmerald
                                        )
                                    }
                                }
                                if (!showReadingList) {
                                    IconButton(onClick = { editingBookmark = item }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Edit bookmark")
                                    }
                                }
                                IconButton(onClick = { onDeleteBookmark(item) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete bookmark")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (addingNew || editingBookmark != null) {
        val current = editingBookmark
        var title by remember(current) { mutableStateOf(current?.title ?: "") }
        var url by remember(current) { mutableStateOf(current?.url ?: "https://") }
        var folder by remember(current) { mutableStateOf(current?.folder ?: "General") }

        AlertDialog(
            onDismissRequest = {
                addingNew = false
                editingBookmark = null
            },
            title = { Text(if (current == null) "Add Bookmark" else "Edit Bookmark") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = folder,
                        onValueChange = { folder = it },
                        label = { Text("Folder") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    addingNew = false
                    editingBookmark = null
                }) { Text("Cancel") }
            },
            confirmButton = {
                Button(onClick = {
                    val cleanUrl = if (url.startsWith("http://") || url.startsWith("https://")) url.trim() else "https://${url.trim()}"
                    onSaveBookmark(
                        (current ?: BookmarkEntity(title = "", url = "")).copy(
                            title = title.ifBlank { cleanUrl },
                            url = cleanUrl,
                            folder = folder.ifBlank { "General" },
                            isReadingList = false
                        )
                    )
                    addingNew = false
                    editingBookmark = null
                }) { Text("Save") }
            }
        )
    }
}
