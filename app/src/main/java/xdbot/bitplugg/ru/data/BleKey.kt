package xdbot.bitplugg.ru.data

import xdbot.bitplugg.ru.protocol.Hex

/**
 * A BLE device key supplied by the owner of the scooter.
 * XDDBOT only stores and transmits keys the user enters; it never generates or guesses them.
 */
data class BleKey(
    val id: String,
    val label: String,
    val keyHex: String,
    val deviceType: Int = DEFAULT_DEVICE_TYPE,
) {
    val keyBytes: ByteArray get() = Hex.decode(keyHex)

    companion object {
        const val DEFAULT_DEVICE_TYPE = 66
        const val MAX_KEY_BYTES = 64

        /** Normalizes and validates a user-entered hex key. */
        fun parse(raw: String, deviceType: Int = DEFAULT_DEVICE_TYPE): Result<String> {
            val hex = raw.filterNot(Char::isWhitespace)
                .removePrefix("0x")
                .removePrefix("0X")
            return when {
                hex.isEmpty() ->
                    Result.failure(KeyFormatException("Введите ключ"))
                hex.any { it !in '0'..'9' && it.lowercaseChar() !in 'a'..'f' } ->
                    Result.failure(KeyFormatException("Допустимы только hex-символы 0-9, A-F"))
                hex.length % 2 != 0 ->
                    Result.failure(KeyFormatException("Нечётное число hex-символов"))
                hex.length / 2 > MAX_KEY_BYTES ->
                    Result.failure(KeyFormatException("Ключ длиннее $MAX_KEY_BYTES байт"))
                deviceType !in 0..255 ->
                    Result.failure(KeyFormatException("deviceType должен быть в диапазоне 0..255"))
                else -> Result.success(hex.lowercase())
            }
        }
    }
}

class KeyFormatException(message: String) : IllegalArgumentException(message)
