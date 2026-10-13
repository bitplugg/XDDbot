package xdbot.bitplugg.ru.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xdbot.bitplugg.ru.data.BleKey
import xdbot.bitplugg.ru.data.KeyFile
import xdbot.bitplugg.ru.data.KeyRepository

@Composable
fun SettingsScreen(
    repository: KeyRepository,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var revision by remember { mutableIntStateOf(0) }
    val keys = remember(revision) { repository.keys() }
    val activeId = remember(revision) { repository.activeKey()?.id }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BleKey?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var queueOpen by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = readText(context, uri)
        if (text == null) {
            status = "Не удалось прочитать файл"
            return@rememberLauncherForActivityResult
        }
        val result = KeyFile.parse(text)
        result.keys.forEach { repository.add(it.label, it.code, it.deviceType) }
        if (result.keys.isNotEmpty() && repository.activeKey() == null) {
            repository.keys().firstOrNull()?.let { repository.setActive(it.id) }
        }
        revision++
        status = buildString {
            append("Импортировано: ${result.keys.size}")
            if (result.skipped > 0) append(" · пропущено: ${result.skipped}")
        }
        if (result.keys.isNotEmpty()) queueOpen = true
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Text("Ключи устройства", style = MaterialTheme.typography.titleLarge) }
        item {
            Text(
                "Укажите ключ только для своего самоката. XDDBOT не генерирует и не подбирает ключи.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { editing = null; showEditor = true }) {
                    Text("Добавить ключ")
                }
                OutlinedButton(onClick = {
                    status = null
                    importLauncher.launch(arrayOf("*/*"))
                }) { Text("Загрузить файл") }
            }
        }
        item {
            OutlinedButton(
                onClick = { queueOpen = true },
                enabled = keys.isNotEmpty(),
            ) { Text("Очередь кодов (${keys.size})") }
        }
        status?.let { message ->
            item {
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        items(keys, key = { it.id }) { key ->
            ListItem(
                headlineContent = { Text(key.label.ifBlank { "Без названия" }) },
                supportingContent = {
                    Text(
                        "${key.code} · type ${key.deviceType} · ${key.keyBytes.size} байт",
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
            onSave = { label, code, deviceType ->
                val current = editing
                if (current == null) {
                    val created = repository.add(label, code, deviceType)
                    repository.setActive(created.id)
                } else {
                    repository.upsert(current.copy(label = label, code = code, deviceType = deviceType))
                }
                revision++
                showEditor = false
            },
        )
    }

    if (queueOpen && keys.isNotEmpty()) {
        KeyQueueDialog(
            keys = keys,
            activeId = activeId,
            onSetActive = {
                repository.setActive(it)
                revision++
            },
            onDismiss = { queueOpen = false },
        )
    }
}

@Composable
private fun KeyQueueDialog(
    keys: List<BleKey>,
    activeId: String?,
    onSetActive: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var index by remember { mutableIntStateOf(0) }
    val current = keys.getOrNull(index.coerceIn(0, keys.lastIndex)) ?: return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Очередь кодов")
                Text(
                    "${index + 1} / ${keys.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    current.label.ifBlank { "Без названия" },
                    style = MaterialTheme.typography.titleMedium,
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        current.code,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AssistChip(onClick = {}, label = { Text("type ${current.deviceType}") })
                    AssistChip(onClick = {}, label = { Text("${current.keyBytes.size} байт") })
                    if (current.id == activeId) {
                        AssistChip(onClick = {}, label = { Text("активный") })
                    }
                }
                HorizontalDivider()
                Text(
                    "Выберите код вручную и нажмите «Сделать активным». «Далее» просто переключает код — автоматического перебора нет.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { if (index > 0) index-- },
                    enabled = index > 0,
                ) { Text("Назад") }
                TextButton(
                    onClick = { if (index < keys.lastIndex) index++ },
                    enabled = index < keys.lastIndex,
                ) { Text("Далее") }
                TextButton(onClick = {
                    onSetActive(current.id)
                    onDismiss()
                }) { Text("Сделать активным") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}

@Composable
private fun KeyEditorDialog(
    initial: BleKey?,
    onDismiss: () -> Unit,
    onSave: (label: String, code: String, deviceType: Int) -> Unit,
) {
    var label by remember { mutableStateOf(initial?.label.orEmpty()) }
    var keyText by remember { mutableStateOf(initial?.code.orEmpty()) }
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
                    label = { Text("Код или ключ") },
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

private fun readText(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.openInputStream(uri)?.use { stream ->
        val bytes = stream.readBytes()
        if (bytes.size > MAX_FILE_BYTES) null else bytes.toString(Charsets.UTF_8)
    }
}.getOrNull()

private const val MAX_FILE_BYTES = 512 * 1024
