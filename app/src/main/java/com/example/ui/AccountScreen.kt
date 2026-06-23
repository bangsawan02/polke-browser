package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.BrowserViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    viewModel: BrowserViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activeUser = viewModel.activeUser
    val bookmarks by viewModel.bookmarksState.collectAsState()
    val tabs by viewModel.tabsState.collectAsState()
    val syncInProgress = viewModel.syncInProgress
    val syncResult = viewModel.syncResultMessage
    val authError = viewModel.authError
    val authSuccess = viewModel.authSuccessMsg

    var customGmail by remember { mutableStateOf("") }
    var selectedDnsShowDropdown by remember { mutableStateOf(false) }

    val userBookmarksCount = remember(bookmarks, activeUser) {
        if (activeUser == null) 0
        else bookmarks.count { it.userId == activeUser.email }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Akun & Pengaturan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightSurface,
                    titleContentColor = CyberCyan,
                    navigationIconContentColor = CyberCyan
                )
            )
        },
        containerColor = MidnightBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Visual Header Widget
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(CyberCyan, CyberPurple)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = CleanWhite,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sinkronisasi Google Chrome-Link",
                        color = CleanWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 1: USER ACCOUNT & SYNC
            if (activeUser == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                    border = BorderStroke(1.dp, MidnightSurfaceCard)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Masuk Akun Google Anda",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Gunakan Akun Google untuk mengaktifkan sinkronisasi otomatis bookmark, tab terbuka, dan preferensi penjelajahan layaknya Google Chrome.",
                            fontSize = 12.sp,
                            color = SoftGrey,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Quick Login Card with detected user email
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.loginWithGoogle("telokuh@gmail.com")
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MidnightSurfaceCard),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(CyberPurple),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("T", color = CleanWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Masuk Cepat Google", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CleanWhite)
                                    Text("telokuh@gmail.com", fontSize = 12.sp, color = CyberCyan)
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom Google Authentication Button
                        Button(
                            onClick = {
                                viewModel.loginWithGoogle("telokuh@gmail.com")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.onTertiaryContainer),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("G ", color = Color(0xFF4285F4), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                Text("o", color = Color(0xFFEA4335), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                Text("o", color = Color(0xFFFBBC05), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                Text("g", color = Color(0xFF4285F4), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                Text("l", color = Color(0xFF34A853), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                Text("e ", color = Color(0xFFEA4335), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sambungkan Akun Google", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("ATAU MASUKKAN GMAIL MANUAL", fontSize = 10.sp, color = SoftGrey, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = customGmail,
                            onValueChange = { customGmail = it },
                            label = { Text("Masukkan Gmail") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = CyberCyan) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                focusedLabelColor = CyberCyan,
                                unfocusedBorderColor = SoftGrey
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (customGmail.isBlank()) {
                                    viewModel.loginWithGoogle("telokuh@gmail.com")
                                } else if (customGmail.contains("@") && customGmail.endsWith(".com")) {
                                    viewModel.loginWithGoogle(customGmail.trim())
                                } else {
                                    viewModel.loginWithGoogle("$customGmail@gmail.com")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = MaterialTheme.colorScheme.onPrimary),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Text("Hubungkan Akun Custom", fontWeight = FontWeight.Bold)
                        }

                        AnimatedVisibility(visible = authError != null) {
                            Text(
                                text = authError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                    border = BorderStroke(1.dp, MidnightSurfaceCard)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(CyberPurple),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = activeUser.email.take(1).uppercase(),
                                    color = CleanWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Akun Google Chrome",
                                        fontSize = 11.sp,
                                        color = CyberCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = activeUser.email,
                                    fontSize = 15.sp,
                                    color = CleanWhite,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Status: Sinkronisasi Aktif",
                                    fontSize = 11.sp,
                                    color = SecureGreen
                                )
                            }
                        }

                        AnimatedVisibility(visible = authSuccess != null) {
                            Text(
                                text = authSuccess ?: "",
                                color = SecureGreen,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MidnightSurfaceCard)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Parameter Sinkronisasi", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Data Bookmark", fontSize = 13.sp, color = SoftGrey)
                            Text("$userBookmarksCount Item", fontSize = 13.sp, color = CleanWhite, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Data Tab Terbuka", fontSize = 13.sp, color = SoftGrey)
                            Text("${tabs.size} Tab", fontSize = 13.sp, color = CleanWhite, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sinkronisasi Pengaturan", fontSize = 13.sp, color = SoftGrey)
                            Text("Aktif", fontSize = 13.sp, color = SecureGreen, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Kunci Enkripsi", fontSize = 13.sp, color = SoftGrey)
                            Text("AES-GCM-256", fontSize = 13.sp, color = SecureGreen, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Waktu Sinkron Terakhir", fontSize = 13.sp, color = SoftGrey)
                            val syncTimeStr = if (activeUser.lastSyncTime > 0) {
                                SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault()).format(Date(activeUser.lastSyncTime))
                            } else {
                                "Belum sinkron"
                            }
                            Text(syncTimeStr, fontSize = 13.sp, color = CleanWhite, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        if (syncInProgress) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = CyberCyan, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = syncResult ?: "Menghubungkan cloud sync Google...",
                                    fontSize = 13.sp,
                                    color = CyberCyan
                                )
                            }
                        } else {
                            Button(
                                onClick = { viewModel.runLocalAndCloudSync() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = MaterialTheme.colorScheme.onPrimary),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Paksa Sinkron Sekarang", fontWeight = FontWeight.Bold)
                            }
                        }

                        AnimatedVisibility(visible = syncResult != null && !syncInProgress) {
                            Text(
                                text = syncResult ?: "",
                                color = if (syncResult?.contains("berhasil", ignoreCase = true) == true) SecureGreen else Color.Red,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            border = BorderStroke(1.dp, Color.Red),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Logout Akun Google")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 2: ADVANCED WEB ENGINE SETTINGS (GPU / DNS / H264IFY)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                border = BorderStroke(1.dp, MidnightSurfaceCard)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Pengaturan Engine Web Lanjut",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // GPU Rendering Option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Akselerasi GPU Hardware",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CleanWhite
                            )
                            Text(
                                text = "Menggunakan rendering hardware GPU untuk mempercepat visual halaman web & scrolling lancar 60fps.",
                                fontSize = 11.sp,
                                color = SoftGrey
                            )
                        }
                        Switch(
                            checked = viewModel.isGpuRenderingEnabled,
                            onCheckedChange = { viewModel.isGpuRenderingEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = CyberCyan,
                                uncheckedThumbColor = SoftGrey,
                                uncheckedTrackColor = MidnightSurfaceCard
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MidnightSurfaceCard)
                    Spacer(modifier = Modifier.height(16.dp))

                    // h264ify Switch Option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Akselerator h264ify YouTube",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CleanWhite
                            )
                            Text(
                                text = "Memaksa YouTube mengalirkan video berkode H.264 (AVC) yang didukung penuh akselerasi GPU chip agar hemat baterai.",
                                fontSize = 11.sp,
                                color = SoftGrey
                            )
                        }
                        Switch(
                            checked = viewModel.isH264ifyEnabled,
                            onCheckedChange = { viewModel.isH264ifyEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = CyberCyan,
                                uncheckedThumbColor = SoftGrey,
                                uncheckedTrackColor = MidnightSurfaceCard
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MidnightSurfaceCard)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Cloud Secure DNS IP / Resolver selection
                    Text(
                        text = "Secure DNS-over-HTTPS (DoH)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CleanWhite
                    )
                    Text(
                        text = "Amankan kueri pencarian Web dan sinkronisasi menggunakan proteksi server DNS terenkripsi HTTPS.",
                        fontSize = 11.sp,
                        color = SoftGrey
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Box {
                        OutlinedButton(
                            onClick = { selectedDnsShowDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = viewModel.selectedDnsProvider,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanWhite
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = CyberCyan)
                            }
                        }

                        DropdownMenu(
                            expanded = selectedDnsShowDropdown,
                            onDismissRequest = { selectedDnsShowDropdown = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(MidnightSurface)
                        ) {
                            val providers = listOf(
                                "Sistem (Default ISP)",
                                "Google Secure DNS",
                                "Cloudflare Protected DNS",
                                "AdGuard Encrypted DNS"
                            )
                            providers.forEach { provider ->
                                DropdownMenuItem(
                                    text = { Text(provider, color = CleanWhite, fontSize = 13.sp) },
                                    onClick = {
                                        viewModel.selectedDnsProvider = provider
                                        selectedDnsShowDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 3: PRIVACY & SYSTEM CLEANUP (COOKIE, CACHE, HISTORY)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                border = BorderStroke(1.dp, MidnightSurfaceCard)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Privasi & Pembersihan Data",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Kelola data lokal peramban Anda dengan membersihkan cookie, cache sistem, dan riwayat penjelajahan secara instan.",
                        fontSize = 11.sp,
                        color = SoftGrey
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Buttons of clear data
                    Button(
                        onClick = { viewModel.clearBrowserCacheAndCookies(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.15f), contentColor = Color.Red),
                        border = BorderStroke(1.dp, Color.Red),
                        shape = RoundedCornerShape(23.dp)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bersihkan Cookie & Cache", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.clearAllHistory() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MidnightSurfaceCard, contentColor = CleanWhite),
                        border = BorderStroke(1.dp, SoftGrey.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(23.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kosongkan Semua Riwayat", fontWeight = FontWeight.Bold)
                    }

                    // Feedbacks message
                    if (authSuccess != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = authSuccess ?: "",
                            color = SecureGreen,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
