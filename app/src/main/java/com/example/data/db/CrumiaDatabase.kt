package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BookmarkEntity::class,
        HistoryEntity::class,
        DownloadEntity::class,
        ShortcutEntity::class,
        SiteSettingEntity::class,
        FilterRuleEntity::class,
        ClosedTabEntity::class,
        SavedTabEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CrumiaDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun shortcutDao(): ShortcutDao
    abstract fun siteSettingDao(): SiteSettingDao
    abstract fun filterRuleDao(): FilterRuleDao
    abstract fun closedTabDao(): ClosedTabDao
    abstract fun savedTabDao(): SavedTabDao

    companion object {
        @Volatile
        private var INSTANCE: CrumiaDatabase? = null

        fun getInstance(context: Context): CrumiaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CrumiaDatabase::class.java,
                    "crumia_local.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
