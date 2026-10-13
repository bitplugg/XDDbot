package xdbot.bitplugg.ru.ui.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xdbot.bitplugg.ru.ble.ConnState
import xdbot.bitplugg.ru.ble.ScooterBleClient

@Composable
fun DiagnosticsScreen(
    client: ScooterBleClient,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val status by client.telemetry.collectAsState()
    val state by client.state.collectAsState()
    val ready = state == ConnState.READY

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Данные устройства", style = MaterialTheme.typography.titleLarge)
        Text(
            "Значения с самоката. Нажмите «Обновить», чтобы запросить свежие.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Батарея", style = MaterialTheme.typography.labelLarge)
                val battery = status.batteryPercent
                if (battery == null) {
                    Text("нет данных", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LinearProgressIndicator(
                        progress = { battery / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("$battery%", style = MaterialTheme.typography.headlineSmall)
                }
                StatusRow("Скорость", status.speedKmh?.let { "${fmt(it, 1)} км/ч" })
                StatusRow("Пробег", status.tripKm?.let { "${fmt(it, 2)} км" })
                StatusRow("Запас хода", status.rangeKm?.let { "${fmt(it, 2)} км" })
                StatusRow("Ток", status.current?.let { "${fmt(it, 2)} A" })
                StatusRow("Напряжение", status.voltage?.let { "${fmt(it, 2)} В" })
                StatusRow("Режим", status.mode?.toString())
                StatusRow(
                    "Замок",
                    status.locked?.let { if (it) "закрыт" else "открыт" },
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Прошивка", style = MaterialTheme.typography.labelLarge)
                StatusRow("Версия", status.firmware)
                StatusRow("Инфо", status.firmwareInfo)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("RFID", style = MaterialTheme.typography.labelLarge)
                StatusRow("ID", status.rfidCardId?.toString())
                StatusRow("Hex", status.rfidCardHex)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = {
                    client.requestInfo()
                    client.requestRealtime()
                    client.requestDeviceInfo()
                },
                enabled = ready,
                modifier = Modifier.weight(1f),
            ) { Text("Обновить") }
            OutlinedButton(
                onClick = { client.readRfid() },
                enabled = ready,
                modifier = Modifier.weight(1f),
            ) { Text("RFID") }
            OutlinedButton(
                onClick = { client.requestFirmwareInfo() },
                enabled = ready,
                modifier = Modifier.weight(1f),
            ) { Text("Прошивка") }
        }

        if (!ready) {
            Text(
                "Команды доступны после подключения к устройству.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value ?: "—",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun fmt(value: Double, decimals: Int): String =
    String.format(java.util.Locale.US, "%.${decimals}f", value)
