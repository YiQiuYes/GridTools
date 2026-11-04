package com.yiqiu.toolkit.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yiqiu.toolkit.core.di.AppContainer
import com.yiqiu.toolkit.data.model.Tool
import com.yiqiu.toolkit.data.model.ToolCategory
import com.yiqiu.toolkit.domain.usecase.ToolUseCases
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Immutable snapshot of everything the Home screen needs to render. */
data class HomeUiState(
    val isLoading: Boolean = true,
    val tools: List<Tool> = emptyList(),
    val totalCount: Int = 0,
    val favoriteCount: Int = 0,
    val query: String = "",
    val selectedCategory: ToolCategory? = null,
)

/** All user intents, dispatched through [HomeViewModel.onEvent] (UDF). */
sealed interface HomeUiEvent {
    data class QueryChanged(val query: String) : HomeUiEvent
    data class CategorySelected(val category: ToolCategory?) : HomeUiEvent
    data class ToggleFavorite(val toolId: String) : HomeUiEvent
}

/** One-shot events (snackbars, navigation) kept out of [HomeUiState]. */
sealed interface HomeUiEffect {
    data class ShowMessage(val message: String) : HomeUiEffect
}

/**
 * MVI-style state holder. Exposes a single [StateFlow] of [HomeUiState] and a
 * stream of [HomeUiEffect]s; the UI never mutates state directly.
 */
class HomeViewModel(private val useCases: ToolUseCases) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _category = MutableStateFlow<ToolCategory?>(null)
    private val _effects = MutableSharedFlow<HomeUiEffect>(extraBufferCapacity = 1)
    val effects: kotlinx.coroutines.flow.SharedFlow<HomeUiEffect> = _effects.asSharedFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        useCases.getTools(),
        _query,
        _category,
    ) { tools, query, category ->
        val filtered = tools.filter { tool ->
            val matchesCategory = category == null || tool.category == category
            val matchesQuery = query.isBlank() ||
                tool.title.contains(query, ignoreCase = true) ||
                tool.description.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
        HomeUiState(
            isLoading = false,
            tools = filtered,
            totalCount = tools.size,
            favoriteCount = tools.count { it.isFavorite },
            query = query,
            selectedCategory = category,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.QueryChanged -> _query.value = event.query
            is HomeUiEvent.CategorySelected -> _category.value = event.category
            is HomeUiEvent.ToggleFavorite -> viewModelScope.launch {
                useCases.toggleFavorite(event.toolId)
                _effects.tryEmit(HomeUiEffect.ShowMessage("已更新收藏"))
            }
        }
    }

    /** Manual DI factory — no annotation processor required. */
    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(container.toolUseCases) as T
    }
}
