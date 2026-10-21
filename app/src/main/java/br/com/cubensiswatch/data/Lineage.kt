package br.com.cubensiswatch.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lineages")
data class Lineage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val species: String = "",
    val variety: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
