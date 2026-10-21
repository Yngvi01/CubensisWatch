package br.com.cubensiswatch.data

import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.io.File

class LineageRepository(
    private val database: AppDatabase,
    private val photoStore: LocalPhotoStore,
) {
    private val lineageDao = database.lineageDao()
    private val cultureDao = database.cultureDao()
    val lineages = lineageDao.observeAll()
    val cultures = cultureDao.observeAll()
    val activeCultureCount = database.cultureDao().observeActiveCount()
    val cultivations = database.cultivationDao().observeAll()

    fun observeLineage(id: Long): Flow<Lineage?> = lineageDao.observeById(id)
    fun observeCultures(id: Long): Flow<List<Culture>> = cultureDao.observeForLineage(id)
    fun observeCulture(id: Long): Flow<Culture?> = cultureDao.observeById(id)
    fun observeCulturePhotos(id: Long): Flow<List<CulturePhoto>> = database.culturePhotoDao().observeForCulture(id)
    fun observeLineageCultivations(id: Long): Flow<List<Cultivation>> = database.cultivationDao().observeForLineage(id)

    suspend fun saveLineage(lineage: Lineage): Long {
        val name = lineage.name.trim()
        require(name.isNotEmpty()) { "Informe um nome para a linhagem." }
        return if (lineage.id == 0L) {
            lineageDao.insert(lineage.copy(name = name, code = lineage.code.trim().ifBlank { "LIN-${(System.currentTimeMillis() % 1_000_000).toString().padStart(6, '0')}" }))
        } else {
            lineageDao.update(lineage.copy(name = name))
            lineage.id
        }
    }

    suspend fun saveCulture(culture: Culture): Long = database.withTransaction {
        val name = culture.name.trim()
        require(name.isNotEmpty()) { "Informe um nome para a cultura." }
        val lineageId = checkNotNull(culture.lineageId) { "Selecione a linhagem desta cultura." }
        checkNotNull(lineageDao.getById(lineageId)) { "A linhagem selecionada não existe mais." }
        if (culture.id != 0L) {
            val children = cultureDao.getChildren(culture.id)
            require(children.all { it.lineageId == lineageId }) { "Esta cultura possui derivados. Mantenha a mesma linhagem para preservar a árvore." }
            val cultivationChildren = database.cultivationDao().getFromCulture(culture.id)
            require(cultivationChildren.all { it.lineageId == null || it.lineageId == lineageId }) { "Esta cultura possui cultivos derivados de outra linhagem." }
        }
        culture.parentCultureId?.let { parentId ->
            val parent = checkNotNull(cultureDao.getById(parentId)) { "A cultura de origem não existe mais." }
            require(parent.lineageId == lineageId) { "A cultura de origem precisa pertencer à mesma linhagem." }
            var ancestor: Culture? = parent
            val visited = mutableSetOf<Long>()
            while (ancestor != null) {
                require(ancestor.id != culture.id || culture.id == 0L) { "A relação criaria um ciclo na árvore da linhagem." }
                if (!visited.add(ancestor.id)) error("A árvore atual já contém uma relação circular.")
                ancestor = ancestor.parentCultureId?.let { cultureDao.getById(it) }
            }
        }
        val normalized = culture.copy(name = name, code = culture.code.trim().ifBlank { "CU-${(System.currentTimeMillis() % 1_000_000).toString().padStart(6, '0')}" })
        if (culture.id == 0L) cultureDao.insert(normalized) else {
            cultureDao.update(normalized)
            culture.id
        }
    }

    suspend fun deleteCulture(culture: Culture) {
        val photos = database.culturePhotoDao().getForCulture(culture.id)
        cultureDao.delete(culture)
        photos.forEach { photoStore.delete(it.localPath) }
    }

    suspend fun addCulturePhotos(cultureId: Long, uris: List<Uri>, caption: String) {
        val files = mutableListOf<java.io.File>()
        try {
            uris.forEach { files += photoStore.import(it) }
            database.withTransaction {
                files.forEach { file ->
                    database.culturePhotoDao().insert(CulturePhoto(cultureId = cultureId, localPath = file.absolutePath, caption = caption))
                }
            }
        } catch (error: Throwable) {
            files.forEach { photoStore.delete(it.absolutePath) }
            throw error
        }
    }

    suspend fun deleteCulturePhoto(photo: CulturePhoto) {
        database.culturePhotoDao().delete(photo.id)
        photoStore.delete(photo.localPath)
    }

    suspend fun createCultureCaptureFile(): File = photoStore.createCaptureFile()

    suspend fun addCapturedCulturePhoto(cultureId: Long, file: File, caption: String) {
        try {
            database.culturePhotoDao().insert(CulturePhoto(cultureId = cultureId, localPath = file.absolutePath, caption = caption))
        } catch (error: Throwable) {
            photoStore.delete(file.absolutePath)
            throw error
        }
    }
}
