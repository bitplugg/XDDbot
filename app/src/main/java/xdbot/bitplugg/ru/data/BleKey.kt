package xdbot.bitplugg.ru.data

/**
 * A code/key entered by the owner of the scooter.
 * The reference client sends the raw string as UTF-8 bytes in the handshake (opcode 1),
 * so this is text, not hex-decoded bytes. XDDBOT only stores and transmits codes the
 * user supplies; it never generates or guesses them.
 */
data class BleKey(
    val id: String,
    val label: String,
    val code: String,
    val deviceType: Int = DEFAULT_DEVICE_TYPE,
) {
    val keyBytes: ByteArray get() = code.toByteArray(Charsets.UTF_8)

    companion object {
        const val DEFAULT_DEVICE_TYPE = 66
        const val MAX_CODE_BYTES = 255

        /** Trims and validates a user-entered code/key. */
        fun parse(raw: String, deviceType: Int = DEFAULT_DEVICE_TYPE): Result<String> {
            val code = raw.trim()
            val bytes = code.toByteArray(Charsets.UTF_8)
            return when {
                code.isEmpty() ->
                    Result.failure(KeyFormatException("Введите код или ключ"))
                bytes.size > MAX_CODE_BYTES ->
                    Result.failure(KeyFormatException("Код длиннее $MAX_CODE_BYTES байт"))
                deviceType !in 0..255 ->
                    Result.failure(KeyFormatException("deviceType должен быть в диапазоне 0..255"))
                else -> Result.success(code)
            }
        }
    }
}

class KeyFormatException(message: String) : IllegalArgumentException(message)
