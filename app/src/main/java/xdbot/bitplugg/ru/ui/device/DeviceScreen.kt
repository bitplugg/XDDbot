package xdbot.bitplugg.ru.ui.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import xdbot.bitplugg.ru.ble.ConnState
import xdbot.bitplugg.ru.ble.ScooterBleClient
import xdbot.bitplugg.ru.data.KeyRepository

@Composable
fun DeviceScreen(
    client: ScooterBleClient,
    repository: KeyRepository,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val active = repository.activeKey()
    val state by client.state.collectAsState()
    val status by client.status.collectAsState()
    val deviceType by client.deviceType.collectAsState()
    val demo by client.demo.collectAsState()
    val ready = state == ConnState.READY

    Column(
        modifier = modifier.fillMaxSize().padding(contentPadding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Устройство", style = MaterialTheme.typography.titleLarge)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (active == null) {
                    Text("Ключ не выбран")
                    Text(
                        "Добавьте код своего самоката в разделе «Настройки».",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(active.label.ifBlank { "Без названия" }, style = MaterialTheme.typography.titleMedium)
                    Text("type $deviceType", style = MaterialTheme.typography.bodySmall)
                    Text(active.code, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Text("Статус: $status", style = MaterialTheme.typography.bodyMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = { client.unlock() }, enabled = ready, modifier = Modifier.weight(1f)) {
                Text("Открыть")
            }
            Button(onClick = { client.lock() }, enabled = ready, modifier = Modifier.weight(1f)) {
                Text("Закрыть")
            }
            OutlinedButton(onClick = { client.findCar() }, enabled = ready, modifier = Modifier.weight(1f)) {
                Text("Сигнал")
            }
        }

        OutlinedButton(
            onClick = { client.disconnect() },
            enabled = state != ConnState.IDLE,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Отключиться")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Демо-режим", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Имитация BLE без реального устройства.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = demo, onCheckedChange = { client.demoMode = it })
        }
    }
}
