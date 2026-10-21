package br.com.cubensiswatch

import android.app.Application
import br.com.cubensiswatch.data.AppDatabase
import br.com.cubensiswatch.data.CultivationRepository
import br.com.cubensiswatch.data.LocalPhotoStore
import br.com.cubensiswatch.data.LineageRepository
import br.com.cubensiswatch.data.IncubatorRepository
import br.com.cubensiswatch.data.MaterialRepository
import br.com.cubensiswatch.data.ReminderRepository
import br.com.cubensiswatch.notifications.LocalNotifications
import br.com.cubensiswatch.notifications.ReminderScheduler
import br.com.cubensiswatch.notifications.StockAlertScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CubensisWatchApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val photoStore by lazy { LocalPhotoStore(this) }
    val cultivationRepository by lazy { CultivationRepository(database, photoStore) }
    val lineageRepository by lazy { LineageRepository(database, photoStore) }
    val incubatorRepository by lazy { IncubatorRepository(database.incubatorDao()) }
    val materialRepository by lazy { MaterialRepository(database) }
    val reminderRepository by lazy { ReminderRepository(this, database.reminderDao()) }

    override fun onCreate() {
        super.onCreate()
        LocalNotifications.initialize(this)
        StockAlertScheduler.schedule(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            database.reminderDao().observeAll().first().filter { it.enabled }.forEach { ReminderScheduler.schedule(this@CubensisWatchApp, it) }
        }
    }
}
