package xdbot.bitplugg.ru.data

/** One key parsed from an imported text file. */
data class ParsedKey(val label: String, val keyHex: String, val deviceType: Int)

data class KeyFileResult(val keys: List<ParsedKey>, val skipped: Int)

/**
 * Parses a plain-text key file. Supported lines:
 *  - `a1b2c3d4`              (bare hex, default deviceType)
 *  - `Мой самокат=a1b2c3d4`  (`label=hex`)
 *  - `Мой самокат=a1b2c3d4,type=66`
 * Lines starting with `#` and blank lines are ignored. Separators like `:` or `-`
 * inside the hex are tolerated. Invalid keys are counted in [KeyFileResult.skipped].
 */
object KeyFile {

    private val TYPE_RE = Regex("""[,;]\s*type\s*=\s*(\d+)""", RegexOption.IGNORE_CASE)
    private val SEPARATORS = Regex("""[:\-_]""")

    fun parse(text: String, defaultLabel: String = "Импорт"): KeyFileResult {
        val keys = mutableListOf<ParsedKey>()
        var skipped = 0
        var index = 0

        for (rawLine in text.lineSequence()) {
            val line = rawLine.substringBefore('#').trim()
            if (line.isEmpty()) continue

            val hasLabel = line.contains('=')
            val label = if (hasLabel) line.substringBefore('=').trim() else ""
            var rest = if (hasLabel) line.substringAfter('=') else line

            var deviceType = BleKey.DEFAULT_DEVICE_TYPE
            TYPE_RE.find(rest)?.let { match ->
                deviceType = match.groupValues[1].toInt()
                rest = rest.removeRange(match.range)
            }

            val parsed = BleKey.parse(rest.replace(SEPARATORS, ""), deviceType)
            if (parsed.isSuccess) {
                index++
                keys += ParsedKey(
                    label = label.ifBlank { "$defaultLabel $index" },
                    keyHex = parsed.getOrThrow(),
                    deviceType = deviceType,
                )
            } else {
                skipped++
            }
        }
        return KeyFileResult(keys, skipped)
    }

    /** Renders keys as `label=hex,type=NN` lines for export/backup. */
    fun format(keys: List<BleKey>): String =
        keys.joinToString(separator = "\n", postfix = "\n") { key ->
            "${key.label.ifBlank { "Ключ" }}=${key.keyHex},type=${key.deviceType}"
        }
}
