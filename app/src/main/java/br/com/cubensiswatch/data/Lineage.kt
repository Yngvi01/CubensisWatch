package br.com.cubensiswatch.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "lineages", indices = [Index(value = ["code"], unique = true)])
data class Lineage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val species: String = "",
    val variety: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
