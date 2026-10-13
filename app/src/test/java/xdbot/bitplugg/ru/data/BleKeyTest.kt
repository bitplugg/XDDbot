package xdbot.bitplugg.ru.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BleKeyTest {

    @Test
    fun trimsAndAcceptsCode() {
        assertEquals("12AB34CD", BleKey.parse("  12AB34CD  ").getOrThrow())
    }

    @Test
    fun bytesAreUtf8OfCode() {
        val key = BleKey(id = "1", label = "test", code = "A1")
        assertArrayEquals(byteArrayOf(0x41, 0x31), key.keyBytes)
    }

    @Test
    fun acceptsNonHexText() {
        assertEquals("scooter-code-1", BleKey.parse("scooter-code-1").getOrThrow())
    }

    @Test
    fun rejectsEmpty() {
        assertTrue(BleKey.parse("   ").isFailure)
    }

    @Test
    fun rejectsTooLong() {
        assertTrue(BleKey.parse("a".repeat(256)).isFailure)
    }

    @Test
    fun rejectsOutOfRangeDeviceType() {
        assertTrue(BleKey.parse("12AB", deviceType = 300).isFailure)
    }
}
