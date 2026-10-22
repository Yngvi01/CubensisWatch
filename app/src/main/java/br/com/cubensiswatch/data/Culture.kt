package br.com.cubensiswatch.data

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cultures",
    foreignKeys = [
        ForeignKey(entity = Lineage::class, parentColumns = ["id"], childColumns = ["lineageId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = Culture::class, parentColumns = ["id"], childColumns = ["parentCultureId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = Incubator::class, parentColumns = ["id"], childColumns = ["incubatorId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("lineageId"), Index("parentCultureId"), Index("incubatorId"), Index(value = ["code"], unique = true)],
)
data class Culture(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val type: String,
    val status: String = "Ativo",
    @ColumnInfo(defaultValue = "''") val species: String = "",
    @ColumnInfo(defaultValue = "''") val variety: String = "",
    val lineageId: Long? = null,
    val parentCultureId: Long? = null,
    @ColumnInfo(defaultValue = "0") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val origin: String = "",
    @ColumnInfo(defaultValue = "''") val location: String = "",
    @ColumnInfo(defaultValue = "''") val incubator: String = "",
    @ColumnInfo(defaultValue = "''") val notes: String = "",
    val incubatorId: Long? = null,
)
