package br.com.cubensiswatch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.cubensiswatch.data.Reminder
import br.com.cubensiswatch.data.ReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReminderViewModel(private val repository: ReminderRepository) : ViewModel() {
    val reminders = repository.reminders
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    fun save(reminder: Reminder, done: () -> Unit) = viewModelScope.launch {
        _error.value = null
        runCatching { repository.save(reminder) }.onSuccess { done() }.onFailure { _error.value = it.message ?: "Não foi possível salvar o lembrete." }
    }
    fun setEnabled(reminder: Reminder, enabled: Boolean) = viewModelScope.launch {
        runCatching { repository.setEnabled(reminder, enabled) }.onFailure { _error.value = it.message ?: "Não foi possível atualizar o lembrete." }
    }
    fun delete(reminder: Reminder) = viewModelScope.launch { repository.delete(reminder) }
    fun clearError() { _error.value = null }
    class Factory(private val repository: ReminderRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = ReminderViewModel(repository) as T
    }
}
