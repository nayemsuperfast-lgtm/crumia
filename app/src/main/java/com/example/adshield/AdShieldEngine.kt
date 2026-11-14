package com.example.adshield

import android.net.Uri
import android.webkit.WebResourceResponse
import com.example.data.db.FilterRuleDao
import com.example.data.db.FilterRuleEntity
import com.example.data.db.SiteSettingDao
import com.example.data.db.SiteSettingEntity
import com.example.data.model.FilterCategory
import com.example.data.model.FilterListSource
import com.example.data.prefs.CrumiaPreferences
import com.example.data.prefs.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

sealed interface BlockDecision {
    data object Allow : BlockDecision
    data class Block(val category: FilterCategory, val matchedRule: String) : BlockDecision
}

class AdShieldEngine(
    private val filterRuleDao: FilterRuleDao,
    private val siteSettingDao: SiteSettingDao,
    private val preferencesRepository: PreferencesRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .build()

    // Lock-free snapshots for O(1) main/IO thread checks inside shouldInterceptRequest
    @Volatile
    private var adDomains: Set<String> = DefaultFilterLists.easyListAdDomains.toHashSet() +
        DefaultFilterLists.adGuardMobileAndPopupDomains.toHashSet()

    @Volatile
    private var trackerDomains: Set<String> = DefaultFilterLists.easyPrivacyTrackerDomains.toHashSet()

    @Volatile
    private var popupDomains: Set<String> = setOf("popads.net", "popcash.net", "onclickads.net", "onclkds.com", "clickadu.com")

    @Volatile
    private var currentPrefs: CrumiaPreferences = CrumiaPreferences()

    private val allowlistedHosts = ConcurrentHashMap.newKeySet<String>()

    // Buffered counter flush to DataStore to avoid excessive disk writes on heavy pages
    private val pendingAds = AtomicLong(0L)
    private val pendingTrackers = AtomicLong(0L)

    // Per-session blocked counts per tab/host for quick UI feedback
    private val _sessionBlockedCount = MutableStateFlow(0L)
    val sessionBlockedCount: StateFlow<Long> = _sessionBlockedCount.asStateFlow()

    init {
        scope.launch {
            seedIfNeeded()
            reloadRulesCache()
            reloadAllowlistCache()
            launch {
                preferencesRepository.preferencesFlow.collect { prefs ->
                    currentPrefs = prefs
                    reloadRulesCache()
                }
            }
            launch {
                siteSettingDao.observeAllowlisted().collect { list ->
                    allowlistedHosts.clear()
                    list.forEach { allowlistedHosts.add(it.host.lowercase()) }
                }
            }
        }
    }

    private suspend fun seedIfNeeded() {
        if (filterRuleDao.count() == 0) {
            filterRuleDao.insertAll(DefaultFilterLists.seedEntities())
        }
    }

    suspend fun reloadRulesCache() = withContext(Dispatchers.IO) {
        val rules = filterRuleDao.getEnabledOnce()
        val prefs = currentPrefs
        val ads = HashSet<String>(rules.size + 64)
        val trackers = HashSet<String>(rules.size + 64)
        val popups = HashSet<String>(32)

        for (rule in rules) {
            val sourceAllowed = when (rule.listSource) {
                FilterListSource.EASYLIST.name -> prefs.easyListEnabled
                FilterListSource.EASYPRIVACY.name -> prefs.easyPrivacyEnabled
                FilterListSource.ADGUARD.name -> prefs.adGuardEnabled
                else -> true
            }
            if (!sourceAllowed) continue
            val clean = rule.pattern.lowercase().trim()
            if (clean.isEmpty()) continue
            when (rule.category) {
                FilterCategory.TRACKER.name -> trackers.add(clean)
                FilterCategory.POPUP.name -> popups.add(clean)
                else -> ads.add(clean)
            }
        }
        adDomains = ads
        trackerDomains = trackers
        popupDomains = popups
    }

    private suspend fun reloadAllowlistCache() {
        val all = siteSettingDao.getAllOnce()
        allowlistedHosts.clear()
        all.filter { it.adShieldAllowlisted }.forEach { allowlistedHosts.add(it.host.lowercase()) }
    }

    fun isSiteAllowlisted(pageHost: String?): Boolean {
        val normalized = normalizeHost(pageHost) ?: return false
        return allowlistedHosts.contains(normalized)
    }

    /**
     * Evaluates a subresource request off the UI thread (called by WebView's IO thread).
     * Never blocks main-frame navigation unless it is a known pop-up/malicious ad domain.
     */
    fun evaluateRequest(
        requestUrl: String,
        pageUrl: String?,
        isMainFrame: Boolean
    ): BlockDecision {
        val prefs = currentPrefs
        if (!prefs.adShieldEnabled) return BlockDecision.Allow

        val pageHost = normalizeHost(pageUrl?.let { safeHost(it) })
        if (pageHost != null && allowlistedHosts.contains(pageHost)) {
            return BlockDecision.Allow
        }

        val reqUri = runCatching { Uri.parse(requestUrl) }.getOrNull() ?: return BlockDecision.Allow
        val scheme = reqUri.scheme?.lowercase() ?: return BlockDecision.Allow
        if (scheme != "http" && scheme != "https") return BlockDecision.Allow

        val reqHost = normalizeHost(reqUri.host) ?: return BlockDecision.Allow

        // Never block the primary domain the user explicitly navigated to on the main frame
        // unless it is a dedicated pop-up ad redirect domain
        if (isMainFrame) {
            if (prefs.blockPopups && matchesDomainSet(reqHost, popupDomains)) {
                recordBlocked(FilterCategory.POPUP)
                return BlockDecision.Block(FilterCategory.POPUP, reqHost)
            }
            return BlockDecision.Allow
        }

        // First-party assets on same registrable host are allowed unless URL explicitly matches an ad path
        val isSameHost = pageHost != null && (reqHost == pageHost || reqHost.endsWith(".$pageHost"))

        if (prefs.blockAds) {
            if (!isSameHost && matchesDomainSet(reqHost, adDomains)) {
                recordBlocked(FilterCategory.AD)
                return BlockDecision.Block(FilterCategory.AD, reqHost)
            }
            val lowerUrl = requestUrl.lowercase()
            for (sub in DefaultFilterLists.adUrlSubstrings) {
                if (lowerUrl.contains(sub)) {
                    recordBlocked(FilterCategory.AD)
                    return BlockDecision.Block(FilterCategory.AD, sub)
                }
            }
        }

        if (prefs.blockTrackers) {
            if (!isSameHost && matchesDomainSet(reqHost, trackerDomains)) {
                recordBlocked(FilterCategory.TRACKER)
                return BlockDecision.Block(FilterCategory.TRACKER, reqHost)
            }
            val lowerUrl = requestUrl.lowercase()
            for (sub in DefaultFilterLists.trackerUrlSubstrings) {
                if (!isSameHost && lowerUrl.contains(sub)) {
                    recordBlocked(FilterCategory.TRACKER)
                    return BlockDecision.Block(FilterCategory.TRACKER, sub)
                }
            }
        }

        if (prefs.blockPopups && !isSameHost && matchesDomainSet(reqHost, popupDomains)) {
            recordBlocked(FilterCategory.POPUP)
            return BlockDecision.Block(FilterCategory.POPUP, reqHost)
        }

        return BlockDecision.Allow
    }

    fun shouldBlockPopupUrl(popupUrl: String?, openerPageUrl: String?): Boolean {
        val prefs = currentPrefs
        if (!prefs.adShieldEnabled || !prefs.blockPopups) return false
        val pageHost = normalizeHost(openerPageUrl?.let { safeHost(it) })
        if (pageHost != null && allowlistedHosts.contains(pageHost)) return false

        if (popupUrl.isNullOrBlank()) {
            recordBlocked(FilterCategory.POPUP)
            return true
        }
        val targetHost = normalizeHost(safeHost(popupUrl))
        if (targetHost != null && (matchesDomainSet(targetHost, adDomains) || matchesDomainSet(targetHost, popupDomains))) {
            recordBlocked(FilterCategory.POPUP)
            return true
        }
        // By default, block unsolicited window.open popups when blockPopups is ON
        recordBlocked(FilterCategory.POPUP)
        return true
    }

    fun emptyBlockedResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "utf-8",
            204,
            "No Content",
            mapOf("Access-Control-Allow-Origin" to "*"),
            ByteArrayInputStream(ByteArray(0))
        )
    }

    /**
     * Lightweight cosmetic CSS injection to collapse empty ad containers after ad requests are blocked.
     */
    fun buildCosmeticHideScript(pageUrl: String?): String? {
        val prefs = currentPrefs
        if (!prefs.adShieldEnabled || !prefs.blockAds) return null
        val pageHost = normalizeHost(pageUrl?.let { safeHost(it) })
        if (pageHost != null && allowlistedHosts.contains(pageHost)) return null
        return """
            (function() {
              try {
                if (document.getElementById('crumia-ad-shield-style')) return;
                var style = document.createElement('style');
                style.id = 'crumia-ad-shield-style';
                style.textContent = 'ins.adsbygoogle, [id^="div-gpt-ad"], .ad-banner, .ad-container, .sponsored-ad, amp-ad { display: none !important; }';
                (document.head || document.documentElement).appendChild(style);
              } catch (e) {}
            })();
        """.trimIndent()
    }

    private fun recordBlocked(category: FilterCategory) {
        _sessionBlockedCount.value = _sessionBlockedCount.value + 1L
        when (category) {
            FilterCategory.TRACKER -> pendingTrackers.incrementAndGet()
            FilterCategory.AD, FilterCategory.POPUP -> pendingAds.incrementAndGet()
        }
        flushCountersAsync()
    }

    fun flushCountersAsync() {
        val ads = pendingAds.getAndSet(0L)
        val trackers = pendingTrackers.getAndSet(0L)
        if (ads > 0L || trackers > 0L) {
            scope.launch {
                preferencesRepository.incrementBlockedCounters(ads, trackers)
            }
        }
    }

    suspend fun setSiteAllowlisted(host: String, allowlisted: Boolean) {
        val clean = normalizeHost(host) ?: return
        if (allowlisted) {
            allowlistedHosts.add(clean)
        } else {
            allowlistedHosts.remove(clean)
        }
        val existing = siteSettingDao.getByHost(clean)
        val updated = (existing ?: SiteSettingEntity(host = clean)).copy(
            adShieldAllowlisted = allowlisted,
            updatedAt = System.currentTimeMillis()
        )
        siteSettingDao.upsert(updated)
    }

    suspend fun addCustomRule(pattern: String, category: FilterCategory) {
        val clean = normalizeRuleInput(pattern) ?: return
        filterRuleDao.insert(
            FilterRuleEntity(
                pattern = clean,
                category = category.name,
                listSource = FilterListSource.CUSTOM.name,
                isEnabled = true
            )
        )
        reloadRulesCache()
    }

    suspend fun deleteRule(id: Long) {
        filterRuleDao.deleteById(id)
        reloadRulesCache()
    }

    suspend fun resetToDefaults() {
        filterRuleDao.clearAll()
        filterRuleDao.insertAll(DefaultFilterLists.seedEntities())
        preferencesRepository.resetAdShieldSettings()
        reloadRulesCache()
        _sessionBlockedCount.value = 0L
    }

    /**
     * Fetches maintained EasyList / EasyPrivacy / AdGuard domain rules and merges up to 2,500
     * high-signal host rules locally for fast in-memory matching without bloating RAM or storage.
     */
    suspend fun updateFilterLists(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val fetchedRules = ArrayList<FilterRuleEntity>(1200)
            val sourcesToFetch = listOf(
                FilterListSource.EASYLIST to FilterCategory.AD,
                FilterListSource.EASYPRIVACY to FilterCategory.TRACKER,
                FilterListSource.ADGUARD to FilterCategory.AD
            )
            var anyFetched = false
            for ((source, category) in sourcesToFetch) {
                val request = Request.Builder()
                    .url(source.remoteUrl)
                    .header("User-Agent", "Crumia-AdShield/1.0")
                    .build()
                runCatching {
                    httpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            anyFetched = true
                            val body = response.body ?: return@use
                            val reader = body.charStream().buffered()
                            var addedForSource = 0
                            while (addedForSource < 800) {
                                val line = reader.readLine() ?: break
                                val domain = parseAbpDomainLine(line) ?: continue
                                fetchedRules.add(
                                    FilterRuleEntity(
                                        pattern = domain,
                                        category = category.name,
                                        listSource = source.name,
                                        isEnabled = true
                                    )
                                )
                                addedForSource++
                            }
                        }
                    }
                }
            }
            if (!anyFetched && fetchedRules.isEmpty()) {
                throw IllegalStateException("Could not reach filter servers. Check internet connection.")
            }
            if (fetchedRules.isNotEmpty()) {
                filterRuleDao.insertAll(fetchedRules)
            }
            preferencesRepository.setLastFilterUpdate(System.currentTimeMillis())
            reloadRulesCache()
            fetchedRules.size
        }
    }

    companion object {
        fun safeHost(url: String): String? {
            return runCatching { Uri.parse(url).host }.getOrNull()
        }

        fun normalizeHost(host: String?): String? {
            val h = host?.trim()?.lowercase()?.removePrefix("www.")
            return if (h.isNullOrBlank()) null else h
        }

        fun matchesDomainSet(host: String, set: Set<String>): Boolean {
            if (set.contains(host)) return true
            var idx = host.indexOf('.')
            while (idx != -1) {
                val parent = host.substring(idx + 1)
                if (set.contains(parent)) return true
                idx = host.indexOf('.', idx + 1)
            }
            return false
        }

        fun parseAbpDomainLine(line: String): String? {
            val trimmed = line.trim()
            if (!trimmed.startsWith("||")) return null
            if (trimmed.contains("*") || trimmed.contains("$") || trimmed.contains("/")) {
                // Only extract pure domain-anchored rules like ||doubleclick.net^
                val clean = trimmed.removePrefix("||")
                val caretIdx = clean.indexOf('^')
                if (caretIdx <= 0 || caretIdx != clean.length - 1) return null
                val host = clean.substring(0, caretIdx).lowercase()
                return if (isValidDomain(host)) host else null
            }
            val candidate = trimmed.removePrefix("||").removeSuffix("^").lowercase()
            return if (isValidDomain(candidate)) candidate else null
        }

        private fun normalizeRuleInput(input: String): String? {
            val cleaned = input.trim()
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("||")
                .removeSuffix("^")
                .substringBefore("/")
                .lowercase()
            return if (isValidDomain(cleaned)) cleaned else null
        }

        private fun isValidDomain(host: String): Boolean {
            if (host.length < 4 || host.length > 253 || !host.contains('.')) return false
            return host.all { it.isLetterOrDigit() || it == '.' || it == '-' }
        }
    }
}
