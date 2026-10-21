package br.com.cubensiswatch.data

import androidx.room.withTransaction
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import java.io.File

class CultivationRepository(
    private val database: AppDatabase,
    private val photoStore: LocalPhotoStore,
) {
    private val dao = database.cultivationDao()
    val cultivations = dao.observeAll()
    val activeCount = dao.observeActiveCount()
    val incubatingCount = dao.observeIncubatingCount()

    fun observeCultivation(id: Long) = dao.observeById(id)
    fun observeEvents(id: Long): Flow<List<CultivationEvent>> = database.eventDao().observeForCultivation(id)
    fun observePhotos(id: Long): Flow<List<Photo>> = database.photoDao().observeForCultivation(id)

    suspend fun save(cultivation: Cultivation): Long = database.withTransaction {
        val sourceCulture = cultivation.sourceCultureId?.let { database.cultureDao().getById(it) }
        if (cultivation.sourceCultureId != null) checkNotNull(sourceCulture) { "A cultura de origem não existe mais." }
        val lineageId = cultivation.lineageId ?: sourceCulture?.lineageId
        val lineage = lineageId?.let { database.lineageDao().getById(it) }
        if (lineageId != null) checkNotNull(lineage) { "A linhagem selecionada não existe mais." }
        require(sourceCulture == null || sourceCulture.lineageId == lineageId) { "O cultivo e sua cultura de origem precisam pertencer à mesma linhagem." }
        val normalized = cultivation.copy(lineageId = lineageId, lineage = lineage?.name ?: cultivation.lineage)
        if (cultivation.id == 0L) {
            val id = dao.insert(normalized)
            database.eventDao().insert(
                CultivationEvent(
                    cultivationId = id,
                    type = "Início",
                    description = "Cultivo iniciado",
                    occurredAt = normalized.startedAt,
                    stage = normalized.status,
                ),
            )
            id
        } else {
            val old = dao.getById(cultivation.id)
            val saved = normalized.copy(
                completedAt = when {
                    cultivation.status == "Finalizado" -> cultivation.completedAt ?: System.currentTimeMillis()
                    old?.status == "Finalizado" -> null
                    else -> cultivation.completedAt
                },
            )
            dao.update(saved)
            if (old != null && old.status != saved.status) {
                database.eventDao().insert(
                    CultivationEvent(
                        cultivationId = saved.id,
                        type = "Etapa",
                        description = "Etapa alterada para ${saved.status}",
                        stage = saved.status,
                    ),
                )
            }
            saved.id
        }
    }

    suspend fun addObservation(cultivationId: Long, text: String, temperature: Double?, humidity: Double?) {
        database.eventDao().insert(
            CultivationEvent(
                cultivationId = cultivationId,
                type = "Observação",
                description = text,
                temperature = temperature,
                humidity = humidity,
            ),
        )
    }

    suspend fun changeStatus(cultivationId: Long, status: String) {
        val cultivation = checkNotNull(dao.getById(cultivationId)) { "Cultivo não encontrado." }
        database.withTransaction {
            dao.update(cultivation.copy(status = status, completedAt = if (status == "Finalizado") System.currentTimeMillis() else null))
            database.eventDao().insert(CultivationEvent(cultivationId = cultivationId, type = "Etapa", description = "Etapa alterada para $status", stage = status))
        }
    }

    suspend fun addPhotos(cultivationId: Long, uris: List<Uri>, caption: String = ""): List<File> {
        val files = mutableListOf<File>()
        try {
            uris.forEach { uri -> files += photoStore.import(uri) }
            val eventId = database.eventDao().insert(
                CultivationEvent(cultivationId = cultivationId, type = "Foto", description = if (caption.isBlank()) "${files.size} foto(s) adicionada(s)" else caption),
            )
            database.withTransaction {
                files.forEach { file -> database.photoDao().insert(Photo(cultivationId = cultivationId, eventId = eventId, localPath = file.absolutePath, caption = caption)) }
            }
            return files
        } catch (error: Throwable) {
            files.forEach { photoStore.delete(it.absolutePath) }
            throw error
        }
    }

    suspend fun createCaptureFile(): File = photoStore.createCaptureFile()

    suspend fun addCapturedPhoto(cultivationId: Long, file: File, caption: String = "") {
        try {
            database.withTransaction {
                val eventId = database.eventDao().insert(CultivationEvent(cultivationId = cultivationId, type = "Foto", description = caption.ifBlank { "Foto tirada" }))
                database.photoDao().insert(Photo(cultivationId = cultivationId, eventId = eventId, localPath = file.absolutePath, caption = caption))
            }
        } catch (error: Throwable) {
            photoStore.delete(file.absolutePath)
            throw error
        }
    }

    suspend fun deletePhoto(photo: Photo) {
        database.photoDao().delete(photo.id)
        photoStore.delete(photo.localPath)
    }

    suspend fun deleteCultivation(cultivation: Cultivation) {
        val photos = database.photoDao().getForCultivation(cultivation.id)
        dao.delete(cultivation)
        photos.forEach { photoStore.delete(it.localPath) }
    }
}
