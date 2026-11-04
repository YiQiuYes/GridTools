package com.yiqiu.toolkit.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
)

/** Generous, soft corners are a hallmark of the Material 3 Expressive look. */
private val ExpressiveShapes = Shapes(
    RoundedCornerShape(6.dp),   // extraSmall
    RoundedCornerShape(12.dp),  // small
    RoundedCornerShape(20.dp),  // medium
    RoundedCornerShape(28.dp),  // large
    RoundedCornerShape(36.dp),  // extraLarge
)

/**
 * Single source of truth for the app's visual language.
 *
 * Uses [MaterialExpressiveTheme] (Material 3 Expressive) so that every M3
 * component renders its expressive variant, and swaps in the expressive
 * [MotionScheme] for springy, physics-based motion. Dynamic color is used on
 * Android 12+, falling back to a static scheme on older devices.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ToolKitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = ExpressiveShapes,
        typography = Typography,
    ) {
        content()
    }
}
