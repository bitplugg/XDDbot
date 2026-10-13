package xdbot.bitplugg.ru.data

/** One code parsed from an imported text file. */
data class ParsedKey(val label: String, val code: String, val deviceType: Int)

data class KeyFileResult(val keys: List<ParsedKey>, val skipped: Int)

/**
 * Parses a plain-text file with codes/keys. Supported lines:
 *  - `12AB34CD`                 (bare code, default deviceType)
 *  - `Мой самокат=12AB34CD`     (`label=code`)
 *  - `Мой самокат=12AB34CD,type=42`
 * Blank lines and lines starting with `#` are ignored. The code is kept verbatim
 * (only trimmed), because the handshake transmits it as raw UTF-8 text.
 */
object KeyFile {

    private val TYPE_RE = Regex("""[,;]\s*type\s*=\s*(\d+)""", RegexOption.IGNORE_CASE)

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

            val parsed = BleKey.parse(rest, deviceType)
            if (parsed.isSuccess) {
                index++
                keys += ParsedKey(
                    label = label.ifBlank { "$defaultLabel $index" },
                    code = parsed.getOrThrow(),
                    deviceType = deviceType,
                )
            } else {
                skipped++
            }
        }
        return KeyFileResult(keys, skipped)
    }

    /** Renders keys as `label=code,type=NN` lines for export/backup. */
    fun format(keys: List<BleKey>): String =
        keys.joinToString(separator = "\n", postfix = "\n") { key ->
            "${key.label.ifBlank { "Ключ" }}=${key.code},type=${key.deviceType}"
        }
}
