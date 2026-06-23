package com.example.extensions

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView

interface BrowserExtension {
    val id: String
    val name: String
    val description: String
    val iconName: String
    var isEnabled: Boolean

    fun onLoad(context: Context) {}
    fun onPageStarted(url: String) {}
    fun onPageFinished(url: String, webView: WebView) {}
    fun shouldIntercept(request: WebResourceRequest): WebResourceResponse? = null
    fun onMessageFromPage(message: String, webView: WebView) {}
}
