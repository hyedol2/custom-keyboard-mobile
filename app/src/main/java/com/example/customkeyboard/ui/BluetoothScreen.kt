package com.example.customkeyboard.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.customkeyboard.bluetooth.HidConnectionState
import com.example.customkeyboard.bluetooth.HidKeyboardManager

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScreen(hidManager: HidKeyboardManager, onDone: () -> Unit) {
    val context = LocalContext.current
    val connState by hidManager.state
    val deviceName by hidManager.connectedDeviceName
    var devices by remember { mutableStateOf(hidManager.pairedDevices()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("블루투스 연결") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.Filled.ArrowBack, contentDescription = null) }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text(
                "컴퓨터의 블루투스 설정에서 먼저 이 폰과 페어링을 해주세요.\n" +
                    "페어링된 기기가 아래 목록에 뜨면 눌러서 연결하세요.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            }) {
                Icon(Icons.Filled.Bluetooth, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("시스템 블루투스 설정 열기 (페어링)")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { devices = hidManager.pairedDevices() }) {
                Text("페어링된 기기 새로고침")
            }
            Spacer(Modifier.height(16.dp))

            Text(
                when (connState) {
                    HidConnectionState.CONNECTED -> "✅ 연결됨: ${deviceName ?: ""}"
                    HidConnectionState.CONNECTING -> "연결 시도 중..."
                    HidConnectionState.ERROR -> "연결 오류"
                    else -> "연결된 기기 없음"
                },
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(12.dp))

            if (devices.isEmpty()) {
                Text("페어링된 기기가 없습니다.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(devices) { device ->
                        ElevatedCard {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(device.name ?: "알 수 없는 기기")
                                    Text(device.address, style = MaterialTheme.typography.bodySmall)
                                }
                                Button(onClick = { hidManager.connect(device) }) {
                                    Text("연결")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
