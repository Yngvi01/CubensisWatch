package br.com.cubensiswatch.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.cubensiswatch.data.Reminder
import br.com.cubensiswatch.notifications.LocalNotifications
import java.text.DateFormat
import java.util.Calendar

@Composable
fun ReminderScreen(viewModel: ReminderViewModel) {
    val context = LocalContext.current
    val reminders by viewModel.reminders.collectAsStateWithLifecycle(initialValue = emptyList())
    val error by viewModel.error.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Reminder?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Reminder?>(null) }
    var permissionGranted by remember { mutableStateOf(LocalNotifications.hasPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted = it }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Lembretes e notificações", style = MaterialTheme.typography.headlineMedium) }
        item {
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Alertas locais", style = MaterialTheme.typography.titleMedium)
                Text(if (permissionGranted) "Notificações ativadas. Os alertas de estoque são verificados diariamente." else "Ative as notificações para receber lembretes e alertas de estoque.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!permissionGranted && Build.VERSION.SDK_INT >= 33) Button(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Ativar notificações") }
            } }
        }
        if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(4.dp)) }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Seus lembretes", style = MaterialTheme.typography.titleLarge); Button(onClick = { editing = null; creating = true }) { Text("＋ Novo") } } }
        if (reminders.isEmpty()) item { Text("Nenhum lembrete. Crie um para agendar verificações ou tarefas.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(reminders, key = { it.id }) { reminder ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(reminder.title, style = MaterialTheme.typography.titleMedium)
                        Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(reminder.dueAt), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = reminder.enabled, onCheckedChange = { viewModel.setEnabled(reminder, it) })
                }
                if (reminder.description.isNotBlank()) Text(reminder.description)
                Text(reminder.repeatIntervalMinutes?.let { "Repete a cada ${formatInterval(it)}" } ?: "Sem repetição", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row { TextButton(onClick = { editing = reminder; creating = true }) { Text("Editar") }; TextButton(onClick = { deleteTarget = reminder }) { Text("Excluir") } }
            } }
        }
    }
    if (creating) ReminderEditorDialog(editing, onDismiss = { creating = false; editing = null }) { reminder -> viewModel.save(reminder) { creating = false; editing = null } }
    deleteTarget?.let { reminder -> AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("Excluir lembrete?") }, text = { Text("O agendamento deste lembrete também será cancelado.") }, confirmButton = { TextButton(onClick = { viewModel.delete(reminder); deleteTarget = null }) { Text("Excluir") } }, dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") } }) }
}

@Composable
private fun ReminderEditorDialog(initial: Reminder?, onDismiss: () -> Unit, onSave: (Reminder) -> Unit) {
    val context = LocalContext.current
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var description by remember(initial?.id) { mutableStateOf(initial?.description.orEmpty()) }
    var dueAt by remember(initial?.id) { mutableLongStateOf(initial?.dueAt ?: System.currentTimeMillis() + 60 * 60 * 1000L) }
    var repeat by remember(initial?.id) { mutableStateOf(initial?.repeatIntervalMinutes?.toString() ?: "") }
    var enabled by remember(initial?.id) { mutableStateOf(initial?.enabled ?: true) }
    var repeatOpen by remember { mutableStateOf(false) }
    val intervals = listOf("Sem repetição" to "", "A cada 15 minutos" to "15", "A cada hora" to "60", "A cada 6 horas" to "360", "Diariamente" to "1440", "Semanalmente" to "10080", "Personalizado…" to "custom")
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (initial == null) "Novo lembrete" else "Editar lembrete") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("Título *") }, singleLine = true)
            OutlinedTextField(description, { description = it }, label = { Text("Descrição") }, minLines = 2)
            OutlinedButton(onClick = {
                val calendar = Calendar.getInstance().apply { timeInMillis = dueAt }
                DatePickerDialog(context, { _, year, month, day ->
                    calendar.set(year, month, day)
                    TimePickerDialog(context, { _, hour, minute -> calendar.set(Calendar.HOUR_OF_DAY, hour); calendar.set(Calendar.MINUTE, minute); calendar.set(Calendar.SECOND, 0); dueAt = calendar.timeInMillis }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
            }, modifier = Modifier.fillMaxWidth()) { Text("Data e hora: ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(dueAt)}") }
            Box { OutlinedButton(onClick = { repeatOpen = true }, modifier = Modifier.fillMaxWidth()) { Text(intervals.firstOrNull { it.second == repeat }?.first ?: "Intervalo: ${repeat} min") }; DropdownMenu(repeatOpen, { repeatOpen = false }) { intervals.forEach { (label, value) -> DropdownMenuItem(text = { Text(label) }, onClick = { repeat = value; repeatOpen = false }) } } }
            if (repeat == "custom" || repeat.toLongOrNull() == null && repeat.isNotBlank()) OutlinedTextField(if (repeat == "custom") "" else repeat, { repeat = it }, label = { Text("Intervalo em minutos (mínimo 15)") }, singleLine = true)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Ativado"); Switch(enabled, { enabled = it }) }
        }
    }, confirmButton = { TextButton(onClick = {
        val repeatMinutes = repeat.takeIf { it.isNotBlank() && it != "custom" }?.toLongOrNull()
        if (repeat == "custom") return@TextButton
        if (repeat.isNotBlank() && repeatMinutes == null) return@TextButton
        onSave(Reminder(initial?.id ?: 0, title, description, dueAt, repeatMinutes, enabled))
    }) { Text("Salvar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

private fun formatInterval(minutes: Long): String = when {
    minutes % 1440L == 0L -> "${minutes / 1440L} dia(s)"
    minutes % 60L == 0L -> "${minutes / 60L} hora(s)"
    else -> "$minutes minutos"
}
