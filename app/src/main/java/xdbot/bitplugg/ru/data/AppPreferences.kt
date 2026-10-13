package xdbot.bitplugg.ru.data

import android.content.Context
import android.content.SharedPreferences

/** Non-secret app preferences (last device, auto-connect). */
class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("xddbot_prefs", Context.MODE_PRIVATE)

    var autoConnect: Boolean
        get() = prefs.getBoolean("auto_connect", false)
        set(value) = prefs.edit().putBoolean("auto_connect", value).apply()

    var lastDeviceAddress: String?
        get() = prefs.getString("last_device_address", null)
        set(value) = prefs.edit().putString("last_device_address", value).apply()

    var lastDeviceName: String?
        get() = prefs.getString("last_device_name", null)
        set(value) = prefs.edit().putString("last_device_name", value).apply()
}
