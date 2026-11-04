package com.yiqiu.toolkit.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.graphics.shapes.RoundedPolygon
import com.yiqiu.toolkit.data.model.ToolCategory

/**
 * Shared visual identity for the tool catalogue.
 *
 * Material 3 Expressive is built on playful, soft "polymorphic" shapes and a
 * rich tonal palette. Each tool gets its own morphing badge shape and accent
 * tone, so the grid reads as a lively collection instead of a wall of
 * identical grey cards.
 */

/** Number of distinct badge/tone variants a tool can be seeded with. */
private const val VisualVariants = 12

/**
 * Stable visual seed for a tool, derived from its id so that newly registered
 * tools pick up a shape and tone without editing this file.
 */
fun visualIndexOf(toolId: String): Int {
    var hash = 0
    for (char in toolId) hash = hash * 31 + char.code
    return ((hash % VisualVariants) + VisualVariants) % VisualVariants
}

/** Icon shown for a tool, derived from the category it belongs to. */
fun iconFor(category: ToolCategory): ImageVector = when (category) {
    ToolCategory.RECORD -> Icons.AutoMirrored.Filled.Assignment
    ToolCategory.PRODUCTIVITY -> Icons.Filled.Bolt
    ToolCategory.CALCULATOR -> Icons.Filled.Calculate
    ToolCategory.CONVERTER -> Icons.Filled.SwapHoriz
    ToolCategory.UTILITY -> Icons.Filled.Widgets
}

/** A tonal colour triple derived from the active (possibly dynamic) scheme. */
data class ToolTone(
    val container: Color,
    val onContainer: Color,
    val accent: Color,
) {
    val badgeBrush: Brush
        get() = Brush.linearGradient(
            listOf(accent, lerp(accent, container, 0.65f)),
        )
}

@Composable
fun toolTone(index: Int): ToolTone {
    val scheme = MaterialTheme.colorScheme
    return when (index % 6) {
        0 -> ToolTone(scheme.primaryContainer, scheme.onPrimaryContainer, scheme.primary)
        1 -> ToolTone(scheme.secondaryContainer, scheme.onSecondaryContainer, scheme.secondary)
        2 -> ToolTone(scheme.tertiaryContainer, scheme.onTertiaryContainer, scheme.tertiary)
        3 -> ToolTone(
            lerp(scheme.primaryContainer, scheme.tertiaryContainer, 0.5f),
            scheme.onPrimaryContainer,
            lerp(scheme.primary, scheme.tertiary, 0.5f),
        )
        4 -> ToolTone(
            lerp(scheme.secondaryContainer, scheme.primaryContainer, 0.5f),
            scheme.onSecondaryContainer,
            lerp(scheme.secondary, scheme.primary, 0.5f),
        )
        else -> ToolTone(
            lerp(scheme.tertiaryContainer, scheme.secondaryContainer, 0.5f),
            scheme.onTertiaryContainer,
            lerp(scheme.tertiary, scheme.secondary, 0.5f),
        )
    }
}

/**
 * The Expressive shape library: cookies, clovers, bursts, gems… Every badge
 * uses a different polygon, which is the single most recognizable M3
 * Expressive motif.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val BadgePolygons: List<RoundedPolygon> = listOf(
    MaterialShapes.Cookie6Sided,
    MaterialShapes.Sunny,
    MaterialShapes.Clover4Leaf,
    MaterialShapes.Gem,
    MaterialShapes.Pentagon,
    MaterialShapes.SoftBurst,
    MaterialShapes.Puffy,
    MaterialShapes.Cookie9Sided,
    MaterialShapes.Diamond,
    MaterialShapes.Arch,
    MaterialShapes.Pill,
    MaterialShapes.Cookie4Sided,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun toolBadgeShape(index: Int): Shape =
    BadgePolygons[index % BadgePolygons.size].toShape()

/** Large decorative polygons used behind the hero header. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun heroDecorationShape(): Shape = MaterialShapes.Cookie12Sided.toShape()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun heroAccentShape(): Shape = MaterialShapes.SoftBoom.toShape()
