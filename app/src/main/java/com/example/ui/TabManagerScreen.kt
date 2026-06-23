package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.db.Tab
import com.example.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabManagerScreen(
    viewModel: BrowserViewModel,
    onBack: () -> Unit
) {
    val tabs by viewModel.tabsState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Tab Peramban", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.createNewTab("Beranda", "cyber://home")
                        onBack()
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Tab Baru", tint = CyberCyan)
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
        ) {
            Text(
                text = "${tabs.size} Tab Aktif",
                fontSize = 14.sp,
                color = SoftGrey,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(tabs, key = { it.id }) { tab ->
                    TabListItem(
                        tab = tab,
                        isSelected = viewModel.activeTab?.id == tab.id,
                        onClick = {
                            viewModel.selectTab(tab)
                            onBack()
                        },
                        onClose = {
                            viewModel.closeTab(tab)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.createNewTab("Beranda", "cyber://home")
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = MaterialTheme.colorScheme.onPrimary),
                shape = RoundedCornerShape(25.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buka Tab Baru", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TabListItem(
    tab: Tab,
    isSelected: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(2.dp, CyberCyan) else BorderStroke(1.dp, MidnightSurfaceCard),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MidnightSurfaceCard else MidnightSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = if (isSelected) CyberCyan else SoftGrey,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tab.title.ifEmpty { "Beranda" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tab.url,
                    fontSize = 11.sp,
                    color = SoftGrey,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Tutup Tab",
                    tint = SoftGrey,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
