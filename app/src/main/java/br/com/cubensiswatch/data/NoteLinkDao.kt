package br.com.cubensiswatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteLinkDao {
    @Query("SELECT * FROM note_links ORDER BY noteId, id")
    fun observeAll(): Flow<List<NoteLink>>

    @Query("SELECT * FROM note_links WHERE noteId = :noteId ORDER BY id")
    fun observeForNote(noteId: Long): Flow<List<NoteLink>>

    @Query("DELETE FROM note_links WHERE noteId = :noteId")
    suspend fun deleteForNote(noteId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(links: List<NoteLink>)
}
