package com.example

import android.app.Application
import com.example.adshield.AdShieldEngine
import com.example.data.BrowserRepository
import com.example.data.db.CrumiaDatabase
import com.example.data.prefs.PreferencesRepository
import com.example.downloads.CrumiaDownloadManager
import com.example.search.SearchSuggestionService

class CrumiaApplication : Application() {

    val database: CrumiaDatabase by lazy { CrumiaDatabase.getInstance(this) }
    val preferencesRepository: PreferencesRepository by lazy { PreferencesRepository(this) }
    val browserRepository: BrowserRepository by lazy { BrowserRepository(this, database) }
    val adShieldEngine: AdShieldEngine by lazy {
        AdShieldEngine(
            filterRuleDao = database.filterRuleDao(),
            siteSettingDao = database.siteSettingDao(),
            preferencesRepository = preferencesRepository
        )
    }
    val downloadManager: CrumiaDownloadManager by lazy {
        CrumiaDownloadManager(this, database.downloadDao())
    }
    val searchSuggestionService: SearchSuggestionService by lazy {
        SearchSuggestionService(database.historyDao())
    }
}
