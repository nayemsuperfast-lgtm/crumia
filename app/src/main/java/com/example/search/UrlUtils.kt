package com.example.search

import android.net.Uri
import android.util.Patterns
import com.example.data.model.SearchEngineOption
import java.net.URLEncoder

object UrlUtils {

    private val knownDomainSuffixes = setOf(
        "com", "org", "net", "edu", "gov", "io", "dev", "app", "co", "uk", "de",
        "fr", "jp", "ca", "au", "in", "ai", "me", "tv", "info", "biz", "xyz", "tech", "online"
    )

    fun resolveInput(
        rawInput: String,
        searchEngine: SearchEngineOption,
        customTemplate: String,
        httpsUpgrade: Boolean = true
    ): String {
        val input = rawInput.trim()
        if (input.isEmpty()) return ""

        // Direct scheme URLs
        if (input.startsWith("http://", ignoreCase = true) ||
            input.startsWith("https://", ignoreCase = true) ||
            input.startsWith("file://", ignoreCase = true) ||
            input.startsWith("about:", ignoreCase = true)
        ) {
            return if (httpsUpgrade && input.startsWith("http://", ignoreCase = true) && !isLocalAddress(input)) {
                "https://" + input.substring(7)
            } else {
                input
            }
        }

        // Localhost or IPv4 address
        if (input.startsWith("localhost", ignoreCase = true) || isIpv4WithOptionalPort(input)) {
            return "http://$input"
        }

        // Domain-like input without scheme (no spaces, valid host pattern or known TLD)
        if (!input.contains(' ') && looksLikeUrl(input)) {
            return "https://$input"
        }

        // Otherwise treat as search query
        val encoded = URLEncoder.encode(input, "UTF-8")
        val template = if (searchEngine == SearchEngineOption.CUSTOM) {
            customTemplate.ifBlank { SearchEngineOption.GOOGLE.searchUrlTemplate }
        } else {
            searchEngine.searchUrlTemplate
        }
        return if (template.contains("{query}")) {
            template.replace("{query}", encoded)
        } else {
            "$template$encoded"
        }
    }

    fun looksLikeUrl(input: String): Boolean {
        if (input.contains(' ')) return false
        if (Patterns.WEB_URL.matcher(input).matches()) return true
        val hostPart = input.substringBefore('/').substringBefore('?').substringBefore(':')
        val dotIndex = hostPart.lastIndexOf('.')
        if (dotIndex <= 0 || dotIndex >= hostPart.length - 1) return false
        val tld = hostPart.substring(dotIndex + 1).lowercase()
        return tld in knownDomainSuffixes || (tld.length in 2..6 && tld.all { it.isLetter() })
    }

    private fun isIpv4WithOptionalPort(input: String): Boolean {
        val host = input.substringBefore('/').substringBefore(':')
        val parts = host.split('.')
        if (parts.size != 4) return false
        return parts.all { p -> p.toIntOrNull()?.let { it in 0..255 } == true }
    }

    private fun isLocalAddress(url: String): Boolean {
        val host = runCatching { Uri.parse(url).host?.lowercase() }.getOrNull() ?: return false
        return host == "localhost" || host.startsWith("127.") || host.startsWith("192.168.") || host.startsWith("10.")
    }

    fun extractDisplayHost(url: String): String {
        if (url.isBlank()) return ""
        return runCatching {
            val uri = Uri.parse(url)
            uri.host?.removePrefix("www.") ?: url
        }.getOrDefault(url)
    }

    fun isHttps(url: String): Boolean = url.startsWith("https://", ignoreCase = true)
}
