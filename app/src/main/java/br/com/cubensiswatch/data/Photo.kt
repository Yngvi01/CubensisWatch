package br.com.cubensiswatch.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(entity = Cultivation::class, parentColumns = ["id"], childColumns = ["cultivationId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CultivationEvent::class, parentColumns = ["id"], childColumns = ["eventId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("cultivationId"), Index("eventId")],
)
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cultivationId: Long,
    val eventId: Long? = null,
    val localPath: String,
    val caption: String = "",
    val capturedAt: Long = System.currentTimeMillis(),
)
