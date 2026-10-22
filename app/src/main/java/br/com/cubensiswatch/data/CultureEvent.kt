package br.com.cubensiswatch.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "culture_events",
    foreignKeys = [ForeignKey(entity = Culture::class, parentColumns = ["id"], childColumns = ["cultureId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("cultureId"), Index("occurredAt")],
)
data class CultureEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cultureId: Long,
    val type: String,
    val description: String,
    val occurredAt: Long = System.currentTimeMillis(),
    val status: String? = null,
    val sourceCultureCode: String? = null,
)
