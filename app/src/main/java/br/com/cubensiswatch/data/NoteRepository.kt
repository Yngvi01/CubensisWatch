package br.com.cubensiswatch.data

import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.io.File

class NoteRepository(
    private val database: AppDatabase,
    private val photoStore: LocalPhotoStore,
) {
    private val dao = database.noteDao()
    fun search(query: String): Flow<List<Note>> = dao.observeSearch(query.trim())
    fun links(): Flow<List<NoteLink>> = database.noteLinkDao().observeAll()
    fun links(noteId: Long): Flow<List<NoteLink>> = database.noteLinkDao().observeForNote(noteId)

    suspend fun save(note: Note, links: List<NoteLink> = emptyList()) = database.withTransaction {
        val title = note.title.trim()
        val text = note.text.trim()
        require(title.isNotEmpty()) { "Informe um título para a anotação." }
        require(text.isNotEmpty()) { "Escreva o conteúdo da anotação." }
        val tags = note.tags.split(',').map(String::trim).filter(String::isNotEmpty).distinct().joinToString(", ")
        require(note.noteType != "Hipótese" || note.hypothesis.isNotBlank()) { "Descreva a hipótese que deseja acompanhar." }
        val distinctLinks = links.distinctBy { it.relatedType to it.relatedId }
        val first = distinctLinks.firstOrNull()
        val normalized = note.copy(
            title = title,
            text = text,
            tags = tags,
            updatedAt = System.currentTimeMillis(),
            relatedType = first?.relatedType,
            relatedId = first?.relatedId,
            hypothesis = note.hypothesis.trim(),
            findings = note.findings.trim(),
            hypothesisOutcome = if (note.noteType == "Hipótese") note.hypothesisOutcome ?: "Em aberto" else null,
        )
        val noteId = if (normalized.id == 0L) dao.insert(normalized) else {
            checkNotNull(dao.getById(normalized.id)) { "Anotação não encontrada." }
            dao.update(normalized)
            normalized.id
        }
        database.noteLinkDao().deleteForNote(noteId)
        database.noteLinkDao().insertAll(distinctLinks.map { it.copy(id = 0, noteId = noteId) })
    }

    suspend fun delete(note: Note) = deleteWithPhotos(note)

    fun photos(noteId: Long): Flow<List<NotePhoto>> = database.notePhotoDao().observeForNote(noteId)

    suspend fun addPhotos(noteId: Long, uris: List<Uri>, caption: String = "") {
        require(uris.isNotEmpty()) { "Selecione ao menos uma foto." }
        val files = mutableListOf<File>()
        try {
            uris.forEach { files += photoStore.import(it) }
            database.withTransaction {
                checkNotNull(dao.getById(noteId)) { "Anotação não encontrada." }
                files.forEach { file -> database.notePhotoDao().insert(NotePhoto(noteId = noteId, localPath = file.absolutePath, caption = caption.trim())) }
            }
        } catch (error: Throwable) {
            files.forEach { photoStore.delete(it.absolutePath) }
            throw error
        }
    }

    suspend fun deletePhoto(photo: NotePhoto) {
        database.notePhotoDao().delete(photo.id)
        photoStore.delete(photo.localPath)
    }

    suspend fun deleteWithPhotos(note: Note) {
        val photos = database.notePhotoDao().getForNote(note.id)
        database.withTransaction { dao.delete(note.id) }
        photos.forEach { photoStore.delete(it.localPath) }
    }
}
