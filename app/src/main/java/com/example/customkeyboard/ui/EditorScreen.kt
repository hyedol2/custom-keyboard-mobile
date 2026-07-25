package com.example.customkeyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.customkeyboard.data.KeyCatalog
import com.example.customkeyboard.data.KeyItem
import com.example.customkeyboard.data.KeyShape
import com.example.customkeyboard.data.KeyboardLayout
import com.example.customkeyboard.data.LayoutRepository
import com.example.customkeyboard.data.Modifiers
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(layoutId: String, repository: LayoutRepository, onDone: () -> Unit) {
    val initial = remember {
        if (layoutId == "new") KeyboardLayout()
        else repository.loadAll().firstOrNull { it.id == layoutId } ?: KeyboardLayout()
    }
    var layoutName by remember { mutableStateOf(initial.name) }
    var keys by remember { mutableStateOf(initial.keys.toMutableList()) }
    var editingKey by remember { mutableStateOf<KeyItem?>(null) }
    var canvasSize by remember { mutableStateOf(Offset(1f, 1f)) }

    fun save() {
        val layout = KeyboardLayout(id = initial.id, name = layoutName, keys = keys)
        repository.upsert(layout)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = layoutName,
                        onValueChange = { layoutName = it },
                        singleLine = true,
                        label = { Text("키보드 이름") }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { save(); onDone() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { save(); onDone() }) {
                        Icon(Icons.Filled.Check, contentDescription = "저장")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val newKey = KeyItem()
                keys = (keys + newKey).toMutableList()
                editingKey = newKey
            }) { Icon(Icons.Filled.Add, contentDescription = "키 추가") }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .padding(12.dp)
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .background(Color(0xFF11111B), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF44445A), RoundedCornerShape(16.dp))
                .onGloballyPositioned { canvasSize = Offset(it.size.width.toFloat(), it.size.height.toFloat()) }
        ) {
            keys.forEach { key ->
                EditableKey(
                    key = key,
                    canvasSize = canvasSize,
                    onMoved = { nx, ny ->
                        keys = keys.map { if (it.id == key.id) it.copy(x = nx, y = ny) else it }.toMutableList()
                    },
                    onResized = { nw, nh ->
                        keys = keys.map { if (it.id == key.id) it.copy(width = nw, height = nh) else it }.toMutableList()
                    },
                    onTap = { editingKey = key }
                )
            }
            if (keys.isEmpty()) {
                Text(
                    "오른쪽 아래 + 버튼으로 키를 추가하세요",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray
                )
            }
        }
        Text(
            "💡 키를 드래그해서 위치를 옮기고, 오른쪽 아래 모서리를 드래그하면 크기가 바뀌어요. 탭하면 상세 설정이 열려요.",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }

    editingKey?.let { key ->
        KeyEditDialog(
            key = key,
            onDismiss = { editingKey = null },
            onSave = { updated ->
                keys = keys.map { if (it.id == updated.id) updated else it }.toMutableList()
                editingKey = null
            },
            onDelete = {
                keys = keys.filterNot { it.id == key.id }.toMutableList()
                editingKey = null
            }
        )
    }
}

@Composable
private fun EditableKey(
    key: KeyItem,
    canvasSize: Offset,
    onMoved: (Float, Float) -> Unit,
    onResized: (Float, Float) -> Unit,
    onTap: () -> Unit
) {
    val w = canvasSize.x
    val h = canvasSize.y
    if (w <= 0f || h <= 0f) return

    val pxX = key.x * w
    val pxY = key.y * h
    val pxW = key.width * w
    val pxH = key.height * h

    val shapeModifier = when (key.shape) {
        KeyShape.CIRCLE -> RoundedCornerShape(50)
        KeyShape.ROUNDED -> RoundedCornerShape(10.dp)
        KeyShape.RECT -> RoundedCornerShape(0.dp)
    }

    Box(
        modifier = Modifier
            .offset { androidx.compose.ui.unit.IntOffset(pxX.roundToInt(), pxY.roundToInt()) }
            .size(
                with(androidx.compose.ui.platform.LocalDensity.current) { pxW.toDp() },
                with(androidx.compose.ui.platform.LocalDensity.current) { pxH.toDp() }
            )
            .background(Color(key.colorArgb), shapeModifier)
            .border(1.dp, Color.White.copy(alpha = 0.3f), shapeModifier)
            .pointerInput(key.id, canvasSize) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val nx = ((key.x * w + dragAmount.x) / w).coerceIn(0f, 1f - key.width)
                    val ny = ((key.y * h + dragAmount.y) / h).coerceIn(0f, 1f - key.height)
                    onMoved(nx, ny)
                }
            }
            .pointerInput(key.id) {
                androidx.compose.foundation.gestures.detectTapGestures(onTap = { onTap() })
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            key.label,
            color = Color.Black,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium
        )
        // 리사이즈 핸들
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(16.dp)
                .background(Color.White.copy(alpha = 0.6f), CircleShape)
                .pointerInput(key.id, canvasSize) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val nw = ((key.width * w + dragAmount.x) / w).coerceIn(0.04f, 1f - key.x)
                        val nh = ((key.height * h + dragAmount.y) / h).coerceIn(0.03f, 1f - key.y)
                        onResized(nw, nh)
                    }
                }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeyEditDialog(
    key: KeyItem,
    onDismiss: () -> Unit,
    onSave: (KeyItem) -> Unit,
    onDelete: () -> Unit
) {
    var label by remember { mutableStateOf(key.label) }
    var hidUsageId by remember { mutableStateOf(key.hidUsageId) }
    var modMask by remember { mutableStateOf(key.modifierMask) }
    var color by remember { mutableStateOf(Color(key.colorArgb)) }
    var shape by remember { mutableStateOf(key.shape) }
    var width by remember { mutableStateOf(key.width) }
    var height by remember { mutableStateOf(key.height) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("키 설정") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("표시 이름") },
                    singleLine = true
                )

                Text("키 종류", style = MaterialTheme.typography.labelLarge)
                Text("문자", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                KeyPickerRow(KeyCatalog.letters, hidUsageId) { name, id -> hidUsageId = id; label = name }
                Text("숫자", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                KeyPickerRow(KeyCatalog.digits, hidUsageId) { name, id -> hidUsageId = id; label = name }
                Text("기능키(F1~F12)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                KeyPickerRow(KeyCatalog.functionKeys, hidUsageId) { name, id -> hidUsageId = id; label = name }
                Text("특수키", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                KeyPickerRow(KeyCatalog.special, hidUsageId) { name, id -> hidUsageId = id; label = name }

                Text("조합키(모디파이어)", style = MaterialTheme.typography.labelLarge)
                Row {
                    ModifierChip("Ctrl", modMask, Modifiers.LEFT_CTRL) { modMask = modMask xor Modifiers.LEFT_CTRL }
                    ModifierChip("Shift", modMask, Modifiers.LEFT_SHIFT) { modMask = modMask xor Modifiers.LEFT_SHIFT }
                    ModifierChip("Alt", modMask, Modifiers.LEFT_ALT) { modMask = modMask xor Modifiers.LEFT_ALT }
                    ModifierChip("Win", modMask, Modifiers.LEFT_GUI) { modMask = modMask xor Modifiers.LEFT_GUI }
                }

                Text("색깔 (RGB)", style = MaterialTheme.typography.labelLarge)
                RgbSliders(color) { color = it }

                Text("모양", style = MaterialTheme.typography.labelLarge)
                Row {
                    FilterChip(selected = shape == KeyShape.RECT, onClick = { shape = KeyShape.RECT }, label = { Text("사각") })
                    Spacer(Modifier.width(6.dp))
                    FilterChip(selected = shape == KeyShape.ROUNDED, onClick = { shape = KeyShape.ROUNDED }, label = { Text("둥근사각") })
                    Spacer(Modifier.width(6.dp))
                    FilterChip(selected = shape == KeyShape.CIRCLE, onClick = { shape = KeyShape.CIRCLE }, label = { Text("원") })
                }

                Text("가로 크기: ${(width * 100).roundToInt()}%")
                Slider(value = width, onValueChange = { width = it }, valueRange = 0.04f..0.5f)
                Text("세로 크기: ${(height * 100).roundToInt()}%")
                Slider(value = height, onValueChange = { height = it }, valueRange = 0.03f..0.4f)

                Text("누른 횟수: ${key.pressCount}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    key.copy(
                        label = label,
                        hidUsageId = hidUsageId,
                        modifierMask = modMask,
                        colorArgb = color.toArgb(),
                        shape = shape,
                        width = width,
                        height = height
                    )
                )
            }) { Text("저장") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = null); Text("삭제") }
                TextButton(onClick = onDismiss) { Text("취소") }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeyPickerRow(items: List<Pair<String, Int>>, selectedId: Int, onPick: (String, Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(items) { (name, id) ->
            FilterChip(
                selected = id == selectedId,
                onClick = { onPick(name, id) },
                label = { Text(name) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModifierChip(label: String, mask: Int, bit: Int, onToggle: () -> Unit) {
    FilterChip(
        selected = (mask and bit) != 0,
        onClick = onToggle,
        label = { Text(label) },
        modifier = Modifier.padding(end = 6.dp)
    )
}

@Composable
private fun RgbSliders(color: Color, onChange: (Color) -> Unit) {
    var r by remember(color) { mutableStateOf(color.red) }
    var g by remember(color) { mutableStateOf(color.green) }
    var b by remember(color) { mutableStateOf(color.blue) }

    fun update(nr: Float, ng: Float, nb: Float) {
        r = nr; g = ng; b = nb
        onChange(Color(nr, ng, nb))
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color(r, g, b), RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    )
    Text("R: ${(r * 255).roundToInt()}")
    Slider(value = r, onValueChange = { update(it, g, b) })
    Text("G: ${(g * 255).roundToInt()}")
    Slider(value = g, onValueChange = { update(r, it, b) })
    Text("B: ${(b * 255).roundToInt()}")
    Slider(value = b, onValueChange = { update(r, g, it) })
}
