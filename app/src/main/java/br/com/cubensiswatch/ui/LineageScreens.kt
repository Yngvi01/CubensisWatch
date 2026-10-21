package br.com.cubensiswatch.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.cubensiswatch.data.Culture
import br.com.cubensiswatch.data.CulturePhoto
import br.com.cubensiswatch.data.Cultivation
import br.com.cubensiswatch.data.Lineage
import br.com.cubensiswatch.data.Incubator
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.io.File

val cultureTypes = listOf("Cultura líquida", "Ágar", "Spawn", "Matriz", "Outro")
val cultureStatuses = listOf("Ativo", "Em observação", "Suspeita de contaminação", "Contaminado", "Descartado", "Arquivado")
private val lineageDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault())

@Composable
fun LineageListScreen(viewModel: LineageViewModel, onCreate: () -> Unit, onOpen: (Long) -> Unit) {
    val lineages by viewModel.lineages.collectAsStateWithLifecycle(initialValue = emptyList())
    val cultures by viewModel.cultures.collectAsStateWithLifecycle(initialValue = emptyList())
    val error by viewModel.error.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Linhagens", style = MaterialTheme.typography.headlineMedium)
                Button(onClick = onCreate) { Text("＋ Nova") }
            }
        }
        error?.let { item { PhaseError(it) { viewModel.clearError() } } }
        if (lineages.isEmpty()) item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Nenhuma linhagem cadastrada", style = MaterialTheme.typography.titleMedium)
                    Text("Crie uma linhagem para manter culturas e derivados organizados em uma árvore.", modifier = Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(lineages, key = { it.id }) { lineage ->
            val cultureCount = cultures.count { it.lineageId == lineage.id }
            Card(Modifier.fillMaxWidth().clickable { onOpen(lineage.id) }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(lineage.name, style = MaterialTheme.typography.titleLarge)
                    Text("${lineage.code} · ${lineage.species.ifBlank { "Espécie não informada" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$cultureCount ${if (cultureCount == 1) "cultura" else "culturas"} registradas", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun LineageEditorScreen(initial: Lineage?, viewModel: LineageViewModel, onSaved: (Long) -> Unit, onCancel: () -> Unit) {
    var code by remember(initial?.id) { mutableStateOf(initial?.code.orEmpty()) }
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var species by remember(initial?.id) { mutableStateOf(initial?.species.orEmpty()) }
    var variety by remember(initial?.id) { mutableStateOf(initial?.variety.orEmpty()) }
    var description by remember(initial?.id) { mutableStateOf(initial?.description.orEmpty()) }
    val error by viewModel.error.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(if (initial == null) "Nova linhagem" else "Editar linhagem", style = MaterialTheme.typography.headlineMedium) }
        if (error != null) item { PhaseError(error!!) { viewModel.clearError() } }
        item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome *") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Código (gerado se vazio)") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(value = species, onValueChange = { species = it }, label = { Text("Espécie") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(value = variety, onValueChange = { variety = it }, label = { Text("Variedade") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição / origem") }, modifier = Modifier.fillMaxWidth(), minLines = 3) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                Button(onClick = {
                    viewModel.saveLineage(Lineage(id = initial?.id ?: 0, code = code, name = name, species = species, variety = variety, description = description, createdAt = initial?.createdAt ?: System.currentTimeMillis()), onSaved)
                }, modifier = Modifier.weight(1f)) { Text("Salvar") }
            }
        }
    }
}

@Composable
fun LineageDetailScreen(
    id: Long,
    viewModel: LineageViewModel,
    onEdit: () -> Unit,
    onAddCulture: (Long) -> Unit,
    onCulture: (Long) -> Unit,
    onCultivation: (Long) -> Unit,
) {
    val lineage by viewModel.lineage(id).collectAsStateWithLifecycle(initialValue = null)
    val cultures by viewModel.culturesForLineage(id).collectAsStateWithLifecycle(initialValue = emptyList())
    val cultivations by viewModel.cultivationsForLineage(id).collectAsStateWithLifecycle(initialValue = emptyList())
    val error by viewModel.error.collectAsStateWithLifecycle()
    if (lineage == null) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) { Text("Carregando linhagem…") }
        return
    }
    val item = lineage!!
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.headlineMedium)
                    Text("${item.code} · ${item.species.ifBlank { "Espécie não informada" }}${item.variety.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onEdit) { Text("Editar") }
            }
            if (item.description.isNotBlank()) Text(item.description, Modifier.padding(top = 6.dp))
        }
        if (error != null) item { PhaseError(error!!) { viewModel.clearError() } }
        item { Button(onClick = { onAddCulture(item.id) }, modifier = Modifier.fillMaxWidth()) { Text("＋ Nova cultura derivada") } }
        item { Text("Árvore da linhagem", style = MaterialTheme.typography.titleLarge) }
        if (cultures.isEmpty() && cultivations.isEmpty()) {
            item { Text("Ainda não há culturas ou cultivos ligados a esta linhagem.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            cultures.filter { it.parentCultureId == null }.forEach { root ->
                item(key = "culture-${root.id}") {
                    CultureTreeNode(root, 0, cultures, cultivations, onCulture, onCultivation)
                }
            }
            cultivations.filter { it.sourceCultureId == null }.forEach { cultivation ->
                item(key = "cultivation-${cultivation.id}") { TreeCultivationNode(cultivation, 0, onCultivation) }
            }
        }
    }
}

@Composable
private fun CultureTreeNode(
    culture: Culture,
    depth: Int,
    cultures: List<Culture>,
    cultivations: List<Cultivation>,
    onCulture: (Long) -> Unit,
    onCultivation: (Long) -> Unit,
) {
    Card(Modifier.fillMaxWidth().padding(start = (depth * 18).dp).clickable { onCulture(culture.id) }) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("${if (depth == 0) "└──" else "├──"} ${culture.type}: ${culture.name}", style = MaterialTheme.typography.titleSmall)
            Text("${culture.code} · ${culture.status}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    cultivations.filter { it.sourceCultureId == culture.id }.forEach { child ->
        TreeCultivationNode(child, depth + 1, onCultivation)
    }
    cultures.filter { it.parentCultureId == culture.id }.forEach { child ->
        CultureTreeNode(child, depth + 1, cultures, cultivations, onCulture, onCultivation)
    }
}

@Composable
private fun TreeCultivationNode(cultivation: Cultivation, depth: Int, onClick: (Long) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(start = (depth * 18).dp).clickable { onClick(cultivation.id) }) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("└── Cultivo: ${cultivation.name}", style = MaterialTheme.typography.titleSmall)
            Text("${cultivation.code} · ${cultivation.status}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CultureListScreen(viewModel: LineageViewModel, onCreate: () -> Unit, onOpen: (Long) -> Unit) {
    val cultures by viewModel.cultures.collectAsStateWithLifecycle(initialValue = emptyList())
    val lineages by viewModel.lineages.collectAsStateWithLifecycle(initialValue = emptyList())
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Culturas", style = MaterialTheme.typography.headlineMedium)
                Button(onClick = onCreate) { Text("＋ Nova") }
            }
        }
        if (cultures.isEmpty()) item { Text("Nenhuma cultura cadastrada. Crie uma linhagem primeiro para vincular a origem.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(cultures, key = { it.id }) { culture ->
            val lineageName = lineages.firstOrNull { it.id == culture.lineageId }?.name ?: "Sem linhagem"
            Card(Modifier.fillMaxWidth().clickable { onOpen(culture.id) }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(culture.name, style = MaterialTheme.typography.titleLarge)
                    Text("${culture.code} · ${culture.type} · $lineageName", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(culture.status, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun CultureEditorScreen(
    initial: Culture?,
    initialLineageId: Long?,
    lineages: List<Lineage>,
    cultures: List<Culture>,
    incubators: List<Incubator>,
    viewModel: LineageViewModel,
    onSaved: (Long) -> Unit,
    onCancel: () -> Unit,
) {
    var code by remember(initial?.id) { mutableStateOf(initial?.code.orEmpty()) }
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var species by remember(initial?.id) { mutableStateOf(initial?.species.orEmpty()) }
    var variety by remember(initial?.id) { mutableStateOf(initial?.variety.orEmpty()) }
    var type by remember(initial?.id) { mutableStateOf(initial?.type ?: cultureTypes.first()) }
    var status by remember(initial?.id) { mutableStateOf(initial?.status ?: "Ativo") }
    var lineageId by remember(initial?.id) { mutableStateOf(initial?.lineageId ?: initialLineageId) }
    var parentId by remember(initial?.id) { mutableStateOf(initial?.parentCultureId) }
    var origin by remember(initial?.id) { mutableStateOf(initial?.origin.orEmpty()) }
    var location by remember(initial?.id) { mutableStateOf(initial?.location.orEmpty()) }
    var incubator by remember(initial?.id) { mutableStateOf(initial?.incubator.orEmpty()) }
    var incubatorId by remember(initial?.id) { mutableStateOf(initial?.incubatorId) }
    var notes by remember(initial?.id) { mutableStateOf(initial?.notes.orEmpty()) }
    val error by viewModel.error.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(if (initial == null) "Nova cultura" else "Editar cultura", style = MaterialTheme.typography.headlineMedium) }
        if (error != null) item { PhaseError(error!!) { viewModel.clearError() } }
        item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome *") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Código (gerado se vazio)") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(value = species, onValueChange = { species = it }, label = { Text("Espécie") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(value = variety, onValueChange = { variety = it }, label = { Text("Variedade") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { ChoiceSelector("Tipo", type, cultureTypes) { type = it } }
        item { ChoiceSelector("Status", status, cultureStatuses) { status = it } }
        item { OptionalEntitySelector("Linhagem *", lineages, lineageId, { it.name }) { lineageId = it; if (cultures.none { culture -> culture.id == parentId && culture.lineageId == it }) parentId = null } }
        item {
            val possibleParents = cultures.filter { it.lineageId == lineageId && it.id != initial?.id }
            OptionalEntitySelector("Cultura de origem (opcional)", possibleParents, parentId, { "${it.code} · ${it.name}" }, nullable = true) { parentId = it }
        }
        item { OutlinedTextField(value = origin, onValueChange = { origin = it }, label = { Text("Origem") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Local") }, modifier = Modifier.fillMaxWidth()) }
        if (incubators.isNotEmpty()) item { IncubatorSelector(incubators, incubatorId) { incubatorId = it } }
        item { OutlinedTextField(value = incubator, onValueChange = { incubator = it }, label = { Text("Outro local") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Observações") }, modifier = Modifier.fillMaxWidth(), minLines = 3) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                Button(onClick = {
                    viewModel.saveCulture(
                        Culture(
                            id = initial?.id ?: 0, code = code, name = name, species = species, variety = variety,
                            type = type, status = status, lineageId = lineageId, parentCultureId = parentId,
                            createdAt = initial?.createdAt ?: System.currentTimeMillis(), origin = origin,
                            location = location, incubator = incubator, notes = notes,
                            incubatorId = incubatorId,
                        ), onSaved,
                    )
                }, modifier = Modifier.weight(1f)) { Text("Salvar") }
            }
        }
    }
}

@Composable
fun CultureDetailScreen(id: Long, viewModel: LineageViewModel, onEdit: () -> Unit, onDeleted: () -> Unit, onNewCultivation: (Long, Long) -> Unit) {
    val culture by viewModel.culture(id).collectAsStateWithLifecycle(initialValue = null)
    val photos by viewModel.culturePhotos(id).collectAsStateWithLifecycle(initialValue = emptyList())
    val lineages by viewModel.lineages.collectAsStateWithLifecycle(initialValue = emptyList())
    val cultures by viewModel.cultures.collectAsStateWithLifecycle(initialValue = emptyList())
    val error by viewModel.error.collectAsStateWithLifecycle()
    var caption by remember { mutableStateOf("") }
    var deleteConfirm by remember { mutableStateOf(false) }
    var captureFile by remember { mutableStateOf<File?>(null) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) viewModel.addCulturePhotos(id, uris, caption)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val file = captureFile
        captureFile = null
        if (file != null) {
            if (saved) viewModel.addCapturedCulturePhoto(id, file, caption) else file.delete()
        }
    }
    if (culture == null) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) { Text("Carregando cultura…") }
        return
    }
    val item = culture!!
    val parent = cultures.firstOrNull { it.id == item.parentCultureId }
    val lineage = lineages.firstOrNull { it.id == item.lineageId }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) { Text(item.name, style = MaterialTheme.typography.headlineMedium); Text("${item.code} · ${item.type}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                TextButton(onClick = onEdit) { Text("Editar") }
            }
        }
        if (error != null) item { PhaseError(error!!) { viewModel.clearError() } }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(item.status, color = MaterialTheme.colorScheme.primary)
                    Text("Linhagem: ${lineage?.name ?: "Não vinculada"}")
                    Text("Origem: ${parent?.name ?: item.origin.ifBlank { "Registro inicial" }}")
                    Text("Criada em ${lineageDateFormatter.format(Instant.ofEpochMilli(item.createdAt))}")
                    if (item.location.isNotBlank()) Text("Local: ${item.location}")
                    if (item.incubator.isNotBlank()) Text("Incubadora: ${item.incubator}")
                    if (item.notes.isNotBlank()) Text(item.notes)
                }
            }
        }
        if (item.lineageId != null) item {
            Button(onClick = { onNewCultivation(item.lineageId!!, item.id) }, modifier = Modifier.fillMaxWidth()) { Text("＋ Criar cultivo derivado") }
        }
        item { OutlinedTextField(value = caption, onValueChange = { caption = it }, label = { Text("Legenda das próximas fotos (opcional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.fillMaxWidth()) { Text("＋ Adicionar fotos") } }
        item {
            OutlinedButton(onClick = {
                viewModel.createCultureCaptureFile { file ->
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
            }, modifier = Modifier.fillMaxWidth()) { Text("＋ Tirar foto") }
        }
        item { Text("Fotos", style = MaterialTheme.typography.titleLarge) }
        if (photos.isEmpty()) item { Text("Nenhuma foto registrada.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(photos, key = { "culture-photo-${it.id}" }) { photo -> CulturePhotoCard(photo) { viewModel.deleteCulturePhoto(photo) } }
        item {
            OutlinedButton(onClick = { deleteConfirm = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Excluir cultura") }
        }
    }
    if (deleteConfirm) AlertDialog(
        onDismissRequest = { deleteConfirm = false }, title = { Text("Excluir cultura?") },
        text = { Text("As fotos locais desta cultura serão excluídas. Derivados e cultivos relacionados serão preservados e ficarão sem esse registro de origem.") },
        confirmButton = { TextButton(onClick = { deleteConfirm = false; viewModel.deleteCulture(item, onDeleted) }) { Text("Excluir", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { deleteConfirm = false }) { Text("Cancelar") } },
    )
}

@Composable
private fun CulturePhotoCard(photo: CulturePhoto, onDelete: () -> Unit) = LocalPhotoCard(photo.localPath, photo.caption, photo.capturedAt, onDelete)

@Composable
private fun ChoiceSelector(label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("$label: $selected") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false }) }
        }
    }
}

@Composable
private fun <T> OptionalEntitySelector(
    label: String,
    options: List<T>,
    selectedId: Long?,
    text: (T) -> String,
    nullable: Boolean = false,
    onSelect: (Long?) -> Unit,
) where T : Any {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { entityId(it) == selectedId }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label: ${selected?.let(text) ?: if (nullable) "Nenhuma" else "Selecionar"}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (nullable) DropdownMenuItem(text = { Text("Nenhuma") }, onClick = { onSelect(null); expanded = false })
            options.forEach { option -> DropdownMenuItem(text = { Text(text(option)) }, onClick = { onSelect(entityId(option)); expanded = false }) }
        }
    }
}

private fun entityId(entity: Any): Long = when (entity) {
    is Lineage -> entity.id
    is Culture -> entity.id
    else -> error("Unsupported selector item")
}

@Composable
private fun PhaseError(message: String, onDismiss: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(message, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    }
}
