package xdbot.bitplugg.ru.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import xdbot.bitplugg.ru.data.BleKey
import xdbot.bitplugg.ru.data.KeyRepository

@Composable
fun SettingsScreen(
    repository: KeyRepository,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var revision by remember { mutableIntStateOf(0) }
    val keys = remember(revision) { repository.keys() }
    val activeId = remember(revision) { repository.activeKey()?.id }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BleKey?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                "Ключи устройства",
                style = MaterialTheme.typography.titleLarge,
            )
        }
        item {
            Text(
                "Укажите ключ только для своего самоката. XDDBOT не генерирует и не подбирает ключи.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            FilledTonalButton(onClick = { editing = null; showEditor = true }) {
                Text("Добавить ключ")
            }
        }
        items(keys, key = { it.id }) { key ->
            ListItem(
                headlineContent = { Text(key.label.ifBlank { "Без названия" }) },
                supportingContent = {
                    Text(
                        "${key.keyHex} · type ${key.deviceType} · ${key.keyBytes.size} байт",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                trailingContent = {
                    Row {
                        RadioButton(
                            selected = key.id == activeId,
                            onClick = {
                                repository.setActive(key.id)
                                revision++
                            },
                        )
                        TextButton(onClick = { editing = key; showEditor = true }) { Text("Изм.") }
                        TextButton(onClick = {
                            repository.remove(key.id)
                            revision++
                        }) { Text("Удал.") }
                    }
                },
            )
        }
    }

    if (showEditor) {
        KeyEditorDialog(
            initial = editing,
            onDismiss = { showEditor = false },
            onSave = { label, hex, deviceType ->
                val current = editing
                if (current == null) {
                    val created = repository.add(label, hex, deviceType)
                    repository.setActive(created.id)
                } else {
                    repository.upsert(current.copy(label = label, keyHex = hex, deviceType = deviceType))
                }
                revision++
                showEditor = false
            },
        )
    }
}

@Composable
private fun KeyEditorDialog(
    initial: BleKey?,
    onDismiss: () -> Unit,
    onSave: (label: String, keyHex: String, deviceType: Int) -> Unit,
) {
    var label by remember { mutableStateOf(initial?.label.orEmpty()) }
    var keyText by remember { mutableStateOf(initial?.keyHex.orEmpty()) }
    var deviceTypeText by remember { mutableStateOf((initial?.deviceType ?: BleKey.DEFAULT_DEVICE_TYPE).toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Новый ключ" else "Изменить ключ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = keyText,
                    onValueChange = {
                        keyText = it
                        error = null
                    },
                    label = { Text("Ключ (hex)") },
                    placeholder = { Text("напр. 1A2B3C4D5E6F") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = deviceTypeText,
                    onValueChange = { deviceTypeText = it.filter(Char::isDigit).take(3) },
                    label = { Text("deviceType (0..255)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val deviceType = deviceTypeText.toIntOrNull()
                if (deviceType == null) {
                    error = "deviceType должен быть числом"
                    return@TextButton
                }
                BleKey.parse(keyText, deviceType)
                    .onSuccess { hex -> onSave(label.trim(), hex, deviceType) }
                    .onFailure { error = it.message }
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
