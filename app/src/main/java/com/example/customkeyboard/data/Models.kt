package com.example.customkeyboard.data

import java.util.UUID

/** 키 모양 */
enum class KeyShape { RECT, ROUNDED, CIRCLE }

/**
 * 하나의 키 정의.
 * x, y, width, height 는 화면 비율(0f~1f) 기준 상대 좌표/크기.
 * hidUsageId 는 USB HID Keyboard/Keypad Usage Table 상의 표준 코드값.
 * modifierMask 는 이 키를 누를 때 함께 보낼 modifier 비트 (Ctrl/Shift/Alt/Win 조합 가능).
 */
data class KeyItem(
    val id: String = UUID.randomUUID().toString(),
    var label: String = "A",
    var hidUsageId: Int = 0x04,
    var modifierMask: Int = 0,
    var x: Float = 0.1f,
    var y: Float = 0.1f,
    var width: Float = 0.12f,
    var height: Float = 0.08f,
    var colorArgb: Int = 0xFF7C4DFF.toInt(),
    var shape: KeyShape = KeyShape.ROUNDED,
    var pressCount: Long = 0L
)

data class KeyboardLayout(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "새 키보드",
    var keys: MutableList<KeyItem> = mutableListOf()
)

/** modifier 비트 (USB HID Boot Keyboard 표준) */
object Modifiers {
    const val LEFT_CTRL = 0x01
    const val LEFT_SHIFT = 0x02
    const val LEFT_ALT = 0x04
    const val LEFT_GUI = 0x08 // Windows/Command 키
    const val RIGHT_CTRL = 0x10
    const val RIGHT_SHIFT = 0x20
    const val RIGHT_ALT = 0x40
}

/** 자주 쓰는 키 목록: 표시 이름 -> HID Usage ID */
object KeyCatalog {
    val letters: List<Pair<String, Int>> = ('A'..'Z').mapIndexed { i, c -> c.toString() to (0x04 + i) }
    val digits: List<Pair<String, Int>> = listOf(
        "1" to 0x1E, "2" to 0x1F, "3" to 0x20, "4" to 0x21, "5" to 0x22,
        "6" to 0x23, "7" to 0x24, "8" to 0x25, "9" to 0x26, "0" to 0x27
    )
    val functionKeys: List<Pair<String, Int>> = (1..12).map { "F$it" to (0x3A + (it - 1)) }
    val special: List<Pair<String, Int>> = listOf(
        "Enter" to 0x28, "Esc" to 0x29, "Backspace" to 0x2A, "Tab" to 0x2B,
        "Space" to 0x2C, "-" to 0x2D, "=" to 0x2E, "[" to 0x2F, "]" to 0x30,
        "\\" to 0x31, ";" to 0x33, "'" to 0x34, "," to 0x36, "." to 0x37, "/" to 0x38,
        "CapsLock" to 0x39, "PrintScreen" to 0x46, "Delete" to 0x4C,
        "Home" to 0x4A, "End" to 0x4D, "PageUp" to 0x4B, "PageDown" to 0x4E,
        "→" to 0x4F, "←" to 0x50, "↓" to 0x51, "↑" to 0x52
    )

    val all: List<Pair<String, Int>> = letters + digits + functionKeys + special

    fun findLabel(hidUsageId: Int): String =
        all.firstOrNull { it.second == hidUsageId }?.first ?: "?"
}
