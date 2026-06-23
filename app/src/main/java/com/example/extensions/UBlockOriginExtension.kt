package com.example.extensions

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import com.example.adblock.AdBlocker

class UBlockOriginExtension : BrowserExtension {
    override val id = "ublock_origin_extension"
    override val name = "uBlock Origin"
    override val description = "Pemblokir konten efisien yang ramah memori dan CPU untuk memblokir iklan, pelacak, pop-up pengalih perhatian, dan situs berbahaya secara otomatis."
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
