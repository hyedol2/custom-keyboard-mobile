package com.example.customkeyboard.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import java.util.concurrent.Executors

/**
 * 표준 USB HID Boot Keyboard 리포트 디스크립터.
 * (USB HID Usage Tables 공개 표준, 모든 하드웨어 키보드가 사용하는 것과 동일한 형식)
 */
private val KEYBOARD_REPORT_DESC: ByteArray = byteArrayOf(
    0x05, 0x01,       // Usage Page (Generic Desktop)
    0x09, 0x06,       // Usage (Keyboard)
    0xA1.toByte(), 0x01, // Collection (Application)
    0x85.toByte(), 0x01, //   Report ID (1)
    0x05, 0x07,       //   Usage Page (Key Codes)
    0x19, 0xE0.toByte(), //   Usage Minimum (224)
    0x29, 0xE7.toByte(), //   Usage Maximum (231)
    0x15, 0x00,       //   Logical Minimum (0)
    0x25, 0x01,       //   Logical Maximum (1)
    0x75, 0x01,       //   Report Size (1)
    0x95.toByte(), 0x08, //   Report Count (8)
    0x81.toByte(), 0x02, //   Input (modifier byte)
    0x95.toByte(), 0x01,
    0x75, 0x08,
    0x81.toByte(), 0x01, //   Input (reserved byte)
    0x95.toByte(), 0x05,
    0x75, 0x01,
    0x05, 0x08,
    0x19, 0x01,
    0x29, 0x05,
    0x91.toByte(), 0x02, //   Output (LED report)
    0x95.toByte(), 0x01,
    0x75, 0x03,
    0x91.toByte(), 0x01, //   Output (LED padding)
    0x95.toByte(), 0x06,
    0x75, 0x08,
    0x15, 0x00,
    0x25, 0x65,
    0x05, 0x07,
    0x19, 0x00,
    0x29, 0x65,
    0x81.toByte(), 0x00, //   Input (key array, 6 bytes)
    0xC0.toByte()        // End Collection
)

enum class HidConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

@SuppressLint("MissingPermission")
class HidKeyboardManager(private val context: Context) {

    private var hidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null
    private val executor = Executors.newSingleThreadExecutor()

    val state = mutableStateOf(HidConnectionState.DISCONNECTED)
    val connectedDeviceName = mutableStateOf<String?>(null)

    private val sdpSettings = BluetoothHidDeviceAppSdpSettings(
        "CustomKeyboard",
        "Custom Bluetooth Keyboard",
        "CustomKeyboardApp",
        BluetoothHidDevice.SUBCLASS1_KEYBOARD,
        KEYBOARD_REPORT_DESC
    )

    private val qos = BluetoothHidDeviceAppQosSettings(
        BluetoothHidDeviceAppQosSettings.SERVICE_GUARANTEED,
        800, 9, 0, 11250, 11250
    )

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            if (!registered) state.value = HidConnectionState.DISCONNECTED
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, connState: Int) {
            when (connState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevice = device
                    connectedDeviceName.value = device?.name
                    state.value = HidConnectionState.CONNECTED
                }
                BluetoothProfile.STATE_CONNECTING -> state.value = HidConnectionState.CONNECTING
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (connectedDevice?.address == device?.address) {
                        connectedDevice = null
                        connectedDeviceName.value = null
                        state.value = HidConnectionState.DISCONNECTED
                    }
                }
            }
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = proxy as BluetoothHidDevice
                hidDevice?.registerApp(sdpSettings, null, qos, executor, hidCallback)
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
                state.value = HidConnectionState.DISCONNECTED
            }
        }
    }

    /** 서비스 등록 시작. MainActivity onCreate 등에서 한 번 호출. */
    fun init() {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
        adapter.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
    }

    fun pairedDevices(): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return adapter.bondedDevices?.toList() ?: emptyList()
    }

    fun connect(device: BluetoothDevice) {
        state.value = HidConnectionState.CONNECTING
        hidDevice?.connect(device)
    }

    fun disconnect() {
        connectedDevice?.let { hidDevice?.disconnect(it) }
    }

    /**
     * 키 입력을 전송. usageId=0 이면 modifier만 있는 순수 눌림/뗌 없이 그냥 눌렀다 뗌.
     * 딜레이를 최소화하기 위해 press 리포트 전송 직후 곧바로 release 리포트 전송.
     */
    fun sendKey(hidUsageId: Int, modifierMask: Int) {
        val device = connectedDevice ?: return
        val hid = hidDevice ?: return

        val pressReport = ByteArray(8)
        pressReport[0] = modifierMask.toByte()
        pressReport[1] = 0
        pressReport[2] = hidUsageId.toByte()
        hid.sendReport(device, 1, pressReport)

        val releaseReport = ByteArray(8)
        hid.sendReport(device, 1, releaseReport)
    }

    fun teardown() {
        hidDevice?.let {
            connectedDevice?.let { d -> it.disconnect(d) }
            it.unregisterApp()
        }
        val adapter = BluetoothAdapter.getDefaultAdapter()
        adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
    }
}
