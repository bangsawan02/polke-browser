package com.example.adblock

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.net.URI

object AdBlocker {
    // List of common ad/tracking host keywords or exact domains
    private val AD_HOSTS = hashSetOf(
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "adservice.google.com",
        "adnxs.com",
        "adcolony.com",
        "admob.com",
        "media.net",
        "scorecardresearch.com",
        "taboola.com",
        "outbrain.com",
        "mgid.com",
        "popads.net",
        "propellerads.com",
        "adform.net",
        "amazon-adsystem.com",
        "pubmatic.com",
        "rubiconproject.com",
        "openx.net",
        "criteo.com",
        "adskeeper.co.uk",
        "adsterra.com",
        "exoclick.com"
    )

    private val AD_PATH_KEYWORDS = arrayOf(
        "/ads/", "/ad_", "?ad_", "&ad_", "/adserver", "/banners/", 
        "adsbygoogle", "ad_banner", "advertisement", "sponsor"
    )

    /**
     * Checks if a network request URL is an ad resource.
     */
    fun isAdRequest(url: String): Boolean {
        try {
            val uri = URI(url)
            val host = uri.host ?: return false
            
            // Check domain match
            for (adHost in AD_HOSTS) {
                if (host.contains(adHost, ignoreCase = true)) {
                    return true
                }
            }
            
            // Check path/query keywords
            val lowerUrl = url.lowercase()
            for (keyword in AD_PATH_KEYWORDS) {
                if (lowerUrl.contains(keyword)) {
                    return true
                }
            }
        } catch (_: Exception) {}
        return false
    }

    /**
     * Generates an empty stub response to block the request.
     */
    fun createStubResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            ByteArrayInputStream(ByteArray(0))
        )
    }

    /**
     * JS payload injected to filter out and remove ad banners, iframe widgets,
     * and scripts that dynamically insert promotional structures.
     */
    fun getAdRemovalJavascript(): String {
        return """
            (function() {
                const selectors = [
                    '.adsbygoogle', '.ad-banner', '.ad-box', '.ad-container', '.ad_container', 
                    '#ad-slot', '[id^="div-gpt-ad"]', '[id^="ad-"]', '[class*="ad-unit"]', 
                    '.sponsor-post', '.native-ad', '#ad-banner', '.advertisement', 
                    '.banner-ads', '.mgid-widget', '[class*="sponsored-"]', '.trc_related_div'
                ];
                function purge() {
                    selectors.forEach(selector => {
                        try {
                            const elements = document.querySelectorAll(selector);
                            elements.forEach(el => {
                                el.style.display = 'none';
                                el.remove();
                            });
                        } catch(e) {}
                    });
                }
                // Purge immediately
                purge();
                // Schedule progressive sweeps for dynamic ads
                for (let delay of [500, 1000, 2000, 3000, 5000]) {
                    setTimeout(purge, delay);
                }
            })();
        """.trimIndent()
    }
}
