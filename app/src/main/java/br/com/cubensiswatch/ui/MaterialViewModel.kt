package br.com.cubensiswatch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.cubensiswatch.data.Material
import br.com.cubensiswatch.data.MaterialRepository
import br.com.cubensiswatch.data.ShoppingItem
import br.com.cubensiswatch.data.StockMovement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MaterialViewModel(private val repository: MaterialRepository) : ViewModel() {
    val materials = repository.materials
    val lowCount = repository.lowStockCount
    val shopping = repository.shopping
    val cultivations = repository.cultivations
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    fun clearError() { _error.value = null }
    fun material(id: Long) = repository.material(id)
    fun movements(id: Long) = repository.movements(id)
    fun save(item: Material, done: (Long) -> Unit) = viewModelScope.launch {
        _error.value = null
        runCatching { repository.save(item) }.onSuccess(done).onFailure { _error.value = it.message ?: "Não foi possível salvar o material." }
    }
    fun move(item: StockMovement, done: () -> Unit = {}) = viewModelScope.launch {
        _error.value = null
        runCatching { repository.move(item) }.onSuccess { done() }.onFailure { _error.value = it.message ?: "Não foi possível movimentar o estoque." }
    }
    fun addShopping(name: String) = viewModelScope.launch { _error.value = null; runCatching { repository.addShopping(name) }.onFailure { _error.value = it.message ?: "Não foi possível adicionar à lista." } }
    fun check(item: ShoppingItem, checked: Boolean) = viewModelScope.launch { repository.setShoppingChecked(item, checked) }
    fun remove(item: ShoppingItem) = viewModelScope.launch { repository.removeShopping(item) }
    class Factory(private val repository: MaterialRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = MaterialViewModel(repository) as T
    }
}
