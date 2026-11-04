package com.yiqiu.toolkit.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material.icons.filled.Timer
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

/**
 * Shared visual identity for the tool catalogue.
 *
 * Material 3 Expressive is built on playful, soft "polymorphic" shapes and a
 * rich tonal palette. Each tool gets its own icon, morphing badge shape and
 * accent tone, so the grid reads as a lively collection instead of a wall of
 * identical grey cards.
 */

/** Stable display order — also used as the seed for a tool's shape/tone. */
private val ToolVisualOrder = listOf(
    "qr", "unit", "calc", "color", "countdown", "password",
    "text", "base", "tip", "date", "case", "hash",
)

fun visualIndexOf(toolId: String): Int =
    ToolVisualOrder.indexOf(toolId).let { if (it >= 0) it else 0 }

fun iconFor(toolId: String): ImageVector = when (toolId) {
    "qr" -> Icons.Filled.QrCode
    "unit" -> Icons.Filled.SwapHoriz
    "calc" -> Icons.Filled.Calculate
    "color" -> Icons.Filled.Palette
    "countdown" -> Icons.Filled.Timer
    "password" -> Icons.Filled.Password
    "text" -> Icons.Filled.TextFields
    "base" -> Icons.Filled.Numbers
    "tip" -> Icons.Filled.Paid
    "date" -> Icons.Filled.Event
    "case" -> Icons.Filled.TextFormat
    "hash" -> Icons.Filled.Fingerprint
    else -> Icons.Filled.Build
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
