package com.yiqiu.toolkit.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.yiqiu.toolkit.R
import kotlinx.serialization.Serializable

/**
 * Type-safe, serializable navigation keys for Navigation 3.
 *
 * `@Serializable` + [NavKey] lets `rememberNavBackStack` persist the back stack
 * across configuration changes and process death. A sealed hierarchy keeps all
 * destinations in one discoverable place.
 */
@Serializable
sealed interface Route : NavKey

@Serializable
data object HomeRoute : Route

/** The list pane of the tools list-detail flow. */
@Serializable
data object ToolsRoute : Route

/** The detail pane of the tools list-detail flow. */
@Serializable
data class ToolDetailRoute(val toolId: String) : Route

/** Full-screen working surface of an implemented tool. */
@Serializable
data class ToolRuntimeRoute(
    val toolId: String,
    /** When set, the runtime loads and updates this persisted record. */
    val recordId: String? = null,
) : Route

/** Maintenance screen for the switching-operation phrase library. */
@Serializable
data object PhraseMaintenanceRoute : Route

/** Saved switching-operation records (历史记录). */
@Serializable
data object SwitchRecordHistoryRoute : Route

@Serializable
data object SettingsRoute : Route

/**
 * The three top-level destinations shown by the adaptive NavigationSuite
 * (bottom bar on phones, navigation rail on tablets/foldables/desktops).
 */
enum class TopLevelDestination(
    @param:StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: Route,
) {
    HOME(R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, HomeRoute),
    TOOLS(R.string.nav_tools, Icons.Filled.Build, Icons.Outlined.Build, ToolsRoute),
    SETTINGS(R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings, SettingsRoute),
}
