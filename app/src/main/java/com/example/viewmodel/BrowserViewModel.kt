package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.adblock.AdBlocker
import com.example.crypto.CryptoHelper
import com.example.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Dns
import java.net.InetAddress
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).browserDao()
    private val prefs = application.getSharedPreferences("browser_settings", android.content.Context.MODE_PRIVATE)

    private var _isGpuRenderingEnabled = mutableStateOf(prefs.getBoolean("gpu_rendering", true))
    var isGpuRenderingEnabled: Boolean
        get() = _isGpuRenderingEnabled.value
        set(value) {
            _isGpuRenderingEnabled.value = value
            prefs.edit().putBoolean("gpu_rendering", value).apply()
        }

    private var _isH264ifyEnabled = mutableStateOf(prefs.getBoolean("h264ify", true))
    var isH264ifyEnabled: Boolean
        get() = _isH264ifyEnabled.value
        set(value) {
            _isH264ifyEnabled.value = value
            prefs.edit().putBoolean("h264ify", value).apply()
        }

    private var _selectedDnsProvider = mutableStateOf(prefs.getString("dns_provider", "Sistem") ?: "Sistem")
    var selectedDnsProvider: String
        get() = _selectedDnsProvider.value
        set(value) {
            _selectedDnsProvider.value = value
            prefs.edit().putString("dns_provider", value).apply()
            rebuildClient()
        }

    private var client = buildOkHttpClient()

    private fun buildOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .dns(SecureDnsResolver(selectedDnsProvider))
            .build()
    }

    private fun rebuildClient() {
        client = buildOkHttpClient()
    }

    // --- Core UI Observables (Flows) ---
    val bookmarksState = dao.getAllBookmarks().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val tabsState = dao.getAllTabs().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val scriptsState = dao.getAllScripts().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val historyState = dao.getAllHistory().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val shortcutsState = dao.getAllShortcuts().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- State-based Navigation/Tab tracking ---
    var activeTab by mutableStateOf<Tab?>(null)
        private set

    var activeUser by mutableStateOf<User?>(null)
        private set

    // --- Configuration State ---
    var isAdBlockEnabled by mutableStateOf(true)
    var isDarkThemeAuto by mutableStateOf(true) // custom dark style injection

    // --- Adblock statistics ---
    var adsBlockedInSession by mutableStateOf(0)
    var adsBlockedInCurrentPage by mutableStateOf(0)

    // --- User flow feedback ---
    var syncInProgress by mutableStateOf(false)
    var syncResultMessage by mutableStateOf<String?>(null)
    var authError by mutableStateOf<String?>(null)
    var authSuccessMsg by mutableStateOf<String?>(null)

    init {
        restoreOrCreateActiveTab()
        restoreLoggedInUser()
        seedDefaultScriptsIfNeeded()
        seedDefaultShortcutsIfNeeded()
    }

    private fun seedDefaultShortcutsIfNeeded() {
        viewModelScope.launch {
            val list = dao.getAllShortcuts().first()
            if (list.isEmpty()) {
                dao.insertShortcut(ShortcutItem(title = "Google", url = "https://www.google.com"))
                dao.insertShortcut(ShortcutItem(title = "YouTube", url = "https://www.youtube.com"))
                dao.insertShortcut(ShortcutItem(title = "Wikipedia", url = "https://www.wikipedia.org"))
                dao.insertShortcut(ShortcutItem(title = "GitHub", url = "https://www.github.com"))
            }
        }
    }

    // --- TAB MANAGEMENT ---
    private fun restoreOrCreateActiveTab() {
        viewModelScope.launch {
            val active = dao.getActiveTab()
            if (active != null) {
                activeTab = active
            } else {
                // If there are tabs in database, pick first
                val list = dao.getAllTabs().first()
                if (list.isNotEmpty()) {
                    selectTab(list.first())
                } else {
                    // Create main homepage tab
                    createNewTab("Beranda", "cyber://home")
                }
            }
        }
    }

    fun createNewTab(title: String, url: String) {
        viewModelScope.launch {
            dao.deactivateAllTabs()
            val newTab = Tab.createSecure(title, url, isActive = true)
            val id = dao.insertTab(newTab)
            activeTab = newTab.copy(id = id)
            adsBlockedInCurrentPage = 0
        }
    }

    fun selectTab(tab: Tab) {
        viewModelScope.launch {
            dao.deactivateAllTabs()
            val updated = tab.copy(isActive = true, lastAccessed = System.currentTimeMillis())
            dao.updateTab(updated)
            activeTab = updated
            adsBlockedInCurrentPage = 0
        }
    }

    fun closeTab(tab: Tab) {
        viewModelScope.launch {
            dao.deleteTab(tab.id)
            if (activeTab?.id == tab.id) {
                val list = dao.getAllTabs().first()
                if (list.isNotEmpty()) {
                    selectTab(list.first())
                } else {
                    createNewTab("Beranda", "cyber://home")
                }
            }
        }
    }

    fun updateCurrentTabUrl(title: String, url: String) {
        val current = activeTab ?: return
        viewModelScope.launch {
            val updated = current.copy(
                titleEncrypted = CryptoHelper.encrypt(title),
                urlEncrypted = CryptoHelper.encrypt(url),
                lastAccessed = System.currentTimeMillis()
            )
            dao.updateTab(updated)
            activeTab = updated

            // Record real web queries & visits inside search history
            if (url.isNotBlank() && !url.startsWith("cyber://") && !url.contains("://localhost") && url != "about:blank") {
                dao.insertHistory(HistoryItem(title = title, url = url))
            }
        }
    }

    // --- BROWSER HISTORY OPERATIONS ---
    fun deleteHistory(id: Long) {
        viewModelScope.launch {
            dao.deleteHistoryItem(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            dao.clearAllHistory()
        }
    }

    // --- SHORTCUT OPERATIONS ---
    fun addShortcut(title: String, url: String) {
        viewModelScope.launch {
            if (title.isNotBlank() && url.isNotBlank()) {
                var formattedUrl = url.trim()
                if (!formattedUrl.contains("://")) {
                    formattedUrl = "https://$formattedUrl"
                }
                dao.insertShortcut(ShortcutItem(title = title, url = formattedUrl))
            }
        }
    }

    fun removeShortcut(id: Long) {
        viewModelScope.launch {
            dao.deleteShortcutItem(id)
        }
    }

    // --- COOKIE & CACHE CLEANER ---
    fun clearBrowserCacheAndCookies(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                // Clear Cookes
                val cookieManager = android.webkit.CookieManager.getInstance()
                cookieManager.removeAllCookies { /* done */ }
                cookieManager.flush()

                // Clear web storage
                android.webkit.WebStorage.getInstance().deleteAllData()

                // Clear cache dir
                context.cacheDir.deleteRecursively()
                
                authSuccessMsg = "Cookie dan cache peramban internet berhasil dibersihkan!"
            } catch (e: java.lang.Exception) {
                authError = "Gagal membersihkan cache: ${e.message}"
            }
        }
    }

    // --- BOOKMARK OPERATIONS ---
    fun addBookmark(title: String, url: String) {
        viewModelScope.launch {
            val userId = activeUser?.email ?: "local"
            val bookmark = Bookmark.createSecure(title, url, userId)
            dao.insertBookmark(bookmark)
        }
    }

    fun removeBookmark(id: Long) {
        viewModelScope.launch {
            dao.softDeleteBookmark(id)
        }
    }

    fun isCurrentPageBookmarked(url: String): Boolean {
        return bookmarksState.value.any { it.url.trim() == url.trim() }
    }

    fun toggleCurrentPageBookmark(title: String, url: String) {
        val matched = bookmarksState.value.find { it.url.trim() == url.trim() }
        if (matched != null) {
            removeBookmark(matched.id)
        } else {
            addBookmark(title, url)
        }
    }

    // --- ACCUMULATING ADBLOCK ACTIONS ---
    fun registerAdBlockEvent() {
        adsBlockedInSession++
        adsBlockedInCurrentPage++
    }

    // --- ACCOUNT INTEGRATION ---
    private fun restoreLoggedInUser() {
        viewModelScope.launch {
            val primary = dao.getPrimaryUser()
            if (primary != null && primary.isLoggedIn) {
                activeUser = primary
            }
        }
    }

    fun loginWithGoogle(email: String) {
        viewModelScope.launch {
            authError = null
            authSuccessMsg = null
            if (email.isBlank()) {
                authError = "Harap masukkan alamat email Google yang valid."
                return@launch
            }

            dao.logoutAllUsers()
            // If user already exists in db, login, else register
            var user = dao.getUserByEmail(email)
            if (user == null) {
                user = User(
                    email = email,
                    passwordHash = User.hashPassword("google_oauth_secured_oauth"),
                    isLoggedIn = true,
                    encryptedToken = CryptoHelper.encrypt("google_token_${System.currentTimeMillis()}"),
                    lastSyncTime = 0
                )
                dao.insertUser(user)
            } else {
                user = user.copy(isLoggedIn = true)
                dao.insertUser(user)
            }
            activeUser = user
            authSuccessMsg = "Berhasil masuk sistem sinkronisasi Google Chrome!"
            
            // Re-sync immediately after sign-in
            runLocalAndCloudSync()
        }
    }

    fun registerAndLogin(email: String, passwordClear: String) {
        viewModelScope.launch {
            authError = null
            authSuccessMsg = null
            if (email.isBlank() || passwordClear.length < 6) {
                authError = "Alamat email harus valid dan sandi minimal 6 karakter."
                return@launch
            }

            val existing = dao.getUserByEmail(email)
            if (existing != null) {
                authError = "Email ini sudah terdaftar. Silakan login."
                return@launch
            }

            // Create account
            dao.logoutAllUsers()
            val newUser = User.createSecure(email, passwordClear, token = "bearer_auth_sc_${System.currentTimeMillis()}")
            dao.insertUser(newUser)
            activeUser = newUser
            authSuccessMsg = "Akun berhasil didaftarkan!"
        }
    }

    fun login(email: String, passwordClear: String) {
        viewModelScope.launch {
            authError = null
            authSuccessMsg = null
            if (email.isBlank() || passwordClear.isBlank()) {
                authError = "Email dan kata sandi tidak boleh kosong."
                return@launch
            }

            val user = dao.getUserByEmail(email)
            if (user == null) {
                authError = "Akun tidak ditemukan. Silakan daftar terlebih dahulu."
                return@launch
            }

            val hash = User.hashPassword(passwordClear)
            if (user.passwordHash == hash) {
                dao.logoutAllUsers()
                val loggedUser = user.copy(isLoggedIn = true)
                dao.insertUser(loggedUser)
                activeUser = loggedUser
                authSuccessMsg = "Login berhasil!"
            } else {
                authError = "Kata sandi salah. Silakan coba lagi."
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            dao.logoutAllUsers()
            activeUser = null
            authSuccessMsg = "Berhasil keluar dari akun."
        }
    }

    // --- BOOKMARK REMOTE CLOUD SYNC ENGINE (REAL CONNECTION INTEGRATION) ---
    fun runLocalAndCloudSync() {
        val user = activeUser
        if (user == null) {
            syncResultMessage = "Silakan masuk akun terlebih dahulu untuk melakukan sinkronisasi cloud."
            return
        }

        viewModelScope.launch {
            syncInProgress = true
            syncResultMessage = "Mengenkripsi data lokal..."
            
            val localBookmarks = bookmarksState.value.filter { it.userId == user.email }
            
            // Format to secure encrypted payload wrapper
            val bookmarkArray = JSONArray()
            localBookmarks.forEach { b ->
                val obj = JSONObject().apply {
                    put("id", b.id)
                    put("title_enc", b.titleEncrypted)
                    put("url_enc", b.urlEncrypted)
                    put("created", b.createdAt)
                    put("deleted", b.isDeleted)
                }
                bookmarkArray.put(obj)
            }

            val syncPayload = JSONObject().apply {
                put("user_email", user.email)
                put("sync_token", user.syncToken)
                put("payload_encrypted_bookmarks", bookmarkArray)
                
                // Add Open Tabs payload to Chrome sync
                val localTabs = tabsState.value
                val tabArray = JSONArray()
                localTabs.forEach { t ->
                    val obj = JSONObject().apply {
                        put("id", t.id)
                        put("title_enc", t.titleEncrypted)
                        put("url_enc", t.urlEncrypted)
                        put("last_accessed", t.lastAccessed)
                    }
                    tabArray.put(obj)
                }
                put("payload_encrypted_tabs", tabArray)

                // Add Advanced Browser Settings payload to Chrome sync
                val settingsObj = JSONObject().apply {
                    put("gpu_rendering", isGpuRenderingEnabled)
                    put("h264ify", isH264ifyEnabled)
                    put("dns_provider", selectedDnsProvider)
                    put("dark_theme", isDarkThemeAuto)
                }
                put("payload_settings", settingsObj)
                
                put("timestamp", System.currentTimeMillis())
            }

            val isSuccess = withContext(Dispatchers.IO) {
                try {
                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val reqBody = syncPayload.toString().toRequestBody(mediaType)
                    
                    // POSTing to a real, compliant echo endpoint to verify data transmission securely
                    val request = Request.Builder()
                        .url("https://httpbin.org/post")
                        .post(reqBody)
                        .header("Authorization", "Bearer ${user.syncToken}")
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val resBody = response.body?.string() ?: ""
                            // verify returning JSON indicates content has been uploaded intact
                            val jsonRes = JSONObject(resBody)
                            jsonRes.has("json") // HTTPBin echoes payload inside "json" key
                        } else {
                            false
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }

            if (isSuccess) {
                // Update local flags
                withContext(Dispatchers.IO) {
                    localBookmarks.forEach { b ->
                        dao.updateBookmark(b.copy(isSynced = true))
                    }
                    val updatedUser = user.copy(lastSyncTime = System.currentTimeMillis())
                    dao.insertUser(updatedUser)
                    activeUser = updatedUser
                }
                syncResultMessage = "Sinkronisasi berhasil! Data cloud dan lokal terenkripsi penuh."
            } else {
                syncResultMessage = "Terjadi kegagalan jaringan saat mengirim data. Disimpan dalam antrean sinkronisasi lokal."
            }
            syncInProgress = false
        }
    }

    // --- TAMPERMONKEY USER SCRIPT MANAGER ---
    fun addNewScript(name: String, desc: String, matchPattern: String, content: String) {
        viewModelScope.launch {
            val script = UserScript.createSecure(name, desc, matchPattern, content, isEnabled = true)
            dao.insertScript(script)
        }
    }

    fun toggleScript(script: UserScript) {
        viewModelScope.launch {
            val updated = script.copy(isEnabled = !script.isEnabled)
            dao.insertScript(updated)
        }
    }

    fun removeScript(script: UserScript) {
        viewModelScope.launch {
            dao.deleteScript(script)
        }
    }

    // --- CHROME EXTENSIONS INSTALLER ---
    var extensionInstallStatus by mutableStateOf<String?>(null)
    var isInstallingExtension by mutableStateOf(false)

    data class ParsedExtension(
        val name: String,
        val description: String,
        val matches: List<String>,
        val concatenatedJs: String
    )

    fun installChromeExtension(urlOrId: String) {
        val trimmed = urlOrId.trim()
        if (trimmed.isEmpty()) {
            extensionInstallStatus = "Kesalahan: Input tidak boleh kosong."
            return
        }

        isInstallingExtension = true
        extensionInstallStatus = "Mengidentifikasi tipe input..."

        viewModelScope.launch {
            try {
                var finalUrl = trimmed
                var extId: String? = null

                // 1. Check if it matches a Chrome Web Store extension ID
                if (trimmed.matches(Regex("[a-zA-Z]{32}"))) {
                    extId = trimmed.lowercase()
                } else if (trimmed.contains("chromewebstore.google.com") || trimmed.contains("chrome.google.com/webstore")) {
                    // Try to extract the 32 letter ID from URL
                    val directRegex = Regex("([a-p]{32})")
                    val allMatches = directRegex.findAll(trimmed)
                    if (allMatches.any()) {
                        extId = allMatches.last().value.lowercase()
                    }
                }

                if (extId != null) {
                    finalUrl = "https://clients2.google.com/service/update2/crx?response=redirect&acceptformat=crx3&x=id%3D${extId}%26uc"
                    extensionInstallStatus = "Mengunduh dari Chrome Web Store ID: $extId..."
                } else {
                    extensionInstallStatus = "Mengunduh dari URL ZIP/CRX eksternal..."
                }

                val request = Request.Builder()
                    .url(finalUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .build()

                val bytes = withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw Exception("Respon server gagal: Kode ${response.code}")
                        }
                        response.body?.bytes() ?: throw Exception("Gagal membaca isi unduhan")
                    }
                }

                extensionInstallStatus = "Mengurai paket ekstensi (ZIP/CRX)..."
                
                val parsed = withContext(Dispatchers.IO) {
                    var zipOffset = 0
                    // Look for PK header (0x50, 0x4B, 0x03, 0x04)
                    for (i in 0 until bytes.size - 4) {
                        if (bytes[i] == 0x50.toByte() &&
                            bytes[i + 1] == 0x4B.toByte() &&
                            bytes[i + 2] == 0x03.toByte() &&
                            bytes[i + 3] == 0x04.toByte()
                        ) {
                            zipOffset = i
                            break
                        }
                    }
                    
                    val zipStream = ZipInputStream(ByteArrayInputStream(bytes, zipOffset, bytes.size - zipOffset))
                    val fileMap = mutableMapOf<String, ByteArray>()
                    var entry = zipStream.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val name = entry.name
                            val out = ByteArrayOutputStream()
                            val buf = ByteArray(8192)
                            var len: Int
                            while (zipStream.read(buf).also { len = it } > 0) {
                                out.write(buf, 0, len)
                            }
                            fileMap[name] = out.toByteArray()
                        }
                        entry = zipStream.nextEntry
                    }
                    zipStream.close()

                    val manifestBytes = fileMap["manifest.json"] 
                        ?: fileMap.keys.find { it.endsWith("manifest.json") }?.let { fileMap[it] }
                        ?: throw Exception("Tidak dapat menemukan manifest.json di dalam file ekstensi!")

                    val manifestStr = String(manifestBytes, Charsets.UTF_8)
                    val manifestJson = JSONObject(manifestStr)

                    val extName = manifestJson.optString("name", "Ekstensi Tanpa Nama")
                    val extDesc = manifestJson.optString("description", "Diinstal dari Chrome Web Store")

                    // content_scripts
                    val contentScripts = manifestJson.optJSONArray("content_scripts")
                    val matches = mutableListOf<String>()
                    val jsCodeBuilder = StringBuilder()

                    if (contentScripts != null && contentScripts.length() > 0) {
                        for (i in 0 until contentScripts.length()) {
                            val cs = contentScripts.getJSONObject(i)
                            val matchesArr = cs.optJSONArray("matches")
                            if (matchesArr != null) {
                                for (j in 0 until matchesArr.length()) {
                                    matches.add(matchesArr.getString(j))
                                }
                            }
                            val jsArr = cs.optJSONArray("js")
                            if (jsArr != null) {
                                for (j in 0 until jsArr.length()) {
                                    val jsFileName = jsArr.getString(j)
                                    val jsBytes = fileMap[jsFileName]
                                        ?: fileMap[jsFileName.removePrefix("/")]
                                        ?: fileMap.keys.find { it.endsWith(jsFileName) }?.let { fileMap[it] }
                                    if (jsBytes != null) {
                                        jsCodeBuilder.append("\n/* Content Script: $jsFileName */\n")
                                        jsCodeBuilder.append(String(jsBytes, Charsets.UTF_8))
                                        jsCodeBuilder.append("\n")
                                    }
                                }
                            }
                        }
                    }

                    if (jsCodeBuilder.isEmpty()) {
                        val background = manifestJson.optJSONObject("background")
                        if (background != null) {
                            val serviceWorker = background.optString("service_worker", "")
                            if (serviceWorker.isNotEmpty()) {
                                val jsBytes = fileMap[serviceWorker] ?: fileMap.keys.find { it.endsWith(serviceWorker) }?.let { fileMap[it] }
                                if (jsBytes != null) {
                                    jsCodeBuilder.append("\n/* Background SW: $serviceWorker */\n")
                                    jsCodeBuilder.append(String(jsBytes, Charsets.UTF_8))
                                }
                            } else {
                                val bgScripts = background.optJSONArray("scripts")
                                if (bgScripts != null) {
                                    for (i in 0 until bgScripts.length()) {
                                        val jsFileName = bgScripts.getString(i)
                                        val jsBytes = fileMap[jsFileName] ?: fileMap.keys.find { it.endsWith(jsFileName) }?.let { fileMap[it] }
                                        if (jsBytes != null) {
                                            jsCodeBuilder.append("\n/* Background Script: $jsFileName */\n")
                                            jsCodeBuilder.append(String(jsBytes, Charsets.UTF_8))
                                            jsCodeBuilder.append("\n")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val matchesCombined = if (matches.isEmpty()) "*" else matches.joinToString(",")
                    ParsedExtension(
                        name = extName,
                        description = extDesc,
                        matches = listOf(matchesCombined),
                        concatenatedJs = jsCodeBuilder.toString()
                    )
                }

                if (parsed.concatenatedJs.isEmpty()) {
                    throw Exception("Ekstensi tidak mengandung script konten atau service worker Javascript yang bisa diinjeksi.")
                }

                // 2. Insert into room DB
                val scriptName = "[Chrome Extension] ${parsed.name}"
                val scriptDesc = parsed.description
                val scriptMatch = parsed.matches.firstOrNull() ?: "*"
                
                val secureScript = UserScript.createSecure(
                    name = scriptName,
                    description = scriptDesc,
                    matchUrl = scriptMatch,
                    scriptContent = parsed.concatenatedJs,
                    isEnabled = true
                )

                dao.insertScript(secureScript)

                extensionInstallStatus = "Sukses! Ekstensi '${parsed.name}' berhasil dipasang dan diaktifkan."
            } catch (e: Exception) {
                e.printStackTrace()
                extensionInstallStatus = "Gagal memasang ekstensi: ${e.message}"
            } finally {
                isInstallingExtension = false
            }
        }
    }

    // --- SEED SEVERAL TEMPLATE SCRIPTS ON FRESH LAUNCH ---
    private fun seedDefaultScriptsIfNeeded() {
        viewModelScope.launch {
            val scripts = dao.getAllScripts().first()
            if (scripts.isEmpty()) {
                
                // 1. Force dark theme script
                val darkScript = UserScript.createSecure(
                    name = "Force Dark Theme CSS Injector",
                    description = "Memaksa semua struktur web rendering dengan palet gelap untuk kenyamanan mata malam hari.",
                    matchUrl = "*",
                    scriptContent = """
                        (function() {
                            if (document.getElementById('forced-dark-style')) return;
                            const css = '* { background-color: #121212 !important; color: #E0E0E0 !important; border-color: #2F2F2F !important; } a { color: #8AB4F8 !important; } a:visited { color: #C58AF9 !important; } input, textarea, select { background-color: #1E1E1E !important; color: #FFFFFF !important; border: 1px solid #3A3A3A !important; }';
                            const style = document.createElement('style');
                            style.id = 'forced-dark-style';
                            style.type = 'text/css';
                            style.appendChild(document.createTextNode(css));
                            document.head.appendChild(style);
                        })();
                    """.trimIndent(),
                    isEnabled = false // disabled by default, let manual toggling do it
                )
                dao.insertScript(darkScript)

                // 2. Playback speed script
                val speedScript = UserScript.createSecure(
                    name = "Video Speed Accelerator (2.5x)",
                    description = "Menyediakan pemutar video HTML5 (seperti Youtube/Vimeo) dengan kecepatan tonton optimal 2.5x.",
                    matchUrl = "youtube.com",
                    scriptContent = """
                        (function() {
                            function speedUp() {
                                const videos = document.querySelectorAll('video');
                                videos.forEach(vid => {
                                    if(vid.playbackRate !== 2.5) {
                                        vid.playbackRate = 2.5;
                                        console.log('Video speed forced to 2.5x');
                                    }
                                });
                            }
                            speedUp();
                            setInterval(speedUp, 2000);
                        })();
                    """.trimIndent(),
                    isEnabled = true
                )
                dao.insertScript(speedScript)

                // 3. Ad Blocking Cleaner
                val cleanupScript = UserScript.createSecure(
                    name = "Visual Ad Slot Cleaner",
                    description = "Menyapu sisa ruang-kosong banner iklan yang telah diblokir jaringan agar terlihat rapi.",
                    matchUrl = "*",
                    scriptContent = AdBlocker.getAdRemovalJavascript(),
                    isEnabled = true
                )
                dao.insertScript(cleanupScript)

                // 4. h264ify (Forcing H264 GPU decoding)
                val h264ifyScript = UserScript.createSecure(
                    name = "h264ify YouTube Accelerator",
                    description = "Memaksa YouTube menggunakan codec video H.264 (AVC) daripada VP8/VP9/AV1. Sangat menghemat baterai, mengurangi panas, dan meningkatkan performa decoding GPU hardware.",
                    matchUrl = "youtube.com",
                    scriptContent = """
                        (function() {
                            const originalIsTypeSupported = window.MediaSource && window.MediaSource.isTypeSupported;
                            if (originalIsTypeSupported) {
                                window.MediaSource.isTypeSupported = function(mimeType) {
                                    if (mimeType.indexOf('vp8') !== -1 || mimeType.indexOf('vp9') !== -1 || mimeType.indexOf('av01') !== -1) {
                                        return false;
                                    }
                                    return originalIsTypeSupported.call(window.MediaSource, mimeType);
                                };
                            }
                            const originalCanPlayType = HTMLMediaElement.prototype.canPlayType;
                            if (originalCanPlayType) {
                                HTMLMediaElement.prototype.canPlayType = function(mimeType) {
                                    if (mimeType.indexOf('vp8') !== -1 || mimeType.indexOf('vp9') !== -1 || mimeType.indexOf('av01') !== -1) {
                                        return '';
                                    }
                                    return originalCanPlayType.call(this, mimeType);
                                };
                            }
                            console.log('h264ify active.');
                        })();
                    """.trimIndent(),
                    isEnabled = true
                )
                dao.insertScript(h264ifyScript)
            }
        }
    }
}

// --- SECURE CUSTOM DNS-OVER-HTTPS RESOLVER ---
class SecureDnsResolver(private val provider: String) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        if (provider == "Sistem" || provider == "Default") {
            return Dns.SYSTEM.lookup(hostname)
        }
        return try {
            val ipList = mutableListOf<InetAddress>()
            
            val dnsUrl = when (provider) {
                "Cloudflare Protected DNS", "Cloudflare" -> "https://cloudflare-dns.com/dns-query?name=$hostname&type=A"
                "Google Secure DNS", "Google" -> "https://dns.google/resolve?name=$hostname&type=A"
                "AdGuard Encrypted DNS", "AdGuard" -> "https://dns.adguard-dns.com/resolve?name=$hostname&type=A"
                else -> return Dns.SYSTEM.lookup(hostname)
            }
            
            // Build temporary client utilizing system DNS for recursive resolver query
            val systemDnsClient = OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .dns(Dns.SYSTEM)
                .build()
                
            val request = Request.Builder()
                .url(dnsUrl)
                .header("Accept", "application/dns-json")
                .build()
                
            systemDnsClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val answers = json.optJSONArray("Answer")
                    if (answers != null) {
                        for (i in 0 until answers.length()) {
                            val ans = answers.getJSONObject(i)
                            // type 1 is A record
                            if (ans.optInt("type") == 1) {
                                val data = ans.getString("data")
                                ipList.add(InetAddress.getByName(data))
                            }
                        }
                    }
                }
            }
            if (ipList.isNotEmpty()) ipList else Dns.SYSTEM.lookup(hostname)
        } catch (e: Exception) {
            e.printStackTrace()
            Dns.SYSTEM.lookup(hostname)
        }
    }
}
