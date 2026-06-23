package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.db.UserScript
import com.example.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionManagerScreen(
    viewModel: BrowserViewModel,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ekstensi Chrome", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
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
        ) {
            ChromeExtensionInstaller(viewModel)
        }
    }
}


@Composable
fun ChromeExtensionInstaller(viewModel: BrowserViewModel) {
    var extInputUrl by remember { mutableStateOf("") }
    val installStatus = viewModel.extensionInstallStatus
    val isInstalling = viewModel.isInstallingExtension

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core native intelligent extensions list
        NativeExtensionsList(viewModel)
        
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Instal Ekstensi Chrome (CRX / ZIP)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = CyberCyan
                    )
                }

                Text(
                    text = "Peramban ini mendukung pemasangan ekstensi Chrome secara otomatis. Ekstensi akan diunduh, diurai manifest nya, dan diinjeksi sebagai pelindung / fitur peramban secara aman.",
                    fontSize = 12.sp,
                    color = SoftGrey
                )

                OutlinedTextField(
                    value = extInputUrl,
                    onValueChange = { extInputUrl = it },
                    placeholder = { Text("Masukkan ID / Link Chrome Web Store atau URL ZIP...", fontSize = 12.sp, color = SoftGrey) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CleanWhite,
                        unfocusedTextColor = CleanWhite,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = SoftGrey
                    ),
                    singleLine = true,
                    enabled = !isInstalling
                )

                Button(
                    onClick = {
                        viewModel.installChromeExtension(extInputUrl)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = MaterialTheme.colorScheme.onPrimary),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isInstalling && extInputUrl.isNotBlank()
                ) {
                    if (isInstalling) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Memasang Ekstensi...", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Pasang Ekstensi", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Installation Log / Status Info
        if (installStatus != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (installStatus.startsWith("Sukses")) SecureGreen.copy(alpha = 0.1f) 
                                     else if (installStatus.startsWith("Gagal") || installStatus.startsWith("Kesalahan")) Color.Red.copy(alpha = 0.1f) 
                                     else MidnightSurface
                ),
                border = BorderStroke(
                    1.dp,
                    if (installStatus.startsWith("Sukses")) SecureGreen.copy(alpha = 0.5f)
                    else if (installStatus.startsWith("Gagal") || installStatus.startsWith("Kesalahan")) Color.Red.copy(alpha = 0.5f)
                    else CyberPurple.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (installStatus.startsWith("Sukses")) Icons.Default.CheckCircle 
                                     else if (installStatus.startsWith("Gagal") || installStatus.startsWith("Kesalahan")) Icons.Default.Error 
                                     else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (installStatus.startsWith("Sukses")) SecureGreen 
                               else if (installStatus.startsWith("Gagal") || installStatus.startsWith("Kesalahan")) Color.Red 
                               else CyberPurple,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = installStatus,
                        fontSize = 13.sp,
                        color = CleanWhite,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick Installation Section
        Text(
            text = "Rekomendasi Ekstensi Cepat",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = CyberPurple,
            modifier = Modifier.padding(top = 8.dp)
        )

        val quickExtensions = listOf(
            QuickExtItem(
                name = "uBlock Origin",
                desc = "Pemblokir iklan super efisien untuk menghemat kuota dan memblokir iklan pop-up.",
                id = "cjpalhdlnbpafiamejdnhcphjbkeiagm"
            ),
            QuickExtItem(
                name = "Dark Reader",
                desc = "Memaksa mode gelap ambient yang elegan pada setiap situs web untuk kesehatan mata.",
                id = "eimadpbcafjaebdcmjgbgogajbpeemgo"
            ),
            QuickExtItem(
                name = "Google Translate",
                desc = "Terjemahkan kata atau paragraf di halaman web mana saja secara instan.",
                id = "aapbdbdomjkkjkaonfhkkikfgjicgneb"
            )
        )

        quickExtensions.forEach { qExt ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        extInputUrl = qExt.id
                        viewModel.installChromeExtension(qExt.id)
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightSurface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = qExt.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = CleanWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = qExt.desc,
                            fontSize = 11.sp,
                            color = SoftGrey
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            extInputUrl = qExt.id
                            viewModel.installChromeExtension(qExt.id)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPurple, contentColor = MaterialTheme.colorScheme.onSecondary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Pasang", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

data class QuickExtItem(
    val name: String,
    val desc: String,
    val id: String
)

@Composable
fun NativeExtensionsList(viewModel: BrowserViewModel) {
    val nativeExts = viewModel.nativeExtensionsList
    
    LaunchedEffect(Unit) {
        viewModel.refreshNativeExtensions()
    }

    if (nativeExts.isNotEmpty()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Extension,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Kontrol Ekstensi Peramban (Bawaan)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = CyberCyan
                )
            }

            nativeExts.forEach { ext ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                    border = BorderStroke(1.dp, if (ext.isEnabled) CyberCyan.copy(alpha = 0.3f) else SoftGrey.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (ext.isEnabled) CyberCyan.copy(alpha = 0.1f) 
                                    else SoftGrey.copy(alpha = 0.1f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (ext.iconName) {
                                    "search" -> Icons.Default.Search
                                    "block" -> Icons.Default.Security
                                    "theme" -> Icons.Default.DarkMode
                                    "autofill" -> Icons.Default.Edit
                                    else -> Icons.Default.Extension
                                },
                                contentDescription = null,
                                tint = if (ext.isEnabled) CyberCyan else SoftGrey,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ext.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = CleanWhite
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = ext.description,
                                fontSize = 11.sp,
                                color = SoftGrey,
                                lineHeight = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Switch(
                            checked = ext.isEnabled,
                            onCheckedChange = { viewModel.toggleNativeExtension(ext.id) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = CyberCyan,
                                uncheckedThumbColor = SoftGrey,
                                uncheckedTrackColor = MidnightBackground
                            ),
                            modifier = Modifier.scale(0.85f)
                        )
                    }
                }
            }
        }
    }
}
