package com.example.extensions

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import com.example.adblock.AdBlocker

class AdBlockExtension : BrowserExtension {
    override val id = "ad_block_extension"
    override val name = "Ad-Blocker Pro"
    override val description = "Secara otomatis memblokir iklan, pelacak, dan pop-up pengalih perhatian dari halaman web untuk pemuatan 2x lebih cepat."
    override val iconName = "block"
    override var isEnabled = true

    override fun onPageStarted(url: String) {
        // No-op for network block prior to request, tracked in shouldIntercept
    }

    override fun onPageFinished(url: String, webView: WebView) {
        // Inject script to clean remaining ad containers from DOM
        webView.evaluateJavascript(AdBlocker.getAdRemovalJavascript(), null)
    }

    override fun shouldIntercept(request: WebResourceRequest): WebResourceResponse? {
        val url = request.url.toString()
        if (AdBlocker.isAdRequest(url)) {
            return AdBlocker.createStubResponse()
        }
        return null
    }
}
