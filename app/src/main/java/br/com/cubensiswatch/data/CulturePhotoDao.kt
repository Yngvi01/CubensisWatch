package br.com.cubensiswatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CulturePhotoDao {
    @Query("SELECT * FROM culture_photos WHERE cultureId = :cultureId ORDER BY capturedAt DESC")
    fun observeForCulture(cultureId: Long): Flow<List<CulturePhoto>>

    @Insert
    suspend fun insert(photo: CulturePhoto): Long

    @Query("SELECT * FROM culture_photos WHERE cultureId = :cultureId")
    suspend fun getForCulture(cultureId: Long): List<CulturePhoto>

    @Query("DELETE FROM culture_photos WHERE id = :id")
    suspend fun delete(id: Long)
}
