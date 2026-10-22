package br.com.cubensiswatch.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "harvest_photos",
    foreignKeys = [ForeignKey(
        entity = Harvest::class,
        parentColumns = ["id"],
        childColumns = ["harvestId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("harvestId")],
)
data class HarvestPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val harvestId: Long,
    val localPath: String,
    val caption: String = "",
    val capturedAt: Long = System.currentTimeMillis(),
)
