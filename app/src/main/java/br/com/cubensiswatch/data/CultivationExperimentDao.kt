package br.com.cubensiswatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CultivationExperimentDao {
    @Query("SELECT * FROM cultivation_experiments WHERE cultivationId = :cultivationId LIMIT 1")
    fun observeForCultivation(cultivationId: Long): Flow<CultivationExperiment?>

    @Query("""
        SELECT experiments.* FROM cultivation_experiments AS experiments
        INNER JOIN cultivations AS current ON current.id = :cultivationId
        INNER JOIN cultivations AS other ON other.id = experiments.cultivationId
        WHERE experiments.cultivationId != :cultivationId AND (
            (current.lineageId IS NOT NULL AND other.lineageId = current.lineageId)
            OR (current.sourceCultureId IS NOT NULL AND other.sourceCultureId = current.sourceCultureId)
            OR (current.species != '' AND other.species = current.species)
        )
        ORDER BY other.startedAt DESC
    """)
    fun observeSimilar(cultivationId: Long): Flow<List<CultivationExperiment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(experiment: CultivationExperiment): Long
}
