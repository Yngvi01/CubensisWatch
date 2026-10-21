package br.com.cubensiswatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface IncubatorDao {
    @Query("SELECT * FROM incubators ORDER BY name COLLATE NOCASE") fun observeAll(): Flow<List<Incubator>>
    @Query("SELECT * FROM incubators WHERE id = :id") fun observeById(id: Long): Flow<Incubator?>
    @Query("SELECT * FROM incubators WHERE id = :id") suspend fun getById(id: Long): Incubator?
    @Query("SELECT * FROM incubator_readings WHERE incubatorId = :id ORDER BY recordedAt DESC") fun observeReadings(id: Long): Flow<List<IncubatorReading>>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(item: Incubator): Long
    @Update suspend fun update(item: Incubator)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertReading(reading: IncubatorReading): Long
}
