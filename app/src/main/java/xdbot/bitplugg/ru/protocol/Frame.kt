package xdbot.bitplugg.ru.protocol

/** A decoded wire frame. */
data class DecodedFrame(
    val command: Int,
    val payload: ByteArray,
    val deviceType: Int,
    val random: Int,
    val raw: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DecodedFrame) return false
        return command == other.command &&
            deviceType == other.deviceType &&
            random == other.random &&
            payload.contentEquals(other.payload) &&
            raw.contentEquals(other.raw)
    }

    override fun hashCode(): Int {
        var result = command
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + deviceType
        result = 31 * result + random
        result = 31 * result + raw.contentHashCode()
        return result
    }
}

class FrameException(message: String) : Exception(message)

/** Source of the per-frame obfuscation byte. */
fun interface RandSource {
    fun next(): Int

    companion object {
        /** Incrementing counter, matching the behaviour of the reference client's nonce. */
        fun counter(start: Int = 0): RandSource {
            var value = start
            return RandSource { value++ }
        }

        fun from(javaRandom: java.util.Random): RandSource = RandSource { javaRandom.nextInt(256) }
    }
}

/**
 * Wire framing: `A3 A4 len (rand+50) (devType^rand) (cmd^rand) payload^rand crc8`.
 * Recovered from the reference client (ru.lostbotapp, A2/f.java).
 */
object FrameCodec {

    const val HEADER0 = 0xA3
    const val HEADER1 = 0xA4
    const val MAX_PAYLOAD = 255

    fun encode(command: Int, payload: ByteArray, deviceType: Int, rand: Int): ByteArray {
        require(payload.size <= MAX_PAYLOAD) { "Payload too long: ${payload.size}" }
        val r = rand and 0xFF
        val out = ByteArray(6 + payload.size + 1)
        out[0] = HEADER0.toByte()
        out[1] = HEADER1.toByte()
        out[2] = payload.size.toByte()
        out[3] = ((r + 50) and 0xFF).toByte()
        out[4] = ((deviceType xor r) and 0xFF).toByte()
        out[5] = ((command xor r) and 0xFF).toByte()
        for (i in payload.indices) {
            out[6 + i] = ((payload[i].toInt() xor r) and 0xFF).toByte()
        }
        out[out.size - 1] = Crc8.compute(out.copyOf(out.size - 1))
        return out
    }

    fun decode(frame: ByteArray): DecodedFrame {
        if (frame.size < 7) throw FrameException("Frame too short")
        if ((frame[0].toInt() and 0xFF) != HEADER0 || (frame[1].toInt() and 0xFF) != HEADER1) {
            throw FrameException("Invalid frame header")
        }
        val len = frame[2].toInt() and 0xFF
        val expected = 7 + len
        if (frame.size != expected) {
            throw FrameException("Invalid frame length: expected=$expected got=${frame.size}")
        }
        val crc = Crc8.compute(frame.copyOf(frame.size - 1))
        val got = frame[frame.size - 1]
        if (crc != got) {
            throw FrameException("CRC mismatch: expected=${Hex.encode(byteArrayOf(crc))} got=${Hex.encode(byteArrayOf(got))}")
        }
        val rand = ((frame[3].toInt() and 0xFF) - 50) and 0xFF
        val deviceType = (frame[4].toInt() and 0xFF) xor rand
        val command = (frame[5].toInt() and 0xFF) xor rand
        val payload = ByteArray(len) { i -> ((frame[6 + i].toInt() and 0xFF) xor rand).toByte() }
        return DecodedFrame(command, payload, deviceType, rand, frame.copyOf())
    }
}
