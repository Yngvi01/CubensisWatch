package br.com.cubensiswatch.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IncubatorRepositoryTest {
    private val database by lazy {
        Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After fun tearDown() { database.close() }

    @Test fun savesIncubatorAndManualEnvironmentalHistory() = runBlocking {
        val repository = IncubatorRepository(database.incubatorDao())
        val incubatorId = repository.save(Incubator(name = "Caixa de teste"))
        repository.addReading(IncubatorReading(incubatorId = incubatorId, temperature = 23.5, humidity = 68.0, notes = "Leitura manual"))
        val updated = repository.incubator(incubatorId).first()!!
        assertEquals(23.5, updated.temperature!!, 0.0)
        assertEquals(68.0, updated.humidity!!, 0.0)
        val readings = repository.readings(incubatorId).first()
        assertEquals(1, readings.size)
        assertEquals("Leitura manual", readings.first().notes)
    }

    @Test fun rejectsInvalidHumidityAndEmptyReading() = runBlocking {
        val repository = IncubatorRepository(database.incubatorDao())
        val incubatorId = repository.save(Incubator(name = "Caixa de teste"))
        val empty = runCatching { repository.addReading(IncubatorReading(incubatorId = incubatorId)) }
        val invalid = runCatching { repository.addReading(IncubatorReading(incubatorId = incubatorId, humidity = 120.0)) }
        assertEquals(true, empty.isFailure)
        assertEquals(true, invalid.isFailure)
    }
}
