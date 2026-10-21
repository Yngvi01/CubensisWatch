package br.com.cubensiswatch.data

import kotlinx.coroutines.flow.Flow

class IncubatorRepository(private val dao: IncubatorDao) {
    val incubators: Flow<List<Incubator>> = dao.observeAll()
    fun incubator(id: Long) = dao.observeById(id)
    fun readings(id: Long) = dao.observeReadings(id)

    suspend fun save(item: Incubator): Long {
        val name = item.name.trim()
        require(name.isNotEmpty()) { "Informe o nome da incubadora." }
        require(item.temperature == null || item.temperature in -20.0..100.0) { "Temperatura fora do intervalo válido." }
        require(item.humidity == null || item.humidity in 0.0..100.0) { "Umidade deve estar entre 0 e 100%." }
        val normalized = item.copy(name = name, code = item.code.trim().ifBlank { "INC-${(System.currentTimeMillis() % 1_000_000).toString().padStart(6, '0')}" })
        return if (item.id == 0L) dao.insert(normalized) else { dao.update(normalized); item.id }
    }

    suspend fun addReading(reading: IncubatorReading) {
        require(reading.temperature != null || reading.humidity != null || reading.notes.isNotBlank()) { "Informe temperatura, umidade ou uma observação." }
        require(reading.temperature == null || reading.temperature in -20.0..100.0) { "Temperatura fora do intervalo válido." }
        require(reading.humidity == null || reading.humidity in 0.0..100.0) { "Umidade deve estar entre 0 e 100%." }
        val incubator = checkNotNull(dao.getById(reading.incubatorId)) { "A incubadora selecionada não existe mais." }
        dao.insertReading(reading.copy(notes = reading.notes.trim()))
        dao.update(incubator.copy(
            temperature = reading.temperature ?: incubator.temperature,
            humidity = reading.humidity ?: incubator.humidity,
        ))
    }
}
