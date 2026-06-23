package com.example.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.*
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.viewinterop.AndroidView
import com.example.adblock.AdBlocker
import com.example.db.Tab
import com.example.db.ShortcutItem
import com.example.db.HistoryItem
import com.example.viewmodel.BrowserViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    activeTabUrl: String,
    activeTabId: Long,
    onNavigateToMenu: (ScreenType) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var inputUrl by remember { mutableStateOf(activeTabUrl) }
    var pageTitle by remember { mutableStateOf("Web Browser") }
    var webProgress by remember { mutableStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }
    
    var customView by remember { mutableStateOf<android.view.View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    
    var isMenuExpanded by remember { mutableStateOf(false) }

    var isTyping by remember { mutableStateOf(false) }

    // Sync input bar URL with active tab
    LaunchedEffect(activeTabUrl, activeTabId) {
        if (!isTyping) {
            inputUrl = if (activeTabUrl == "cyber://home") "" else activeTabUrl
        }
        if (activeTabUrl != "cyber://home" && activeTabUrl.isNotBlank()) {
            val currentUrl = webViewInstance?.url
            val orgUrl = webViewInstance?.originalUrl
            
            // Only load if the webview is not already showing something similar
            if (currentUrl != null) {
               val cUrl = currentUrl.removeSuffix("/")
               val aUrl = activeTabUrl.removeSuffix("/")
               if (cUrl != aUrl && orgUrl?.removeSuffix("/") != aUrl) {
                   webViewInstance?.loadUrl(activeTabUrl)
               }
            } else {
               webViewInstance?.loadUrl(activeTabUrl)
            }
        }
    }

    // Capture hardware/gesture back press inside WebView history
    BackHandler(enabled = customView != null || webViewInstance?.canGoBack() == true) {
        if (customView != null) {
            customViewCallback?.onCustomViewHidden()
        } else {
            webViewInstance?.goBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                // Top Address Controller Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Adblock shield badge indicator
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (viewModel.isAdBlockEnabled && viewModel.adsBlockedInCurrentPage > 0) {
                                SecureGreen.copy(alpha = 0.2f)
                            } else {
                                MidnightBackground
                            }
                        ),
                        border = if (viewModel.isAdBlockEnabled && viewModel.adsBlockedInCurrentPage > 0) {
                            BorderStroke(1.dp, SecureGreen)
                        } else {
                            BorderStroke(1.dp, SoftGrey.copy(alpha = 0.4f))
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .clickable { viewModel.isAdBlockEnabled = !viewModel.isAdBlockEnabled }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (viewModel.isAdBlockEnabled) Icons.Default.Shield else Icons.Default.ShieldMoon,
                                contentDescription = "AdBlock Status",
                                tint = if (viewModel.isAdBlockEnabled) SecureGreen else SoftGrey,
                                modifier = Modifier.size(18.dp)
                            )
                            AnimatedVisibility(visible = viewModel.isAdBlockEnabled && viewModel.adsBlockedInCurrentPage > 0) {
                                Row {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${viewModel.adsBlockedInCurrentPage}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SecureGreen
                                    )
                                }
                            }
                        }
                    }

                    // Main search / URL input field
                    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .onFocusChanged { isTyping = it.isFocused },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search,
                            autoCorrect = false
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                isTyping = false
                                focusManager.clearFocus()
                                val trimmed = inputUrl.trim()
                                if (trimmed.isNotEmpty()) {
                                    var destination = trimmed
                                    if (!destination.startsWith("http://") && !destination.startsWith("https://")) {
                                        if (destination.contains(".") && !destination.contains(" ")) {
                                            destination = "https://$destination"
                                        } else {
                                            destination = "https://www.google.com/search?q=${destination.replace(" ", "+")}"
                                        }
                                    }
                                    inputUrl = destination
                                    viewModel.updateCurrentTabUrl("Situs Baru", destination)
                                    webViewInstance?.loadUrl(destination)
                                }
                            }
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = if (inputUrl.startsWith("https://")) Icons.Default.Lock else Icons.Default.Language,
                                contentDescription = null,
                                tint = if (inputUrl.startsWith("https://")) SecureGreen else SoftGrey,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            if (inputUrl.isNotEmpty()) {
                                IconButton(
                                    onClick = { inputUrl = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Search",
                                        tint = SoftGrey,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = SoftGrey.copy(alpha = 0.5f),
                            focusedContainerColor = MidnightBackground,
                            unfocusedContainerColor = MidnightBackground,
                            cursorColor = CyberCyan
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )

                    // Bookmark star toggle button
                    IconButton(
                        onClick = {
                            viewModel.toggleCurrentPageBookmark(pageTitle, inputUrl)
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        val isBookmarked = viewModel.isCurrentPageBookmarked(inputUrl)
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Simpan Bookmark",
                            tint = if (isBookmarked) CyberCyan else SoftGrey
                        )
                    }
                }

                // Loading progress indicator
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = webProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = CyberCyan,
                        trackColor = Color.Transparent
                    )
                }
            }
        },
        bottomBar = {
            // Elegant Navigation Toolbar
            Surface(
                tonalElevation = 8.dp,
                color = MidnightSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { webViewInstance?.goBack() },
                        enabled = webViewInstance?.canGoBack() == true
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali",
                            tint = if (webViewInstance?.canGoBack() == true) CyberCyan else SoftGrey
                        )
                    }

                    IconButton(
                        onClick = { webViewInstance?.goForward() },
                        enabled = webViewInstance?.canGoForward() == true
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Maju",
                            tint = if (webViewInstance?.canGoForward() == true) CyberCyan else SoftGrey
                        )
                    }

                    // Reload/Stop action based on status
                    IconButton(
                        onClick = {
                            if (isLoading) webViewInstance?.stopLoading() else webViewInstance?.reload()
                        }
                    ) {
                        Icon(
                            imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                            contentDescription = "Muat Ulang / Stop",
                            tint = CyberCyan
                        )
                    }

                    // Tabs Count indicator
                    IconButton(onClick = { onNavigateToMenu(ScreenType.TABS) }) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Tab, contentDescription = "Active Tabs", tint = CyberCyan)
                            Box(
                                modifier = Modifier
                                    .offset(x = 10.dp, y = (-10).dp)
                                    .size(17.dp)
                                    .clip(CircleShape)
                                    .background(CyberPurple),
                                contentAlignment = Alignment.Center
                            ) {
                                val tabsCount by viewModel.tabsState.collectAsState()
                                Text(
                                    text = "${tabsCount.size}",
                                    color = CleanWhite,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Menu button
                    Box {
                        IconButton(onClick = { isMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Bilah Menu", tint = CyberCyan)
                        }

                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false },
                            modifier = Modifier.background(MidnightSurfaceCard)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Beranda Cyber Link", color = CleanWhite) },
                                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = CyberCyan) },
                                onClick = {
                                    isMenuExpanded = false
                                    viewModel.updateCurrentTabUrl("Beranda", "cyber://home")
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Riwayat Penjelajahan", color = CleanWhite) },
                                leadingIcon = { Icon(Icons.Default.History, contentDescription = null, tint = CyberCyan) },
                                onClick = {
                                    isMenuExpanded = false
                                    onNavigateToMenu(ScreenType.HISTORY)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Pengelola Unduhan", color = CleanWhite) },
                                leadingIcon = { Icon(androidx.compose.material.icons.Icons.Default.Download, contentDescription = null, tint = CyberCyan) },
                                onClick = {
                                    isMenuExpanded = false
                                    onNavigateToMenu(ScreenType.DOWNLOADS)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Ekstensi Chrome", color = CleanWhite) },
                                leadingIcon = { Icon(Icons.Default.Extension, contentDescription = null, tint = CyberCyan) },
                                onClick = {
                                    isMenuExpanded = false
                                    onNavigateToMenu(ScreenType.EXTENSIONS)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Bookmark Tersimpan", color = CleanWhite) },
                                leadingIcon = { Icon(androidx.compose.material.icons.Icons.Default.Book, contentDescription = null, tint = CyberCyan) },
                                onClick = {
                                    isMenuExpanded = false
                                    onNavigateToMenu(ScreenType.BOOKMARKS)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Sinkronisasi Akun", color = CleanWhite) },
                                leadingIcon = { Icon(Icons.Default.CloudSync, contentDescription = null, tint = CyberCyan) },
                                onClick = {
                                    isMenuExpanded = false
                                    onNavigateToMenu(ScreenType.ACCOUNT)
                                }
                            )

                            Divider(color = MidnightBackground)

                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Force Dark Mode", color = CleanWhite)
                                        Switch(
                                            checked = viewModel.isDarkThemeAuto,
                                            onCheckedChange = { viewModel.isDarkThemeAuto = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                                checkedTrackColor = CyberCyan
                                            ),
                                            modifier = Modifier.scale(0.7f)
                                        )
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null, tint = CyberCyan) },
                                onClick = {
                                    viewModel.isDarkThemeAuto = !viewModel.isDarkThemeAuto
                                    isMenuExpanded = false
                                    webViewInstance?.reload()
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MidnightBackground)
        ) {
            if (activeTabUrl == "cyber://home" || activeTabUrl.isBlank() || activeTabUrl == "about:blank") {
                CustomComposeHome(
                    viewModel = viewModel,
                    onNavigateToUrl = { targetUrl ->
                        inputUrl = targetUrl
                        viewModel.updateCurrentTabUrl("Situs Baru", targetUrl)
                        webViewInstance?.loadUrl(targetUrl)
                    }
                )
            } else {
                key(activeTabId) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            val cachedWebView = viewModel.webViewCache.getOrPut(activeTabId) {
                                WebView(context).apply {
                                    webViewInstance = this
                                    
                                    // Register the secure Javascript ↔ Kotlin communication bridge
                                    addJavascriptInterface(object {
                                        @android.webkit.JavascriptInterface
                                        fun postMessage(message: String) {
                                            viewModel.extensionManager.onMessageFromPage(message, this@apply)
                                        }
                                    }, "PolkeJSBridge")
                                    
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        databaseEnabled = true
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                        cacheMode = WebSettings.LOAD_DEFAULT
                                        offscreenPreRaster = true
                                        mediaPlaybackRequiresUserGesture = false
                                    }
                            
                            // Apply initial GPU hardware acceleration state
                            if (viewModel.isGpuRenderingEnabled) {
                                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                            } else {
                                setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                            }
                            
                            webViewClient = object : WebViewClient() {
                                
                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    if (request == null) return null
                                    
                                    // Let our ExtensionManager handle interception
                                    val response = viewModel.extensionManager.shouldIntercept(request)
                                    if (response != null) {
                                        return response
                                    }
                                    
                                    return super.shouldInterceptRequest(view, request)
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    
                                    if (url != null) {
                                        viewModel.extensionManager.onPageStarted(url)
                                    }
                                    
                                    // Early injection of h264ify to intercept media source queries instantly
                                    if (viewModel.isH264ifyEnabled) {
                                        val h264ifyJs = """
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
                                                console.log('h264ify early shield activated.');
                                            })();
                                        """.trimIndent()
                                        view?.evaluateJavascript(h264ifyJs, null)
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    if (url != null) {
                                        inputUrl = url
                                        pageTitle = view?.title ?: "Web Page"
                                        viewModel.updateCurrentTabUrl(pageTitle, url)

                                        if (view != null) {
                                            viewModel.extensionManager.onPageFinished(url, view)
                                        }

                                        // --- Injeksi custom h264ify pada halaman selesai dimuat ---
                                        if (viewModel.isH264ifyEnabled) {
                                            val h264ifyJs = """
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
                                                    console.log('h264ify persistent shield injected.');
                                                })();
                                            """.trimIndent()
                                            view?.evaluateJavascript(h264ifyJs, null)
                                        }
                                    }
                                }
                            }

                            // Handle page web chrome actions, update progress bar
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    webProgress = newProgress / 100f
                                    isLoading = newProgress < 100
                                }
                                override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                                    super.onShowCustomView(view, callback)
                                    customView = view
                                    customViewCallback = callback
                                }
                                override fun onHideCustomView() {
                                    super.onHideCustomView()
                                    customView = null
                                    customViewCallback = null
                                }
                            }

                            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                                val filename = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype)
                                viewModel.startDownload(url, filename)
                                Toast.makeText(context, "Mulai Mengunduh $filename", Toast.LENGTH_SHORT).show()
                            }

                            loadUrl(activeTabUrl)
                                }
                            }
                            (cachedWebView.parent as? android.view.ViewGroup)?.removeView(cachedWebView)
                            cachedWebView.also { webViewInstance = it }
                        },
                    update = { webView ->
                        // Dynamically toggle GPU rendering layer on runtime setting changes
                        if (viewModel.isGpuRenderingEnabled) {
                            webView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                        } else {
                            webView.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                        }
                    }
                )
                }
            }
        }
    }
        
        if (customView != null) {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { customView!! }
                )
            }
        }
    }
}

/**
 * Validates match glob or domain string against target loaded URL.
 */
fun isUrlScriptMatched(pattern: String, url: String): Boolean {
    val cleanPattern = pattern.trim().lowercase()
    if (cleanPattern == "*" || cleanPattern == "*://*/*" || cleanPattern == "<all_urls>" || cleanPattern.isEmpty()) return true
    val cleanUrl = url.lowercase()
    
    val parts = if (cleanPattern.contains(",")) {
        cleanPattern.split(",").map { it.trim() }
    } else {
        listOf(cleanPattern)
    }

    return parts.any { part ->
        if (part == "*" || part == "*://*/*" || part == "<all_urls>" || part.isEmpty()) {
            true
        } else {
            // Convert wildcard pattern like *youtube.com* to regex
            // Safely escape regex characters then map * to .*
            val escaped = part
                .replace("\\", "\\\\")
                .replace(".", "\\.")
                .replace("?", "\\?")
                .replace("+", "\\+")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("^", "\\^")
                .replace("$", "\\$")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("*", ".*")
            try {
                cleanUrl.matches(Regex(escaped)) || cleanUrl.contains(part)
            } catch (e: Exception) {
                cleanUrl.contains(part)
            }
        }
    }
}

@Composable
fun CustomComposeHome(
    viewModel: BrowserViewModel,
    onNavigateToUrl: (String) -> Unit
) {
    val shortcuts by viewModel.shortcutsState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    var newTitle by remember { mutableStateOf("") }
    var newUrl by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .background(MidnightBackground),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Branding Glow
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "CYBER LINK",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = CleanWhite,
                    letterSpacing = 2.sp
                )
            )
        }
        Text(
            text = "Asisten Peramban Aman",
            fontSize = 12.sp,
            color = CyberPurple,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Large Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari web atau masukkan URL...", color = SoftGrey) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyberCyan) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SoftGrey)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    val trimmed = searchQuery.trim()
                    if (trimmed.isNotEmpty()) {
                        var target = trimmed
                        if (!target.startsWith("http://") && !target.startsWith("https://")) {
                            if (target.contains(".") && !target.contains(" ")) {
                                target = "https://$target"
                            } else {
                                target = "https://www.google.com/search?q=${target.replace(" ", "+")}"
                            }
                        }
                        onNavigateToUrl(target)
                    }
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = MidnightSurfaceCard,
                focusedContainerColor = MidnightSurface,
                unfocusedContainerColor = MidnightSurface,
                focusedTextColor = CleanWhite,
                unfocusedTextColor = CleanWhite
            ),
            shape = RoundedCornerShape(28.dp),
            singleLine = true
        )

        // Shortcuts Title
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pintasan Cepat",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = CyberCyan
            )
            Text(
                text = "Klik silang kecil untuk hapus",
                fontSize = 11.sp,
                color = SoftGrey
            )
        }

        // Shortcuts Grid / Row-based arrangement
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp)
        ) {
            items(shortcuts.size) { index ->
                val shortcut = shortcuts[index]
                Box(
                    modifier = Modifier
                        .height(96.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MidnightSurface)
                        .border(1.dp, MidnightSurfaceCard, RoundedCornerShape(12.dp))
                        .clickable { onNavigateToUrl(shortcut.url) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CyberPurple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = shortcut.title.take(1).uppercase(),
                                fontWeight = FontWeight.ExtraBold,
                                color = CyberPurple,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = shortcut.title,
                            fontSize = 11.sp,
                            color = CleanWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Little delete button on top-right
                    IconButton(
                        onClick = { viewModel.removeShortcut(shortcut.id) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hapus",
                            tint = Color.Red.copy(alpha = 0.8f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // ADD SHORTCUT CARD ITEM
            item {
                Box(
                    modifier = Modifier
                        .height(96.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MidnightSurface)
                        .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { showAddDialog = true }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Tambah",
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tambah",
                            fontSize = 11.sp,
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    // ADD SHORTCUT DIALOG
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tambah Pintasan Baru", fontWeight = FontWeight.Bold, color = CyberCyan) },
            containerColor = MidnightSurface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Nama Situs", color = SoftGrey) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CleanWhite,
                            unfocusedTextColor = CleanWhite,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = SoftGrey
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newUrl,
                        onValueChange = { newUrl = it },
                        label = { Text("Alamat URL (www.situs.com)", color = SoftGrey) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CleanWhite,
                            unfocusedTextColor = CleanWhite,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = SoftGrey
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank() && newUrl.isNotBlank()) {
                            viewModel.addShortcut(newTitle.trim(), newUrl.trim())
                            newTitle = ""
                            newUrl = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("Tambah", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Batal", color = SoftGrey)
                }
            }
        )
    }
}
