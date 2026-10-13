package xdbot.bitplugg.ru.data

/** Minimal semantic-version comparison for update checks. */
object SemVer {

    /** Returns >0 if [a] is newer than [b], <0 if older, 0 if equal. */
    fun compare(a: String, b: String): Int {
        val left = parse(a)
        val right = parse(b)
        for (i in 0 until maxOf(left.size, right.size)) {
            val l = left.getOrElse(i) { 0 }
            val r = right.getOrElse(i) { 0 }
            if (l != r) return l - r
        }
        return 0
    }

    private fun parse(version: String): List<Int> =
        version.trim()
            .removePrefix("v")
            .substringBefore('-')
            .split('.')
            .map { it.toIntOrNull() ?: 0 }
}
