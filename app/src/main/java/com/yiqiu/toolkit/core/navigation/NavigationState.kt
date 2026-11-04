package com.yiqiu.toolkit.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Owns the app's navigation state.
 *
 * Each top-level destination gets its own [NavBackStack] created with
 * `rememberNavBackStack`, so tab history survives configuration changes and
 * process death. The selected tab is stored as a saveable String key. The
 * currently active stack is what `NavDisplay` renders.
 */
class NavigationState(
    val startDestination: TopLevelDestination,
    private val topLevelKey: MutableState<String>,
    val backStacks: Map<TopLevelDestination, NavBackStack<NavKey>>,
) {
    var topLevelDestination: TopLevelDestination
        get() = TopLevelDestination.valueOf(topLevelKey.value)
        private set(value) {
            topLevelKey.value = value.name
        }

    /** The observable back stack of the currently selected tab. */
    val backStack: NavBackStack<NavKey>
        get() = backStacks.getValue(topLevelDestination)

    internal fun switchTo(destination: TopLevelDestination) {
        topLevelDestination = destination
    }
}

@Composable
fun rememberNavigationState(
    startDestination: TopLevelDestination = TopLevelDestination.HOME,
): NavigationState {
    val topLevelKey = rememberSaveable { mutableStateOf(startDestination.name) }

    // One saveable back stack per tab. `associateWith` is inline, so the
    // `remember*` composable call inside its lambda is valid.
    val backStacks = TopLevelDestination.entries.associateWith { destination ->
        rememberNavBackStack(destination.route)
    }

    return remember(startDestination) {
        NavigationState(startDestination, topLevelKey, backStacks)
    }
}

/**
 * Single entry point for navigation mutations (unidirectional data flow):
 * the UI emits intents here, and the [NavigationState] is the only thing that
 * changes. Keeping this separate makes navigation testable in isolation.
 */
class Navigator(private val state: NavigationState) {

    private val _reselectEvents = MutableSharedFlow<TopLevelDestination>(extraBufferCapacity = 1)
    val reselectEvents = _reselectEvents.asSharedFlow()

    /** Switch tabs; re-selecting the current tab pops it back to its root. */
    fun switchTo(destination: TopLevelDestination) {
        if (destination == state.topLevelDestination) {
            _reselectEvents.tryEmit(destination)
            val stack = state.backStacks.getValue(destination)
            while (stack.size > 1) stack.removeLastOrNull()
        } else {
            state.switchTo(destination)
        }
    }

    /** Push a destination onto the current tab's back stack. */
    fun navigate(route: Route) {
        state.backStack.add(route)
    }

    /** Open a tool from anywhere: jump to the Tools tab, then show its detail. */
    fun openTool(toolId: String) {
        pushInTools(ToolDetailRoute(toolId))
    }

    /** Open a tool's working surface from anywhere: jump to the Tools tab. */
    fun openToolRuntime(toolId: String) {
        pushInTools(ToolRuntimeRoute(toolId))
    }

    /** Open the switching-operation phrase maintenance screen (Tools stack). */
    fun openPhraseMaintenance() {
        pushInTools(PhraseMaintenanceRoute)
    }

    /**
     * Push onto the Tools stack without resetting it: [switchTo] would pop the
     * stack back to its root when the tab is already selected, which would
     * throw away the detail entry we are navigating from.
     */
    private fun pushInTools(route: Route) {
        if (state.topLevelDestination != TopLevelDestination.TOOLS) {
            state.switchTo(TopLevelDestination.TOOLS)
        }
        state.backStacks.getValue(TopLevelDestination.TOOLS).add(route)
    }

    /**
     * @return true if the navigation was consumed, false when the Activity
     * should finish (i.e. we are already at the start destination's root).
     */
    fun goBack(): Boolean {
        val stack = state.backStack
        if (stack.size > 1) {
            stack.removeLastOrNull()
            return true
        }
        if (state.topLevelDestination != state.startDestination) {
            state.switchTo(state.startDestination)
            return true
        }
        return false
    }
}
