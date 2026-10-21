package br.com.cubensiswatch.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import br.com.cubensiswatch.MainActivity
import br.com.cubensiswatch.data.AppDatabase
import br.com.cubensiswatch.data.Reminder
import java.util.concurrent.TimeUnit

object LocalNotifications {
    const val CHANNEL_REMINDERS = "cultivation_reminders"
    const val CHANNEL_STOCK = "stock_alerts"
    const val REMINDER_ID = "reminder_id"

    fun initialize(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL_REMINDERS, "Lembretes", NotificationManager.IMPORTANCE_DEFAULT).apply { description = "Lembretes locais do Cubensis Watch" })
            manager.createNotificationChannel(NotificationChannel(CHANNEL_STOCK, "Alertas de estoque", NotificationManager.IMPORTANCE_DEFAULT).apply { description = "Avisos quando materiais atingem o estoque mínimo" })
        }
    }

    fun hasPermission(context: Context): Boolean = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(context: Context, channel: String, notificationId: Int, title: String, text: String) {
        initialize(context)
        if (!hasPermission(context)) return
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}

object ReminderScheduler {
    fun schedule(context: Context, reminder: Reminder) {
        val manager = WorkManager.getInstance(context)
        if (!reminder.enabled) { manager.cancelUniqueWork(workName(reminder.id)); return }
        val input = Data.Builder().putLong(LocalNotifications.REMINDER_ID, reminder.id).build()
        val delay = (reminder.dueAt - System.currentTimeMillis()).coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(input)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        manager.enqueueUniqueWork(workName(reminder.id), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context, reminderId: Long) = WorkManager.getInstance(context).cancelUniqueWork(workName(reminderId))
    private fun workName(id: Long) = "reminder-$id"
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getLong(LocalNotifications.REMINDER_ID, -1L)
        if (id < 0) return Result.failure()
        val database = AppDatabase.getInstance(applicationContext)
        val reminder = database.reminderDao().getById(id) ?: return Result.success()
        if (!reminder.enabled) return Result.success()
        val now = System.currentTimeMillis()
        if (reminder.dueAt > now) { ReminderScheduler.schedule(applicationContext, reminder); return Result.success() }
        LocalNotifications.show(applicationContext, LocalNotifications.CHANNEL_REMINDERS, id.toInt(), reminder.title, reminder.description.ifBlank { "Lembrete agendado no Cubensis Watch." })
        val nextDue = reminder.repeatIntervalMinutes?.takeIf { it > 0 }?.let { interval ->
            var next = reminder.dueAt + TimeUnit.MINUTES.toMillis(interval)
            while (next <= now) next += TimeUnit.MINUTES.toMillis(interval)
            next
        }
        if (nextDue == null) database.reminderDao().update(reminder.copy(enabled = false))
        else {
            val nextReminder = reminder.copy(dueAt = nextDue)
            database.reminderDao().update(nextReminder)
            ReminderScheduler.schedule(applicationContext, nextReminder)
        }
        return Result.success()
    }
}

class StockAlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val low = AppDatabase.getInstance(applicationContext).materialDao().lowMaterials()
        if (low.isNotEmpty()) {
            val names = low.take(5).joinToString(", ") { "${it.name} (${it.quantity} ${it.unit})" }
            val extra = if (low.size > 5) " e mais ${low.size - 5}" else ""
            LocalNotifications.show(applicationContext, LocalNotifications.CHANNEL_STOCK, 88001, "Estoque abaixo do mínimo", "$names$extra. Confira sua lista de compras.")
        }
        return Result.success()
    }
}

object StockAlertScheduler {
    fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "check-stock-now",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<StockAlertWorker>().build(),
        )
        val request = PeriodicWorkRequestBuilder<StockAlertWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("daily-stock-alert", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
