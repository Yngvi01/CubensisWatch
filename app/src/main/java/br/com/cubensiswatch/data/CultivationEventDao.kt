package br.com.cubensiswatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CultivationEventDao {
    @Query("SELECT * FROM cultivation_events ORDER BY occurredAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<CultivationEvent>>

    @Query("SELECT * FROM cultivation_events WHERE cultivationId = :cultivationId ORDER BY occurredAt ASC")
    fun observeForCultivation(cultivationId: Long): Flow<List<CultivationEvent>>

    @Insert
    suspend fun insert(event: CultivationEvent): Long
}
