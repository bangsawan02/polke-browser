package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(viewModel: BrowserViewModel, onBack: () -> Unit) {
    val downloads by viewModel.downloads.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengelola Unduhan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightSurface,
                    titleContentColor = CleanWhite,
                    navigationIconContentColor = CyberCyan
                )
            )
        },
        containerColor = MidnightBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Hardware Acceleration Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MidnightSurfaceCard, MaterialTheme.shapes.medium)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Akselerasi Download Hardware", color = CleanWhite, fontWeight = FontWeight.Bold)
                    Text(
                        "Aktifkan multi-thread chunck renderer untuk kecepatan unduh ultra dengan buffer memori yang lebih optimal.",
                        color = SoftGrey, fontSize = 12.sp, lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Switch(
                    checked = viewModel.isDownloadHardwareAcceleration,
                    onCheckedChange = { viewModel.isDownloadHardwareAcceleration = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = CyberCyan,
                        uncheckedThumbColor = SoftGrey,
                        uncheckedTrackColor = MidnightBackground
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MidnightSurfaceCard)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Riwayat Unduhan", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))

            if (downloads.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada file yang diunduh.", color = SoftGrey)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(downloads) { download ->
                        DownloadItemRow(download)
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadItemRow(download: Download) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurfaceCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = download.filename,
                    color = CleanWhite,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (download.status == "Completed") {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Selesai", tint = SecureGreen, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Default.Timer, contentDescription = "Mengunduh", tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            val progressRatio = download.progress.coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progressRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = if (download.status == "Completed") SecureGreen else CyberCyan,
                trackColor = MidnightBackground
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(download.status, fontSize = 12.sp, color = SoftGrey)
                Text("${(progressRatio * 100).toInt()}% • ${download.totalSize}", fontSize = 12.sp, color = CyberCyan)
            }
        }
    }
}
