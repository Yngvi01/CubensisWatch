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
    val problemCount = dao.observeProblemCount()

    fun observeCultivation(id: Long) = dao.observeById(id)
    fun observeEvents(id: Long): Flow<List<CultivationEvent>> = database.eventDao().observeForCultivation(id)
    fun observeRecentEvents(limit: Int = 6): Flow<List<CultivationEvent>> = database.eventDao().observeRecent(limit)
    fun observePhotos(id: Long): Flow<List<Photo>> = database.photoDao().observeForCultivation(id)
    fun observeHarvests(id: Long): Flow<List<Harvest>> = database.harvestDao().observeForCultivation(id)
    fun observeHarvestPhotos(harvestId: Long): Flow<List<HarvestPhoto>> = database.harvestPhotoDao().observeForHarvest(harvestId)
    fun observeExperiment(id: Long): Flow<CultivationExperiment?> = database.cultivationExperimentDao().observeForCultivation(id)
    fun observeSimilarExperiments(id: Long): Flow<List<CultivationExperiment>> = database.cultivationExperimentDao().observeSimilar(id)

    suspend fun saveExperiment(experiment: CultivationExperiment) {
        database.withTransaction {
            checkNotNull(dao.getById(experiment.cultivationId)) { "Cultivo não encontrado." }
            database.cultivationExperimentDao().save(experiment.copy(
                plannedConditions = experiment.plannedConditions.trim(),
                observedConditions = experiment.observedConditions.trim(),
                materialsAndAmounts = experiment.materialsAndAmounts.trim(),
                plannedStageDurations = experiment.plannedStageDurations.trim(),
                actualStageDurations = experiment.actualStageDurations.trim(),
                result = experiment.result.trim(),
                notes = experiment.notes.trim(),
                updatedAt = System.currentTimeMillis(),
            ))
        }
    }

    suspend fun addHarvestPhotos(harvestId: Long, uris: List<Uri>, caption: String = "") {
        require(uris.isNotEmpty()) { "Selecione ao menos uma foto." }
        val dao = database.harvestPhotoDao()
        val files = mutableListOf<File>()
        try {
            uris.forEach { files += photoStore.import(it) }
            database.withTransaction {
                checkNotNull(database.harvestDao().getById(harvestId)) { "Colheita não encontrada." }
                files.forEach { file -> dao.insert(HarvestPhoto(harvestId = harvestId, localPath = file.absolutePath, caption = caption.trim())) }
            }
        } catch (error: Throwable) {
            files.forEach { photoStore.delete(it.absolutePath) }
            throw error
        }
    }

    suspend fun addCapturedHarvestPhoto(harvestId: Long, file: File) {
        try {
            database.withTransaction {
                checkNotNull(database.harvestDao().getById(harvestId)) { "Colheita não encontrada." }
                database.harvestPhotoDao().insert(HarvestPhoto(harvestId = harvestId, localPath = file.absolutePath))
            }
        } catch (error: Throwable) {
            photoStore.delete(file.absolutePath)
            throw error
        }
    }

    suspend fun deleteHarvestPhoto(photo: HarvestPhoto) {
        database.harvestPhotoDao().delete(photo.id)
        photoStore.delete(photo.localPath)
    }

    suspend fun addHarvest(cultivationId: Long, quantity: Double, unit: String, harvestedAt: Long, notes: String) {
        require(quantity > 0.0 && quantity.isFinite()) { "A quantidade da colheita deve ser maior que zero." }
        require(unit.isNotBlank()) { "Informe a unidade da colheita." }
        database.withTransaction {
            checkNotNull(dao.getById(cultivationId)) { "Cultivo não encontrado." }
            val number = database.harvestDao().nextNumber(cultivationId)
            database.harvestDao().insert(Harvest(cultivationId = cultivationId, number = number, harvestedAt = harvestedAt, quantity = quantity, unit = unit.trim(), notes = notes.trim()))
            database.eventDao().insert(CultivationEvent(cultivationId = cultivationId, type = "Colheita", description = "Colheita $number — ${quantity} ${unit.trim()}${notes.trim().takeIf(String::isNotEmpty)?.let { " · $it" }.orEmpty()}", occurredAt = harvestedAt, stage = "Colheita"))
        }
    }

    suspend fun deleteHarvest(harvest: Harvest) {
        val photos = database.harvestPhotoDao().getForHarvest(harvest.id)
        database.withTransaction {
            database.harvestDao().delete(harvest.id)
            database.eventDao().insert(CultivationEvent(cultivationId = harvest.cultivationId, type = "Nota", description = "Colheita ${harvest.number} removida do registro."))
        }
        photos.forEach { photoStore.delete(it.localPath) }
    }

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

    suspend fun addObservation(
        cultivationId: Long,
        type: String,
        text: String,
        temperature: Double?,
        humidity: Double?,
        photoUri: Uri?,
    ) {
        require(type == "Observação" || type == "Problema") { "Tipo de registro inválido." }
        require(text.isNotBlank() || temperature != null || humidity != null || photoUri != null) { "Adicione um texto, uma leitura ou uma foto." }
        val photoFile = photoUri?.let { photoStore.import(it) }
        try {
            database.withTransaction {
                val cultivation = checkNotNull(dao.getById(cultivationId)) { "Cultivo não encontrado." }
                val recordedAt = System.currentTimeMillis()
                val description = text.trim().ifBlank {
                    if (type == "Problema") "Problema registrado" else "Registro rápido"
                }
                val eventId = database.eventDao().insert(
                    CultivationEvent(
                        cultivationId = cultivationId,
                        type = type,
                        description = description,
                        occurredAt = recordedAt,
                        temperature = temperature,
                        humidity = humidity,
                        stage = cultivation.status,
                    ),
                )
                photoFile?.let { file ->
                    database.photoDao().insert(Photo(cultivationId = cultivationId, eventId = eventId, localPath = file.absolutePath, caption = text.trim(), capturedAt = recordedAt))
                }
            }
        } catch (error: Throwable) {
            photoFile?.let { photoStore.delete(it.absolutePath) }
            throw error
        }
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
            database.withTransaction {
                val cultivation = checkNotNull(dao.getById(cultivationId)) { "Cultivo não encontrado." }
                val recordedAt = System.currentTimeMillis()
                val eventId = database.eventDao().insert(
                    CultivationEvent(cultivationId = cultivationId, type = "Foto", description = if (caption.isBlank()) "${files.size} foto(s) adicionada(s)" else caption, occurredAt = recordedAt, stage = cultivation.status),
                )
                files.forEach { file -> database.photoDao().insert(Photo(cultivationId = cultivationId, eventId = eventId, localPath = file.absolutePath, caption = caption, capturedAt = recordedAt)) }
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
                val cultivation = checkNotNull(dao.getById(cultivationId)) { "Cultivo não encontrado." }
                val recordedAt = System.currentTimeMillis()
                val eventId = database.eventDao().insert(CultivationEvent(cultivationId = cultivationId, type = "Foto", description = caption.ifBlank { "Foto tirada" }, occurredAt = recordedAt, stage = cultivation.status))
                database.photoDao().insert(Photo(cultivationId = cultivationId, eventId = eventId, localPath = file.absolutePath, caption = caption, capturedAt = recordedAt))
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
