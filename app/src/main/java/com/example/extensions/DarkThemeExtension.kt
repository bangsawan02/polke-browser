package com.example.extensions

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView

class DarkThemeExtension : BrowserExtension {
    override val id = "dark_theme_extension"
    override val name = "Force Dark Mode"
    override val description = "Memaksa setiap situs web menggunakan tema gelap untuk melindungi penglihatan Anda dalam kondisi cahaya rendah."
    override val iconName = "theme"
    override var isEnabled = true

    override fun onPageStarted(url: String) {
        // No-Op
    }

    override fun onPageFinished(url: String, webView: WebView) {
        val css = """
            * { 
                background-color: #0E0F15 !important; 
                color: #DBE0E8 !important; 
                border-color: #242735 !important; 
            } 
            a { 
                color: #00E5FF !important; 
            } 
            a:visited { 
                color: #BD00FF !important; 
            } 
            input, textarea, select { 
                background-color: #191B24 !important; 
                color: #FFFFFF !important; 
                border: 1px solid #2F334A !important; 
            } 
            img { 
                opacity: 0.82 !important; 
                filter: contrast(1.1) !important; 
            }
        """.trimIndent().replace("\r", "").replace("\n", " ")

        val injectJs = """
            (function() {
                var style = document.getElementById('force-dark-mode-style');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'force-dark-mode-style';
                    style.type = 'text/css';
                    style.appendChild(document.createTextNode('$css'));
                    document.head.appendChild(style);
                }
            })();
        """.trimIndent()

        webView.evaluateJavascript(injectJs, null)
    }

    override fun shouldIntercept(request: WebResourceRequest): WebResourceResponse? {
        return null
    }
}
