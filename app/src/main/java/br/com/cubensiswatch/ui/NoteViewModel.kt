package br.com.cubensiswatch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.net.Uri
import br.com.cubensiswatch.data.Note
import br.com.cubensiswatch.data.NotePhoto
import br.com.cubensiswatch.data.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun search(query: String) = repository.search(query)
    fun photos(noteId: Long) = repository.photos(noteId)
    fun clearError() { _error.value = null }

    fun save(note: Note, onSaved: () -> Unit = {}) = viewModelScope.launch {
        _error.value = null
        runCatching { repository.save(note) }
            .onSuccess { onSaved() }
            .onFailure { _error.value = it.message ?: "Não foi possível salvar a anotação." }
    }

    fun delete(note: Note) = viewModelScope.launch {
        _error.value = null
        runCatching { repository.delete(note) }
            .onFailure { _error.value = it.message ?: "Não foi possível excluir a anotação." }
    }

    fun addPhotos(note: Note, uris: List<Uri>, caption: String) = viewModelScope.launch {
        _error.value = null
        runCatching { repository.addPhotos(note.id, uris, caption) }
            .onFailure { _error.value = it.message ?: "Não foi possível importar as fotos." }
    }

    fun deletePhoto(photo: NotePhoto) = viewModelScope.launch {
        _error.value = null
        runCatching { repository.deletePhoto(photo) }
            .onFailure { _error.value = it.message ?: "Não foi possível excluir a foto." }
    }

    class Factory(private val repository: NoteRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = NoteViewModel(repository) as T
    }
}
