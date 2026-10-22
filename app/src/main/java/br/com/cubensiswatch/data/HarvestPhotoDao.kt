package br.com.cubensiswatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HarvestPhotoDao {
    @Query("SELECT * FROM harvest_photos WHERE harvestId = :harvestId ORDER BY capturedAt DESC")
    fun observeForHarvest(harvestId: Long): Flow<List<HarvestPhoto>>

    @Insert
    suspend fun insert(photo: HarvestPhoto): Long

    @Query("SELECT * FROM harvest_photos WHERE harvestId = :harvestId")
    suspend fun getForHarvest(harvestId: Long): List<HarvestPhoto>

    @Query("SELECT localPath FROM harvest_photos")
    suspend fun getAllLocalPaths(): List<String>

    @Query("DELETE FROM harvest_photos WHERE id = :photoId")
    suspend fun delete(photoId: Long)
}
