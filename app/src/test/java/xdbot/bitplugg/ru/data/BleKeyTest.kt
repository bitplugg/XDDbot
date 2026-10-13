package xdbot.bitplugg.ru.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BleKeyTest {

    @Test
    fun normalizesSpacesAnd0xPrefix() {
        val result = BleKey.parse("0x1a 2B 3c")
        assertEquals("1a2b3c", result.getOrThrow())
    }

    @Test
    fun parsesBytes() {
        val key = BleKey(id = "1", label = "test", keyHex = "deadbeef")
        assertEquals(4, key.keyBytes.size)
        assertEquals(-34, key.keyBytes[0].toInt())
    }

    @Test
    fun rejectsNonHex() {
        assertTrue(BleKey.parse("zz11").isFailure)
    }

    @Test
    fun rejectsOddLength() {
        assertTrue(BleKey.parse("abc").isFailure)
    }

    @Test
    fun rejectsEmpty() {
        assertTrue(BleKey.parse("   ").isFailure)
    }

    @Test
    fun rejectsOutOfRangeDeviceType() {
        assertTrue(BleKey.parse("aa", deviceType = 300).isFailure)
    }
}
