package xdbot.bitplugg.ru.protocol

/**
 * CRC-16/MODBUS checksum (reflected poly 0xA001, init 0xFFFF) used for payload transfers.
 * Recovered from the reference client (ru.lostbotapp, C2/F.java).
 */
object Crc16 {

    fun compute(data: ByteArray, offset: Int = 0, length: Int = data.size - offset): Int {
        var crc = 0xFFFF
        for (i in offset until offset + length) {
            crc = crc xor (data[i].toInt() and 0xFF)
            repeat(8) {
                val lsb = crc and 1
                crc = crc ushr 1
                if (lsb != 0) crc = crc xor 0xA001
            }
        }
        return crc and 0xFFFF
    }
}
