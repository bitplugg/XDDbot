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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xdbot.bitplugg.ru.R
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
        Text(stringResource(R.string.data_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.data_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(stringResource(R.string.data_battery), style = MaterialTheme.typography.labelLarge)
                val battery = status.batteryPercent
                if (battery == null) {
                    Text(stringResource(R.string.data_no_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LinearProgressIndicator(
                        progress = { battery / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(stringResource(R.string.data_battery_percent, battery), style = MaterialTheme.typography.headlineSmall)
                }
                StatusRow(stringResource(R.string.data_speed), status.speedKmh?.let { stringResource(R.string.unit_kmh, fmt(it, 1)) })
                StatusRow(stringResource(R.string.data_trip), status.tripKm?.let { stringResource(R.string.unit_km, fmt(it, 2)) })
                StatusRow(stringResource(R.string.data_range), status.rangeKm?.let { stringResource(R.string.unit_km, fmt(it, 2)) })
                StatusRow(stringResource(R.string.data_current), status.current?.let { stringResource(R.string.unit_a, fmt(it, 2)) })
                StatusRow(stringResource(R.string.data_voltage), status.voltage?.let { stringResource(R.string.unit_v, fmt(it, 2)) })
                StatusRow(stringResource(R.string.data_mode), status.mode?.toString())
                StatusRow(
                    stringResource(R.string.data_lock),
                    status.locked?.let {
                        stringResource(if (it) R.string.data_locked else R.string.data_unlocked)
                    },
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(stringResource(R.string.data_firmware), style = MaterialTheme.typography.labelLarge)
                StatusRow(stringResource(R.string.data_version), status.firmware)
                StatusRow(stringResource(R.string.data_info), status.firmwareInfo)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(stringResource(R.string.data_rfid), style = MaterialTheme.typography.labelLarge)
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
            ) { Text(stringResource(R.string.data_refresh)) }
            OutlinedButton(
                onClick = { client.readRfid() },
                enabled = ready,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.data_rfid)) }
            OutlinedButton(
                onClick = { client.requestFirmwareInfo() },
                enabled = ready,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.data_firmware)) }
        }

        if (!ready) {
            Text(
                stringResource(R.string.data_not_ready),
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
