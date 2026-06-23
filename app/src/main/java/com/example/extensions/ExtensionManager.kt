package com.example.extensions

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject

class ExtensionManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("native_extensions_prefs", Context.MODE_PRIVATE)
    private val mainHandler = Handler(Looper.getMainLooper())

    val extensions = mutableListOf<BrowserExtension>()

    init {
        // 1. Tambah ekstensi bawaan kompilasi Kotlin
        extensions.add(AdBlockExtension())
        extensions.add(BlockSiteExtension())
        extensions.add(DarkThemeExtension())

        // 2. Muat ekstensi skrip deklaratif dari aset manifes
        loadAssetsExtensions()

        // 3. Muat status aktifkan/nonaktifkan dari penyimpanan lokal
        extensions.forEach { ext ->
            ext.isEnabled = prefs.getBoolean(ext.id, true)
            try {
                ext.onLoad(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadAssetsExtensions() {
        try {
            val jsonString = context.assets.open("extensions/ext-manifest.json").use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            }
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                val description = obj.getString("description")
                val iconName = obj.getString("iconName")
                val script = obj.getString("script")
                
                val matchesList = mutableListOf<String>()
                val matchesArray = obj.getJSONArray("matches")
                for (j in 0 until matchesArray.length()) {
                    matchesList.add(matchesArray.getString(j))
                }

                val ext = AssetScriptExtension(
                    id = id,
                    name = name,
                    description = description,
                    iconName = iconName,
                    scriptPath = script,
                    matches = matchesList
                )
                extensions.add(ext)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getExtensionById(id: String): BrowserExtension? {
        return extensions.firstOrNull { it.id == id }
    }

    fun toggleExtension(id: String): Boolean {
        val ext = getExtensionById(id) ?: return false
        ext.isEnabled = !ext.isEnabled
        prefs.edit().putBoolean(ext.id, ext.isEnabled).apply()
        return ext.isEnabled
    }

    fun onPageStarted(url: String) {
        extensions.forEach { ext ->
            if (ext.isEnabled) {
                try {
                    ext.onPageStarted(url)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun onPageFinished(url: String, webView: WebView) {
        extensions.forEach { ext ->
            if (ext.isEnabled) {
                try {
                     ext.onPageFinished(url, webView)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun shouldIntercept(request: WebResourceRequest): WebResourceResponse? {
        for (ext in extensions) {
            if (ext.isEnabled) {
                try {
                    val response = ext.shouldIntercept(request)
                    if (response != null) {
                        return response
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return null
    }

    fun onMessageFromPage(message: String, webView: WebView) {
        extensions.forEach { ext ->
            if (ext.isEnabled) {
                try {
                    ext.onMessageFromPage(message, webView)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Tampilkan notifikasi siber jembatan komunikasi JS-Kotlin pada Thread Utama
        mainHandler.post {
            try {
                val orgJson = JSONObject(message)
                val extId = orgJson.optString("extensionId", "anonymous")
                val extMessage = orgJson.optString("message", "")
                
                val ext = getExtensionById(extId)
                if (ext != null && ext.isEnabled && extMessage.isNotEmpty()) {
                    Toast.makeText(context, "[Sinyal ${ext.name}] $extMessage", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
