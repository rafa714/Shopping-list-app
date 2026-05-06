package com.entgldb.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entgldb.app.db.ShoppingRepository
import com.entgldb.app.db.SyncStatus
import com.entgldb.app.models.ShoppingListItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingListViewModel : ViewModel() {

    val items: StateFlow<List<ShoppingListItem>> = ShoppingRepository.items
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val syncStatus: StateFlow<SyncStatus> = ShoppingRepository.syncStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncStatus.DISCONNECTED)

    fun addItem(name: String, quantity: String, category: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            ShoppingRepository.addItem(
                ShoppingListItem(
                    name     = name.trim(),
                    quantity = quantity.trim(),
                    category = category.trim().ifBlank { "General" }
                )
            )
        }
    }

    fun toggleChecked(id: String) {
        viewModelScope.launch { ShoppingRepository.toggleChecked(id) }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch { ShoppingRepository.deleteItem(id) }
    }
}
