package br.com.cubensiswatch.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** An optional plan-versus-observed worksheet belonging to one cultivation run. */
@Entity(
    tableName = "cultivation_experiments",
    foreignKeys = [ForeignKey(entity = Cultivation::class, parentColumns = ["id"], childColumns = ["cultivationId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["cultivationId"], unique = true)],
)
data class CultivationExperiment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cultivationId: Long,
    val plannedConditions: String = "",
    val observedConditions: String = "",
    val materialsAndAmounts: String = "",
    val plannedStageDurations: String = "",
    val actualStageDurations: String = "",
    val result: String = "",
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
)
