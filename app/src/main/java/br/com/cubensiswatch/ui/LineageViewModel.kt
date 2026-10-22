package br.com.cubensiswatch.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.cubensiswatch.data.Culture
import br.com.cubensiswatch.data.CulturePhoto
import br.com.cubensiswatch.data.Lineage
import br.com.cubensiswatch.data.LineageRepository
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LineageViewModel(private val repository: LineageRepository) : ViewModel() {
    val lineages = repository.lineages
    val cultures = repository.cultures
    val cultivations = repository.cultivations
    val activeCultureCount = repository.activeCultureCount
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun lineage(id: Long) = repository.observeLineage(id)
    fun culturesForLineage(id: Long) = repository.observeCultures(id)
    fun cultivationsForLineage(id: Long) = repository.observeLineageCultivations(id)
    fun culture(id: Long) = repository.observeCulture(id)
    fun cultureEvents(id: Long) = repository.observeCultureEvents(id)
    fun culturePhotos(id: Long) = repository.observeCulturePhotos(id)
    fun clearError() { _error.value = null }
    fun reportError(message: String) { _error.value = message }

    fun saveLineage(lineage: Lineage, onSaved: (Long) -> Unit) = viewModelScope.launch {
        _error.value = null
        runCatching { repository.saveLineage(lineage) }
            .onSuccess(onSaved)
            .onFailure { _error.value = it.message ?: "Não foi possível salvar a linhagem." }
    }

    fun saveCulture(culture: Culture, onSaved: (Long) -> Unit) = viewModelScope.launch {
        _error.value = null
        runCatching { repository.saveCulture(culture) }
            .onSuccess(onSaved)
            .onFailure { _error.value = it.message ?: "Não foi possível salvar a cultura." }
    }

    fun deleteCulture(culture: Culture, onDeleted: () -> Unit) = viewModelScope.launch {
        _error.value = null
        runCatching { repository.deleteCulture(culture) }
            .onSuccess { onDeleted() }
            .onFailure { _error.value = it.message ?: "Não foi possível excluir a cultura." }
    }

    fun addCulturePhotos(id: Long, uris: List<Uri>, caption: String) = viewModelScope.launch {
        _error.value = null
        runCatching { repository.addCulturePhotos(id, uris, caption.trim()) }
            .onFailure { _error.value = it.message ?: "Não foi possível importar as fotos." }
    }

    fun deleteCulturePhoto(photo: CulturePhoto) = viewModelScope.launch {
        _error.value = null
        runCatching { repository.deleteCulturePhoto(photo) }
            .onFailure { _error.value = it.message ?: "Não foi possível excluir a foto." }
    }

    fun createCultureCaptureFile(onReady: (File?) -> Unit) = viewModelScope.launch {
        runCatching { repository.createCultureCaptureFile() }
            .onSuccess(onReady)
            .onFailure { _error.value = it.message ?: "Não foi possível preparar a câmera." }
    }

    fun addCapturedCulturePhoto(id: Long, file: File, caption: String) = viewModelScope.launch {
        runCatching { repository.addCapturedCulturePhoto(id, file, caption.trim()) }
            .onFailure { _error.value = it.message ?: "Não foi possível salvar a foto." }
    }

    class Factory(private val repository: LineageRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = LineageViewModel(repository) as T
    }
}
