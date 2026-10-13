package xdbot.bitplugg.ru.protocol

import java.util.Locale

/**
 * Decodes scooter response payloads into [ScooterStatus] and human-readable log lines.
 * Field layouts mirror the reference client (A2/c.java) — no guessing.
 */
object PayloadDecoder {

    /** Applies a decoded response to [status] and returns the updated copy. */
    fun apply(status: ScooterStatus, command: Int, payload: ByteArray): ScooterStatus {
        val now = System.currentTimeMillis()
        val base = status.copy(lastCommand = command and 0xFF, lastUpdated = now)
        return when (command and 0xFF) {
            96 -> if (payload.size >= 8) {
                base.copy(
                    batteryPercent = u(payload[0]),
                    mode = u(payload[1]),
                    speedKmh = ((u(payload[2]) shl 8) or u(payload[3])) / 10.0,
                    tripKm = (((u(payload[4]) shl 8) or u(payload[5])) * 10) / 1000.0,
                    rangeKm = (((u(payload[7])) or (u(payload[6]) shl 8)) * 10) / 1000.0,
                )
            } else base

            99 -> if (payload.size >= 6) {
                base.copy(
                    batteryPercent = u(payload[0]),
                    speedKmh = ((u(payload[1]) shl 8) or u(payload[2])) / 10.0,
                    current = ((u(payload[3]) shl 8) or u(payload[4])) / 100.0,
                )
            } else base

            49 -> if (payload.size >= 7) {
                base.copy(
                    voltage = ((u(payload[0]) shl 8) or u(payload[1])) / 100.0,
                    locked = (u(payload[2]) and 1) != 1,
                    firmware = "${u(payload[4])}.${u(payload[5])}.${u(payload[6])}",
                )
            } else base

            133 -> if (payload.size >= 4) {
                base.copy(
                    rfidCardId = (u(payload[0]).toLong() shl 24) or
                        (u(payload[1]).toLong() shl 16) or
                        (u(payload[2]).toLong() shl 8) or u(payload[3]).toLong(),
                    rfidCardHex = Hex.encode(payload.copyOf(4), spaced = false),
                )
            } else base

            250 -> base.copy(firmwareInfo = ascii(payload))

            else -> base
        }
    }

    /** One-line summary for the transfer log. */
    fun describe(command: Int, payload: ByteArray): String = when (command and 0xFF) {
        1 -> if (payload.size >= 2) "handshake ACK key=0x${hex(payload[1])}" else "handshake response"
        5 -> ack("unlock", payload)
        21 -> ack("lock", payload)
        16 -> if (payload.isEmpty()) {
            "ERROR (no detail)"
        } else {
            "ERROR rejected=0x${hex(payload[0])}" +
                if (payload.size > 1) " reason=0x${hex(payload[1])}" else ""
        }

        49 -> if (payload.size >= 7) {
            val locked = (u(payload[2]) and 1) != 1
            val v = ((u(payload[0]) shl 8) or u(payload[1])) / 100.0
            "device info: V=${fmt(v, 2)} flags=0x${hex(payload[2])} " +
                (if (locked) "LOCKED" else "UNLOCKED") +
                " fw=${u(payload[4])}.${u(payload[5])}.${u(payload[6])}"
        } else "device info (short)"

        96 -> if (payload.size >= 8) {
            "info: battery=${u(payload[0])}% mode=${u(payload[1])} " +
                "speed=${fmt(((u(payload[2]) shl 8) or u(payload[3])) / 10.0, 1)}km/h " +
                "trip=${fmt((((u(payload[4]) shl 8) or u(payload[5])) * 10) / 1000.0, 2)}km " +
                "range=${fmt((((u(payload[7])) or (u(payload[6]) shl 8)) * 10) / 1000.0, 2)}km"
        } else "info (short)"

        99 -> if (payload.size >= 6) {
            "realtime: battery=${u(payload[0])}% " +
                "speed=${fmt(((u(payload[1]) shl 8) or u(payload[2])) / 10.0, 1)}km/h " +
                "current=${fmt(((u(payload[3]) shl 8) or u(payload[4])) / 100.0, 2)}A " +
                "flags=0x${hex(payload[5])}"
        } else "realtime (short)"

        129 -> if (payload.size >= 2) {
            "battery/out_lock tag=0x${hex(payload[0])} status=0x${hex(payload[1])}"
        } else "battery/out_lock (short)"

        133 -> if (payload.size >= 4) {
            val id = (u(payload[0]).toLong() shl 24) or (u(payload[1]).toLong() shl 16) or
                (u(payload[2]).toLong() shl 8) or u(payload[3]).toLong()
            "RFID card id=$id (0x${Hex.encode(payload.copyOf(4), spaced = false)})"
        } else ack("read RFID", payload)

        250 -> "firmware info ascii=\"${ascii(payload)}\""
        251 -> when (if (payload.isEmpty()) -1 else u(payload[0])) {
            0 -> "transmission: password error"
            1 -> "transmission: READY"
            2 -> "transmission: COMPLETE"
            3 -> "transmission: timeout"
            4 -> "transmission: total CRC error"
            else -> "transmission status=0x${hex(payload.getOrElse(0) { 0 })}"
        }

        252 -> if (payload.size >= 2) {
            "pack request idx=${(u(payload[0]) shl 8) or u(payload[1])}"
        } else "firmware data chunk len=${payload.size}"

        else -> ""
    }

    private fun ack(label: String, payload: ByteArray): String = when {
        payload.isEmpty() -> "$label ACK (no status)"
        u(payload[0]) == 0 -> "$label OK"
        else -> "$label FAIL status=0x${hex(payload[0])}"
    }

    /** Android-free ASCII preview; non-printables become '.'. */
    fun ascii(bytes: ByteArray): String = bytes.map { b ->
        val v = b.toInt() and 0xFF
        if (v in 32..126) v.toChar() else '.'
    }.joinToString("")

    private fun u(b: Byte): Int = b.toInt() and 0xFF
    private fun hex(b: Byte): String = String.format(Locale.US, "%02X", b.toInt() and 0xFF)
    private fun fmt(value: Double, decimals: Int): String =
        String.format(Locale.US, "%.${decimals}f", value)
}
