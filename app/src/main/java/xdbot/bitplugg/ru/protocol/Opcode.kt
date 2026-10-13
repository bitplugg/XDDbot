package xdbot.bitplugg.ru.protocol

/**
 * Scooter command opcodes. Names/directions recovered from the reference client
 * (ru.lostbotapp, A2/c.java). Direction describes whether the phone sends the command,
 * receives it, or both.
 */
enum class Direction { BIDIRECTIONAL, SET_ONLY, NOTIFICATION }

enum class Opcode(val code: Int, val dir: Direction, val description: String? = null) {
    COMMUNICATION_KEY(1, Direction.BIDIRECTIONAL, "Handshake / session key exchange"),
    UNLOCK(5, Direction.BIDIRECTIONAL),
    ERROR(16, Direction.NOTIFICATION, "Scooter rejected previous command"),
    LOCK(21, Direction.BIDIRECTIONAL),
    DEVICE_INFO(49, Direction.BIDIRECTIONAL),
    HELMET_UNLOCK_MQTT_ACCOUNT(52, Direction.SET_ONLY),
    MQTT_PASSWORD(53, Direction.SET_ONLY),
    INSTALLATION_MODE(55, Direction.SET_ONLY),
    AUTO_LOCK_MODE(61, Direction.SET_ONLY),
    STEERING_MODE(64, Direction.SET_ONLY),
    DISABLE_GSM_COMMUNICATION(80, Direction.SET_ONLY),
    SCOOTER_OLD_DATA(81, Direction.BIDIRECTIONAL),
    CLEAR_DATA(82, Direction.SET_ONLY),
    GSM_AUTO_DETECT(83, Direction.SET_ONLY),
    CLEAR_BT_PAIRED_USERS(84, Direction.SET_ONLY),
    GPS_TRACKING_SETTINGS(85, Direction.SET_ONLY),
    METER_CONFIG(86, Direction.SET_ONLY),
    SCOOTER_INFO(96, Direction.BIDIRECTIONAL),
    SCOOTER_SET(97, Direction.SET_ONLY),
    SCOOTER_CONFIG(98, Direction.SET_ONLY),
    VEHICLE_REALTIME_INFO(99, Direction.BIDIRECTIONAL),
    SET_WHEEL_DIAMETER(100, Direction.SET_ONLY),
    CALIBRATE_INSTRUMENT_TIME(114, Direction.SET_ONLY),
    METER_RESET(115, Direction.SET_ONLY),
    NAVIGATION(116, Direction.SET_ONLY),
    AUDIO_TOGGLE(117, Direction.SET_ONLY),
    START_TESTING(118, Direction.SET_ONLY),
    CLEAR_MILEAGE(119, Direction.SET_ONLY),
    FIND_CAR(120, Direction.SET_ONLY),
    AUTOMATIC_SCREEN_OFF(121, Direction.SET_ONLY),
    SCOOTER_BATTERY_OUT_LOCK(129, Direction.BIDIRECTIONAL),
    SCOOTER_COLOR_LIGHT(130, Direction.SET_ONLY),
    READ_RFID_CARD(133, Direction.BIDIRECTIONAL),
    DELETE_RFID(134, Direction.SET_ONLY),
    XM_POWER_CONTROL(144, Direction.SET_ONLY),
    POWER_CONTROL(145, Direction.SET_ONLY),
    IOT_RESTART(146, Direction.SET_ONLY),
    CHAIN_LOCK_PASSWORD_ENTRY(149, Direction.SET_ONLY),
    CHAIN_LOCK_PASSWORD_DELETE(150, Direction.SET_ONLY),
    CHAIN_LOCK_GET_USER_PASSWORD(151, Direction.BIDIRECTIONAL),
    VEHICLE_COMMANDS(160, Direction.BIDIRECTIONAL),
    QR_CODE_READ_WRITE(161, Direction.BIDIRECTIONAL),
    FIND_CAR_SOUND(162, Direction.SET_ONLY),
    BATTERY_UNLOCK(192, Direction.SET_ONLY),
    ACCESSORY_CONTROL(195, Direction.SET_ONLY, "Saddle / tail-box / helmet open or close"),
    UPDATE_CERTIFICATE(196, Direction.SET_ONLY),
    SWITCH_SIM(197, Direction.SET_ONLY),
    SWITCH_NEXT_SIM(198, Direction.SET_ONLY),
    BATTERY_LIGHT_OR_LOG_DATA(199, Direction.BIDIRECTIONAL),
    AVAS_SWITCH(220, Direction.SET_ONLY),
    AVAS_SWITCH_2(222, Direction.SET_ONLY),
    FW_DEVICE_INFO(250, Direction.BIDIRECTIONAL),
    FW_DEVICE_DATA(251, Direction.BIDIRECTIONAL),
    FW_GET_FIRMWARE_DATA(252, Direction.BIDIRECTIONAL),
    ;

    companion object {
        private val BY_CODE = entries.associateBy { it.code }
        fun from(code: Int): Opcode? = BY_CODE[code and 0xFF]
    }
}
