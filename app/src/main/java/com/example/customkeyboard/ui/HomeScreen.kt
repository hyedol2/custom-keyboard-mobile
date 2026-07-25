package com.example.customkeyboard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.customkeyboard.bluetooth.HidConnectionState
import com.example.customkeyboard.bluetooth.HidKeyboardManager
import com.example.customkeyboard.data.KeyboardLayout
import com.example.customkeyboard.data.LayoutRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: LayoutRepository,
    hidManager: HidKeyboardManager,
    onNewLayout: () -> Unit,
    onEditLayout: (String) -> Unit,
    onRunLayout: (String) -> Unit,
    onOpenBluetooth: () -> Unit
) {
    var layouts by remember { mutableStateOf(repository.loadAll()) }
    val connState by hidManager.state
    val deviceName by hidManager.connectedDeviceName

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("커스텀 키보드") },
                actions = {
                    TextButton(onClick = onOpenBluetooth) {
                        Icon(Icons.Filled.Bluetooth, contentDescription = "블루투스")
                        Spacer(Modifier.width(4.dp))
                        Text(
                            when (connState) {
                                HidConnectionState.CONNECTED -> deviceName ?: "연결됨"
                                HidConnectionState.CONNECTING -> "연결중..."
                                else -> "연결 안됨"
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewLayout) {
                Icon(Icons.Filled.Add, contentDescription = "새 키보드")
            }
        }
    ) { padding ->
        if (layouts.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("아직 만든 키보드가 없어요.\n오른쪽 아래 + 버튼으로 새로 만들어보세요.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(layouts, key = { it.id }) { layout ->
                    LayoutCard(
                        layout = layout,
                        onEdit = { onEditLayout(layout.id) },
                        onRun = { onRunLayout(layout.id) },
                        onDelete = {
                            repository.delete(layout.id)
                            layouts = repository.loadAll()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LayoutCard(
    layout: KeyboardLayout,
    onEdit: () -> Unit,
    onRun: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(layout.name, style = MaterialTheme.typography.titleMedium)
                Text("키 ${layout.keys.size}개", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onRun) { Icon(Icons.Filled.PlayArrow, contentDescription = "실행") }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "편집") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "삭제") }
        }
    }
}
