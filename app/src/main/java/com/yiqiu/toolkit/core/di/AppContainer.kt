package com.yiqiu.toolkit.core.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.yiqiu.toolkit.data.repository.InMemoryToolRepository
import com.yiqiu.toolkit.data.repository.ToolRepository
import com.yiqiu.toolkit.domain.usecase.GetToolUseCase
import com.yiqiu.toolkit.domain.usecase.GetToolsUseCase
import com.yiqiu.toolkit.domain.usecase.ToggleFavoriteUseCase
import com.yiqiu.toolkit.domain.usecase.ToolUseCases

/**
 * Manual dependency-injection container (service locator).
 *
 * Kept deliberately free of a DI framework: dependencies are exposed through
 * an interface, constructed once per process and lazily, and consumed via
 * [LocalAppContainer]. Swapping to Hilt/Koin later only means providing a
 * different implementation of this interface.
 */
interface AppContainer {
    val toolRepository: ToolRepository
    val toolUseCases: ToolUseCases
}

class DefaultAppContainer(
    @Suppress("UNUSED_PARAMETER") context: Context,
) : AppContainer {

    override val toolRepository: ToolRepository by lazy { InMemoryToolRepository() }

    override val toolUseCases: ToolUseCases by lazy {
        ToolUseCases(
            getTools = GetToolsUseCase(toolRepository),
            getTool = GetToolUseCase(toolRepository),
            toggleFavorite = ToggleFavoriteUseCase(toolRepository),
        )
    }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer was not provided. Wrap the app content in ToolKitApp.")
}
