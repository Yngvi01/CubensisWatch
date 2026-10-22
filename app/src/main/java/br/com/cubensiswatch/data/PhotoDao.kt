package br.com.cubensiswatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE cultivationId = :cultivationId ORDER BY capturedAt DESC")
    fun observeForCultivation(cultivationId: Long): Flow<List<Photo>>

    @Insert
    suspend fun insert(photo: Photo): Long

    @Query("SELECT * FROM photos WHERE cultivationId = :cultivationId")
    suspend fun getForCultivation(cultivationId: Long): List<Photo>

    @Query("SELECT localPath FROM photos")
    suspend fun getAllLocalPaths(): List<String>

    @Query("DELETE FROM photos WHERE id = :photoId")
    suspend fun delete(photoId: Long)
}
