package xdbot.bitplugg.ru.ui.diagnostics

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xdbot.bitplugg.ru.R
import xdbot.bitplugg.ru.ble.ConnState
import xdbot.bitplugg.ru.ble.ScooterBleClient
import xdbot.bitplugg.ru.data.TripRecorder
import java.util.Locale

@Composable
fun DiagnosticsScreen(
    client: ScooterBleClient,
    trip: TripRecorder,
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

        TripCard(trip)

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

@Composable
private fun TripCard(trip: TripRecorder) {
    val recording by trip.recording.collectAsState()
    val samples by trip.samples.collectAsState()
    val context = LocalContext.current

    val export = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(trip.csv().toByteArray(Charsets.UTF_8))
            }
            Toast.makeText(context, "CSV saved", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Trip recorder", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { if (recording) trip.stop() else trip.start() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (recording) "Stop" else "Record")
                }
                OutlinedButton(onClick = { trip.clear() }, enabled = !recording && samples.isNotEmpty()) {
                    Text("Clear")
                }
                OutlinedButton(
                    onClick = { export.launch("xddbot-trip-${System.currentTimeMillis()}.csv") },
                    enabled = samples.isNotEmpty(),
                ) {
                    Text("Export CSV")
                }
            }
            SpeedChart(samples, modifier = Modifier.height(180.dp))
            Text("${samples.size} points", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SpeedChart(samples: List<xdbot.bitplugg.ru.data.TripSample>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth()) {
        if (samples.size < 2) return@Canvas
        val speeds = samples.mapNotNull { it.speedKmh }
        if (speeds.isEmpty()) return@Canvas
        val min = speeds.minOrNull() ?: 0.0
        val max = speeds.maxOrNull() ?: 0.0
        val range = (max - min).takeIf { it > 0.0 } ?: 1.0
        val dx = size.width / (samples.size - 1)
        val pathY = { s: xdbot.bitplugg.ru.data.TripSample ->
            val v = s.speedKmh ?: min
            size.height * (1f - ((v - min).toFloat() / range.toFloat()))
        }
        var prev: Offset? = null
        samples.forEachIndexed { i, s ->
            val x = dx * i
            val y = pathY(s)
            val p = Offset(x, y)
            prev = prev?.let { p0 -> drawLine(Color.Gray, p0, p, strokeWidth = 2f); p } ?: p
        }
    }
}

private fun fmt(value: Double, decimals: Int): String =
    String.format(Locale.US, "%.${decimals}f", value)
