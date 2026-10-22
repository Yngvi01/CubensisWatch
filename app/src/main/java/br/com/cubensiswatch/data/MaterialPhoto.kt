package br.com.cubensiswatch.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "material_photos",
    foreignKeys = [ForeignKey(
        entity = Material::class,
        parentColumns = ["id"],
        childColumns = ["materialId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("materialId")],
)
data class MaterialPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val materialId: Long,
    val localPath: String,
    val caption: String = "",
    val capturedAt: Long = System.currentTimeMillis(),
)
