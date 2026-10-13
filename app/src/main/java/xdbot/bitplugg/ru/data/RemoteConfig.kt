package xdbot.bitplugg.ru.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import xdbot.bitplugg.ru.BuildConfig
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val latest: String,
    val minimum: String,
    val mandatory: Boolean,
    val notes: String,
    val url: String,
)

data class DeviceProfile(
    val id: String,
    val name: String,
    val deviceType: Int,
    val transferType: Int,
    val notes: String,
)

/**
 * Reads the static JSON API published on GitHub Pages (`docs/api`), which replaces
 * the original hosted backend. Non-blocking; returns null on any failure.
 */
object RemoteConfig {

    private val base = BuildConfig.API_BASE_URL

    suspend fun fetchVersion(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val json = JSONObject(get("${base}version.json"))
            UpdateInfo(
                latest = json.getString("latest"),
                minimum = json.optString("minimum", json.getString("latest")),
                mandatory = json.optBoolean("mandatory", false),
                notes = json.optString("notes", ""),
                url = json.optString("url", ""),
            )
        }.getOrNull()
    }

    suspend fun fetchProfiles(): List<DeviceProfile>? = withContext(Dispatchers.IO) {
        runCatching {
            val array = JSONObject(get("${base}devices.json")).getJSONArray("profiles")
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        DeviceProfile(
                            id = o.getString("id"),
                            name = o.getString("name"),
                            deviceType = o.optInt("deviceType", BleKey.DEFAULT_DEVICE_TYPE),
                            transferType = o.optInt("transferType", 0),
                            notes = o.optString("notes", ""),
                        ),
                    )
                }
            }
        }.getOrNull()
    }

    private fun get(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("accept", "application/json")
        }
        try {
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
            return connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        } finally {
            connection.disconnect()
        }
    }
}
