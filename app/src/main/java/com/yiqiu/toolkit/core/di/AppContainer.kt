package com.yiqiu.toolkit.core.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.yiqiu.toolkit.core.storage.KeyValueStore
import com.yiqiu.toolkit.data.repository.InMemoryToolRepository
import com.yiqiu.toolkit.data.repository.PhraseLibraryRepository
import com.yiqiu.toolkit.data.repository.SwitchRecordRepository
import com.yiqiu.toolkit.data.repository.ToolRepository
import com.yiqiu.toolkit.data.storage.SharedPreferencesStore
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
    val phraseLibraryRepository: PhraseLibraryRepository
    val switchRecordRepository: SwitchRecordRepository
}

class DefaultAppContainer(context: Context) : AppContainer {

    /** Single persistence backend shared by every repository. */
    private val keyValueStore: KeyValueStore by lazy { SharedPreferencesStore(context) }

    override val toolRepository: ToolRepository by lazy { InMemoryToolRepository() }

    override val toolUseCases: ToolUseCases by lazy {
        ToolUseCases(
            getTools = GetToolsUseCase(toolRepository),
            getTool = GetToolUseCase(toolRepository),
            toggleFavorite = ToggleFavoriteUseCase(toolRepository),
        )
    }

    override val phraseLibraryRepository: PhraseLibraryRepository by lazy {
        PhraseLibraryRepository(keyValueStore)
    }

    override val switchRecordRepository: SwitchRecordRepository by lazy {
        SwitchRecordRepository(keyValueStore)
    }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer was not provided. Wrap the app content in ToolKitApp.")
}
