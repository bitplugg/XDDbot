package xdbot.bitplugg.ru

import android.app.Application

class XddbotApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: XddbotApp
            private set
    }
}
