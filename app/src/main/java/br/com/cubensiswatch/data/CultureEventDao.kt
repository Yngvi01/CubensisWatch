package br.com.cubensiswatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CultureEventDao {
    @Query("SELECT * FROM culture_events WHERE cultureId = :cultureId ORDER BY occurredAt DESC, id DESC")
    fun observeForCulture(cultureId: Long): Flow<List<CultureEvent>>

    @Insert
    suspend fun insert(event: CultureEvent): Long
}
