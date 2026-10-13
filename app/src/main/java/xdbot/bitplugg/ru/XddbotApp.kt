package xdbot.bitplugg.ru

import android.app.Application
import xdbot.bitplugg.ru.ble.ScooterBleClient
import xdbot.bitplugg.ru.data.KeyRepository
import xdbot.bitplugg.ru.data.Telemetry

class XddbotApp : Application() {

    val keyRepository: KeyRepository by lazy { KeyRepository(this) }
    val bleClient: ScooterBleClient by lazy { ScooterBleClient(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Telemetry.init(this)
        Telemetry.event("app_start")
    }

    companion object {
        lateinit var instance: XddbotApp
            private set
    }
}
