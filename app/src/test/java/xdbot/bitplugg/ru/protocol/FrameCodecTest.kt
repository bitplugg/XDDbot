package xdbot.bitplugg.ru.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FrameCodecTest {

    @Test
    fun encodeDecode_roundTrips() {
        val payload = byteArrayOf(0x01, 0x02, 0x7F, 0x80.toByte(), 0xFF.toByte())
        val frame = FrameCodec.encode(Opcode.UNLOCK.code, payload, 66, 0x2A)
        val decoded = FrameCodec.decode(frame)
        assertEquals(Opcode.UNLOCK.code, decoded.command)
        assertEquals(66, decoded.deviceType)
        assertEquals(0x2A, decoded.random)
        assertArrayEquals(payload, decoded.payload)
    }

    @Test
    fun encode_producesExpectedHeaderAndLength() {
        val frame = FrameCodec.encode(0x01, byteArrayOf(0x0A, 0x0B), 0, 0)
        assertEquals(0xA3, frame[0].toInt() and 0xFF)
        assertEquals(0xA4, frame[1].toInt() and 0xFF)
        assertEquals(2, frame[2].toInt() and 0xFF)
        assertEquals(9, frame.size) // 6 header + 2 payload + 1 crc
    }

    @Test
    fun decode_rejectsBadCrc() {
        val frame = FrameCodec.encode(0x05, byteArrayOf(), 66, 1)
        frame[frame.size - 1] = (frame[frame.size - 1] + 1).toByte()
        assertThrows(FrameException::class.java) { FrameCodec.decode(frame) }
    }

    @Test
    fun hex_roundTrips() {
        val bytes = byteArrayOf(0x00, 0x7F, 0x80.toByte(), 0xFF.toByte())
        assertArrayEquals(bytes, Hex.decode(Hex.encode(bytes, spaced = false)))
    }
}
