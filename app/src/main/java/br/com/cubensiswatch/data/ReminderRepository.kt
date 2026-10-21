package br.com.cubensiswatch.data

import android.content.Context
import br.com.cubensiswatch.notifications.ReminderScheduler
import kotlinx.coroutines.flow.Flow

class ReminderRepository(context: Context, private val dao: ReminderDao) {
    private val appContext = context.applicationContext
    val reminders: Flow<List<Reminder>> = dao.observeAll()

    suspend fun save(reminder: Reminder): Long {
        require(reminder.title.isNotBlank()) { "Informe o título do lembrete." }
        require(reminder.repeatIntervalMinutes == null || reminder.repeatIntervalMinutes >= 15) { "A repetição deve ser de pelo menos 15 minutos." }
        val clean = reminder.copy(title = reminder.title.trim(), description = reminder.description.trim())
        val id = if (clean.id == 0L) dao.insert(clean) else { dao.update(clean); clean.id }
        ReminderScheduler.schedule(appContext, clean.copy(id = id))
        return id
    }

    suspend fun delete(reminder: Reminder) {
        dao.delete(reminder.id)
        ReminderScheduler.cancel(appContext, reminder.id)
    }

    suspend fun setEnabled(reminder: Reminder, enabled: Boolean) = save(reminder.copy(enabled = enabled))
}
