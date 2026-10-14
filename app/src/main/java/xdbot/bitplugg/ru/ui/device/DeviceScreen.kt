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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import xdbot.bitplugg.ru.R
import xdbot.bitplugg.ru.ble.Accessory
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
        Text(stringResource(R.string.device_title), style = MaterialTheme.typography.titleLarge)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (active == null) {
                    Text(stringResource(R.string.device_no_key))
                    Text(
                        stringResource(R.string.device_no_key_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        active.label.ifBlank { stringResource(R.string.device_unnamed_short) },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.device_type_fmt, deviceType),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(active.code, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Text(stringResource(R.string.device_status_fmt, status), style = MaterialTheme.typography.bodyMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = { client.unlock() }, enabled = ready, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.action_unlock))
            }
            Button(onClick = { client.lock() }, enabled = ready, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.action_lock))
            }
            OutlinedButton(onClick = { client.findCar() }, enabled = ready, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.action_find))
            }
        }

        OutlinedButton(
            onClick = { client.disconnect() },
            enabled = state != ConnState.IDLE,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.action_disconnect))
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.extras_title), style = MaterialTheme.typography.titleMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = { client.headlight(true) }, enabled = ready, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.extra_light_on))
                    }
                    OutlinedButton(onClick = { client.headlight(false) }, enabled = ready, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.extra_light_off))
                    }
                }

                Text(stringResource(R.string.extra_sound), style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(1, 2, 3, 6).forEach { id ->
                        OutlinedButton(
                            onClick = { client.findCarSound(id) },
                            enabled = ready,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text(id.toString())
                        }
                    }
                }

                Text(stringResource(R.string.extra_accessories), style = MaterialTheme.typography.labelLarge)
                AccessoryRow(R.string.accessory_helmet, Accessory.HELMET, ready, client)
                AccessoryRow(R.string.accessory_seat, Accessory.SEAT, ready, client)
                AccessoryRow(R.string.accessory_trunk, Accessory.TRUNK, ready, client)

                OutlinedButton(
                    onClick = { client.batteryUnlock() },
                    enabled = ready,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.extra_battery_unlock))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(stringResource(R.string.demo_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.demo_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = demo, onCheckedChange = { client.demoMode = it })
        }
    }
}

@Composable
private fun AccessoryRow(
    labelRes: Int,
    accessory: Accessory,
    enabled: Boolean,
    client: ScooterBleClient,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(labelRes), modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { client.accessory(accessory, true) }, enabled = enabled) {
            Text(stringResource(R.string.action_unlock))
        }
        OutlinedButton(onClick = { client.accessory(accessory, false) }, enabled = enabled) {
            Text(stringResource(R.string.common_close))
        }
    }
}
