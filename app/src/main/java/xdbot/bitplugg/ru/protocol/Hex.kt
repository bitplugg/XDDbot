package xdbot.bitplugg.ru.protocol

object Hex {

    private const val DIGITS = "0123456789ABCDEF"

    fun encode(bytes: ByteArray, spaced: Boolean = true): String {
        val sb = StringBuilder(bytes.size * 3)
        for ((i, b) in bytes.withIndex()) {
            if (spaced && i > 0) sb.append(' ')
            val v = b.toInt() and 0xFF
            sb.append(DIGITS[v ushr 4]).append(DIGITS[v and 0x0F])
        }
        return sb.toString()
    }

    /** Lenient parser: ignores spaces/separators, accepts 0-9a-fA-F, requires even length. */
    fun decode(input: String): ByteArray {
        val clean = input.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
        require(clean.length % 2 == 0) { "Hex string must contain an even number of characters" }
        val out = ByteArray(clean.length / 2)
        for (i in out.indices) {
            out[i] = clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return out
    }
}
