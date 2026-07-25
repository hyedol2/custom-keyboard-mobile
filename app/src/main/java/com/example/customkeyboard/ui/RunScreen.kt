package com.example.customkeyboard.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.customkeyboard.bluetooth.HidConnectionState
import com.example.customkeyboard.bluetooth.HidKeyboardManager
import com.example.customkeyboard.data.KeyItem
import com.example.customkeyboard.data.KeyShape
import com.example.customkeyboard.data.KeyboardLayout
import com.example.customkeyboard.data.LayoutRepository
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunScreen(
    layoutId: String,
    repository: LayoutRepository,
    hidManager: HidKeyboardManager,
    onExit: () -> Unit
) {
    var layout by remember {
        mutableStateOf(repository.loadAll().firstOrNull { it.id == layoutId } ?: KeyboardLayout())
    }
    var canvasSize by remember { mutableStateOf(Offset(1f, 1f)) }
    val connState by hidManager.state

    // 화면 나갈 때 누른 횟수 저장
    DisposableEffect(Unit) {
        onDispose { repository.upsert(layout) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(layout.name) },
                navigationIcon = {
                    IconButton(onClick = { repository.upsert(layout); onExit() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    Text(
                        if (connState == HidConnectionState.CONNECTED) "🔵 연결됨" else "⚪ 미연결",
                        modifier = Modifier.padding(end = 12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF11111B))
                .onGloballyPositioned { canvasSize = Offset(it.size.width.toFloat(), it.size.height.toFloat()) }
        ) {
            layout.keys.forEach { key ->
                RunnableKey(
                    key = key,
                    canvasSize = canvasSize,
                    onPress = {
                        hidManager.sendKey(key.hidUsageId, key.modifierMask)
                        layout = layout.copy(
                            keys = layout.keys.map {
                                if (it.id == key.id) it.copy(pressCount = it.pressCount + 1) else it
                            }.toMutableList()
                        )
                    }
                )
            }
            if (connState != HidConnectionState.CONNECTED) {
                Text(
                    "⚠️ 블루투스 미연결 상태입니다. 홈에서 기기를 먼저 연결하세요.",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp)
                        .background(Color(0xAAFF5555), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RunnableKey(key: KeyItem, canvasSize: Offset, onPress: () -> Unit) {
    val w = canvasSize.x
    val h = canvasSize.y
    if (w <= 0f || h <= 0f) return

    var pressed by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(
        if (pressed) Color(key.colorArgb).copy(alpha = 0.6f) else Color(key.colorArgb),
        label = "keyColor"
    )

    val shapeModifier = when (key.shape) {
        KeyShape.CIRCLE -> RoundedCornerShape(50)
        KeyShape.ROUNDED -> RoundedCornerShape(10.dp)
        KeyShape.RECT -> RoundedCornerShape(0.dp)
    }

    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .offset { IntOffset((key.x * w).roundToInt(), (key.y * h).roundToInt()) }
            .size(
                with(density) { (key.width * w).toDp() },
                with(density) { (key.height * h).toDp() }
            )
            .background(bgColor, shapeModifier)
            .border(1.dp, Color.White.copy(alpha = 0.25f), shapeModifier)
            .pointerInput(key.id) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        // 눌리는 즉시 입력 전송 -> 딜레이 최소화
                        onPress()
                        tryAwaitRelease()
                        pressed = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(key.label, color = Color.Black, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            Text("${key.pressCount}", color = Color.Black.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
        }
    }
}
