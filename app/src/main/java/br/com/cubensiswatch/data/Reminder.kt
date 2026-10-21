package br.com.cubensiswatch.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "reminders", indices = [Index("dueAt"), Index("enabled")])
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueAt: Long,
    val repeatIntervalMinutes: Long? = null,
    val enabled: Boolean = true,
)
