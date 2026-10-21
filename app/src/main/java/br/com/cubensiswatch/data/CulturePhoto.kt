package br.com.cubensiswatch.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "culture_photos",
    foreignKeys = [ForeignKey(entity = Culture::class, parentColumns = ["id"], childColumns = ["cultureId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("cultureId")],
)
data class CulturePhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cultureId: Long,
    val localPath: String,
    val caption: String = "",
    val capturedAt: Long = System.currentTimeMillis(),
)
