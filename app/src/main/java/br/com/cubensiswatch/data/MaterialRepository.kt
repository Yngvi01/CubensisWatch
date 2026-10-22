package br.com.cubensiswatch.data

import androidx.room.withTransaction
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import java.io.File

class MaterialRepository(private val database: AppDatabase, private val photoStore: LocalPhotoStore) {
    private val dao = database.materialDao()
    val materials: Flow<List<Material>> = dao.observeAll()
    val lowStockCount = dao.observeLowCount()
    val shopping: Flow<List<ShoppingItem>> = dao.observeShopping()
    val cultivations: Flow<List<Cultivation>> = database.cultivationDao().observeAll()
    fun material(id: Long) = dao.observeById(id)
    fun movements(id: Long) = dao.observeMovements(id)
    fun photos(id: Long): Flow<List<MaterialPhoto>> = database.materialPhotoDao().observeForMaterial(id)

    suspend fun addPhotos(materialId: Long, uris: List<Uri>) {
        require(uris.isNotEmpty()) { "Selecione ao menos uma foto." }
        val files = mutableListOf<File>()
        try {
            uris.forEach { files += photoStore.import(it) }
            database.withTransaction {
                checkNotNull(dao.getById(materialId)) { "O material não existe mais." }
                files.forEach { file -> database.materialPhotoDao().insert(MaterialPhoto(materialId = materialId, localPath = file.absolutePath)) }
            }
        } catch (error: Throwable) {
            files.forEach { photoStore.delete(it.absolutePath) }
            throw error
        }
    }

    suspend fun deletePhoto(photo: MaterialPhoto) {
        database.materialPhotoDao().delete(photo.id)
        photoStore.delete(photo.localPath)
    }

    suspend fun save(material: Material): Long {
        require(material.name.isNotBlank()) { "Informe o nome do material." }
        require(material.quantity >= 0 && material.minimum >= 0 && material.ideal >= material.minimum) { "Confira as quantidades e o estoque ideal." }
        require(material.price == null || material.price >= 0) { "O preço não pode ser negativo." }
        return database.withTransaction {
            if (material.id == 0L) {
                val id = dao.insert(material.copy(name = material.name.trim()))
                syncShopping(material.copy(id = id, name = material.name.trim()))
                id
            } else {
                dao.update(material.copy(name = material.name.trim()))
                syncShopping(material)
                material.id
            }
        }
    }

    suspend fun move(movement: StockMovement) = database.withTransaction {
        require(if (movement.type == "Ajuste") movement.quantity >= 0 else movement.quantity > 0) {
            if (movement.type == "Ajuste") "A nova quantidade não pode ser negativa." else "A quantidade deve ser maior que zero."
        }
        require(movement.reason.isNotBlank()) { "Informe o motivo da movimentação." }
        val material = checkNotNull(dao.getById(movement.materialId)) { "O material não existe mais." }
        val delta = when (movement.type) {
            "Entrada" -> movement.quantity
            "Saída" -> -movement.quantity
            "Ajuste" -> movement.quantity - material.quantity
            else -> error("Tipo de movimentação inválido.")
        }
        val newQuantity = material.quantity + delta
        require(newQuantity >= 0) { "Estoque insuficiente. A saída foi cancelada." }
        dao.insertMovement(movement.copy(reason = movement.reason.trim(), notes = movement.notes.trim()))
        val updated = material.copy(quantity = newQuantity, lastPurchaseAt = if (movement.type == "Entrada") movement.occurredAt else material.lastPurchaseAt)
        dao.update(updated)
        syncShopping(updated)
    }

    suspend fun addShopping(name: String) {
        require(name.isNotBlank()) { "Informe o item da lista." }
        dao.insertShopping(ShoppingItem(name = name.trim()))
    }

    suspend fun setShoppingChecked(item: ShoppingItem, checked: Boolean) { dao.updateShopping(item.copy(checked = checked)) }
    suspend fun removeShopping(item: ShoppingItem) { dao.deleteShopping(item.id) }

    private suspend fun syncShopping(material: Material) {
        val existing = dao.shoppingForMaterial(material.id)
        if (material.quantity < material.minimum && existing == null) {
            dao.insertShopping(ShoppingItem(name = material.name, materialId = material.id))
        } else if (material.quantity >= material.minimum && existing != null && !existing.checked) {
            dao.deleteShopping(existing.id)
        }
    }
}
