package xdbot.bitplugg.ru.protocol

/** Latest values decoded from the scooter's responses. */
data class ScooterStatus(
    val batteryPercent: Int? = null,
    val mode: Int? = null,
    val speedKmh: Double? = null,
    val tripKm: Double? = null,
    val rangeKm: Double? = null,
    val current: Double? = null,
    val voltage: Double? = null,
    val locked: Boolean? = null,
    val firmware: String? = null,
    val rfidCardHex: String? = null,
    val rfidCardId: Long? = null,
    val firmwareInfo: String? = null,
    val lastCommand: Int? = null,
    val lastUpdated: Long? = null,
)
