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

    suspend fun save(note: Note) {
        val title = note.title.trim()
        val text = note.text.trim()
        require(title.isNotEmpty()) { "Informe um título para a anotação." }
        require(text.isNotEmpty()) { "Escreva o conteúdo da anotação." }
        val tags = note.tags.split(',').map(String::trim).filter(String::isNotEmpty).distinct().joinToString(", ")
        val normalized = note.copy(title = title, text = text, tags = tags, updatedAt = System.currentTimeMillis())
        if (normalized.id == 0L) dao.insert(normalized) else dao.update(normalized)
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
