package xdbot.bitplugg.ru

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import xdbot.bitplugg.ru.ui.XddbotRoot
import xdbot.bitplugg.ru.ui.theme.XddbotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XddbotTheme {
                XddbotRoot()
            }
        }
    }
}
