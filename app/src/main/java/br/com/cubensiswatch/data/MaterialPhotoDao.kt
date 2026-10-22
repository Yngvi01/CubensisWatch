package br.com.cubensiswatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialPhotoDao {
    @Query("SELECT * FROM material_photos ORDER BY capturedAt DESC")
    fun observeAll(): Flow<List<MaterialPhoto>>

    @Query("SELECT * FROM material_photos WHERE materialId = :materialId ORDER BY capturedAt DESC")
    fun observeForMaterial(materialId: Long): Flow<List<MaterialPhoto>>

    @Insert
    suspend fun insert(photo: MaterialPhoto): Long

    @Query("SELECT * FROM material_photos WHERE materialId = :materialId")
    suspend fun getForMaterial(materialId: Long): List<MaterialPhoto>

    @Query("SELECT localPath FROM material_photos")
    suspend fun getAllLocalPaths(): List<String>

    @Query("DELETE FROM material_photos WHERE id = :id")
    suspend fun delete(id: Long)
}
