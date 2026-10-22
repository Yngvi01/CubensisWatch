package br.com.cubensiswatch.data

import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID

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
    fun observeCultureEvents(id: Long): Flow<List<CultureEvent>> = database.cultureEventDao().observeForCulture(id)
    fun observeCulturePhotos(id: Long): Flow<List<CulturePhoto>> = database.culturePhotoDao().observeForCulture(id)
    fun observeLineageCultivations(id: Long): Flow<List<Cultivation>> = database.cultivationDao().observeForLineage(id)

    suspend fun saveLineage(lineage: Lineage): Long {
        val name = lineage.name.trim()
        require(name.isNotEmpty()) { "Informe um nome para a linhagem." }
        val code = lineage.code.trim().uppercase().ifBlank { if (lineage.id == 0L) generateUniqueLineageCode() else "LIN-${lineage.id}" }
        require(!lineageDao.codeExists(code, lineage.id)) { "Este identificador de linhagem já está em uso." }
        return if (lineage.id == 0L) {
            lineageDao.insert(lineage.copy(name = name, code = code))
        } else {
            lineageDao.update(lineage.copy(name = name, code = code))
            lineage.id
        }
    }

    suspend fun saveCulture(culture: Culture): Long = database.withTransaction {
        val name = culture.name.trim()
        require(name.isNotEmpty()) { "Informe um nome para a cultura." }
        val code = culture.code.trim().uppercase().ifBlank { generateUniqueCultureCode() }
        require(!cultureDao.codeExists(code, culture.id)) { "Este identificador de amostra já está em uso." }
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
        val old = if (culture.id == 0L) null else checkNotNull(cultureDao.getById(culture.id)) { "A amostra não existe mais." }
        val normalized = culture.copy(name = name, code = code)
        if (culture.id == 0L) {
            val id = cultureDao.insert(normalized)
            val parent = culture.parentCultureId?.let { cultureDao.getById(it) }
            database.cultureEventDao().insert(
                CultureEvent(
                    cultureId = id,
                    type = if (parent == null) "Registro inicial" else "Transferência",
                    description = if (parent == null) "Amostra física registrada" else "Amostra derivada de ${parent.code} · ${parent.name}",
                    occurredAt = culture.createdAt,
                    status = culture.status,
                    sourceCultureCode = parent?.code,
                ),
            )
            id
        } else {
            val oldRecord = checkNotNull(old)
            cultureDao.update(normalized)
            if (oldRecord.status != culture.status) {
                database.cultureEventDao().insert(CultureEvent(cultureId = culture.id, type = "Status", description = "Status alterado de ${oldRecord.status} para ${culture.status}", status = culture.status))
            }
            if (oldRecord.lineageId != culture.lineageId) {
                val oldLineage = oldRecord.lineageId?.let { lineageDao.getById(it) }
                val newLineage = culture.lineageId?.let { lineageDao.getById(it) }
                database.cultureEventDao().insert(CultureEvent(cultureId = culture.id, type = "Linhagem genética", description = "Vínculo alterado de ${oldLineage?.code ?: "nenhum"} para ${newLineage?.code ?: "nenhum"}", status = culture.status))
            }
            if (oldRecord.name != culture.name || oldRecord.code != culture.code) {
                database.cultureEventDao().insert(CultureEvent(cultureId = culture.id, type = "Identificação", description = "Identificação alterada de ${oldRecord.code} · ${oldRecord.name} para ${culture.code} · ${culture.name}", status = culture.status))
            }
            if (oldRecord.parentCultureId != culture.parentCultureId) {
                val parent = culture.parentCultureId?.let { cultureDao.getById(it) }
                val previous = oldRecord.parentCultureId?.let { cultureDao.getById(it) }
                val description = when {
                    parent != null -> "Procedência alterada para ${parent.code} · ${parent.name}"
                    previous != null -> "Vínculo de procedência removido (origem anterior: ${previous.code})"
                    else -> "Amostra marcada como origem inicial"
                }
                database.cultureEventDao().insert(CultureEvent(cultureId = culture.id, type = "Procedência", description = description, status = culture.status, sourceCultureCode = parent?.code))
            }
            culture.id
        }
    }

    private suspend fun generateUniqueLineageCode(): String {
        var code: String
        do { code = "LIN-${UUID.randomUUID().toString().take(8).uppercase()}" } while (lineageDao.codeExists(code, 0))
        return code
    }
    private suspend fun generateUniqueCultureCode(): String {
        var code: String
        do { code = "CU-${UUID.randomUUID().toString().take(8).uppercase()}" } while (cultureDao.codeExists(code, 0))
        return code
    }

    suspend fun deleteCulture(culture: Culture) {
        val children = cultureDao.getChildren(culture.id)
        require(children.isEmpty()) { "Esta amostra tem derivados. Altere seu estado para Arquivado ou Descartado para preservar a procedência." }
        val cultivationChildren = database.cultivationDao().getFromCulture(culture.id)
        require(cultivationChildren.isEmpty()) { "Esta amostra está vinculada a cultivos. Arquive-a para preservar a procedência." }
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
