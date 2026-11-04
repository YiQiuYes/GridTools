package com.yiqiu.toolkit.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.yiqiu.toolkit.ToolKitApplication
import com.yiqiu.toolkit.core.di.LocalAppContainer
import com.yiqiu.toolkit.core.navigation.Navigator
import com.yiqiu.toolkit.core.navigation.TopLevelDestination
import com.yiqiu.toolkit.core.navigation.ToolKitNavHost
import com.yiqiu.toolkit.core.navigation.rememberNavigationState
import com.yiqiu.toolkit.ui.theme.ToolKitTheme

/**
 * Root composable of the app.
 *
 * [NavigationSuiteScaffold] is the adaptive navigation container: it renders a
 * bottom bar on compact (phone) widths and a navigation rail on medium/expanded
 * (tablet, unfolded foldable, desktop) widths, choosing automatically from the
 * window adaptive info — no manual breakpoint handling required here.
 */
@Composable
fun ToolKitApp() {
    val appContainer = (LocalContext.current.applicationContext as ToolKitApplication).container

    // App-level state hoisted here so the theme and the Settings screen share
    // a single source of truth (unidirectional data flow).
    var dynamicColor by rememberSaveable { mutableStateOf(true) }

    val navigationState = rememberNavigationState()
    val navigator = remember { Navigator(navigationState) }

    ToolKitTheme(dynamicColor = dynamicColor) {
        CompositionLocalProvider(LocalAppContainer provides appContainer) {
            NavigationSuiteScaffold(
                navigationSuiteItems = {
                    TopLevelDestination.entries.forEach { destination ->
                        val isSelected = destination == navigationState.topLevelDestination
                        item(
                            selected = isSelected,
                            onClick = { navigator.switchTo(destination) },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) {
                                        destination.selectedIcon
                                    } else {
                                        destination.unselectedIcon
                                    },
                                    contentDescription = stringResource(destination.labelRes),
                                )
                            },
                            label = { Text(stringResource(destination.labelRes)) },
                        )
                    }
                },
            ) {
                ToolKitNavHost(
                    navigationState = navigationState,
                    navigator = navigator,
                    dynamicColor = dynamicColor,
                    onDynamicColorChange = { dynamicColor = it },
                )
            }
        }
    }
}
