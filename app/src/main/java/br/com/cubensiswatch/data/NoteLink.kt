package br.com.cubensiswatch.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Polymorphic local link to a cultivation, culture, lineage, or material. */
@Entity(
    tableName = "note_links",
    foreignKeys = [ForeignKey(entity = Note::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("noteId"), Index(value = ["noteId", "relatedType", "relatedId"], unique = true)],
)
data class NoteLink(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val relatedType: String,
    val relatedId: Long,
)
