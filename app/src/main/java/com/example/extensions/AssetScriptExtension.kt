package com.example.extensions

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import java.io.IOException

class AssetScriptExtension(
    override val id: String,
    override val name: String,
    override val description: String,
    override val iconName: String,
    private val scriptPath: String,
    private val matches: List<String>
) : BrowserExtension {

    override var isEnabled: Boolean = true
    private var scriptContent: String = ""

    override fun onLoad(context: Context) {
        try {
            context.assets.open("extensions/$scriptPath").use { inputStream ->
                scriptContent = inputStream.bufferedReader().use { it.readText() }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onPageFinished(url: String, webView: WebView) {
        if (scriptContent.isNotEmpty() && matchesUrl(url)) {
            val guardedScript = """
                (function() {
                    try {
                        $scriptContent
                    } catch (e) {
                        console.error('Error executing extension $name: ', e);
                    }
                })();
            """.trimIndent()
            webView.evaluateJavascript(guardedScript, null)
        }
    }

    private fun matchesUrl(url: String): Boolean {
        if (matches.contains("*")) return true
        return matches.any { pattern ->
            url.lowercase().contains(pattern.lowercase())
        }
    }

    override fun onMessageFromPage(message: String, webView: WebView) {
        // Can be customized for interop actions
    }
}
