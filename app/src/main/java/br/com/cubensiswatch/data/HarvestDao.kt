package br.com.cubensiswatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HarvestDao {
    @Query("SELECT * FROM harvests WHERE cultivationId = :cultivationId ORDER BY number ASC")
    fun observeForCultivation(cultivationId: Long): Flow<List<Harvest>>

    @Query("SELECT COALESCE(MAX(number), 0) + 1 FROM harvests WHERE cultivationId = :cultivationId")
    suspend fun nextNumber(cultivationId: Long): Int

    @Query("SELECT * FROM harvests WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Harvest?

    @Insert
    suspend fun insert(harvest: Harvest): Long

    @Query("DELETE FROM harvests WHERE id = :id")
    suspend fun delete(id: Long)
}
