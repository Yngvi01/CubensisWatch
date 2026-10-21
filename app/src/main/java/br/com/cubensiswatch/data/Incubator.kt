package br.com.cubensiswatch.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "incubators")
data class Incubator(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String = "",
    val location: String = "",
    val description: String = "",
    val temperature: Double? = null,
    val humidity: Double? = null,
    val notes: String = "",
)

@Entity(
    tableName = "incubator_readings",
    foreignKeys = [ForeignKey(entity = Incubator::class, parentColumns = ["id"], childColumns = ["incubatorId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("incubatorId"), Index("recordedAt")],
)
data class IncubatorReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val incubatorId: Long,
    val temperature: Double? = null,
    val humidity: Double? = null,
    val notes: String = "",
    val recordedAt: Long = System.currentTimeMillis(),
)
