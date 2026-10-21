package br.com.cubensiswatch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.cubensiswatch.data.Incubator
import br.com.cubensiswatch.data.IncubatorReading
import br.com.cubensiswatch.data.IncubatorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IncubatorViewModel(private val repository: IncubatorRepository) : ViewModel() {
    val incubators = repository.incubators
    fun incubator(id: Long) = repository.incubator(id)
    fun readings(id: Long) = repository.readings(id)
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    fun clearError() { _error.value = null }
    fun reportError(message: String) { _error.value = message }
    fun save(item: Incubator, done: (Long) -> Unit) = viewModelScope.launch {
        _error.value = null
        runCatching { repository.save(item) }.onSuccess(done).onFailure { _error.value = it.message ?: "Não foi possível salvar." }
    }
    fun addReading(item: IncubatorReading) = viewModelScope.launch {
        _error.value = null
        runCatching { repository.addReading(item) }.onFailure { _error.value = it.message ?: "Não foi possível registrar." }
    }
    class Factory(private val repository: IncubatorRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = IncubatorViewModel(repository) as T
    }
}
