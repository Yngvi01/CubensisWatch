package br.com.cubensiswatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE (:query = '' OR title LIKE '%' || :query || '%' OR text LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' OR hypothesis LIKE '%' || :query || '%' OR findings LIKE '%' || :query || '%' OR noteType LIKE '%' || :query || '%') ORDER BY updatedAt DESC")
    fun observeSearch(query: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Note?

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)
}
