package br.com.cubensiswatch.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "harvests",
    foreignKeys = [ForeignKey(
        entity = Cultivation::class,
        parentColumns = ["id"],
        childColumns = ["cultivationId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("cultivationId"), Index(value = ["cultivationId", "number"], unique = true)],
)
data class Harvest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cultivationId: Long,
    val number: Int,
    val harvestedAt: Long = System.currentTimeMillis(),
    val quantity: Double,
    val unit: String = "g",
    val notes: String = "",
)
