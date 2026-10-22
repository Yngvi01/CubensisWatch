package br.com.cubensiswatch.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "note_photos",
    foreignKeys = [ForeignKey(
        entity = Note::class,
        parentColumns = ["id"],
        childColumns = ["noteId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("noteId")],
)
data class NotePhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val localPath: String,
    val caption: String = "",
    val capturedAt: Long = System.currentTimeMillis(),
)
