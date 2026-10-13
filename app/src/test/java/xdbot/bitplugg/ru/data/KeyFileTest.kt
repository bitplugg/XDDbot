package xdbot.bitplugg.ru.data

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyFileTest {

    @Test
    fun parsesBareCodeLine() {
        val result = KeyFile.parse("12AB34CD")
        assertEquals(1, result.keys.size)
        assertEquals("12AB34CD", result.keys[0].code)
        assertEquals(BleKey.DEFAULT_DEVICE_TYPE, result.keys[0].deviceType)
        assertEquals(0, result.skipped)
    }

    @Test
    fun parsesLabelAndType() {
        val result = KeyFile.parse("Мой самокат=DEADBEEF, type=42")
        assertEquals(1, result.keys.size)
        assertEquals("Мой самокат", result.keys[0].label)
        assertEquals("DEADBEEF", result.keys[0].code)
        assertEquals(42, result.keys[0].deviceType)
    }

    @Test
    fun keepsCodeVerbatim() {
        val result = KeyFile.parse("scooter-01")
        assertEquals("scooter-01", result.keys[0].code)
    }

    @Test
    fun ignoresBlankAndCommentLines() {
        val result = KeyFile.parse("# comment\n\n  \nAABB\n")
        assertEquals(1, result.keys.size)
    }

    @Test
    fun countsInvalidLinesAsSkipped() {
        val result = KeyFile.parse("=\nlabel=\nAABB")
        assertEquals(1, result.keys.size)
        assertEquals(2, result.skipped)
    }

    @Test
    fun exportsRoundTrip() {
        val keys = listOf(
            BleKey(id = "1", label = "A", code = "AABB", deviceType = 10),
            BleKey(id = "2", label = "B", code = "CCDD", deviceType = 20),
        )
        val reparsed = KeyFile.parse(KeyFile.format(keys))
        assertEquals(2, reparsed.keys.size)
        assertEquals("AABB", reparsed.keys[0].code)
        assertEquals(20, reparsed.keys[1].deviceType)
    }
}
