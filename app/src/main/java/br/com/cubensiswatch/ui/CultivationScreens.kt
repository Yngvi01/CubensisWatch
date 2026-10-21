package br.com.cubensiswatch.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.cubensiswatch.data.Cultivation
import br.com.cubensiswatch.data.CultivationEvent
import br.com.cubensiswatch.data.Culture
import br.com.cubensiswatch.data.Lineage
import br.com.cubensiswatch.data.Photo
import br.com.cubensiswatch.data.Incubator
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val cultivationStatuses = listOf("Planejado", "Preparação", "Incubação", "Colonização", "Frutificação", "Colheita", "Finalizado", "Contaminado", "Descartado")
private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm").withZone(ZoneId.systemDefault())
private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault())

@Composable
fun CultivationListScreen(viewModel: CultivationViewModel, onCreate: () -> Unit, onOpen: (Long) -> Unit) {
    val cultivations by viewModel.cultivations.collectAsStateWithLifecycle(initialValue = emptyList())
    val error by viewModel.error.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cultivos", style = MaterialTheme.typography.headlineMedium)
                Button(onClick = onCreate) { Text("＋ Novo") }
            }
        }
        error?.let { message -> item { ErrorMessage(message) { viewModel.clearError() } } }
        if (cultivations.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Nenhum cultivo registrado", style = MaterialTheme.typography.titleMedium)
                        Text("Crie um cultivo para começar a acompanhar sua timeline.", modifier = Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(cultivations, key = { it.id }) { cultivation ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(cultivation.id) }) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(cultivation.name, style = MaterialTheme.typography.titleLarge)
                        Text("${cultivation.code} · ${cultivation.species.ifBlank { "Espécie não informada" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(cultivation.status, color = MaterialTheme.colorScheme.primary)
                            Text(dateFormatter.format(Instant.ofEpochMilli(cultivation.startedAt)), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CultivationEditorScreen(
    initial: Cultivation?,
    lineages: List<Lineage>,
    cultures: List<Culture>,
    incubators: List<Incubator>,
    initialLineageId: Long? = null,
    initialSourceCultureId: Long? = null,
    viewModel: CultivationViewModel,
    onSaved: (Long) -> Unit,
    onCancel: () -> Unit,
) {
    var code by remember(initial?.id) { mutableStateOf(initial?.code.orEmpty()) }
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var species by remember(initial?.id) { mutableStateOf(initial?.species.orEmpty()) }
    var variety by remember(initial?.id) { mutableStateOf(initial?.variety.orEmpty()) }
    var lineage by remember(initial?.id) { mutableStateOf(initial?.lineage.orEmpty()) }
    var lineageId by remember(initial?.id) { mutableStateOf(initial?.lineageId ?: initialLineageId) }
    var sourceCultureId by remember(initial?.id) { mutableStateOf(initial?.sourceCultureId ?: initialSourceCultureId) }
    var method by remember(initial?.id) { mutableStateOf(initial?.method.orEmpty()) }
    var origin by remember(initial?.id) { mutableStateOf(initial?.origin.orEmpty()) }
    var incubator by remember(initial?.id) { mutableStateOf(initial?.incubator.orEmpty()) }
    var incubatorId by remember(initial?.id) { mutableStateOf(initial?.incubatorId) }
    var notes by remember(initial?.id) { mutableStateOf(initial?.notes.orEmpty()) }
    var status by remember(initial?.id) { mutableStateOf(initial?.status ?: "Planejado") }
    var startedAt by remember(initial?.id) { mutableLongStateOf(initial?.startedAt ?: System.currentTimeMillis()) }
    var datePickerOpen by remember { mutableStateOf(false) }
    val error by viewModel.error.collectAsStateWithLifecycle()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(if (initial == null) "Novo cultivo" else "Editar cultivo", style = MaterialTheme.typography.headlineMedium) }
        if (error != null) item { ErrorMessage(error!!) { viewModel.clearError() } }
        item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome *") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Código (gerado se vazio)") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(value = species, onValueChange = { species = it }, label = { Text("Espécie") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(value = variety, onValueChange = { variety = it }, label = { Text("Variedade") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        if (lineages.isNotEmpty()) {
            item {
                RecordSelector("Linhagem", lineages, lineageId, { it.name }, onSelect = { selected ->
                    lineageId = selected
                    lineage = lineages.firstOrNull { it.id == selected }?.name.orEmpty()
                    if (cultures.none { it.id == sourceCultureId && it.lineageId == selected }) sourceCultureId = null
                })
            }
            item {
                val sources = cultures.filter { it.lineageId == lineageId }
                RecordSelector("Cultura de origem", sources, sourceCultureId, { "${it.code} · ${it.name}" }, nullable = true) { sourceCultureId = it }
            }
        } else item { OutlinedTextField(value = lineage, onValueChange = { lineage = it }, label = { Text("Linhagem") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { StatusSelector(status) { status = it } }
        item { OutlinedButton(onClick = { datePickerOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("Início: ${dateFormatter.format(Instant.ofEpochMilli(startedAt))}") } }
        item { OutlinedTextField(value = method, onValueChange = { method = it }, label = { Text("Método / substrato") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = origin, onValueChange = { origin = it }, label = { Text("Origem") }, modifier = Modifier.fillMaxWidth()) }
        if (incubators.isNotEmpty()) item { IncubatorSelector(incubators, incubatorId) { incubatorId = it } }
        item { OutlinedTextField(value = incubator, onValueChange = { incubator = it }, label = { Text("Outro local (opcional)") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Observações iniciais") }, modifier = Modifier.fillMaxWidth(), minLines = 3) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                Button(
                    onClick = {
                        viewModel.save(
                            Cultivation(
                                id = initial?.id ?: 0, code = code, name = name, species = species,
                                variety = variety, lineage = lineage, status = status, startedAt = startedAt,
                                method = method, origin = origin, incubator = incubator, notes = notes,
                                lineageId = lineageId, sourceCultureId = sourceCultureId,
                                incubatorId = incubatorId,
                                completedAt = initial?.completedAt,
                            ), onSaved,
                        )
                    }, modifier = Modifier.weight(1f),
                ) { Text("Salvar") }
            }
        }
    }

    if (datePickerOpen) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = startedAt)
        DatePickerDialog(
            onDismissRequest = { datePickerOpen = false },
            confirmButton = {
                TextButton(onClick = { picker.selectedDateMillis?.let { startedAt = it }; datePickerOpen = false }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { datePickerOpen = false }) { Text("Cancelar") } },
        ) { DatePicker(state = picker) }
    }
}

@Composable
fun CultivationDetailScreen(
    id: Long,
    viewModel: CultivationViewModel,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
) {
    val cultivation by viewModel.cultivation(id).collectAsStateWithLifecycle(initialValue = null)
    val events by viewModel.events(id).collectAsStateWithLifecycle(initialValue = emptyList())
    val photos by viewModel.photos(id).collectAsStateWithLifecycle(initialValue = emptyList())
    val error by viewModel.error.collectAsStateWithLifecycle()
    var observationOpen by remember { mutableStateOf(false) }
    var deleteConfirmOpen by remember { mutableStateOf(false) }
    var photoCaption by remember { mutableStateOf("") }
    var pendingDeletePhoto by remember { mutableStateOf<Photo?>(null) }
    var captureFile by remember { mutableStateOf<File?>(null) }
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { selected: List<Uri> ->
        if (selected.isNotEmpty()) viewModel.addPhotos(id, selected, photoCaption)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val file = captureFile
        captureFile = null
        if (file != null) {
            if (saved) viewModel.addCapturedPhoto(id, file, photoCaption) else file.delete()
        }
    }

    if (cultivation == null) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("Carregando cultivo…")
        }
        return
    }
    val item = cultivation!!
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.headlineMedium)
                    Text("${item.code} · ${item.species.ifBlank { "Espécie não informada" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onEdit) { Text("Editar") }
            }
        }
        if (error != null) item { ErrorMessage(error!!) { viewModel.clearError() } }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("${item.status} · início ${dateFormatter.format(Instant.ofEpochMilli(item.startedAt))}", style = MaterialTheme.typography.titleMedium)
                    item.completedAt?.let { Text("Finalizado em ${dateFormatter.format(Instant.ofEpochMilli(it))}") }
                    if (item.variety.isNotBlank()) Text("Variedade: ${item.variety}")
                    if (item.lineage.isNotBlank()) Text("Linhagem: ${item.lineage}")
                    if (item.method.isNotBlank()) Text("Método / substrato: ${item.method}")
                    if (item.origin.isNotBlank()) Text("Origem: ${item.origin}")
                    if (item.incubator.isNotBlank()) Text("Incubadora: ${item.incubator}")
                    if (item.notes.isNotBlank()) Text(item.notes, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    var statusMenu by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { statusMenu = true }) { Text("Mudar etapa") }
                        DropdownMenu(expanded = statusMenu, onDismissRequest = { statusMenu = false }) {
                            cultivationStatuses.forEach { status ->
                                DropdownMenuItem(text = { Text(status) }, onClick = { statusMenu = false; if (status != item.status) viewModel.changeStatus(id, status) })
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { observationOpen = true }, modifier = Modifier.weight(1f)) { Text("＋ Observação") }
                OutlinedButton(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.weight(1f)) { Text("＋ Fotos") }
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    viewModel.createCaptureFile { file ->
                        if (file != null) {
                            captureFile = file
                            runCatching {
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                camera.launch(uri)
                            }.onFailure {
                                file.delete()
                                captureFile = null
                                viewModel.reportError("Não foi possível abrir a câmera neste dispositivo.")
                            }
                        }
                    }
                }, modifier = Modifier.fillMaxWidth(),
            ) { Text("＋ Tirar foto") }
        }
        item { OutlinedTextField(value = photoCaption, onValueChange = { photoCaption = it }, label = { Text("Legenda das próximas fotos (opcional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        if (photos.isNotEmpty()) {
            item { Text("Fotos", style = MaterialTheme.typography.titleLarge) }
            items(photos, key = { "photo-${it.id}" }) { photo ->
                PhotoCard(photo = photo, onDelete = { pendingDeletePhoto = photo })
            }
        }
        item { Text("Timeline", style = MaterialTheme.typography.titleLarge) }
        if (events.isEmpty()) item { Text("Ainda não há eventos. Registre uma observação ou uma foto para começar.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(events, key = { "event-${it.id}" }) { event -> TimelineEventCard(event) }
        item {
            OutlinedButton(onClick = { deleteConfirmOpen = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text("Excluir cultivo")
            }
        }
    }

    if (observationOpen) ObservationDialog(
        onDismiss = { observationOpen = false },
        onSave = { text, temperature, humidity -> viewModel.addObservation(id, text, temperature, humidity); observationOpen = false },
    )
    if (deleteConfirmOpen) AlertDialog(
        onDismissRequest = { deleteConfirmOpen = false },
        title = { Text("Excluir cultivo?") },
        text = { Text("O cultivo, sua timeline e suas fotos serão excluídos do dispositivo.") },
        confirmButton = { TextButton(onClick = { deleteConfirmOpen = false; viewModel.delete(item, onDeleted) }) { Text("Excluir", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { deleteConfirmOpen = false }) { Text("Cancelar") } },
    )
    pendingDeletePhoto?.let { photo ->
        AlertDialog(
            onDismissRequest = { pendingDeletePhoto = null }, title = { Text("Excluir foto?") },
            text = { Text("O arquivo local também será removido.") },
            confirmButton = { TextButton(onClick = { pendingDeletePhoto = null; viewModel.deletePhoto(photo) }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { pendingDeletePhoto = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun StatusSelector(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("Status: $selected") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            cultivationStatuses.forEach { status -> DropdownMenuItem(text = { Text(status) }, onClick = { onSelect(status); expanded = false }) }
        }
    }
}

@Composable
private fun RecordSelector(
    label: String,
    options: List<Lineage>,
    selectedId: Long?,
    labelFor: (Lineage) -> String,
    onSelect: (Long?) -> Unit,
    nullable: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.id == selectedId }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label: ${selected?.let(labelFor) ?: if (nullable) "Nenhuma" else "Selecionar"}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (nullable) DropdownMenuItem(text = { Text("Nenhuma") }, onClick = { onSelect(null); expanded = false })
            options.forEach { option -> DropdownMenuItem(text = { Text(labelFor(option)) }, onClick = { onSelect(option.id); expanded = false }) }
        }
    }
}

@Composable
private fun RecordSelector(
    label: String,
    options: List<Culture>,
    selectedId: Long?,
    labelFor: (Culture) -> String,
    nullable: Boolean = false,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.id == selectedId }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label: ${selected?.let(labelFor) ?: if (nullable) "Nenhuma" else "Selecionar"}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (nullable) DropdownMenuItem(text = { Text("Nenhuma") }, onClick = { onSelect(null); expanded = false })
            options.forEach { option -> DropdownMenuItem(text = { Text(labelFor(option)) }, onClick = { onSelect(option.id); expanded = false }) }
        }
    }
}

@Composable
private fun TimelineEventCard(event: CultivationEvent) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(event.type, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(dateTimeFormatter.format(Instant.ofEpochMilli(event.occurredAt)), style = MaterialTheme.typography.bodySmall)
            }
            Text(event.description)
            val environment = listOfNotNull(event.temperature?.let { "${it} °C" }, event.humidity?.let { "${it}% UR" }).joinToString(" · ")
            if (environment.isNotBlank()) Text(environment, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PhotoCard(photo: Photo, onDelete: () -> Unit) =
    LocalPhotoCard(photo.localPath, photo.caption, photo.capturedAt, onDelete)

@Composable
fun LocalPhotoCard(localPath: String, caption: String, capturedAt: Long, onDelete: () -> Unit) {
    val bitmap by produceState<ImageBitmap?>(null, localPath) {
        value = withContext(Dispatchers.IO) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(localPath, bounds)
            var sample = 1
            while (bounds.outWidth / sample > 1200 || bounds.outHeight / sample > 1200) sample *= 2
            BitmapFactory.decodeFile(localPath, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
        }
    }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column {
            bitmap?.let { Image(it, contentDescription = caption.ifBlank { "Foto do registro" }, modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp), contentScale = ContentScale.Crop) }
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(caption.ifBlank { "Foto" }, style = MaterialTheme.typography.titleSmall)
                    Text(dateTimeFormatter.format(Instant.ofEpochMilli(capturedAt)), style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onDelete) { Text("Excluir") }
            }
        }
    }
}

@Composable
private fun ObservationDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("") }
    var humidity by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova observação") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Observação *") }, minLines = 3)
                OutlinedTextField(value = temperature, onValueChange = { temperature = it }, label = { Text("Temperatura °C (opcional)") }, singleLine = true)
                OutlinedTextField(value = humidity, onValueChange = { humidity = it }, label = { Text("Umidade % (opcional)") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text, temperature, humidity) }, enabled = text.isNotBlank()) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun ErrorMessage(message: String, onDismiss: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(message, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    }
}
