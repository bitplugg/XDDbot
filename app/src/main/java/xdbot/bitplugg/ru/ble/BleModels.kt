package xdbot.bitplugg.ru.ble

import android.bluetooth.BluetoothDevice

data class DiscoveredDevice(
    val name: String?,
    val address: String,
    val rssi: Int,
    val device: BluetoothDevice,
)

enum class ConnState { IDLE, SCANNING, CONNECTING, DISCOVERING, AUTHENTICATING, READY, ERROR }

enum class LogKind { TX, RX, INFO, ERROR }

data class LogEntry(val time: Long, val kind: LogKind, val text: String)
