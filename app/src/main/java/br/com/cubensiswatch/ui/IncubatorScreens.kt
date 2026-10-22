package br.com.cubensiswatch.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.cubensiswatch.data.Incubator
import br.com.cubensiswatch.data.IncubatorReading

@Composable
fun IncubatorSelector(items: List<Incubator>, selectedId: Long?, onSelect: (Long?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = items.firstOrNull { it.id == selectedId }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("Incubadora: ${selected?.name ?: "Nenhuma"}") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Nenhuma") }, onClick = { onSelect(null); expanded = false })
            items.forEach { item -> DropdownMenuItem(text = { Text(item.name) }, onClick = { onSelect(item.id); expanded = false }) }
        }
    }
}

@Composable
fun IncubatorListScreen(vm: IncubatorViewModel, onOpen: (Long) -> Unit, onCreate: () -> Unit) {
    val items by vm.incubators.collectAsStateWithLifecycle(initialValue = emptyList())
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Incubadoras", style = MaterialTheme.typography.headlineMedium); Button(onClick = onCreate) { Text("＋ Nova") } } }
        if (items.isEmpty()) item { Text("Cadastre incubadoras ou locais para organizar culturas e cultivos.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(items, key = { it.id }) { incubator ->
            Card(Modifier.fillMaxWidth().clickable { onOpen(incubator.id) }) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(incubator.name, style = MaterialTheme.typography.titleLarge)
                Text(listOf(incubator.code, incubator.location).filter(String::isNotBlank).joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${incubator.temperature?.let { "$it °C" } ?: "Temperatura —"} · ${incubator.humidity?.let { "$it% UR" } ?: "Umidade —"}", style = MaterialTheme.typography.bodySmall)
            } }
        }
    }
}

@Composable
fun IncubatorEditorScreen(initial: Incubator?, vm: IncubatorViewModel, onSaved: (Long) -> Unit, onCancel: () -> Unit) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var code by remember(initial?.id) { mutableStateOf(initial?.code.orEmpty()) }
    var location by remember(initial?.id) { mutableStateOf(initial?.location.orEmpty()) }
    var description by remember(initial?.id) { mutableStateOf(initial?.description.orEmpty()) }
    var temperature by remember(initial?.id) { mutableStateOf(initial?.temperature?.toString().orEmpty()) }
    var humidity by remember(initial?.id) { mutableStateOf(initial?.humidity?.toString().orEmpty()) }
    var notes by remember(initial?.id) { mutableStateOf(initial?.notes.orEmpty()) }
    val error by vm.error.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(if (initial == null) "Nova incubadora" else "Editar incubadora", style = MaterialTheme.typography.headlineMedium) }
        if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error) }
        item { OutlinedTextField(name, { name = it }, label = { Text("Nome *") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(code, { code = it }, label = { Text("Código") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(location, { location = it }, label = { Text("Local") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(description, { description = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
        item { OutlinedTextField(temperature, { temperature = it }, label = { Text("Temperatura atual (°C)") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(humidity, { humidity = it }, label = { Text("Umidade atual (%)") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(notes, { notes = it }, label = { Text("Observações") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancelar") }
            Button(onClick = {
                val temp = temperature.trim().replace(',', '.').takeIf(String::isNotEmpty)?.toDoubleOrNull()
                val hum = humidity.trim().replace(',', '.').takeIf(String::isNotEmpty)?.toDoubleOrNull()
                if (temperature.isNotBlank() && temp == null || humidity.isNotBlank() && hum == null) vm.reportError("Temperatura e umidade devem ser números válidos.")
                else vm.save(Incubator(initial?.id ?: 0, name, code, location, description, temp, hum, notes), onSaved)
            }, modifier = Modifier.weight(1f)) { Text("Salvar") }
        } }
    }
}

@Composable
fun IncubatorDetailScreen(id: Long, vm: IncubatorViewModel, onEdit: () -> Unit) {
    val item by vm.incubator(id).collectAsStateWithLifecycle(initialValue = null)
    val readings by vm.readings(id).collectAsStateWithLifecycle(initialValue = emptyList())
    val error by vm.error.collectAsStateWithLifecycle()
    var temperature by remember { mutableStateOf("") }
    var humidity by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    if (item == null) { Text("Carregando incubadora…", Modifier.padding(24.dp)); return }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) {
            Text(item!!.name, style = MaterialTheme.typography.headlineMedium)
            Text(listOf(item!!.code, item!!.location).filter(String::isNotBlank).joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(item!!.description)
            Text("Registro atual: ${item!!.temperature?.let { "$it °C" } ?: "—"} · ${item!!.humidity?.let { "$it% UR" } ?: "—"}")
        }; TextButton(onClick = onEdit) { Text("Editar") } } }
        item { Text("Novo registro ambiental", style = MaterialTheme.typography.titleLarge) }
        if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error) }
        item { OutlinedTextField(temperature, { temperature = it }, label = { Text("Temperatura (°C)") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(humidity, { humidity = it }, label = { Text("Umidade (%)") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(notes, { notes = it }, label = { Text("Observação") }, modifier = Modifier.fillMaxWidth()) }
        item { Button(onClick = {
            val temp = temperature.trim().replace(',', '.').takeIf(String::isNotEmpty)?.toDoubleOrNull()
            val hum = humidity.trim().replace(',', '.').takeIf(String::isNotEmpty)?.toDoubleOrNull()
            if (temperature.isNotBlank() && temp == null || humidity.isNotBlank() && hum == null) vm.reportError("Temperatura e umidade devem ser números válidos.")
            else { vm.addReading(IncubatorReading(incubatorId = id, temperature = temp, humidity = hum, notes = notes)); temperature = ""; humidity = ""; notes = "" }
        }, modifier = Modifier.fillMaxWidth()) { Text("Registrar") } }
        item { Text("Histórico ambiental", style = MaterialTheme.typography.titleLarge) }
        if (readings.isEmpty()) item { Text("Nenhum registro ambiental ainda.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(readings, key = { it.id }) { reading -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) {
            Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(reading.recordedAt)), style = MaterialTheme.typography.labelMedium)
            Text("${reading.temperature?.let { "$it °C" } ?: "—"} · ${reading.humidity?.let { "$it% UR" } ?: "—"}")
            if (reading.notes.isNotBlank()) Text(reading.notes)
        } } }
    }
}
