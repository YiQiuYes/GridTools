package com.yiqiu.toolkit.domain.usecase

import com.yiqiu.toolkit.data.model.Tool
import com.yiqiu.toolkit.data.repository.ToolRepository
import kotlinx.coroutines.flow.Flow

/** Observe the full tool catalogue. */
class GetToolsUseCase(private val repository: ToolRepository) {
    operator fun invoke(): Flow<List<Tool>> = repository.observeTools()
}

/** Observe a single tool (used by the detail pane / scoped ViewModel). */
class GetToolUseCase(private val repository: ToolRepository) {
    operator fun invoke(id: String): Flow<Tool?> = repository.observeTool(id)
}

/** Flip a tool's favorite flag. */
class ToggleFavoriteUseCase(private val repository: ToolRepository) {
    suspend operator fun invoke(id: String) = repository.toggleFavorite(id)
}

/**
 * Aggregates the feature's use cases into one injectable dependency. Adding a
 * new operation means adding one class here, not threading repositories into
 * every ViewModel.
 */
data class ToolUseCases(
    val getTools: GetToolsUseCase,
    val getTool: GetToolUseCase,
    val toggleFavorite: ToggleFavoriteUseCase,
)
