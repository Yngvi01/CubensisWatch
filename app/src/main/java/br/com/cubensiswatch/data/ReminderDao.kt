package br.com.cubensiswatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY enabled DESC, dueAt ASC") fun observeAll(): Flow<List<Reminder>>
    @Query("SELECT * FROM reminders WHERE id = :id") suspend fun getById(id: Long): Reminder?
    @Insert suspend fun insert(reminder: Reminder): Long
    @Update suspend fun update(reminder: Reminder)
    @Query("DELETE FROM reminders WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT * FROM reminders WHERE enabled = 1 AND dueAt <= :now") suspend fun due(now: Long): List<Reminder>
}
