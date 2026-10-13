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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xdbot.bitplugg.ru.R
import xdbot.bitplugg.ru.data.BleKey
import xdbot.bitplugg.ru.data.DeviceProfile
import xdbot.bitplugg.ru.data.KeyFile
import xdbot.bitplugg.ru.data.KeyRepository
import xdbot.bitplugg.ru.data.RemoteConfig
import xdbot.bitplugg.ru.data.Telemetry

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
    var telemetryEnabled by remember { mutableStateOf(Telemetry.enabled) }
    var profilesOpen by remember { mutableStateOf(false) }
    var profiles by remember { mutableStateOf<List<DeviceProfile>?>(null) }
    var editorDefaultType by remember { mutableIntStateOf(BleKey.DEFAULT_DEVICE_TYPE) }
    val scope = rememberCoroutineScope()

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = readText(context, uri)
        if (text == null) {
            status = context.getString(R.string.settings_import_read_error)
            return@rememberLauncherForActivityResult
        }
        val result = KeyFile.parse(text)
        result.keys.forEach { repository.add(it.label, it.code, it.deviceType) }
        if (result.keys.isNotEmpty() && repository.activeKey() == null) {
            repository.keys().firstOrNull()?.let { repository.setActive(it.id) }
        }
        revision++
        status = buildString {
            append(context.getString(R.string.settings_imported_fmt, result.keys.size))
            if (result.skipped > 0) {
                append(" · ").append(context.getString(R.string.settings_skipped_fmt, result.skipped))
            }
        }
        if (result.keys.isNotEmpty()) queueOpen = true
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(KeyFile.format(keys).toByteArray(Charsets.UTF_8))
            } ?: error("no output stream")
        }.isSuccess
        status = if (ok) {
            context.getString(R.string.settings_exported_fmt, keys.size)
        } else {
            context.getString(R.string.settings_export_error)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Text(stringResource(R.string.settings_keys_title), style = MaterialTheme.typography.titleLarge) }
        item {
            Text(
                stringResource(R.string.settings_keys_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = {
                    editing = null
                    editorDefaultType = BleKey.DEFAULT_DEVICE_TYPE
                    showEditor = true
                }) {
                    Text(stringResource(R.string.settings_add_key))
                }
                OutlinedButton(onClick = {
                    status = null
                    importLauncher.launch(arrayOf("*/*"))
                }) { Text(stringResource(R.string.settings_import)) }
                OutlinedButton(
                    onClick = {
                        status = null
                        exportLauncher.launch("xddbot-keys.txt")
                    },
                    enabled = keys.isNotEmpty(),
                ) { Text(stringResource(R.string.settings_export)) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { queueOpen = true },
                    enabled = keys.isNotEmpty(),
                ) { Text(stringResource(R.string.settings_queue_fmt, keys.size)) }
                OutlinedButton(onClick = {
                    profiles = null
                    profilesOpen = true
                    scope.launch { profiles = RemoteConfig.fetchProfiles() ?: emptyList() }
                }) { Text(stringResource(R.string.settings_profiles)) }
            }
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
                headlineContent = { Text(key.label.ifBlank { stringResource(R.string.device_unnamed_short) }) },
                supportingContent = {
                    Text(
                        stringResource(R.string.settings_key_summary, key.code, key.deviceType, key.keyBytes.size),
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
                        TextButton(onClick = { editing = key; showEditor = true }) { Text(stringResource(R.string.settings_edit)) }
                        TextButton(onClick = {
                            repository.remove(key.id)
                            revision++
                        }) { Text(stringResource(R.string.settings_delete)) }
                    }
                },
            )
        }

        item { HorizontalDivider(modifier = Modifier.padding(top = 8.dp)) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_telemetry_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.settings_telemetry_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = telemetryEnabled,
                    enabled = Telemetry.available,
                    onCheckedChange = {
                        telemetryEnabled = it
                        Telemetry.enabled = it
                    },
                )
            }
        }
        if (!Telemetry.available) {
            item {
                Text(
                    stringResource(R.string.settings_telemetry_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Text(
                text = stringResource(R.string.settings_footer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 8.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (profilesOpen) {
        ProfilesDialog(
            profiles = profiles,
            onCreate = { profile ->
                editorDefaultType = profile.deviceType
                editing = null
                profilesOpen = false
                showEditor = true
            },
            onDismiss = { profilesOpen = false },
        )
    }

    if (showEditor) {
        KeyEditorDialog(
            initial = editing,
            defaultDeviceType = editorDefaultType,
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
                Text(stringResource(R.string.queue_title))
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
                    current.label.ifBlank { stringResource(R.string.device_unnamed_short) },
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
                    AssistChip(onClick = {}, label = { Text(stringResource(R.string.device_type_fmt, current.deviceType)) })
                    AssistChip(onClick = {}, label = { Text(stringResource(R.string.queue_bytes_fmt, current.keyBytes.size)) })
                    if (current.id == activeId) {
                        AssistChip(onClick = {}, label = { Text(stringResource(R.string.queue_active)) })
                    }
                }
                HorizontalDivider()
                Text(
                    stringResource(R.string.queue_hint),
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
                ) { Text(stringResource(R.string.queue_prev)) }
                TextButton(
                    onClick = { if (index < keys.lastIndex) index++ },
                    enabled = index < keys.lastIndex,
                ) { Text(stringResource(R.string.queue_next)) }
                TextButton(onClick = {
                    onSetActive(current.id)
                    onDismiss()
                }) { Text(stringResource(R.string.queue_set_active)) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) } },
    )
}

@Composable
private fun ProfilesDialog(
    profiles: List<DeviceProfile>?,
    onCreate: (DeviceProfile) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profiles_title)) },
        text = {
            when {
                profiles == null -> Text(stringResource(R.string.common_loading))
                profiles.isEmpty() -> Text(stringResource(R.string.profiles_error))
                else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    profiles.forEach { profile ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(profile.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(R.string.profiles_meta, profile.deviceType, profile.transferType),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (profile.notes.isNotBlank()) {
                                Text(
                                    profile.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { onCreate(profile) }) { Text(stringResource(R.string.profiles_create)) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) } },
    )
}

@Composable
private fun KeyEditorDialog(
    initial: BleKey?,
    defaultDeviceType: Int,
    onDismiss: () -> Unit,
    onSave: (label: String, code: String, deviceType: Int) -> Unit,
) {
    val context = LocalContext.current
    var label by remember { mutableStateOf(initial?.label.orEmpty()) }
    var keyText by remember { mutableStateOf(initial?.code.orEmpty()) }
    var deviceTypeText by remember { mutableStateOf((initial?.deviceType ?: defaultDeviceType).toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.key_new else R.string.key_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.key_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = keyText,
                    onValueChange = {
                        keyText = it
                        error = null
                    },
                    label = { Text(stringResource(R.string.key_code)) },
                    placeholder = { Text(stringResource(R.string.key_code_placeholder)) },
                    singleLine = true,
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = deviceTypeText,
                    onValueChange = { deviceTypeText = it.filter(Char::isDigit).take(3) },
                    label = { Text(stringResource(R.string.key_device_type)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val deviceType = deviceTypeText.toIntOrNull()
                if (deviceType == null) {
                    error = context.getString(R.string.key_error_number)
                    return@TextButton
                }
                BleKey.parse(keyText, deviceType)
                    .onSuccess { code -> onSave(label.trim(), code, deviceType) }
                    .onFailure { error = it.message }
            }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

private fun readText(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.openInputStream(uri)?.use { stream ->
        val bytes = stream.readBytes()
        if (bytes.size > MAX_FILE_BYTES) null else bytes.toString(Charsets.UTF_8)
    }
}.getOrNull()

private const val MAX_FILE_BYTES = 512 * 1024
