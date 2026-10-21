package br.com.cubensiswatch.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cultivation_events",
    foreignKeys = [ForeignKey(
        entity = Cultivation::class,
        parentColumns = ["id"],
        childColumns = ["cultivationId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("cultivationId"), Index("occurredAt")],
)
data class CultivationEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cultivationId: Long,
    val type: String,
    val description: String,
    val occurredAt: Long = System.currentTimeMillis(),
    val temperature: Double? = null,
    val humidity: Double? = null,
    val stage: String? = null,
)
