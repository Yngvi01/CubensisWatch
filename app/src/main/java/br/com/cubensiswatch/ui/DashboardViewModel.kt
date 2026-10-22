package br.com.cubensiswatch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.cubensiswatch.data.CultivationRepository
import br.com.cubensiswatch.data.MaterialRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(repository: CultivationRepository, materials: MaterialRepository) : ViewModel() {
    val activeCount = repository.activeCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val incubatingCount = repository.incubatingCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val problemCount = repository.problemCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val lowStockCount = materials.lowStockCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    class Factory(private val repository: CultivationRepository, private val materials: MaterialRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DashboardViewModel(repository, materials) as T
    }
}
