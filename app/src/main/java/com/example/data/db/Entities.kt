package com.example.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["url"]), Index(value = ["folder"])]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val folder: String = "General",
    val isReadingList: Boolean = false,
    val offlineArchivePath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "history",
    indices = [Index(value = ["visitedAt"]), Index(value = ["url"])]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val visitedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "downloads",
    indices = [Index(value = ["createdAt"])]
)
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val url: String,
    val mimeType: String = "application/octet-stream",
    val totalBytes: Long = -1L,
    val downloadedBytes: Long = 0L,
    val status: String = "QUEUED",
    val filePath: String = "",
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "shortcuts")
data class ShortcutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val sortOrder: Int = 0,
    val accentHex: Long = 0xFF6366F1
)

@Entity(tableName = "site_settings")
data class SiteSettingEntity(
    @PrimaryKey val host: String,
    val adShieldAllowlisted: Boolean = false,
    val desktopMode: Boolean? = null,
    val javascriptEnabled: Boolean? = null,
    val cameraAllowed: Boolean? = null,
    val micAllowed: Boolean? = null,
    val locationAllowed: Boolean? = null,
    val popupsAllowed: Boolean? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "filter_rules",
    indices = [Index(value = ["pattern"], unique = true), Index(value = ["listSource"])]
)
data class FilterRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pattern: String,
    val category: String,
    val listSource: String,
    val isEnabled: Boolean = true
)

@Entity(tableName = "closed_tabs")
data class ClosedTabEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val closedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_tabs")
data class SavedTabEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val isHomepage: Boolean = true,
    val isDesktopMode: Boolean = false,
    val sortOrder: Int = 0,
    val isActive: Boolean = false
)
