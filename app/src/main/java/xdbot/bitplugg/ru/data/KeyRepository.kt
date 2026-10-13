package xdbot.bitplugg.ru.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Stores user-entered BLE keys in an [EncryptedSharedPreferences] file. */
class KeyRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences by lazy { createPrefs(appContext) }

    fun keys(): List<BleKey> {
        val raw = prefs.getString(KEY_LIST, null) ?: return emptyList()
        return runCatching {
            deserialize(JSONArray(raw)).toList()
        }.getOrDefault(emptyList())
    }

    fun activeKey(): BleKey? {
        val id = prefs.getString(ACTIVE_ID, null) ?: return null
        return keys().firstOrNull { it.id == id }
    }

    fun upsert(key: BleKey) {
        val list = keys().toMutableList()
        val index = list.indexOfFirst { it.id == key.id }
        if (index >= 0) list[index] = key else list.add(key)
        persist(list)
    }

    fun add(label: String, keyHex: String, deviceType: Int): BleKey =
        BleKey(UUID.randomUUID().toString(), label, keyHex, deviceType).also(::upsert)

    fun remove(id: String) {
        persist(keys().filterNot { it.id == id })
        if (prefs.getString(ACTIVE_ID, null) == id) {
            prefs.edit().remove(ACTIVE_ID).apply()
        }
    }

    fun setActive(id: String?) {
        prefs.edit().apply {
            if (id == null) remove(ACTIVE_ID) else putString(ACTIVE_ID, id)
        }.apply()
    }

    private fun persist(list: List<BleKey>) {
        val arr = JSONArray()
        list.forEach { key ->
            arr.put(
                JSONObject().apply {
                    put("id", key.id)
                    put("label", key.label)
                    put("keyHex", key.keyHex)
                    put("deviceType", key.deviceType)
                },
            )
        }
        prefs.edit().putString(KEY_LIST, arr.toString()).apply()
    }

    private fun deserialize(arr: JSONArray) = sequence {
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val hex = o.optString("keyHex")
            val parsed = BleKey.parse(hex, o.optInt("deviceType", BleKey.DEFAULT_DEVICE_TYPE))
            if (parsed.isSuccess) {
                yield(
                    BleKey(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        label = o.optString("label"),
                        keyHex = parsed.getOrThrow(),
                        deviceType = o.optInt("deviceType", BleKey.DEFAULT_DEVICE_TYPE),
                    ),
                )
            }
        }
    }

    private fun createPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private companion object {
        const val FILE_NAME = "xddbot_secure"
        const val KEY_LIST = "ble_keys"
        const val ACTIVE_ID = "active_key_id"
    }
}
