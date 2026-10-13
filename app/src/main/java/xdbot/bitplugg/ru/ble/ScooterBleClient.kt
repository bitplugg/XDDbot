package xdbot.bitplugg.ru.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xdbot.bitplugg.ru.data.BleKey
import xdbot.bitplugg.ru.protocol.FrameCodec
import xdbot.bitplugg.ru.protocol.Hex
import xdbot.bitplugg.ru.protocol.Opcode
import xdbot.bitplugg.ru.protocol.RandSource
import java.io.ByteArrayOutputStream
import java.util.concurrent.ArrayBlockingQueue

/**
 * Real BLE transport for the owner's scooter, speaking the recovered NUS + frame protocol.
 * All state is exposed as [StateFlow]; callbacks are marshalled through the flows directly
 * (Kotlin flows are thread-safe for emission).
 */
class ScooterBleClient(private val appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val rand = RandSource.counter()

    private val _state = MutableStateFlow(ConnState.IDLE)
    val state: StateFlow<ConnState> = _state.asStateFlow()

    private val _devices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val devices: StateFlow<List<DiscoveredDevice>> = _devices.asStateFlow()

    private val _log = MutableStateFlow<List<LogEntry>>(emptyList())
    val log: StateFlow<List<LogEntry>> = _log.asStateFlow()

    private val _status = MutableStateFlow("Отключено")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _deviceType = MutableStateFlow(BleKey.DEFAULT_DEVICE_TYPE)
    val deviceType: StateFlow<Int> = _deviceType.asStateFlow()

    private val _demo = MutableStateFlow(false)
    val demo: StateFlow<Boolean> = _demo.asStateFlow()

    var demoMode: Boolean
        get() = _demo.value
        set(value) {
            _demo.value = value
            if (value) {
                _status.value = "Демо-режим"
                _state.value = ConnState.IDLE
            }
        }

    private var gatt: BluetoothGatt? = null
    private var rx: BluetoothGattCharacteristic? = null
    private var tx: BluetoothGattCharacteristic? = null
    private val incoming = ByteArrayOutputStream()
    private val writeQueue = ArrayBlockingQueue<ByteArray>(64)
    private var writing = false
    private var mtu = 23
    private var scanCallback: ScanCallback? = null
    private val scanResults = LinkedHashMap<String, DiscoveredDevice>()
    private var scanTimeout: Job? = null

    private val manager: BluetoothManager?
        get() = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    private val adapter: BluetoothAdapter?
        get() = manager?.adapter

    // ----------------------------- permissions -----------------------------

    fun hasScanPermission(): Boolean = if (Build.VERSION.SDK_INT >= 31) {
        granted(Manifest.permission.BLUETOOTH_SCAN)
    } else {
        granted(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun hasConnectPermission(): Boolean = if (Build.VERSION.SDK_INT >= 31) {
        granted(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        true
    }

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED

    // ------------------------------- scanning ------------------------------

    @Suppress("MissingPermission")
    fun startScan() {
        if (demoMode) {
            _state.value = ConnState.SCANNING
            _devices.value = demoDevices()
            appendLog(LogKind.INFO, "Демо: найдено ${_devices.value.size} устройств")
            return
        }
        if (!hasScanPermission()) {
            appendLog(LogKind.ERROR, "Нет разрешения на сканирование Bluetooth")
            return
        }
        val scanner = adapter?.bluetoothLeScanner
        if (scanner == null) {
            appendLog(LogKind.ERROR, "Bluetooth выключен или недоступен")
            return
        }
        scanResults.clear()
        _devices.value = emptyList()
        _state.value = ConnState.SCANNING

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val name = runCatching { device.name }.getOrNull()
                scanResults[device.address] = DiscoveredDevice(name, device.address, result.rssi, device)
                _devices.value = scanResults.values.sortedByDescending { it.rssi }
            }

            override fun onScanFailed(errorCode: Int) {
                appendLog(LogKind.ERROR, "Ошибка сканирования: $errorCode")
                _state.value = ConnState.ERROR
            }
        }
        scanCallback = callback
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner.startScan(emptyList(), settings, callback)
        appendLog(LogKind.INFO, "Сканирование запущено")

        scanTimeout = scope.launch {
            delay(15_000)
            if (_state.value == ConnState.SCANNING) {
                stopScan()
                appendLog(LogKind.INFO, "Сканирование остановлено (таймаут)")
            }
        }
    }

    @Suppress("MissingPermission")
    fun stopScan() {
        scanTimeout?.cancel()
        val callback = scanCallback ?: return
        runCatching { adapter?.bluetoothLeScanner?.stopScan(callback) }
        scanCallback = null
        if (_state.value == ConnState.SCANNING) _state.value = ConnState.IDLE
    }

    // ------------------------------ connection -----------------------------

    @Suppress("MissingPermission")
    fun connect(device: BluetoothDevice, code: String?) {
        if (demoMode) {
            _state.value = ConnState.CONNECTING
            _status.value = "Демо: подключение"
            scope.launch {
                delay(500)
                _deviceType.value = BleKey.DEFAULT_DEVICE_TYPE
                _state.value = ConnState.AUTHENTICATING
                appendLog(LogKind.INFO, "Демо: рукопожатие")
                delay(400)
                _status.value = "Демо: готово"
                _state.value = ConnState.READY
            }
            return
        }
        if (!hasConnectPermission()) {
            appendLog(LogKind.ERROR, "Нет разрешения на подключение Bluetooth")
            return
        }
        closeGatt()
        incoming.reset()
        _state.value = ConnState.CONNECTING
        _status.value = "Подключение…"
        appendLog(LogKind.INFO, "Подключение к ${device.address}")
        gatt = device.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        pendingCode = code
    }

    @Suppress("MissingPermission")
    fun disconnect() {
        if (demoMode) {
            _state.value = ConnState.IDLE
            _status.value = "Демо: отключено"
            return
        }
        closeGatt()
        _state.value = ConnState.IDLE
        _status.value = "Отключено"
        appendLog(LogKind.INFO, "Отключено")
    }

    @Suppress("MissingPermission")
    private fun closeGatt() {
        runCatching { gatt?.disconnect() }
        runCatching { gatt?.close() }
        gatt = null
        rx = null
        tx = null
        writing = false
        writeQueue.clear()
    }

    private var pendingCode: String? = null

    private val gattCallback = object : BluetoothGattCallback() {
        @Suppress("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                appendLog(LogKind.INFO, "Соединение установлено")
                _state.value = ConnState.DISCOVERING
                g.requestMtu(185)
                g.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                appendLog(LogKind.INFO, "Соединение разорвано")
                _state.value = ConnState.IDLE
                _status.value = "Отключено"
                closeGatt()
            }
        }

        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            this@ScooterBleClient.mtu = mtu
            appendLog(LogKind.INFO, "MTU=$mtu")
        }

        @Suppress("MissingPermission")
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val service = g.getService(ScooterUuids.SERVICE)
            if (service == null) {
                appendLog(LogKind.ERROR, "Сервис NUS не найден")
                _state.value = ConnState.ERROR
                return
            }
            rx = service.getCharacteristic(ScooterUuids.RX_WRITE)
            tx = service.getCharacteristic(ScooterUuids.TX_NOTIFY)
            if (rx == null || tx == null) {
                appendLog(LogKind.ERROR, "Характеристики NUS не найдены")
                _state.value = ConnState.ERROR
                return
            }
            enableNotifications(g, tx!!)
        }

        override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (descriptor.uuid == ScooterUuids.CCCD) {
                appendLog(LogKind.INFO, "Уведомления включены")
                val code = pendingCode
                if (code.isNullOrEmpty()) {
                    _status.value = "Подключено"
                    _state.value = ConnState.READY
                } else {
                    _state.value = ConnState.AUTHENTICATING
                    authenticate(code)
                }
            }
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            onCharacteristicChanged(g, c, c.value ?: return)
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            c: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            incoming.write(value)
            drainIncoming()
        }

        override fun onCharacteristicWrite(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
            writing = false
            writeNext()
        }
    }

    // ------------------------------ handshake ------------------------------

    private fun authenticate(code: String) {
        val frame = FrameCodec.encode(
            command = Opcode.COMMUNICATION_KEY.code,
            payload = code.toByteArray(Charsets.UTF_8),
            deviceType = 0,
            rand = rand.next(),
        )
        appendLog(LogKind.TX, "HANDSHAKE (${code.length} симв.)")
        enqueue(frame)
    }

    fun unlock() = sendCommand(Opcode.UNLOCK)
    fun lock() = sendCommand(Opcode.LOCK)
    fun findCar() = sendCommand(Opcode.FIND_CAR)

    fun sendCommand(opcode: Opcode, payload: ByteArray = ByteArray(0)) {
        if (demoMode) {
            appendLog(LogKind.TX, "TX ${opcode.name} (demo)")
            scope.launch {
                delay(150)
                appendLog(LogKind.RX, "RX ${opcode.name} ok (demo)")
            }
            return
        }
        if (_state.value != ConnState.READY) {
            appendLog(LogKind.ERROR, "Команда ${opcode.name}: устройство не готово")
            return
        }
        val frame = FrameCodec.encode(opcode.code, payload, _deviceType.value, rand.next())
        appendLog(LogKind.TX, "TX ${opcode.name} (0x${opcode.code.toString(16)})")
        enqueue(frame)
    }

    // ----------------------------- frame parsing ---------------------------

    private fun drainIncoming() {
        var buffer = incoming.toByteArray()
        var consumed = 0
        while (buffer.size - consumed >= 7) {
            if ((buffer[consumed].toInt() and 0xFF) != FrameCodec.HEADER0 ||
                (buffer[consumed + 1].toInt() and 0xFF) != FrameCodec.HEADER1
            ) {
                consumed++
                continue
            }
            val len = buffer[consumed + 2].toInt() and 0xFF
            val total = 7 + len
            if (buffer.size - consumed < total) break
            val frame = buffer.copyOfRange(consumed, consumed + total)
            consumed += total
            handleFrame(frame)
        }
        if (consumed > 0) {
            incoming.reset()
            if (consumed < buffer.size) incoming.write(buffer.copyOfRange(consumed, buffer.size))
        }
    }

    private fun handleFrame(frame: ByteArray) {
        val decoded = runCatching { FrameCodec.decode(frame) }.getOrElse {
            appendLog(LogKind.ERROR, "Кадр отброшен: ${it.message}")
            return
        }
        val opcode = Opcode.from(decoded.command)
        appendLog(
            LogKind.RX,
            "RX ${opcode?.name ?: "0x${decoded.command.toString(16)}"} " +
                "[${Hex.encode(decoded.payload, spaced = false)}]",
        )
        if (decoded.command == Opcode.COMMUNICATION_KEY.code) {
            val type = if (decoded.payload.isNotEmpty()) decoded.payload[0].toInt() and 0xFF else _deviceType.value
            _deviceType.value = type
            _status.value = "Готово · type $type"
            _state.value = ConnState.READY
            appendLog(LogKind.INFO, "Рукопожатие принято, deviceType=$type")
        } else if (decoded.command == Opcode.ERROR.code) {
            appendLog(LogKind.ERROR, "Устройство вернуло ошибку")
        }
    }

    // ------------------------------- writing -------------------------------

    private fun enqueue(frame: ByteArray) {
        val max = maxOf(20, mtu - 3)
        var i = 0
        while (i < frame.size) {
            val end = minOf(i + max, frame.size)
            writeQueue.offer(frame.copyOfRange(i, end))
            i = end
        }
        writeNext()
    }

    @Suppress("MissingPermission", "DEPRECATION")
    private fun writeNext() {
        if (writing) return
        val g = gatt ?: return
        val characteristic = rx ?: return
        val chunk = writeQueue.poll() ?: return
        writing = true
        val writeType = if ((characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        }
        val ok = if (Build.VERSION.SDK_INT >= 33) {
            g.writeCharacteristic(characteristic, chunk, writeType) == BluetoothStatusCodes.SUCCESS
        } else {
            characteristic.writeType = writeType
            characteristic.value = chunk
            g.writeCharacteristic(characteristic)
        }
        if (!ok) {
            writing = false
            appendLog(LogKind.ERROR, "Не удалось записать кадр")
        }
    }

    @Suppress("MissingPermission", "DEPRECATION")
    private fun enableNotifications(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        g.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(ScooterUuids.CCCD)
        if (descriptor == null) {
            pendingCode?.let { authenticate(it) } ?: run {
                _state.value = ConnState.READY
                _status.value = "Подключено"
            }
            return
        }
        if (Build.VERSION.SDK_INT >= 33) {
            g.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        } else {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            g.writeDescriptor(descriptor)
        }
    }

    // -------------------------------- misc ---------------------------------

    fun clearLog() {
        _log.value = emptyList()
    }

    private fun appendLog(kind: LogKind, text: String) {
        val entry = LogEntry(System.currentTimeMillis(), kind, text)
        _log.value = (_log.value + entry).takeLast(500)
    }

    private fun demoDevices(): List<DiscoveredDevice> {
        return listOf(
            DiscoveredDevice("LostBot demo", "AA:BB:CC:DD:EE:01", -42, demoDevice("AA:BB:CC:DD:EE:01")),
            DiscoveredDevice("Scooter demo", "AA:BB:CC:DD:EE:02", -67, demoDevice("AA:BB:CC:DD:EE:02")),
        )
    }

    @Suppress("MissingPermission")
    private fun demoDevice(address: String): BluetoothDevice {
        val real = adapter?.getRemoteDevice(address)
        return real ?: error("Bluetooth недоступен для демо")
    }
}
