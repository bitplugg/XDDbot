package xdbot.bitplugg.ru.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import xdbot.bitplugg.ru.XddbotApp
import xdbot.bitplugg.ru.ui.device.DeviceScreen
import xdbot.bitplugg.ru.ui.logs.LogsScreen
import xdbot.bitplugg.ru.ui.scan.ScanScreen
import xdbot.bitplugg.ru.ui.settings.SettingsScreen

private enum class Tab(val label: String) {
    SCAN("Скан"),
    DEVICE("Устройство"),
    SETTINGS("Настройки"),
    LOGS("Логи"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XddbotRoot() {
    val app = LocalContext.current.applicationContext as XddbotApp
    val repository = app.keyRepository
    val client = app.bleClient
    var tab by remember { mutableStateOf(Tab.SCAN) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("XDDBOT") }) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        icon = {},
                        label = { Text(entry.label) },
                    )
                }
            }
        },
    ) { padding ->
        when (tab) {
            Tab.SCAN -> ScanScreen(client, repository, padding)
            Tab.DEVICE -> DeviceScreen(client, repository, padding)
            Tab.SETTINGS -> SettingsScreen(repository, padding)
            Tab.LOGS -> LogsScreen(client, padding)
        }
    }
}
