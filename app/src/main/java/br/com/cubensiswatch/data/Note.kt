package br.com.cubensiswatch.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "notes", indices = [Index("updatedAt")])
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val tags: String = "",
    val relatedType: String? = null,
    val relatedId: Long? = null,
    @ColumnInfo(defaultValue = "'Anotação livre'") val noteType: String = "Anotação livre",
    @ColumnInfo(defaultValue = "''") val hypothesis: String = "",
    @ColumnInfo(defaultValue = "''") val findings: String = "",
    val hypothesisOutcome: String? = null,
)
