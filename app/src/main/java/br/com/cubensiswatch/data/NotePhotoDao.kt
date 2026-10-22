package br.com.cubensiswatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotePhotoDao {
    @Query("SELECT * FROM note_photos WHERE noteId = :noteId ORDER BY capturedAt DESC")
    fun observeForNote(noteId: Long): Flow<List<NotePhoto>>

    @Insert
    suspend fun insert(photo: NotePhoto): Long

    @Query("SELECT * FROM note_photos WHERE noteId = :noteId")
    suspend fun getForNote(noteId: Long): List<NotePhoto>

    @Query("SELECT localPath FROM note_photos")
    suspend fun getAllLocalPaths(): List<String>

    @Query("DELETE FROM note_photos WHERE id = :photoId")
    suspend fun delete(photoId: Long)
}
