package xdbot.bitplugg.ru.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PayloadDecoderTest {

    @Test
    fun scooterInfo_decodesAllFields() {
        val payload = byteArrayOf(80, 2, 0x01, 0x2C, 0x00, 0x64, 0x00, 0xBB.toByte())
        val status = PayloadDecoder.apply(ScooterStatus(), 96, payload)
        assertEquals(80, status.batteryPercent)
        assertEquals(2, status.mode)
        assertEquals(30.0, status.speedKmh!!, 1e-9)
        assertEquals(1.0, status.tripKm!!, 1e-9)
        assertEquals(1.87, status.rangeKm!!, 1e-9)
        assertEquals(96, status.lastCommand)
    }

    @Test
    fun scooterInfo_shortPayloadLeavesFieldsUntouched() {
        val status = PayloadDecoder.apply(ScooterStatus(), 96, byteArrayOf(1, 2, 3))
        assertNull(status.batteryPercent)
        assertEquals(96, status.lastCommand)
    }

    @Test
    fun realtime_decodesSpeedAndCurrent() {
        val payload = byteArrayOf(55, 0x00, 0xFA.toByte(), 0x03, 0xE8.toByte(), 0x05)
        val status = PayloadDecoder.apply(ScooterStatus(), 99, payload)
        assertEquals(55, status.batteryPercent)
        assertEquals(25.0, status.speedKmh!!, 1e-9)
        assertEquals(10.0, status.current!!, 1e-9)
    }

    @Test
    fun deviceInfo_decodesVoltageLockAndFirmware() {
        val payload = byteArrayOf(0x18, 0x38, 0x00, 0x00, 0x01, 0x02, 0x03)
        val status = PayloadDecoder.apply(ScooterStatus(), 49, payload)
        assertEquals(62.0, status.voltage!!, 1e-9)
        assertTrue(status.locked!!)
        assertEquals("1.2.3", status.firmware)
    }

    @Test
    fun deviceInfo_lockBitClearMeansUnlocked() {
        val payload = byteArrayOf(0x18, 0x38, 0x01, 0x00, 0x01, 0x02, 0x03)
        val status = PayloadDecoder.apply(ScooterStatus(), 49, payload)
        assertEquals(false, status.locked)
    }

    @Test
    fun rfid_decodesCardIdAndHex() {
        val payload = byteArrayOf(0x12, 0x34, 0x56, 0x78)
        val status = PayloadDecoder.apply(ScooterStatus(), 133, payload)
        assertEquals(0x12345678L, status.rfidCardId)
        assertEquals("12345678", status.rfidCardHex)
    }

    @Test
    fun firmwareInfo_decodesAscii() {
        val payload = "v1.2".toByteArray(Charsets.US_ASCII)
        val status = PayloadDecoder.apply(ScooterStatus(), 250, payload)
        assertEquals("v1.2", status.firmwareInfo)
    }

    @Test
    fun unknownCommandOnlyUpdatesMeta() {
        val status = PayloadDecoder.apply(ScooterStatus(), 200, byteArrayOf(1))
        assertNull(status.batteryPercent)
        assertEquals(200, status.lastCommand)
    }

    @Test
    fun describe_acknowledgements() {
        assertEquals("unlock ACK (no status)", PayloadDecoder.describe(5, byteArrayOf()))
        assertEquals("unlock OK", PayloadDecoder.describe(5, byteArrayOf(0)))
        assertEquals("lock FAIL status=0x01", PayloadDecoder.describe(21, byteArrayOf(1)))
    }

    @Test
    fun describe_transmissionStatuses() {
        assertEquals("transmission: password error", PayloadDecoder.describe(251, byteArrayOf(0)))
        assertEquals("transmission: READY", PayloadDecoder.describe(251, byteArrayOf(1)))
        assertEquals("transmission: COMPLETE", PayloadDecoder.describe(251, byteArrayOf(2)))
    }

    @Test
    fun ascii_replacesNonPrintableBytes() {
        assertEquals("A.B", PayloadDecoder.ascii(byteArrayOf(0x41, 0x00, 0x42)))
    }
}
