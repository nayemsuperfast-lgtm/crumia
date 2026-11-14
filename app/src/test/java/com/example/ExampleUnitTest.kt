package com.example

import com.example.adshield.AdShieldEngine
import com.example.adshield.DefaultFilterLists
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun adShield_domainMatching_handlesSubdomainsAndParents() {
        val set = setOf("doubleclick.net", "google-analytics.com")
        assertTrue(AdShieldEngine.matchesDomainSet("doubleclick.net", set))
        assertTrue(AdShieldEngine.matchesDomainSet("pubads.g.doubleclick.net", set))
        assertTrue(AdShieldEngine.matchesDomainSet("ssl.google-analytics.com", set))
        assertFalse(AdShieldEngine.matchesDomainSet("notdoubleclick.net", set))
        assertFalse(AdShieldEngine.matchesDomainSet("wikipedia.org", set))
    }

    @Test
    fun adShield_abpParser_extractsValidDomainsOnly() {
        assertEquals("doubleclick.net", AdShieldEngine.parseAbpDomainLine("||doubleclick.net^"))
        assertEquals("ads.example.com", AdShieldEngine.parseAbpDomainLine("||ads.example.com^"))
        assertNull(AdShieldEngine.parseAbpDomainLine("! comment line"))
        assertNull(AdShieldEngine.parseAbpDomainLine("||example.com/ads/*"))
        assertNull(AdShieldEngine.parseAbpDomainLine("##.ad-banner"))
    }

    @Test
    fun defaultFilterLists_containSubstantialSeedRules() {
        val seed = DefaultFilterLists.seedEntities()
        assertTrue(seed.size > 100)
    }
}
