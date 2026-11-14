package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE isReadingList = 0 ORDER BY createdAt DESC")
    fun observeBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE isReadingList = 1 ORDER BY createdAt DESC")
    fun observeReadingList(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE isReadingList = 0")
    suspend fun getAllBookmarksOnce(): List<BookmarkEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url AND isReadingList = 0)")
    fun isBookmarked(url: String): Flow<Boolean>

    @Query("SELECT * FROM bookmarks WHERE url = :url AND isReadingList = 0 LIMIT 1")
    suspend fun findByUrl(url: String): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Update
    suspend fun update(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM bookmarks WHERE url = :url AND isReadingList = 0")
    suspend fun deleteByUrl(url: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY visitedAt DESC LIMIT 500")
    fun observeHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE visitedAt = (SELECT MAX(visitedAt) FROM history h2 WHERE h2.url = history.url) ORDER BY visitedAt DESC LIMIT 12")
    fun observeRecentUnique(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY visitedAt DESC LIMIT 100")
    fun searchHistory(query: String): Flow<List<HistoryEntity>>

    @Query("SELECT DISTINCT title, url, visitedAt, id FROM history WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY visitedAt DESC LIMIT 8")
    suspend fun searchHistorySuggestions(query: String): List<HistoryEntity>

    @Insert
    suspend fun insert(entry: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM history WHERE visitedAt >= :sinceMillis")
    suspend fun deleteSince(sinceMillis: Long)

    @Query("DELETE FROM history")
    suspend fun clearAll()
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getById(id: Long): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity): Long

    @Update
    suspend fun update(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM downloads")
    suspend fun clearAll()
}

@Dao
interface ShortcutDao {
    @Query("SELECT * FROM shortcuts ORDER BY sortOrder ASC, id ASC")
    fun observeShortcuts(): Flow<List<ShortcutEntity>>

    @Query("SELECT COUNT(*) FROM shortcuts")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shortcut: ShortcutEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shortcuts: List<ShortcutEntity>)

    @Update
    suspend fun update(shortcut: ShortcutEntity)

    @Query("DELETE FROM shortcuts WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SiteSettingDao {
    @Query("SELECT * FROM site_settings ORDER BY host ASC")
    fun observeAll(): Flow<List<SiteSettingEntity>>

    @Query("SELECT * FROM site_settings WHERE adShieldAllowlisted = 1 ORDER BY host ASC")
    fun observeAllowlisted(): Flow<List<SiteSettingEntity>>

    @Query("SELECT * FROM site_settings WHERE host = :host LIMIT 1")
    suspend fun getByHost(host: String): SiteSettingEntity?

    @Query("SELECT * FROM site_settings")
    suspend fun getAllOnce(): List<SiteSettingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setting: SiteSettingEntity)

    @Query("DELETE FROM site_settings WHERE host = :host")
    suspend fun deleteByHost(host: String)

    @Query("DELETE FROM site_settings")
    suspend fun clearAll()
}

@Dao
interface FilterRuleDao {
    @Query("SELECT * FROM filter_rules ORDER BY listSource ASC, pattern ASC")
    fun observeAll(): Flow<List<FilterRuleEntity>>

    @Query("SELECT * FROM filter_rules WHERE isEnabled = 1")
    suspend fun getEnabledOnce(): List<FilterRuleEntity>

    @Query("SELECT COUNT(*) FROM filter_rules")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rules: List<FilterRuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: FilterRuleEntity)

    @Query("UPDATE filter_rules SET isEnabled = :enabled WHERE listSource = :listSource")
    suspend fun setSourceEnabled(listSource: String, enabled: Boolean)

    @Query("DELETE FROM filter_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM filter_rules WHERE listSource = :listSource")
    suspend fun deleteBySource(listSource: String)

    @Query("DELETE FROM filter_rules")
    suspend fun clearAll()
}

@Dao
interface ClosedTabDao {
    @Query("SELECT * FROM closed_tabs ORDER BY closedAt DESC LIMIT 25")
    fun observeClosedTabs(): Flow<List<ClosedTabEntity>>

    @Query("SELECT * FROM closed_tabs ORDER BY closedAt DESC LIMIT 1")
    suspend fun getMostRecent(): ClosedTabEntity?

    @Insert
    suspend fun insert(tab: ClosedTabEntity)

    @Query("DELETE FROM closed_tabs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM closed_tabs")
    suspend fun clearAll()
}

@Dao
interface SavedTabDao {
    @Query("SELECT * FROM saved_tabs ORDER BY sortOrder ASC")
    suspend fun getAllOnce(): List<SavedTabEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tabs: List<SavedTabEntity>)

    @Query("DELETE FROM saved_tabs")
    suspend fun clearAll()
}
