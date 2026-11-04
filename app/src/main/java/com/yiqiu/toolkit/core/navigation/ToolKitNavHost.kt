package com.yiqiu.toolkit.core.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.yiqiu.toolkit.data.model.ToolIds
import com.yiqiu.toolkit.feature.home.HomeScreen
import com.yiqiu.toolkit.feature.phraselibrary.PhraseLibraryScreen
import com.yiqiu.toolkit.feature.settings.SettingsScreen
import com.yiqiu.toolkit.feature.tools.ToolDetailPlaceholder
import com.yiqiu.toolkit.feature.tools.ToolDetailScreen
import com.yiqiu.toolkit.feature.tools.ToolListScreen
import com.yiqiu.toolkit.feature.tools.ToolRuntimeScreen

/**
 * Wires the app's destinations into Navigation 3.
 *
 * - `ListDetailSceneStrategy` turns the Tools list + detail into a single-pane
 *   flow on phones and a two-pane layout on tablets / unfolded foldables.
 * - `entryDecorators` scope a ViewModelStoreOwner and saveable state to each
 *   NavEntry, so per-screen state is retained and cleared with the entry.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ToolKitNavHost(
    navigationState: NavigationState,
    navigator: Navigator,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo)
            .copy(horizontalPartitionSpacerSize = 0.dp)
    }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

    val entryProvider = entryProvider<NavKey> {
        entry<HomeRoute> {
            HomeScreen(
                onOpenTool = navigator::openTool,
                onBrowseAll = { navigator.switchTo(TopLevelDestination.TOOLS) },
            )
        }

        entry<ToolsRoute>(
            metadata = ListDetailSceneStrategy.listPane(
                detailPlaceholder = { ToolDetailPlaceholder() },
            ),
        ) {
            ToolListScreen(
                onToolClick = { toolId -> navigator.navigate(ToolDetailRoute(toolId)) },
            )
        }

        entry<ToolDetailRoute>(
            metadata = ListDetailSceneStrategy.detailPane(),
        ) { key ->
            ToolDetailScreen(
                toolId = key.toolId,
                onBack = { navigator.goBack() },
                onRelatedTool = navigator::openTool,
                onLaunch = { navigator.openToolRuntime(key.toolId) },
                // Only tools with a maintenance surface expose the entry;
                // today that is the switching-operation phrase library.
                onMaintain = if (key.toolId == ToolIds.SwitchRecord) {
                    { navigator.openPhraseMaintenance() }
                } else {
                    null
                },
            )
        }

        entry<ToolRuntimeRoute> { key ->
            ToolRuntimeScreen(
                toolId = key.toolId,
                onBack = { navigator.goBack() },
            )
        }

        entry<PhraseMaintenanceRoute>(
            metadata = ListDetailSceneStrategy.detailPane(),
        ) {
            PhraseLibraryScreen(onBack = { navigator.goBack() })
        }

        entry<SettingsRoute> {
            SettingsScreen(
                dynamicColor = dynamicColor,
                onDynamicColorChange = onDynamicColorChange,
            )
        }
    }

    NavDisplay(
        backStack = navigationState.backStack,
        onBack = { navigator.goBack() },
        sceneStrategies = listOf(listDetailStrategy),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider,
        modifier = modifier,
    )
}
