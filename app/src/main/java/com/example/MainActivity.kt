package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.viewmodel.BrowserViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Support Edge-to-Edge display flows
        enableEdgeToEdge()

        // Pre-create WebView JS and Wasm cache directories to eliminate noisy E/chromium opendir errors
        try {
            val jsCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
            val wasmCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
            if (!jsCacheDir.exists()) {
                jsCacheDir.mkdirs()
            }
            if (!wasmCacheDir.exists()) {
                wasmCacheDir.mkdirs()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        setContent {
            val viewModel: BrowserViewModel = viewModel()
            CyberBrowserTheme(darkTheme = viewModel.isDarkThemeAuto) {
                var currentScreen by remember { mutableStateOf(ScreenType.BROWSER) }
                
                // Get active tab URL
                val activeTab = viewModel.activeTab
                val activeTabUrl = activeTab?.url ?: "cyber://home"

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MidnightBackground
                ) {
                    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                        when (screen) {
                            ScreenType.BROWSER -> {
                                BrowserScreen(
                                    viewModel = viewModel,
                                    activeTabUrl = activeTabUrl,
                                    activeTabId = activeTab?.id ?: -1L,
                                    onNavigateToMenu = { targetScreen ->
                                        currentScreen = targetScreen
                                    }
                                )
                            }
                            ScreenType.TABS -> {
                                TabManagerScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = ScreenType.BROWSER }
                                )
                            }
                            ScreenType.BOOKMARKS -> {
                                BookmarkScreen(
                                    viewModel = viewModel,
                                    onNavigateToUrl = { url ->
                                        viewModel.activeTab?.let { active ->
                                            viewModel.updateCurrentTabUrl(active.title, url)
                                        } ?: viewModel.createNewTab("Web Page", url)
                                        currentScreen = ScreenType.BROWSER
                                    },
                                    onBack = { currentScreen = ScreenType.BROWSER }
                                )
                            }
                            ScreenType.EXTENSIONS -> {
                                ExtensionManagerScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = ScreenType.BROWSER }
                                )
                            }
                            ScreenType.ACCOUNT -> {
                                AccountScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = ScreenType.BROWSER }
                                )
                            }
                            ScreenType.DOWNLOADS -> {
                                DownloadScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = ScreenType.BROWSER }
                                )
                            }
                            ScreenType.HISTORY -> {
                                HistoryScreen(
                                    viewModel = viewModel,
                                    onNavigateToUrl = { url ->
                                        viewModel.activeTab?.let { active ->
                                            viewModel.updateCurrentTabUrl(active.title, url)
                                        } ?: viewModel.createNewTab("Web Page", url)
                                        currentScreen = ScreenType.BROWSER
                                    },
                                    onBack = { currentScreen = ScreenType.BROWSER }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
