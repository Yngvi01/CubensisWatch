package br.com.cubensiswatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LineageDao {
    @Query("SELECT * FROM lineages ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Lineage>>

    @Query("SELECT * FROM lineages WHERE id = :id")
    fun observeById(id: Long): Flow<Lineage?>

    @Query("SELECT * FROM lineages WHERE id = :id")
    suspend fun getById(id: Long): Lineage?

    @Insert
    suspend fun insert(lineage: Lineage): Long

    @Update
    suspend fun update(lineage: Lineage)
}
