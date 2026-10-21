package br.com.cubensiswatch.ui

import android.net.Uri
import java.io.File
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.cubensiswatch.data.Cultivation
import br.com.cubensiswatch.data.CultivationRepository
import br.com.cubensiswatch.data.Photo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CultivationViewModel(private val repository: CultivationRepository) : ViewModel() {
    val cultivations = repository.cultivations
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun cultivation(id: Long) = repository.observeCultivation(id)
    fun events(id: Long) = repository.observeEvents(id)
    fun photos(id: Long) = repository.observePhotos(id)
    fun clearError() { _error.value = null }
    fun reportError(message: String) { _error.value = message }

    fun save(cultivation: Cultivation, onSaved: (Long) -> Unit) = viewModelScope.launch {
        _error.value = null
        val normalizedName = cultivation.name.trim()
        if (normalizedName.isEmpty()) {
            _error.value = "Informe um nome para o cultivo."
            return@launch
        }
        runCatching {
            val code = cultivation.code.trim().ifBlank { "CT-${(System.currentTimeMillis() % 1_000_000).toString().padStart(6, '0')}" }
            repository.save(cultivation.copy(code = code, name = normalizedName, species = cultivation.species.trim()))
        }.onSuccess(onSaved).onFailure { _error.value = it.message ?: "Não foi possível salvar o cultivo." }
    }

    fun changeStatus(id: Long, status: String) = viewModelScope.launch {
        _error.value = null
        runCatching { repository.changeStatus(id, status) }
            .onFailure { _error.value = it.message ?: "Não foi possível atualizar a etapa." }
    }

    fun addObservation(id: Long, text: String, temperature: String, humidity: String) = viewModelScope.launch {
        val normalizedText = text.trim()
        if (normalizedText.isEmpty()) {
            _error.value = "Escreva uma observação antes de salvar."
            return@launch
        }
        val parsedTemperature = temperature.trim().replace(',', '.').toDoubleOrNull()
        val parsedHumidity = humidity.trim().replace(',', '.').toDoubleOrNull()
        if (temperature.isNotBlank() && parsedTemperature == null || humidity.isNotBlank() && parsedHumidity == null) {
            _error.value = "Temperatura e umidade precisam ser números válidos."
            return@launch
        }
        if (parsedHumidity != null && parsedHumidity !in 0.0..100.0) {
            _error.value = "A umidade deve estar entre 0% e 100%."
            return@launch
        }
        _error.value = null
        runCatching { repository.addObservation(id, normalizedText, parsedTemperature, parsedHumidity) }
            .onFailure { _error.value = it.message ?: "Não foi possível salvar a observação." }
    }

    fun addPhotos(id: Long, uris: List<Uri>, caption: String) = viewModelScope.launch {
        _error.value = null
        runCatching { repository.addPhotos(id, uris, caption.trim()) }
            .onFailure { _error.value = it.message ?: "Não foi possível importar as fotos." }
    }

    fun createCaptureFile(onReady: (File?) -> Unit) = viewModelScope.launch {
        runCatching { repository.createCaptureFile() }
            .onSuccess(onReady)
            .onFailure { _error.value = it.message ?: "Não foi possível preparar a câmera." }
    }

    fun addCapturedPhoto(id: Long, file: File, caption: String) = viewModelScope.launch {
        runCatching { repository.addCapturedPhoto(id, file, caption.trim()) }
            .onFailure { _error.value = it.message ?: "Não foi possível salvar a foto." }
    }

    fun deletePhoto(photo: Photo) = viewModelScope.launch {
        _error.value = null
        runCatching { repository.deletePhoto(photo) }
            .onFailure { _error.value = it.message ?: "Não foi possível excluir a foto." }
    }

    fun delete(cultivation: Cultivation, onDeleted: () -> Unit) = viewModelScope.launch {
        _error.value = null
        runCatching { repository.deleteCultivation(cultivation) }
            .onSuccess { onDeleted() }
            .onFailure { _error.value = it.message ?: "Não foi possível excluir o cultivo." }
    }

    class Factory(private val repository: CultivationRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CultivationViewModel(repository) as T
    }
}
