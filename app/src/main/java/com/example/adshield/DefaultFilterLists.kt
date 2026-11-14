package com.example.adshield

import com.example.data.db.FilterRuleEntity
import com.example.data.model.FilterCategory
import com.example.data.model.FilterListSource

/**
 * Curated high-signal filter baselines modeled on EasyList, EasyPrivacy, and AdGuard Mobile.
 * Additional rules can be downloaded dynamically from maintained lists via Ad Shield Manual Update.
 */
object DefaultFilterLists {

    val easyListAdDomains: List<String> = listOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "tpc.googlesyndication.com",
        "adnxs.com",
        "adsrvr.org",
        "advertising.com",
        "adform.net",
        "adroll.com",
        "amazon-adsystem.com",
        "aax.amazon-adsystem.com",
        "criteo.com",
        "criteo.net",
        "pubmatic.com",
        "rubiconproject.com",
        "openx.net",
        "casalemedia.com",
        "indexww.com",
        "media.net",
        "outbrain.com",
        "taboola.com",
        "mgid.com",
        "revcontent.com",
        "bidswitch.net",
        "smartadserver.com",
        "yieldmo.com",
        "sharethrough.com",
        "triplelift.com",
        "spotxchange.com",
        "teads.tv",
        "moatads.com",
        "inmobi.com",
        "applovin.com",
        "vungle.com",
        "unityads.unity3d.com",
        "chartboost.com",
        "tapjoy.com",
        "adcolony.com",
        "smaato.net",
        "lijit.com",
        "contextweb.com",
        "33across.com",
        "zemanta.com",
        "popads.net",
        "popcash.net",
        "propellerads.com",
        "adsterra.com",
        "exoclick.com",
        "juicyads.com",
        "trafficjunky.net",
        "hilltopads.net",
        "clickadu.com",
        "pushame.com",
        "ads.yahoo.com",
        "adtech.de",
        "adition.com",
        "flashtalking.com",
        "serving-sys.com",
        "innovid.com",
        "fwmrm.net",
        "imasdk.googleapis.com"
    )

    val easyPrivacyTrackerDomains: List<String> = listOf(
        "google-analytics.com",
        "ssl.google-analytics.com",
        "www.google-analytics.com",
        "googletagmanager.com",
        "googletagservices.com",
        "analytics.google.com",
        "app-measurement.com",
        "firebaseanalytics.googleapis.com",
        "scorecardresearch.com",
        "quantserve.com",
        "hotjar.com",
        "static.hotjar.com",
        "mouseflow.com",
        "fullstory.com",
        "mixpanel.com",
        "api.mixpanel.com",
        "segment.io",
        "api.segment.io",
        "cdn.segment.com",
        "amplitude.com",
        "api.amplitude.com",
        "clarity.ms",
        "heapanalytics.com",
        "kissmetrics.com",
        "optimizely.com",
        "chartbeat.com",
        "newrelic.com",
        "bam.nr-data.net",
        "js-agent.newrelic.com",
        "sentry.io",
        "browser.sentry-cdn.com",
        "bugsnag.com",
        "connect.facebook.net",
        "pixel.facebook.com",
        "analytics.twitter.com",
        "ads-twitter.com",
        "px.ads.linkedin.com",
        "snap.licdn.com",
        "tr.snapchat.com",
        "analytics.tiktok.com",
        "bat.bing.com",
        "ct.pinterest.com",
        "krxd.net",
        "bluekai.com",
        "demdex.net",
        "omtrdc.net",
        "2o7.net",
        "agkn.com",
        "exelator.com",
        "rlcdn.com",
        "crwdcntrl.net",
        "mathtag.com",
        "eyeota.net",
        "lotame.com",
        "tapad.com",
        "branch.io",
        "app.link",
        "adjust.com",
        "appsflyer.com",
        "kochava.com",
        "singular.net"
    )

    val adGuardMobileAndPopupDomains: List<String> = listOf(
        "adblockanalytics.com",
        "adclick.g.doubleclick.net",
        "ads.mopub.com",
        "ads.pubmatic.com",
        "adserver.adtechus.com",
        "adx.g.doubleclick.net",
        "an.facebook.com",
        "analytics.s3.amazonaws.com",
        "banners.itunes.apple.com",
        "beacon.krxd.net",
        "biddingx.com",
        "c.bing.com",
        "clk.tradedoubler.com",
        "cpmstar.com",
        "dpm.demdex.net",
        "dt.adsafeprotected.com",
        "pixel.adsafeprotected.com",
        "adsafeprotected.com",
        "doubleverify.com",
        "cdn.doubleverify.com",
        "g.doubleclick.net",
        "iadsdk.apple.com",
        "metrics.icloud.com",
        "pagead.l.doubleclick.net",
        "partner.googleadservices.com",
        "pubads.g.doubleclick.net",
        "securepubads.g.doubleclick.net",
        "static.ads-twitter.com",
        "stats.g.doubleclick.net",
        "track.hubspot.com",
        "js.hs-analytics.net",
        "mc.yandex.ru",
        "an.yandex.ru",
        "ad.mail.ru",
        "counter.yadro.ru",
        "top-fwz1.mail.ru",
        "adfox.ru",
        "onclickads.net",
        "onclkds.com",
        "popunder.ru",
        "redirectvoluum.com",
        "voluumtrk.com"
    )

    val adUrlSubstrings: List<String> = listOf(
        "/pagead/",
        "/adserver/",
        "/adservice/",
        "/ads/js/",
        "/adunit/",
        "/banner_ad",
        "/popunder",
        "/prebid",
        "googlesyndication.com/pagead",
        "doubleclick.net/gampad",
        "/vast.xml",
        "/vpaid"
    )

    val trackerUrlSubstrings: List<String> = listOf(
        "/gtag/js",
        "/gtm.js",
        "/analytics.js",
        "/fbevents.js",
        "/pixel?",
        "/collect?v=",
        "/telemetry/",
        "/beacon?",
        "utm_source="
    )

    fun seedEntities(): List<FilterRuleEntity> {
        val rules = ArrayList<FilterRuleEntity>(256)
        easyListAdDomains.forEach { domain ->
            rules.add(
                FilterRuleEntity(
                    pattern = domain,
                    category = FilterCategory.AD.name,
                    listSource = FilterListSource.EASYLIST.name,
                    isEnabled = true
                )
            )
        }
        easyPrivacyTrackerDomains.forEach { domain ->
            rules.add(
                FilterRuleEntity(
                    pattern = domain,
                    category = FilterCategory.TRACKER.name,
                    listSource = FilterListSource.EASYPRIVACY.name,
                    isEnabled = true
                )
            )
        }
        adGuardMobileAndPopupDomains.forEach { domain ->
            rules.add(
                FilterRuleEntity(
                    pattern = domain,
                    category = FilterCategory.AD.name,
                    listSource = FilterListSource.ADGUARD.name,
                    isEnabled = true
                )
            )
        }
        return rules
    }
}
