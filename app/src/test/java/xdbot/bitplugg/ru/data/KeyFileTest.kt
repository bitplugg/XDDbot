package xdbot.bitplugg.ru.data

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyFileTest {

    @Test
    fun parsesBareHexLine() {
        val result = KeyFile.parse("a1b2c3")
        assertEquals(1, result.keys.size)
        assertEquals("a1b2c3", result.keys[0].keyHex)
        assertEquals(BleKey.DEFAULT_DEVICE_TYPE, result.keys[0].deviceType)
        assertEquals(0, result.skipped)
    }

    @Test
    fun parsesLabelAndType() {
        val result = KeyFile.parse("Мой самокат=DEADBEEF, type=42")
        assertEquals(1, result.keys.size)
        assertEquals("Мой самокат", result.keys[0].label)
        assertEquals("deadbeef", result.keys[0].keyHex)
        assertEquals(42, result.keys[0].deviceType)
    }

    @Test
    fun ignoresBlankAndCommentLines() {
        val result = KeyFile.parse("# comment\n\n  \naabb\n")
        assertEquals(1, result.keys.size)
    }

    @Test
    fun countsInvalidLinesAsSkipped() {
        val result = KeyFile.parse("zz11\nabc\naabb")
        assertEquals(1, result.keys.size)
        assertEquals(2, result.skipped)
    }

    @Test
    fun toleratesHexSeparators() {
        val result = KeyFile.parse("aa:bb:cc")
        assertEquals("aabbcc", result.keys[0].keyHex)
    }

    @Test
    fun exportsRoundTrip() {
        val keys = listOf(
            BleKey(id = "1", label = "A", keyHex = "aabb", deviceType = 10),
            BleKey(id = "2", label = "B", keyHex = "ccdd", deviceType = 20),
        )
        val reparsed = KeyFile.parse(KeyFile.format(keys))
        assertEquals(2, reparsed.keys.size)
        assertEquals("aabb", reparsed.keys[0].keyHex)
        assertEquals(20, reparsed.keys[1].deviceType)
    }
}
