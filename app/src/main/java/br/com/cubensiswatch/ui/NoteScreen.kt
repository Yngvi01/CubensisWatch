package br.com.cubensiswatch.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.cubensiswatch.data.Note
import br.com.cubensiswatch.data.NotePhoto
import br.com.cubensiswatch.data.NoteLink
import java.text.DateFormat

data class NoteRecordOption(val type: String, val id: Long, val label: String)
private val knowledgeTypes = listOf("Anotação livre", "Procedimento", "Referência", "Hipótese", "Conclusão")
private val hypothesisOutcomes = listOf("Em aberto", "Apoiou", "Não apoiou", "Inconclusiva")

@Composable
fun NoteScreen(viewModel: NoteViewModel, relatedRecords: List<NoteRecordOption>) {
    var query by remember { mutableStateOf("") }
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Note?>(null) }
    var deleteTarget by remember { mutableStateOf<Note?>(null) }
    var deletePhotoTarget by remember { mutableStateOf<NotePhoto?>(null) }
    var noteForPhotoPicker by remember { mutableStateOf<Note?>(null) }
    var pendingPhotoNote by remember { mutableStateOf<Note?>(null) }
    var pendingPhotoUris by remember { mutableStateOf(emptyList<Uri>()) }
    var photoCaption by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Todas") }
    val notes by viewModel.search(query).collectAsStateWithLifecycle(initialValue = emptyList())
    val links by viewModel.links().collectAsStateWithLifecycle(initialValue = emptyList())
    val error by viewModel.error.collectAsStateWithLifecycle()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { selected: List<Uri> ->
        if (selected.isNotEmpty()) {
            pendingPhotoNote = noteForPhotoPicker
            pendingPhotoUris = selected
            photoCaption = ""
        }
        noteForPhotoPicker = null
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Caderno de conhecimento", style = MaterialTheme.typography.headlineMedium)
                Button(onClick = { editing = null; editorOpen = true }) { Text("＋ Nova") }
            }
        }
        item { Text("Procedimentos, referências e hipóteses ligados aos seus registros de cultivo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Buscar em título, texto e tags") }) }
        item { NoteTypeFilter(selectedType) { selectedType = it } }
        if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error) }
        val visibleNotes = notes.filter { selectedType == "Todas" || it.noteType == selectedType }
        if (visibleNotes.isEmpty()) item {
            Text(if (query.isBlank()) "Nenhuma anotação ainda. Registre uma ideia ou detalhe do seu cultivo." else "Nenhuma anotação corresponde à busca.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(visibleNotes, key = { it.id }) { note ->
            val photos by viewModel.photos(note.id).collectAsStateWithLifecycle(initialValue = emptyList())
            val noteLinks = links.filter { it.noteId == note.id }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(note.noteType, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(note.title, style = MaterialTheme.typography.titleMedium)
                    Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(note.updatedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(note.text, style = MaterialTheme.typography.bodyMedium)
                    if (note.noteType == "Hipótese") {
                        Text("Hipótese: ${note.hypothesis}", style = MaterialTheme.typography.bodyMedium)
                        if (note.findings.isNotBlank()) Text("Resultados e evidências: ${note.findings}", style = MaterialTheme.typography.bodyMedium)
                        Text("Avaliação: ${note.hypothesisOutcome ?: "Em aberto"}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
                    }
                    if (note.tags.isNotBlank()) Text("Tags: ${note.tags}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    val linked = noteLinks.mapNotNull { link -> relatedRecords.firstOrNull { it.type == link.relatedType && it.id == link.relatedId } }
                    if (linked.isNotEmpty()) {
                        Text("Registros relacionados", style = MaterialTheme.typography.labelLarge)
                        linked.forEach { relation -> Text("• ${relation.label}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    OutlinedButton(onClick = {
                        noteForPhotoPicker = note
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("＋ Fotos") }
                    photos.forEach { photo ->
                        LocalPhotoCard(photo.localPath, photo.caption, photo.capturedAt) { deletePhotoTarget = photo }
                    }
                    Row {
                        TextButton(onClick = { editing = note; editorOpen = true }) { Text("Editar") }
                        TextButton(onClick = { deleteTarget = note }) { Text("Excluir") }
                    }
                }
            }
        }
    }

    if (editorOpen) NoteEditorDialog(
        initial = editing,
        initialLinks = editing?.let { note -> links.filter { it.noteId == note.id } }.orEmpty(),
        relatedRecords = relatedRecords,
        onDismiss = { editorOpen = false; editing = null },
        onSave = { note, noteLinks -> viewModel.save(note, noteLinks) { editorOpen = false; editing = null } },
    )
    deleteTarget?.let { note ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Excluir anotação?") },
            text = { Text("‘${note.title}’ será removida permanentemente.") },
            confirmButton = { TextButton(onClick = { viewModel.delete(note); deleteTarget = null }) { Text("Excluir", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") } },
        )
    }
    deletePhotoTarget?.let { photo ->
        AlertDialog(
            onDismissRequest = { deletePhotoTarget = null },
            title = { Text("Excluir foto?") },
            text = { Text("O arquivo local também será removido.") },
            confirmButton = { TextButton(onClick = { viewModel.deletePhoto(photo); deletePhotoTarget = null }) { Text("Excluir", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deletePhotoTarget = null }) { Text("Cancelar") } },
        )
    }
    pendingPhotoNote?.let { note ->
        AlertDialog(
            onDismissRequest = { pendingPhotoNote = null; pendingPhotoUris = emptyList(); photoCaption = "" },
            title = { Text("Descrição das fotos") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${pendingPhotoUris.size} foto(s) selecionada(s). A descrição será aplicada a todas.")
                    OutlinedTextField(photoCaption, { photoCaption = it }, label = { Text("Descrição (opcional)") }, minLines = 2)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addPhotos(note, pendingPhotoUris, photoCaption)
                    pendingPhotoNote = null
                    pendingPhotoUris = emptyList()
                    photoCaption = ""
                }) { Text("Salvar fotos") }
            },
            dismissButton = {
                TextButton(onClick = { pendingPhotoNote = null; pendingPhotoUris = emptyList(); photoCaption = "" }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun NoteEditorDialog(
    initial: Note?,
    initialLinks: List<NoteLink>,
    relatedRecords: List<NoteRecordOption>,
    onDismiss: () -> Unit,
    onSave: (Note, List<NoteLink>) -> Unit,
) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var text by remember(initial?.id) { mutableStateOf(initial?.text.orEmpty()) }
    var tags by remember(initial?.id) { mutableStateOf(initial?.tags.orEmpty()) }
    var noteType by remember(initial?.id) { mutableStateOf(initial?.noteType ?: knowledgeTypes.first()) }
    var hypothesis by remember(initial?.id) { mutableStateOf(initial?.hypothesis.orEmpty()) }
    var findings by remember(initial?.id) { mutableStateOf(initial?.findings.orEmpty()) }
    var outcome by remember(initial?.id) { mutableStateOf(initial?.hypothesisOutcome ?: hypothesisOutcomes.first()) }
    var selectedRecords by remember(initial?.id) {
        mutableStateOf(initialLinks.mapNotNull { link -> relatedRecords.firstOrNull { it.type == link.relatedType && it.id == link.relatedId } })
    }
    var recordMenu by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nova entrada no caderno" else "Editar entrada") },
        text = {
            Column(Modifier.heightIn(max = 540.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NoteTypeSelector(noteType, onSelect = { noteType = it })
                OutlinedTextField(title, { title = it }, label = { Text("Título *") }, singleLine = true)
                OutlinedTextField(text, { text = it }, label = { Text("Conteúdo / contexto *") }, minLines = 3)
                if (noteType == "Hipótese") {
                    OutlinedTextField(hypothesis, { hypothesis = it }, label = { Text("Hipótese a acompanhar *") }, minLines = 2)
                    OutlinedTextField(findings, { findings = it }, label = { Text("Resultados e evidências observados") }, minLines = 3)
                    NoteTypeSelector(outcome, { outcome = it }, hypothesisOutcomes, "Avaliação da hipótese")
                }
                OutlinedTextField(tags, { tags = it }, label = { Text("Tags separadas por vírgula") }, singleLine = true)
                Box {
                    OutlinedButton(onClick = { recordMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (selectedRecords.isEmpty()) "Vincular registros (opcional)" else "${selectedRecords.size} registro(s) relacionado(s)")
                    }
                    DropdownMenu(expanded = recordMenu, onDismissRequest = { recordMenu = false }) {
                        relatedRecords.forEach { option ->
                            val checked = selectedRecords.any { it.type == option.type && it.id == option.id }
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                leadingIcon = { Checkbox(checked = checked, onCheckedChange = null) },
                                onClick = {
                                    selectedRecords = if (checked) selectedRecords.filterNot { it.type == option.type && it.id == option.id }
                                    else selectedRecords + option
                                },
                            )
                        }
                    }
                }
                if (selectedRecords.isNotEmpty()) Text("Vinculados: ${selectedRecords.joinToString { it.label }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    Note(
                        id = initial?.id ?: 0,
                        title = title,
                        text = text,
                        createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                        tags = tags,
                        relatedType = selectedRecords.firstOrNull()?.type,
                        relatedId = selectedRecords.firstOrNull()?.id,
                        noteType = noteType,
                        hypothesis = hypothesis,
                        findings = findings,
                        hypothesisOutcome = if (noteType == "Hipótese") outcome else null,
                    ),
                    selectedRecords.map { NoteLink(noteId = initial?.id ?: 0, relatedType = it.type, relatedId = it.id) },
                )
            }, enabled = title.isNotBlank() && text.isNotBlank() && (noteType != "Hipótese" || hypothesis.isNotBlank())) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun NoteTypeSelector(
    selected: String,
    onSelect: (String) -> Unit,
    options: List<String> = knowledgeTypes,
    label: String = "Tipo",
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("$label: $selected") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false }) }
        }
    }
}

@Composable
private fun NoteTypeFilter(selected: String, onSelect: (String) -> Unit) {
    NoteTypeSelector(selected, onSelect, listOf("Todas") + knowledgeTypes, "Filtrar")
}
