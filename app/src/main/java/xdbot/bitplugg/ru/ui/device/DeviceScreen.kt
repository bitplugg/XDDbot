package xdbot.bitplugg.ru.ui.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import xdbot.bitplugg.ru.data.KeyRepository

@Composable
fun DeviceScreen(
    repository: KeyRepository,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val active = repository.activeKey()

    Column(
        modifier = modifier.fillMaxSize().padding(contentPadding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Подключённое устройство", style = MaterialTheme.typography.titleLarge)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (active == null) {
                    Text("Ключ не выбран")
                    Text(
                        "Добавьте ключ своего самоката в разделе «Настройки».",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(active.label.ifBlank { "Без названия" }, style = MaterialTheme.typography.titleMedium)
                    Text("type ${active.deviceType}", style = MaterialTheme.typography.bodySmall)
                    Text(active.keyHex, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Text(
            "Статус: не подключено. Скан и обмен появятся после подключения к вашему устройству.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
