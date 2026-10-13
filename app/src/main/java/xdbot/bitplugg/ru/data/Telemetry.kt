package xdbot.bitplugg.ru.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import xdbot.bitplugg.ru.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Optional, opt-in anonymous telemetry. Disabled by default and completely inert
 * when no endpoint was baked in at build time.
 *
 * It sends only coarse event names and a few non-identifying properties
 * (app version, Android SDK, device type reported by the scooter, command name).
 * It never sends keys, codes, BLE addresses, device or user names.
 */
object Telemetry {

    private const val PREFS = "xddbot_telemetry"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_INSTALL = "install_id"

    private val forbidden = setOf(
        "key", "keys", "code", "codes", "secret", "token", "password",
        "imei", "mac", "address", "serial", "pin",
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun prefs(): SharedPreferences =
        requireNotNull(appContext) { "Telemetry.init() not called" }
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** True when a collector URL was compiled in; the toggle is meaningless otherwise. */
    val available: Boolean get() = BuildConfig.TELEMETRY_URL.isNotBlank()

    var enabled: Boolean
        get() = available && prefs().getBoolean(KEY_ENABLED, false)
        set(value) {
            prefs().edit().putBoolean(KEY_ENABLED, value).apply()
            if (!value) prefs().edit().remove(KEY_INSTALL).apply()
        }

    fun event(name: String, vararg props: Pair<String, Any?>) {
        if (!enabled) return
        val clean = props.associate { it.first to it.second }
        scope.launch { runCatching { send(name, clean) } }
    }

    private fun installId(): String {
        val p = prefs()
        p.getString(KEY_INSTALL, null)?.let { return it }
        val id = UUID.randomUUID().toString()
        p.edit().putString(KEY_INSTALL, id).apply()
        return id
    }

    private fun send(name: String, props: Map<String, Any?>) {
        if (!isSafe(name)) return
        val event = JSONObject().apply {
            put("name", name)
            put("ts", System.currentTimeMillis())
            put("props", JSONObject().apply {
                props.forEach { (key, value) ->
                    if (isSafe(key) && isSafe(value?.toString().orEmpty())) put(key, value)
                }
            })
        }
        val payload = JSONObject().apply {
            put("app", BuildConfig.VERSION_NAME)
            put("sdk", Build.VERSION.SDK_INT)
            put("install", installId())
            put("events", JSONArray().put(event))
        }.toString()

        val connection = (URL(BuildConfig.TELEMETRY_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 10_000
            doOutput = true
            setRequestProperty("content-type", "application/json")
        }
        try {
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            connection.responseCode
        } finally {
            connection.disconnect()
        }
    }

    private fun isSafe(text: String): Boolean {
        val tokens = text.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
        return tokens.none { it in forbidden }
    }
}
