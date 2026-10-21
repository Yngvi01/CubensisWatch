package br.com.cubensiswatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CultivationDao {
    @Query("SELECT * FROM cultivations ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<Cultivation>>

    @Query("SELECT COUNT(*) FROM cultivations WHERE status NOT IN ('Finalizado', 'Contaminado', 'Descartado')")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cultivations WHERE status = 'Incubação'")
    fun observeIncubatingCount(): Flow<Int>

    @Query("SELECT * FROM cultivations WHERE id = :id")
    fun observeById(id: Long): Flow<Cultivation?>

    @Query("SELECT * FROM cultivations WHERE lineageId = :lineageId ORDER BY startedAt ASC")
    fun observeForLineage(lineageId: Long): Flow<List<Cultivation>>

    @Query("SELECT * FROM cultivations WHERE sourceCultureId = :cultureId ORDER BY startedAt ASC")
    fun observeFromCulture(cultureId: Long): Flow<List<Cultivation>>

    @Query("SELECT * FROM cultivations WHERE sourceCultureId = :cultureId")
    suspend fun getFromCulture(cultureId: Long): List<Cultivation>

    @Query("SELECT * FROM cultivations WHERE id = :id")
    suspend fun getById(id: Long): Cultivation?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(cultivation: Cultivation): Long

    @Update
    suspend fun update(cultivation: Cultivation)

    @Delete
    suspend fun delete(cultivation: Cultivation)
}
