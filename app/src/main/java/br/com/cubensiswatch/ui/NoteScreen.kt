package br.com.cubensiswatch.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.cubensiswatch.data.Note
import br.com.cubensiswatch.data.NotePhoto
import java.text.DateFormat

data class NoteRecordOption(val type: String, val id: Long, val label: String)

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
    val notes by viewModel.search(query).collectAsStateWithLifecycle(initialValue = emptyList())
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
                Text("Anotações", style = MaterialTheme.typography.headlineMedium)
                Button(onClick = { editing = null; editorOpen = true }) { Text("＋ Nova") }
            }
        }
        item { OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Buscar em título, texto e tags") }) }
        if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error) }
        if (notes.isEmpty()) item {
            Text(if (query.isBlank()) "Nenhuma anotação ainda. Registre uma ideia ou detalhe do seu cultivo." else "Nenhuma anotação corresponde à busca.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(notes, key = { it.id }) { note ->
            val photos by viewModel.photos(note.id).collectAsStateWithLifecycle(initialValue = emptyList())
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(note.title, style = MaterialTheme.typography.titleMedium)
                    Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(note.updatedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(note.text, style = MaterialTheme.typography.bodyMedium)
                    if (note.tags.isNotBlank()) Text("Tags: ${note.tags}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    val related = relatedRecords.firstOrNull { it.type == note.relatedType && it.id == note.relatedId }
                    if (related != null) Text("Relacionado: ${related.label}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        relatedRecords = relatedRecords,
        onDismiss = { editorOpen = false; editing = null },
        onSave = { note -> viewModel.save(note) { editorOpen = false; editing = null } },
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
    relatedRecords: List<NoteRecordOption>,
    onDismiss: () -> Unit,
    onSave: (Note) -> Unit,
) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var text by remember(initial?.id) { mutableStateOf(initial?.text.orEmpty()) }
    var tags by remember(initial?.id) { mutableStateOf(initial?.tags.orEmpty()) }
    var related by remember(initial?.id) {
        mutableStateOf(initial?.let { note -> relatedRecords.firstOrNull { it.type == note.relatedType && it.id == note.relatedId } })
    }
    var recordMenu by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nova anotação" else "Editar anotação") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Título *") }, singleLine = true)
                OutlinedTextField(text, { text = it }, label = { Text("Anotação *") }, minLines = 4)
                OutlinedTextField(tags, { tags = it }, label = { Text("Tags separadas por vírgula") }, singleLine = true)
                Box {
                    OutlinedButton(onClick = { recordMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(related?.label ?: "Vincular a um registro (opcional)")
                    }
                    DropdownMenu(expanded = recordMenu, onDismissRequest = { recordMenu = false }) {
                        DropdownMenuItem(text = { Text("Sem vínculo") }, onClick = { related = null; recordMenu = false })
                        relatedRecords.forEach { option ->
                            DropdownMenuItem(text = { Text(option.label) }, onClick = { related = option; recordMenu = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(Note(
                    id = initial?.id ?: 0,
                    title = title,
                    text = text,
                    createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                    tags = tags,
                    relatedType = related?.type,
                    relatedId = related?.id,
                ))
            }, enabled = title.isNotBlank() && text.isNotBlank()) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
