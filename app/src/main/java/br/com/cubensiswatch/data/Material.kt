package br.com.cubensiswatch.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "materials")
data class Material(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "Outro",
    val quantity: Double = 0.0,
    val unit: String = "un",
    val minimum: Double = 0.0,
    val ideal: Double = 0.0,
    val location: String = "",
    val notes: String = "",
    val lastPurchaseAt: Long? = null,
    val price: Double? = null,
)

@Entity(
    tableName = "stock_movements",
    foreignKeys = [
        ForeignKey(entity = Material::class, parentColumns = ["id"], childColumns = ["materialId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Cultivation::class, parentColumns = ["id"], childColumns = ["cultivationId"], onDelete = ForeignKey.SET_NULL),
    ], indices = [Index("materialId"), Index("cultivationId"), Index("occurredAt")],
)
data class StockMovement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val materialId: Long,
    val type: String,
    val quantity: Double,
    val reason: String,
    val cultivationId: Long? = null,
    val notes: String = "",
    val occurredAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "shopping_items",
    foreignKeys = [ForeignKey(entity = Material::class, parentColumns = ["id"], childColumns = ["materialId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index(value = ["materialId"], unique = true)],
)
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val materialId: Long? = null,
    val checked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
