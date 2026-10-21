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
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MaterialRepositoryTest {
    private val database by lazy {
        Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }
    @After fun tearDown() { database.close() }

    @Test fun tracksMovementsAndAutomaticallyMaintainsLowStockShoppingItem() = runBlocking {
        val repository = MaterialRepository(database)
        val id = repository.save(Material(name = "Vermiculita", quantity = 1.0, unit = "kg", minimum = 0.5, ideal = 2.0))
        assertEquals(0, repository.shopping.first().size)
        repository.move(StockMovement(materialId = id, type = "Saída", quantity = 0.7, reason = "Cultivo CT-01"))
        assertEquals(0.3, repository.material(id).first()!!.quantity, 0.0001)
        assertEquals(1, repository.lowStockCount.first())
        val shopping = repository.shopping.first().single()
        assertEquals("Vermiculita", shopping.name)
        repository.move(StockMovement(materialId = id, type = "Entrada", quantity = 2.0, reason = "Compra"))
        assertEquals(2.3, repository.material(id).first()!!.quantity, 0.0001)
        assertTrue(repository.shopping.first().isEmpty())
        assertEquals(2, repository.movements(id).first().size)
    }

    @Test fun refusesNegativeStockAndInvalidMinimumOrIdeal() = runBlocking {
        val repository = MaterialRepository(database)
        val id = repository.save(Material(name = "Luvas", quantity = 2.0, minimum = 1.0, ideal = 4.0))
        val negative = runCatching { repository.move(StockMovement(materialId = id, type = "Saída", quantity = 3.0, reason = "Teste")) }
        val badIdeal = runCatching { repository.save(Material(name = "Inválido", minimum = 3.0, ideal = 2.0)) }
        assertTrue(negative.isFailure)
        assertTrue(badIdeal.isFailure)
        assertEquals(2.0, repository.material(id).first()!!.quantity, 0.0)
        assertTrue(repository.movements(id).first().isEmpty())
    }
}
