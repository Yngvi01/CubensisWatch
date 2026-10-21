package br.com.cubensiswatch.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CultureDao {
    @Query("SELECT * FROM cultures ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<Culture>>

    @Query("SELECT * FROM cultures WHERE lineageId = :lineageId ORDER BY createdAt ASC, id ASC")
    fun observeForLineage(lineageId: Long): Flow<List<Culture>>

    @Query("SELECT * FROM cultures WHERE id = :id")
    fun observeById(id: Long): Flow<Culture?>

    @Query("SELECT * FROM cultures WHERE id = :id")
    suspend fun getById(id: Long): Culture?

    @Query("SELECT * FROM cultures WHERE parentCultureId = :id")
    suspend fun getChildren(id: Long): List<Culture>

    @Query("SELECT COUNT(*) FROM cultures WHERE status = 'Ativo'")
    fun observeActiveCount(): Flow<Int>

    @Insert
    suspend fun insert(culture: Culture): Long

    @Update
    suspend fun update(culture: Culture)

    @Delete
    suspend fun delete(culture: Culture)
}
