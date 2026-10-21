package br.com.cubensiswatch.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LineageRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var lineageRepository: LineageRepository
    private lateinit var cultivationRepository: CultivationRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val photoStore = LocalPhotoStore(context)
        lineageRepository = LineageRepository(database, photoStore)
        cultivationRepository = CultivationRepository(database, photoStore)
    }

    @After
    fun tearDown() { database.close() }

    @Test
    fun createsParentChildTreeAndRejectsCycle() = runBlocking {
        val lineageId = lineageRepository.saveLineage(Lineage(code = "", name = "Golden", species = "Agaricus"))
        val parentId = lineageRepository.saveCulture(Culture(code = "AG-001", name = "Ágar inicial", type = "Ágar", lineageId = lineageId))
        val childId = lineageRepository.saveCulture(Culture(code = "CL-001", name = "Cultura líquida", type = "Cultura líquida", lineageId = lineageId, parentCultureId = parentId))
        val cultivationId = cultivationRepository.save(Cultivation(code = "CT-001", name = "Cultivo teste", sourceCultureId = childId))

        assertEquals(parentId, database.cultureDao().getById(childId)?.parentCultureId)
        assertEquals(lineageId, database.cultivationDao().getById(cultivationId)?.lineageId)
        assertEquals(2, lineageRepository.observeCultures(lineageId).first().size)
        assertEquals(1, lineageRepository.observeLineageCultivations(lineageId).first().size)

        val cycleResult = runCatching {
            lineageRepository.saveCulture(database.cultureDao().getById(parentId)!!.copy(parentCultureId = childId))
        }
        assertTrue("A relação circular deveria ser rejeitada", cycleResult.isFailure)
    }
}
