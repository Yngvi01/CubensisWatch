package br.com.cubensiswatch.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.cubensiswatch.data.Cultivation
import br.com.cubensiswatch.data.CultivationEvent
import br.com.cubensiswatch.data.Culture
import br.com.cubensiswatch.data.Lineage
import br.com.cubensiswatch.data.Photo
import br.com.cubensiswatch.data.Incubator
import br.com.cubensiswatch.data.Harvest
import br.com.cubensiswatch.data.HarvestPhoto
import br.com.cubensiswatch.data.CultivationExperiment
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures

val cultivationStatuses = listOf("Planejado", "Preparação", "Incubação", "Colonização", "Frutificação", "Colheita", "Finalizado", "Contaminado", "Descartado")
private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm").withZone(ZoneId.systemDefault())
private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault())
private data class PendingCultivationPhoto(val uri: Uri? = null, val file: File? = null)

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
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
) {
    val cultivation by viewModel.cultivation(id).collectAsStateWithLifecycle(initialValue = null)
    val events by viewModel.events(id).collectAsStateWithLifecycle(initialValue = emptyList())
    val photos by viewModel.photos(id).collectAsStateWithLifecycle(initialValue = emptyList())
    val harvests by viewModel.harvests(id).collectAsStateWithLifecycle(initialValue = emptyList())
    val experiment by viewModel.experiment(id).collectAsStateWithLifecycle(initialValue = null)
    val similarExperiments by viewModel.similarExperiments(id).collectAsStateWithLifecycle(initialValue = emptyList())
    val allCultivations by viewModel.cultivations.collectAsStateWithLifecycle(initialValue = emptyList())
    val error by viewModel.error.collectAsStateWithLifecycle()
    var observationOpen by remember { mutableStateOf(false) }
    var observationType by remember { mutableStateOf("Observação") }
    var experimentOpen by remember { mutableStateOf(false) }
    var comparisonOpen by remember { mutableStateOf(false) }
    var harvestOpen by remember { mutableStateOf(false) }
    var harvestToDelete by remember { mutableStateOf<Harvest?>(null) }
    var deleteConfirmOpen by remember { mutableStateOf(false) }
    var photoSourceOpen by remember { mutableStateOf(false) }
    var pendingPhotos by remember { mutableStateOf(emptyList<PendingCultivationPhoto>()) }
    var pendingPhotoTotal by remember { mutableIntStateOf(0) }
    var photoDescription by remember { mutableStateOf("") }
    var pendingDeletePhoto by remember { mutableStateOf<Photo?>(null) }
    var pendingDeleteHarvestPhoto by remember { mutableStateOf<HarvestPhoto?>(null) }
    var harvestForPhotoPicker by remember { mutableStateOf<Harvest?>(null) }
    var harvestPhotoSourceTarget by remember { mutableStateOf<Harvest?>(null) }
    var harvestForCamera by remember { mutableStateOf<Harvest?>(null) }
    var captureFile by remember { mutableStateOf<File?>(null) }
    var harvestCaptureFile by remember { mutableStateOf<File?>(null) }
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { selected: List<Uri> ->
        if (selected.isNotEmpty()) {
            pendingPhotos = selected.map { PendingCultivationPhoto(uri = it) }
            pendingPhotoTotal = selected.size
        }
    }
    val harvestPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { selected: List<Uri> ->
        harvestForPhotoPicker?.let { harvest -> if (selected.isNotEmpty()) viewModel.addHarvestPhotos(harvest, selected) }
        harvestForPhotoPicker = null
    }
    val harvestCamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val file = harvestCaptureFile
        val harvest = harvestForCamera
        harvestCaptureFile = null
        harvestForCamera = null
        if (file != null) {
            if (saved && harvest != null) viewModel.addCapturedHarvestPhoto(harvest.id, file) else file.delete()
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val file = captureFile
        captureFile = null
        if (file != null) {
            if (saved) {
                pendingPhotos = listOf(PendingCultivationPhoto(file = file))
                pendingPhotoTotal = 1
            } else file.delete()
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onBack) { Text("← Voltar para todos os cultivos") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.headlineMedium)
                    Text("${item.code} · ${item.species.ifBlank { "Espécie não informada" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onEdit) { Text("Editar") }
                }
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { observationType = "Observação"; observationOpen = true }, modifier = Modifier.weight(1f)) { Text("＋ Observação") }
                    OutlinedButton(onClick = { observationType = "Problema"; observationOpen = true }, modifier = Modifier.weight(1f)) { Text("⚠ Problema") }
                }
                OutlinedButton(onClick = { photoSourceOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("＋ Adicionar fotos") }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Ficha do experimento", style = MaterialTheme.typography.titleLarge)
                            Text(if (experiment == null) "Plano, execução e resultado" else "Atualizada em ${dateTimeFormatter.format(Instant.ofEpochMilli(experiment!!.updatedAt))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { experimentOpen = true }) { Text(if (experiment == null) "Preencher" else "Editar") }
                    }
                    if (experiment == null) Text("Registre o que planejou e compare com o que aconteceu.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else {
                        ExperimentSummary(experiment!!)
                        if (similarExperiments.isNotEmpty()) {
                            HorizontalDivider()
                            Text("Execuções semelhantes", style = MaterialTheme.typography.titleSmall)
                            similarExperiments.forEach { previous ->
                                val previousRun = allCultivations.firstOrNull { it.id == previous.cultivationId }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(previousRun?.let { "${it.name} · ${it.status}" } ?: "Outro cultivo", style = MaterialTheme.typography.labelLarge)
                                    Text(listOfNotNull(previous.result.takeIf(String::isNotBlank), previous.observedConditions.takeIf(String::isNotBlank)).joinToString(" · ").ifBlank { "Ficha sem resultado preenchido" }, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Colheitas", style = MaterialTheme.typography.titleLarge)
                OutlinedButton(onClick = { harvestOpen = true }) { Text("＋ Registrar") }
            }
        }
        if (harvests.isEmpty()) item { Text("Nenhuma colheita registrada.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(harvests, key = { "harvest-${it.id}" }) { harvest ->
            val harvestPhotos by viewModel.harvestPhotos(harvest.id).collectAsStateWithLifecycle(initialValue = emptyList())
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Colheita ${harvest.number} — ${harvest.quantity.prettyQuantity()} ${harvest.unit}", style = MaterialTheme.typography.titleMedium)
                            Text(dateFormatter.format(Instant.ofEpochMilli(harvest.harvestedAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (harvest.notes.isNotBlank()) Text(harvest.notes)
                        }
                        TextButton(onClick = { harvestToDelete = harvest }) { Text("Excluir") }
                    }
                    OutlinedButton(onClick = { harvestPhotoSourceTarget = harvest }) { Text("＋ Fotos da colheita") }
                    harvestPhotos.forEach { photo -> LocalPhotoCard(photo.localPath, photo.caption, photo.capturedAt) { pendingDeleteHarvestPhoto = photo } }
                }
            }
        }
        harvests.groupBy { it.unit }.forEach { (unit, records) ->
            item(key = "harvest-total-$unit") {
                Text("Total: ${records.sumOf { it.quantity }.prettyQuantity()} $unit", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        if (photos.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Fotos · linha do tempo", style = MaterialTheme.typography.titleLarge)
                    if (photos.size >= 2) TextButton(onClick = { comparisonOpen = true }) { Text("Comparar") }
                }
            }
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
        type = observationType,
        stage = item.status,
        error = error,
        onClearError = viewModel::clearError,
        onDismiss = { observationOpen = false },
        onSave = { type, text, temperature, humidity, photoUri ->
            viewModel.addObservation(id, type, text, temperature, humidity, photoUri) { observationOpen = false }
        },
    )
    if (experimentOpen) ExperimentDialog(
        initial = experiment,
        cultivationId = id,
        onDismiss = { experimentOpen = false },
        onSave = { value -> viewModel.saveExperiment(value) { experimentOpen = false } },
    )
    if (comparisonOpen && photos.size >= 2) PhotoComparisonDialog(
        photos = photos.map { ComparisonImage(it.id, it.localPath, it.caption, it.capturedAt) },
        onDismiss = { comparisonOpen = false },
    )
    if (harvestOpen) HarvestDialog(
        onDismiss = { harvestOpen = false },
        onSave = { quantity, unit, harvestedAt, notes -> viewModel.addHarvest(id, quantity, unit, harvestedAt, notes); harvestOpen = false },
    )
    harvestPhotoSourceTarget?.let { harvest ->
        AlertDialog(
            onDismissRequest = { harvestPhotoSourceTarget = null },
            title = { Text("Fotos da colheita ${harvest.number}") },
            text = { Text("Escolha uma imagem da galeria ou tire uma foto agora.") },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        harvestPhotoSourceTarget = null
                        harvestForPhotoPicker = harvest
                        harvestPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("Galeria") }
                    Button(onClick = {
                        harvestPhotoSourceTarget = null
                        viewModel.createCaptureFile { file ->
                            if (file != null) {
                                harvestCaptureFile = file
                                harvestForCamera = harvest
                                runCatching {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    harvestCamera.launch(uri)
                                }.onFailure {
                                    file.delete()
                                    harvestCaptureFile = null
                                    harvestForCamera = null
                                    viewModel.reportError("Não foi possível abrir a câmera neste dispositivo.")
                                }
                            }
                        }
                    }) { Text("Câmera") }
                }
            },
            dismissButton = { TextButton(onClick = { harvestPhotoSourceTarget = null }) { Text("Cancelar") } },
        )
    }
    harvestToDelete?.let { harvest ->
        AlertDialog(
            onDismissRequest = { harvestToDelete = null },
            title = { Text("Excluir colheita?") },
            text = { Text("A colheita ${harvest.number} será removida do histórico.") },
            confirmButton = { TextButton(onClick = { harvestToDelete = null; viewModel.deleteHarvest(harvest) }) { Text("Excluir", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { harvestToDelete = null }) { Text("Cancelar") } },
        )
    }
    pendingDeleteHarvestPhoto?.let { photo ->
        AlertDialog(
            onDismissRequest = { pendingDeleteHarvestPhoto = null },
            title = { Text("Excluir foto da colheita?") },
            text = { Text("O arquivo local também será removido.") },
            confirmButton = { TextButton(onClick = { pendingDeleteHarvestPhoto = null; viewModel.deleteHarvestPhoto(photo) }) { Text("Excluir", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { pendingDeleteHarvestPhoto = null }) { Text("Cancelar") } },
        )
    }
    if (photoSourceOpen) AlertDialog(
        onDismissRequest = { photoSourceOpen = false },
        title = { Text("Adicionar fotos") },
        text = { Text("Escolha como deseja adicionar imagens a este cultivo.") },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    photoSourceOpen = false
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text("Selecionar fotos") }
                Button(onClick = {
                    photoSourceOpen = false
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
                }) { Text("Abrir câmera") }
            }
        },
        dismissButton = { TextButton(onClick = { photoSourceOpen = false }) { Text("Cancelar") } },
    )
    val pendingPhoto = pendingPhotos.firstOrNull()
    if (pendingPhoto != null) PhotoDescriptionDialog(
        index = pendingPhotoTotal - pendingPhotos.size + 1,
        total = pendingPhotoTotal,
        value = photoDescription,
        onValueChange = { photoDescription = it },
        onDismiss = {
            pendingPhotos.forEach { it.file?.delete() }
            pendingPhotos = emptyList()
            pendingPhotoTotal = 0
            photoDescription = ""
        },
        onSave = { caption ->
            val next = {
                pendingPhotos = pendingPhotos.drop(1)
                if (pendingPhotos.size <= 1) pendingPhotoTotal = 0
                photoDescription = ""
            }
            when {
                pendingPhoto.uri != null -> viewModel.addPhotos(id, listOf(pendingPhoto.uri), caption, next)
                pendingPhoto.file != null -> viewModel.addCapturedPhoto(id, pendingPhoto.file, caption, next)
            }
        },
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
            event.stage?.takeIf { it.isNotBlank() }?.let { Text("Etapa: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
    var fullScreen by remember(localPath) { mutableStateOf(false) }
    var photoUnavailable by remember(localPath) { mutableStateOf(false) }
    var zoom by remember(localPath) { mutableFloatStateOf(1f) }
    var panX by remember(localPath) { mutableFloatStateOf(0f) }
    var panY by remember(localPath) { mutableFloatStateOf(0f) }
    val bitmap by produceState<ImageBitmap?>(null, localPath) {
        photoUnavailable = false
        value = withContext(Dispatchers.IO) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(localPath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) null
            else {
                var sample = 1
                while (bounds.outWidth / sample > 1200 || bounds.outHeight / sample > 1200) sample *= 2
                BitmapFactory.decodeFile(localPath, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
            }
        }
        photoUnavailable = value == null
    }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column {
            bitmap?.let {
                Image(
                    it,
                    contentDescription = "Ampliar ${caption.ifBlank { "foto do registro" }}",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).clickable {
                        zoom = 1f
                        panX = 0f
                        panY = 0f
                        fullScreen = true
                    },
                    contentScale = ContentScale.Crop,
                )
            }
            if (bitmap == null && photoUnavailable) {
                Box(
                    Modifier.fillMaxWidth().height(140.dp).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    Text("Foto indisponível", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (bitmap == null) {
                Box(
                    Modifier.fillMaxWidth().height(140.dp).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) { CircularProgressIndicator() }
            }
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(caption.ifBlank { "Foto" }, style = MaterialTheme.typography.titleSmall)
                    Text(dateTimeFormatter.format(Instant.ofEpochMilli(capturedAt)), style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onDelete) { Text("Excluir") }
            }
        }
    }
    if (fullScreen && bitmap != null) Dialog(
        onDismissRequest = { fullScreen = false },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.96f).padding(8.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color.Black,
        ) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(caption.ifBlank { "Foto" }, color = Color.White, style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { fullScreen = false }) { Text("Fechar", color = Color.White) }
                }
                Image(
                    bitmap = bitmap!!,
                    contentDescription = caption.ifBlank { "Foto ampliada" },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(localPath) {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                val updatedZoom = (zoom * gestureZoom).coerceIn(1f, 5f)
                                val zoomFactor = updatedZoom / zoom
                                zoom = updatedZoom
                                if (updatedZoom == 1f) {
                                    panX = 0f
                                    panY = 0f
                                } else {
                                    val maxX = size.width * (updatedZoom - 1f) / 2f
                                    val maxY = size.height * (updatedZoom - 1f) / 2f
                                    panX = (panX * zoomFactor + pan.x).coerceIn(-maxX, maxX)
                                    panY = (panY * zoomFactor + pan.y).coerceIn(-maxY, maxY)
                                }
                            }
                        }
                        .pointerInput(localPath) {
                            detectTapGestures(onDoubleTap = {
                                if (zoom > 1f) {
                                    zoom = 1f
                                    panX = 0f
                                    panY = 0f
                                } else zoom = 2.5f
                            })
                        }
                        .graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            translationX = panX
                            translationY = panY
                        },
                    contentScale = ContentScale.Fit,
                )
                Text("Pinçe para ampliar · arraste para mover · toque duas vezes para redefinir", color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                Text(dateTimeFormatter.format(Instant.ofEpochMilli(capturedAt)), color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun PhotoDescriptionDialog(
    index: Int,
    total: Int,
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar descrição?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (total > 1) "Foto $index de $total. A descrição é opcional." else "A descrição da foto é opcional.")
                OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text("Descrição") }, placeholder = { Text("Ex.: aparência, mudança ou detalhe") }, minLines = 2)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(value) }) { Text("Salvar foto") } },
        dismissButton = {
            Row {
                TextButton(onClick = { onSave("") }) { Text("Continuar sem descrição") }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )
}

@Composable
private fun ObservationDialog(
    type: String,
    stage: String,
    error: String?,
    onClearError: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Uri?) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("") }
    var humidity by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { photoUri = it }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (type == "Problema") "Registrar problema" else "Registro rápido") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${dateTimeFormatter.format(Instant.now())} · Etapa: $stage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Descrição (opcional)") }, minLines = 2)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = temperature, onValueChange = { temperature = it }, label = { Text("Temp. °C") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = humidity, onValueChange = { humidity = it }, label = { Text("Umidade %") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedButton(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (photoUri == null) "＋ Anexar foto" else "Foto anexada · trocar")
                }
                if (photoUri != null) TextButton(onClick = { photoUri = null }) { Text("Remover foto") }
                if (!error.isNullOrBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = { onClearError(); onSave(type, text, temperature, humidity, photoUri) }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun ExperimentSummary(value: CultivationExperiment) {
    val rows = listOf(
        "Condições planejadas" to value.plannedConditions,
        "Condições observadas" to value.observedConditions,
        "Materiais e quantidades" to value.materialsAndAmounts,
        "Duração planejada" to value.plannedStageDurations,
        "Duração real" to value.actualStageDurations,
        "Resultado" to value.result,
        "Observações" to value.notes,
    ).filter { it.second.isNotBlank() }
    rows.forEach { (label, content) ->
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ExperimentDialog(
    initial: CultivationExperiment?,
    cultivationId: Long,
    onDismiss: () -> Unit,
    onSave: (CultivationExperiment) -> Unit,
) {
    var planned by remember(initial?.id) { mutableStateOf(initial?.plannedConditions.orEmpty()) }
    var observed by remember(initial?.id) { mutableStateOf(initial?.observedConditions.orEmpty()) }
    var materials by remember(initial?.id) { mutableStateOf(initial?.materialsAndAmounts.orEmpty()) }
    var plannedDuration by remember(initial?.id) { mutableStateOf(initial?.plannedStageDurations.orEmpty()) }
    var actualDuration by remember(initial?.id) { mutableStateOf(initial?.actualStageDurations.orEmpty()) }
    var result by remember(initial?.id) { mutableStateOf(initial?.result.orEmpty()) }
    var notes by remember(initial?.id) { mutableStateOf(initial?.notes.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ficha do experimento") },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Separe o plano do que foi realmente observado. Use texto livre; nenhum campo é obrigatório.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(planned, { planned = it }, label = { Text("Condições planejadas") }, minLines = 2)
                OutlinedTextField(observed, { observed = it }, label = { Text("Condições observadas") }, minLines = 2)
                OutlinedTextField(materials, { materials = it }, label = { Text("Materiais e quantidades usados") }, minLines = 2)
                OutlinedTextField(plannedDuration, { plannedDuration = it }, label = { Text("Duração planejada por etapa") }, minLines = 2)
                OutlinedTextField(actualDuration, { actualDuration = it }, label = { Text("Duração real por etapa") }, minLines = 2)
                OutlinedTextField(result, { result = it }, label = { Text("Resultado final") }, minLines = 2)
                OutlinedTextField(notes, { notes = it }, label = { Text("Observações adicionais") }, minLines = 2)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(CultivationExperiment(id = initial?.id ?: 0, cultivationId = cultivationId, plannedConditions = planned, observedConditions = observed, materialsAndAmounts = materials, plannedStageDurations = plannedDuration, actualStageDurations = actualDuration, result = result, notes = notes)) }) { Text("Salvar ficha") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

data class ComparisonImage(val id: Long, val localPath: String, val caption: String, val capturedAt: Long)

@Composable
fun PhotoComparisonDialog(photos: List<ComparisonImage>, onDismiss: () -> Unit) {
    if (photos.size < 2) return
    var firstId by remember(photos) { mutableLongStateOf(photos.last().id) }
    var secondId by remember(photos) { mutableLongStateOf(photos.first().id) }
    val first = photos.firstOrNull { it.id == firstId } ?: photos.last()
    val second = photos.firstOrNull { it.id == secondId } ?: photos.first()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Comparar fotos") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ComparisonImageSelector("Foto A", photos, first.id, second.id, Modifier.weight(1f)) { selected ->
                        firstId = selected
                        if (selected == secondId) secondId = first.id
                    }
                    ComparisonImageSelector("Foto B", photos, second.id, first.id, Modifier.weight(1f)) { selected ->
                        secondId = selected
                        if (selected == firstId) firstId = second.id
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ComparisonPhotoPane(first, Modifier.weight(1f))
                    ComparisonPhotoPane(second, Modifier.weight(1f))
                }
                Text("Linha do tempo: ${photos.size} fotos · mais antiga ${dateTimeFormatter.format(Instant.ofEpochMilli(photos.minOf { it.capturedAt }))} · mais recente ${dateTimeFormatter.format(Instant.ofEpochMilli(photos.maxOf { it.capturedAt }))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
    )
}

@Composable
private fun ComparisonImageSelector(label: String, photos: List<ComparisonImage>, selectedId: Long, otherId: Long, modifier: Modifier, onSelect: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = photos.firstOrNull { it.id == selectedId }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("$label\n${selected?.let { dateFormatter.format(Instant.ofEpochMilli(it.capturedAt)) } ?: "Selecionar"}") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            photos.filter { it.id != otherId }.sortedByDescending { it.capturedAt }.forEach { photo ->
                DropdownMenuItem(text = { Text("${dateTimeFormatter.format(Instant.ofEpochMilli(photo.capturedAt))} · ${photo.caption.ifBlank { "Foto" }}") }, onClick = { onSelect(photo.id); expanded = false })
            }
        }
    }
}

@Composable
private fun ComparisonPhotoPane(photo: ComparisonImage, modifier: Modifier) {
    val bitmap by produceState<ImageBitmap?>(null, photo.localPath) {
        value = withContext(Dispatchers.IO) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(photo.localPath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) null else {
                var sample = 1
                while (bounds.outWidth / sample > 900 || bounds.outHeight / sample > 900) sample *= 2
                BitmapFactory.decodeFile(photo.localPath, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
            }
        }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (bitmap != null) Image(bitmap!!, contentDescription = photo.caption.ifBlank { "Foto comparativa" }, modifier = Modifier.fillMaxWidth().height(190.dp), contentScale = ContentScale.Fit)
        else Box(Modifier.fillMaxWidth().height(190.dp).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = androidx.compose.ui.Alignment.Center) { Text("Foto indisponível") }
        Text(dateTimeFormatter.format(Instant.ofEpochMilli(photo.capturedAt)), style = MaterialTheme.typography.labelSmall)
        if (photo.caption.isNotBlank()) Text(photo.caption, style = MaterialTheme.typography.bodySmall, maxLines = 2)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HarvestDialog(onDismiss: () -> Unit, onSave: (String, String, Long, String) -> Unit) {
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("g") }
    var notes by remember { mutableStateOf("") }
    var harvestedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var datePickerOpen by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar colheita") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(quantity, { quantity = it }, label = { Text("Quantidade *") }, singleLine = true)
                OutlinedTextField(unit, { unit = it }, label = { Text("Unidade (g, kg, un...) *") }, singleLine = true)
                OutlinedButton(onClick = { datePickerOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("Data: ${dateFormatter.format(Instant.ofEpochMilli(harvestedAt))}") }
                OutlinedTextField(notes, { notes = it }, label = { Text("Observação (opcional)") }, minLines = 2)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(quantity, unit, harvestedAt, notes) }, enabled = (quantity.trim().replace(',', '.').toDoubleOrNull() ?: 0.0) > 0.0 && unit.isNotBlank()) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
    if (datePickerOpen) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = harvestedAt)
        DatePickerDialog(
            onDismissRequest = { datePickerOpen = false },
            confirmButton = { TextButton(onClick = { picker.selectedDateMillis?.let { harvestedAt = it }; datePickerOpen = false }) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = { datePickerOpen = false }) { Text("Cancelar") } },
        ) { DatePicker(state = picker) }
    }
}

private fun Double.prettyQuantity(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()

@Composable
private fun ErrorMessage(message: String, onDismiss: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(message, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    }
}
