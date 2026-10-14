package xdbot.bitplugg.ru.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import xdbot.bitplugg.ru.BuildConfig
import xdbot.bitplugg.ru.R
import xdbot.bitplugg.ru.XddbotApp
import xdbot.bitplugg.ru.ble.ConnState
import xdbot.bitplugg.ru.ble.ScooterService
import xdbot.bitplugg.ru.data.RemoteConfig
import xdbot.bitplugg.ru.data.SemVer
import xdbot.bitplugg.ru.data.UpdateInfo
import xdbot.bitplugg.ru.ui.device.DeviceScreen
import xdbot.bitplugg.ru.ui.diagnostics.DiagnosticsScreen
import xdbot.bitplugg.ru.ui.logs.LogsScreen
import xdbot.bitplugg.ru.ui.scan.ScanScreen
import xdbot.bitplugg.ru.ui.settings.SettingsScreen

private enum class Tab(@StringRes val label: Int) {
    SCAN(R.string.tab_scan),
    DEVICE(R.string.tab_device),
    DATA(R.string.tab_data),
    SETTINGS(R.string.tab_settings),
    LOGS(R.string.tab_logs),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XddbotRoot() {
    val app = LocalContext.current.applicationContext as XddbotApp
    val repository = app.keyRepository
    val client = app.bleClient
    var tab by remember { mutableStateOf(Tab.SCAN) }
    val context = LocalContext.current
    val connState by client.state.collectAsState()

    LaunchedEffect(Unit) {
        if (app.appPreferences.autoConnect) {
            app.appPreferences.lastDeviceAddress?.let { client.connect(it, repository.activeKey()?.code) }
        }
    }

    LaunchedEffect(connState) {
        val active = connState == ConnState.CONNECTING || connState == ConnState.DISCOVERING ||
            connState == ConnState.AUTHENTICATING || connState == ConnState.READY
        if (active) {
            runCatching { ScooterService.start(context) }
        } else {
            ScooterService.stop(context)
        }
    }

    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(Unit) {
        val info = RemoteConfig.fetchVersion() ?: return@LaunchedEffect
        if (SemVer.compare(info.latest, BuildConfig.VERSION_NAME) > 0) update = info
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        icon = {},
                        label = { Text(stringResource(entry.label)) },
                    )
                }
            }
        },
    ) { padding ->
        when (tab) {
            Tab.SCAN -> ScanScreen(client, repository, padding)
            Tab.DEVICE -> DeviceScreen(client, repository, padding)
            Tab.DATA -> DiagnosticsScreen(client, app.tripRecorder, padding)
            Tab.SETTINGS -> SettingsScreen(repository, padding)
            Tab.LOGS -> LogsScreen(client, padding)
        }
    }

    update?.let { info ->
        AlertDialog(
            onDismissRequest = { if (!info.mandatory) update = null },
            title = { Text(stringResource(R.string.update_title, info.latest)) },
            text = {
                Text(info.notes.ifBlank { stringResource(R.string.update_default_notes) })
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (info.url.isNotBlank()) runCatching { uriHandler.openUri(info.url) }
                        if (!info.mandatory) update = null
                    },
                ) { Text(stringResource(R.string.update_download)) }
            },
            dismissButton = {
                if (!info.mandatory) {
                    TextButton(onClick = { update = null }) {
                        Text(stringResource(R.string.update_later))
                    }
                }
            },
        )
    }
}
