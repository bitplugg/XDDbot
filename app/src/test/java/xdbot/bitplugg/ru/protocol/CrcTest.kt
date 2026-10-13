package xdbot.bitplugg.ru.protocol

import org.junit.Assert.assertEquals
import org.junit.Test

class CrcTest {

    private val check = "123456789".toByteArray(Charsets.US_ASCII)

    @Test
    fun crc8_matchesMaximCheckValue() {
        // CRC-8/MAXIM/DALLAS check value for "123456789" is 0xA1.
        assertEquals(0xA1, Crc8.compute(check).toInt() and 0xFF)
    }

    @Test
    fun crc16_matchesModbusCheckValue() {
        // Reflected poly 0xA001 with init 0xFFFF (CRC-16/MODBUS) check value is 0x4B37.
        assertEquals(0x4B37, Crc16.compute(check))
    }
}
