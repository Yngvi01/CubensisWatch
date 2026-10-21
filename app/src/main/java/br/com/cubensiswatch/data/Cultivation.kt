package br.com.cubensiswatch.data

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cultivations",
    foreignKeys = [
        ForeignKey(entity = Lineage::class, parentColumns = ["id"], childColumns = ["lineageId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = Culture::class, parentColumns = ["id"], childColumns = ["sourceCultureId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = Incubator::class, parentColumns = ["id"], childColumns = ["incubatorId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("lineageId"), Index("sourceCultureId"), Index("incubatorId")],
)
data class Cultivation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val species: String = "",
    @ColumnInfo(defaultValue = "''") val variety: String = "",
    @ColumnInfo(defaultValue = "''") val lineage: String = "",
    val status: String = "Planejado",
    val startedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val method: String = "",
    @ColumnInfo(defaultValue = "''") val origin: String = "",
    @ColumnInfo(defaultValue = "''") val incubator: String = "",
    @ColumnInfo(defaultValue = "''") val notes: String = "",
    val completedAt: Long? = null,
    val lineageId: Long? = null,
    val sourceCultureId: Long? = null,
    val incubatorId: Long? = null,
)
