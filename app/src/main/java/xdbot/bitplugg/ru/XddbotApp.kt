package xdbot.bitplugg.ru

import android.app.Application
import xdbot.bitplugg.ru.data.KeyRepository

class XddbotApp : Application() {

    val keyRepository: KeyRepository by lazy { KeyRepository(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: XddbotApp
            private set
    }
}
