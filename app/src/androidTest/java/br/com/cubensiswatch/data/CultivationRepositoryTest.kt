package br.com.cubensiswatch.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CultivationRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: CultivationRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = CultivationRepository(database, LocalPhotoStore(context))
    }

    @After
    fun tearDown() { database.close() }

    @Test
    fun createEditTimelineAndCascadeDelete() = runBlocking {
        val id = repository.save(Cultivation(code = "CT-001", name = "Bandeja de teste", species = "Pleurotus"))
        assertEquals("Bandeja de teste", database.cultivationDao().getById(id)?.name)
        assertEquals("Início", repository.observeEvents(id).first().single().type)

        repository.save(Cultivation(id = id, code = "CT-001", name = "Bandeja editada", species = "Pleurotus", status = "Incubação"))
        repository.addObservation(id, "Colonização avançando", 23.0, 78.0)
        assertEquals("Bandeja editada", database.cultivationDao().getById(id)?.name)
        assertEquals(listOf("Início", "Etapa", "Observação"), repository.observeEvents(id).first().map { it.type })

        val eventId = database.eventDao().insert(CultivationEvent(id = 0, cultivationId = id, type = "Foto", description = "Registro visual"))
        database.photoDao().insert(Photo(cultivationId = id, eventId = eventId, localPath = "/missing/test.jpg"))
        repository.deleteCultivation(checkNotNull(database.cultivationDao().getById(id)))
        assertNull(database.cultivationDao().getById(id))
        assertEquals(emptyList<CultivationEvent>(), repository.observeEvents(id).first())
        assertEquals(emptyList<Photo>(), repository.observePhotos(id).first())
    }

    @Test
    fun harvestsAreNumberedPersistedAndCascadeWithCultivation() = runBlocking {
        val id = repository.save(Cultivation(code = "CT-002", name = "Cultivo com colheitas"))
        repository.addHarvest(id, 120.0, "g", 2_000L, "Primeira leva")
        repository.addHarvest(id, 0.18, "kg", 3_000L, "Segunda leva")

        val harvests = repository.observeHarvests(id).first()
        assertEquals(listOf(1, 2), harvests.map { it.number })
        assertEquals(listOf(120.0, 0.18), harvests.map { it.quantity })
        assertEquals(listOf("Colheita", "Colheita"), repository.observeEvents(id).first().filter { it.type == "Colheita" }.map { it.type })

        repository.deleteCultivation(checkNotNull(database.cultivationDao().getById(id)))
        assertEquals(emptyList<Harvest>(), repository.observeHarvests(id).first())
    }
}
