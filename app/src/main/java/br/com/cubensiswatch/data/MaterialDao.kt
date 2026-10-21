package br.com.cubensiswatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialDao {
    @Query("SELECT * FROM materials ORDER BY name COLLATE NOCASE") fun observeAll(): Flow<List<Material>>
    @Query("SELECT * FROM materials WHERE id = :id") fun observeById(id: Long): Flow<Material?>
    @Query("SELECT * FROM materials WHERE id = :id") suspend fun getById(id: Long): Material?
    @Query("SELECT COUNT(*) FROM materials WHERE quantity < minimum") fun observeLowCount(): Flow<Int>
    @Query("SELECT * FROM materials WHERE quantity < minimum ORDER BY name COLLATE NOCASE") suspend fun lowMaterials(): List<Material>
    @Query("SELECT * FROM stock_movements WHERE materialId = :id ORDER BY occurredAt DESC") fun observeMovements(id: Long): Flow<List<StockMovement>>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(item: Material): Long
    @Update suspend fun update(item: Material)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertMovement(item: StockMovement): Long
    @Query("SELECT * FROM shopping_items WHERE materialId = :id LIMIT 1") suspend fun shoppingForMaterial(id: Long): ShoppingItem?
    @Query("SELECT * FROM shopping_items ORDER BY checked, createdAt DESC") fun observeShopping(): Flow<List<ShoppingItem>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertShopping(item: ShoppingItem): Long
    @Update suspend fun updateShopping(item: ShoppingItem)
    @Query("DELETE FROM shopping_items WHERE id = :id") suspend fun deleteShopping(id: Long)
}
