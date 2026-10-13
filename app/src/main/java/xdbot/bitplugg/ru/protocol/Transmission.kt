package xdbot.bitplugg.ru.protocol

/** Transfer type used by the START (0xFB) packet. Recovered from A2/j.java. */
enum class TransferType(val code: Int) {
    OTA_UPDATE(0),
    SYSTEM_CONFIG(1),
}

/**
 * Builds the payload transfer frames used for firmware/config uploads.
 * START (0xFB) announces total packet count + CRC16; DATA (0xFC) carries 16-byte chunks.
 * Recovered from the reference client (C2/F.java).
 */
object Transmission {

    const val CHUNK_SIZE = 16
    const val START_OPCODE = 251
    const val DATA_OPCODE = 252

    data class Start(
        val payload: ByteArray,
        val totalPackets: Int,
        val crc16: Int,
    )

    /** Zero-pad to a multiple of [CHUNK_SIZE] and compute the total packet count + CRC16. */
    fun prepare(data: ByteArray): Start {
        val rem = data.size % CHUNK_SIZE
        val padded = if (rem == 0) data.copyOf() else data + ByteArray(CHUNK_SIZE - rem)
        val totalPackets = padded.size / CHUNK_SIZE
        return Start(padded, totalPackets, Crc16.compute(padded))
    }

    /** START payload: `type, totalPackets>>8, totalPackets, crc16>>8, crc16, deviceType` + key. */
    fun startPayload(
        type: TransferType,
        totalPackets: Int,
        crc16: Int,
        deviceType: Int,
        key: ByteArray,
    ): ByteArray {
        val head = byteArrayOf(
            type.code.toByte(),
            ((totalPackets ushr 8) and 0xFF).toByte(),
            (totalPackets and 0xFF).toByte(),
            ((crc16 ushr 8) and 0xFF).toByte(),
            (crc16 and 0xFF).toByte(),
            (deviceType and 0xFF).toByte(),
        )
        return head + key
    }

    /** DATA payload: `index>>8, index, deviceType` + 16-byte chunk + CRC16 (big-endian). */
    fun dataPayload(index: Int, deviceType: Int, chunk: ByteArray): ByteArray {
        val head = byteArrayOf(
            ((index ushr 8) and 0xFF).toByte(),
            (index and 0xFF).toByte(),
            (deviceType and 0xFF).toByte(),
        )
        val body = head + chunk
        val crc = Crc16.compute(body)
        return body + byteArrayOf(((crc ushr 8) and 0xFF).toByte(), (crc and 0xFF).toByte())
    }

    fun chunk(padded: ByteArray, index: Int): ByteArray =
        padded.copyOfRange(index * CHUNK_SIZE, index * CHUNK_SIZE + CHUNK_SIZE)
}
