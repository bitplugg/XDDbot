package xdbot.bitplugg.ru.ui.logs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import xdbot.bitplugg.ru.ble.LogKind
import xdbot.bitplugg.ru.ble.ScooterBleClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(
    client: ScooterBleClient,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val log by client.log.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(log.size) {
        if (log.isNotEmpty()) listState.animateScrollToItem(log.size - 1)
    }

    Column(
        modifier = modifier.fillMaxSize().padding(contentPadding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Журнал BLE-обмена", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { client.clearLog() }) { Text("Очистить") }
        }

        if (log.isEmpty()) {
            Text(
                "Пока пусто. Подключитесь к устройству, чтобы увидеть кадры.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(log) { entry ->
                val color = when (entry.kind) {
                    LogKind.TX -> MaterialTheme.colorScheme.primary
                    LogKind.RX -> MaterialTheme.colorScheme.tertiary
                    LogKind.ERROR -> MaterialTheme.colorScheme.error
                    LogKind.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    text = "${TIME.format(Date(entry.time))}  ${entry.text}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = color,
                )
            }
        }
    }
}

private val TIME = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
