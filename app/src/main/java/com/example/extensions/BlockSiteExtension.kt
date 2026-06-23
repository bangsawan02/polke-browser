package com.example.extensions

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import java.io.ByteArrayInputStream

class BlockSiteExtension : BrowserExtension {
    override val id = "block_site_extension"
    override val name = "Mata-mata Cyber Security"
    override val description = "Secara otomatis mendeteksi dan memblokir situs-situs media sosial pengalih perhatian (TikTok, Reddit, Instagram, Facebook, X)."
    override val iconName = "block"
    override var isEnabled = true

    private val blockedDomains = listOf(
        "facebook.com",
        "instagram.com",
        "twitter.com",
        "x.com",
        "tiktok.com",
        "reddit.com"
    )

    override fun onPageStarted(url: String) {
        // No-op
    }

    override fun onPageFinished(url: String, webView: WebView) {
        // No-op
    }

    override fun shouldIntercept(request: WebResourceRequest): WebResourceResponse? {
        val url = request.url.toString()
        val lowercaseUrl = url.lowercase()

        val matchingDomain = blockedDomains.firstOrNull { domain ->
            lowercaseUrl.contains("://$domain") ||
            lowercaseUrl.contains("://www.$domain") ||
            lowercaseUrl.contains("/$domain") ||
            lowercaseUrl.endsWith(".$domain")
        }

        if (matchingDomain != null) {
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Akses Diblokir oleh CyberShield</title>
                    <style>
                        body {
                            background-color: #0E0F15;
                            color: #DBE0E8;
                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            min-height: 100vh;
                            margin: 0;
                            padding: 24px;
                            box-sizing: border-box;
                            text-align: center;
                        }
                        .container {
                            max-width: 500px;
                            background-color: #161824;
                            border: 2px solid #FF3B30;
                            border-radius: 16px;
                            padding: 32px;
                            box-shadow: 0 8px 32px rgba(0,0,0,0.5);
                        }
                        .icon {
                            font-size: 48px;
                            color: #FF3B30;
                            margin-bottom: 16px;
                        }
                        h1 {
                            font-size: 20px;
                            color: #FFFFFF;
                            margin: 0 0 16px 0;
                            font-weight: bold;
                        }
                        p {
                            font-size: 14px;
                            color: #8C94A5;
                            line-height: 1.6;
                            margin: 0 0 24px 0;
                        }
                        .badge {
                            background-color: rgba(255, 59, 48, 0.15);
                            color: #FF3B30;
                            padding: 6px 12px;
                            border-radius: 20px;
                            font-size: 12px;
                            font-weight: bold;
                            display: inline-block;
                            margin-bottom: 24px;
                        }
                        .back-btn {
                            background-color: #00E5FF;
                            color: #0E0F15;
                            border: none;
                            padding: 12px 24px;
                            border-radius: 8px;
                            font-weight: bold;
                            cursor: pointer;
                            font-size: 14px;
                            text-decoration: none;
                            display: inline-block;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="icon">🛡️</div>
                        <h1>Akses Terblokir!</h1>
                        <span class="badge">$matchingDomain</span>
                        <p>Akses ke situs dilarang sementara demi menjaga tingkat fokus dan produktivitas Anda. Ayo kembali ke tugas Anda!</p>
                        <a href="javascript:history.back()" class="back-btn">Kembali ke Situs Sebelumnya</a>
                    </div>
                </body>
                </html>
            """.trimIndent()

            return WebResourceResponse(
                "text/html",
                "UTF-8",
                ByteArrayInputStream(html.toByteArray(Charsets.UTF_8))
            )
        }
        return null
    }
}
