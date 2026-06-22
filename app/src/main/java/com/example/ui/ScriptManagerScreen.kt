package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.db.UserScript
import com.example.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptManagerScreen(
    viewModel: BrowserViewModel,
    onBack: () -> Unit
) {
    val scripts by viewModel.scriptsState.collectAsState()
    var isAddingScript by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var matchPattern by remember { mutableStateOf("") }
    var scriptCode by remember { mutableStateOf("") }
    var addError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Script Manager (Tampermonkey)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        isAddingScript = !isAddingScript 
                        addError = null
                    }) {
                        Icon(
                            imageVector = if (isAddingScript) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Tambah Script",
                            tint = CyberCyan
                        )
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
        var selectedTabIndex by remember { mutableStateOf(0) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MidnightSurface,
                contentColor = CyberCyan
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Script Tampermonkey", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Ekstensi Chrome", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTabIndex == 0) {
                AnimatedVisibility(
                    visible = isAddingScript,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MidnightSurface)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Tambah Skrip Pengguna Baru",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = CyberCyan
                            )

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Nama Skrip (contoh: Ad Block Enhancer)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    focusedLabelColor = CyberCyan,
                                    unfocusedBorderColor = SoftGrey
                                )
                            )

                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Deskripsi") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    focusedLabelColor = CyberCyan,
                                    unfocusedBorderColor = SoftGrey
                                )
                            )

                            OutlinedTextField(
                                value = matchPattern,
                                onValueChange = { matchPattern = it },
                                label = { Text("Pencocokan URL (contoh: youtube.com atau * untuk semua)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    focusedLabelColor = CyberCyan,
                                    unfocusedBorderColor = SoftGrey
                                )
                            )

                            OutlinedTextField(
                                value = scriptCode,
                                onValueChange = { scriptCode = it },
                                label = { Text("Kode Javascript (IIFE direkomendasikan)") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 4,
                                maxLines = 8,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    focusedLabelColor = CyberCyan,
                                    unfocusedBorderColor = SoftGrey
                                )
                            )

                            if (addError != null) {
                                Text(addError ?: "", color = Color.Red, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    if (name.isBlank() || matchPattern.isBlank() || scriptCode.isBlank()) {
                                        addError = "Nama, Pola URL, dan Kode skrip tidak boleh kosong!"
                                        return@Button
                                    }
                                    viewModel.addNewScript(name, description, matchPattern, scriptCode)
                                    // Reset form
                                    name = ""
                                    description = ""
                                    matchPattern = ""
                                    scriptCode = ""
                                    isAddingScript = false
                                    addError = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                            ) {
                                Text("Simpan Skrip", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Text(
                    "Skrip Pengguna Aktif (${scripts.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SoftGrey,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )

                if (scripts.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CodeOff,
                            contentDescription = null,
                            tint = SoftGrey,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Skrip Kosong",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanWhite
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(scripts) { script ->
                            ScriptItemRow(
                                script = script,
                                onToggle = { viewModel.toggleScript(script) },
                                onDelete = { viewModel.removeScript(script) }
                            )
                        }
                    }
                }
            } else {
                ChromeExtensionInstaller(viewModel)
            }
        }
    }
}

@Composable
fun ScriptItemRow(
    script: UserScript,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = if (script.isEnabled) CyberCyan else SoftGrey,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            script.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = CleanWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Match: ",
                                fontSize = 11.sp,
                                color = SoftGrey
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MidnightBackground)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    script.matchUrl,
                                    fontSize = 10.sp,
                                    color = CyberCyan,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
                
                Switch(
                    checked = script.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = CyberCyan,
                        uncheckedThumbColor = SoftGrey,
                        uncheckedTrackColor = MidnightBackground
                    )
                )
            }

            if (script.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    script.description,
                    fontSize = 12.sp,
                    color = SoftGrey
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MidnightBackground)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EnhancedEncryption,
                        contentDescription = null,
                        tint = SecureGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Code Encrypted (AES)",
                        fontSize = 10.sp,
                        color = SecureGreen
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = SoftGrey,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
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
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isInstalling && extInputUrl.isNotBlank()
                ) {
                    if (isInstalling) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
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
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPurple, contentColor = Color.White),
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
