package com.yiqiu.toolkit.feature.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowSizeClass
import com.yiqiu.toolkit.R
import com.yiqiu.toolkit.core.di.LocalAppContainer
import com.yiqiu.toolkit.data.model.Tool
import com.yiqiu.toolkit.data.model.ToolCategory
import com.yiqiu.toolkit.ui.theme.heroAccentShape
import com.yiqiu.toolkit.ui.theme.heroDecorationShape
import com.yiqiu.toolkit.ui.theme.iconFor
import com.yiqiu.toolkit.ui.theme.toolBadgeShape
import com.yiqiu.toolkit.ui.theme.toolTone
import com.yiqiu.toolkit.ui.theme.visualIndexOf

/**
 * Home dashboard — the expressive showpiece.
 *
 * Responsive: [GridCells.Adaptive] grows from one column on phones to several
 * on tablets / unfolded foldables without manual breakpoint logic. Each card
 * carries its own soft polygon badge and tonal accent, and grid items animate
 * their position when the category filter changes.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    onOpenTool: (String) -> Unit,
    onBrowseAll: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(LocalAppContainer.current),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
    )

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HomeUiEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.home_greeting),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.home_title),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                actions = {
                    FilledTonalIconButton(onClick = onBrowseAll) {
                        Icon(Icons.Filled.GridView, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 200.dp),
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                HeroBanner(
                    toolCount = uiState.tools.size,
                    favoriteCount = uiState.favoriteCount,
                    featuredTool = uiState.tools.firstOrNull { it.isFavorite }
                        ?: uiState.tools.firstOrNull(),
                    onOpenFeatured = onOpenTool,
                    onRandomTool = { uiState.tools.randomOrNull()?.let { onOpenTool(it.id) } },
                    onBrowseAll = onBrowseAll,
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(Modifier.height(8.dp))
                SearchField(
                    query = uiState.query,
                    onQueryChange = { viewModel.onEvent(HomeUiEvent.QueryChanged(it)) },
                    onClear = { viewModel.onEvent(HomeUiEvent.QueryChanged("")) },
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                CategoryFilterRow(
                    selected = uiState.selectedCategory,
                    onSelect = { viewModel.onEvent(HomeUiEvent.CategorySelected(it)) },
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(count = uiState.tools.size)
            }

            items(uiState.tools, key = { it.id }) { tool ->
                ToolCard(
                    tool = tool,
                    modifier = Modifier.animateItem(),
                    onClick = { onOpenTool(tool.id) },
                    onFavorite = { viewModel.onEvent(HomeUiEvent.ToggleFavorite(tool.id)) },
                )
            }

            if (uiState.tools.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        catalogueEmpty = uiState.totalCount == 0,
                        onReset = {
                            viewModel.onEvent(HomeUiEvent.QueryChanged(""))
                            viewModel.onEvent(HomeUiEvent.CategorySelected(null))
                        },
                    )
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* Hero                                                                       */
/* -------------------------------------------------------------------------- */

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HeroBanner(
    toolCount: Int,
    favoriteCount: Int,
    featuredTool: Tool?,
    onOpenFeatured: (String) -> Unit,
    onRandomTool: () -> Unit,
    onBrowseAll: () -> Unit,
) {
    val shape = MaterialTheme.shapes.extraLarge
    val scheme = MaterialTheme.colorScheme
    val heroBrush = Brush.linearGradient(
        listOf(
            scheme.primaryContainer,
            lerp(scheme.primaryContainer, scheme.tertiaryContainer, 0.55f),
            scheme.tertiaryContainer,
        ),
    )
    val decoShape = heroDecorationShape()
    val accentShape = heroAccentShape()
    // On wide screens there is room for a featured card beside the copy.
    val isWide = currentWindowAdaptiveInfoV2().windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp)
            .clip(shape)
            .background(heroBrush)
            .padding(24.dp),
    ) {
        // Oversized soft polygon bleeding off the top-right corner.
        Box(
            modifier = Modifier
                .size(190.dp)
                .align(Alignment.TopEnd)
                .offset(x = 62.dp, y = (-62).dp)
                .rotate(18f)
                .clip(decoShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            scheme.primary.copy(alpha = 0.38f),
                            scheme.primary.copy(alpha = 0.08f),
                        ),
                    ),
                ),
        )
        // Smaller accent polygon at the bottom-right.
        Box(
            modifier = Modifier
                .size(92.dp)
                .align(Alignment.BottomEnd)
                .offset(x = (-8).dp, y = 26.dp)
                .rotate(-14f)
                .clip(accentShape)
                .background(scheme.tertiary.copy(alpha = 0.32f)),
        )
        // Scattered dots add depth without competing with the content.
        Box(
            modifier = Modifier
                .size(18.dp)
                .align(Alignment.CenterEnd)
                .offset(x = (-150).dp, y = (-46).dp)
                .clip(CircleShape)
                .background(scheme.secondary.copy(alpha = 0.35f)),
        )
        Box(
            modifier = Modifier
                .size(11.dp)
                .align(Alignment.CenterEnd)
                .offset(x = (-110).dp, y = 54.dp)
                .clip(CircleShape)
                .background(scheme.primary.copy(alpha = 0.28f)),
        )

        Column(modifier = Modifier.fillMaxWidth(if (isWide) 0.56f else 1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = scheme.onPrimaryContainer,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.home_hero_eyebrow),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onPrimaryContainer,
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.home_hero_title),
                style = MaterialTheme.typography.headlineLarge,
                color = scheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onPrimaryContainer.copy(alpha = 0.82f),
            )

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill(
                    icon = Icons.Filled.Widgets,
                    text = stringResource(R.string.home_tools_count, toolCount),
                )
                StatPill(
                    icon = Icons.Filled.Favorite,
                    text = stringResource(R.string.home_favorites_count, favoriteCount),
                )
            }

            Spacer(Modifier.height(18.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(onClick = onRandomTool, enabled = toolCount > 0) {
                    Icon(Icons.Filled.Bolt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.home_random))
                }
                FilledTonalIconButton(onClick = onBrowseAll) {
                    Icon(Icons.Filled.GridView, contentDescription = stringResource(R.string.home_browse_all))
                }
            }
        }

        if (isWide && featuredTool != null) {
            FeaturedCard(
                tool = featuredTool,
                onClick = { onOpenFeatured(featuredTool.id) },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(250.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FeaturedCard(
    tool: Tool,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val index = visualIndexOf(tool.id)
    val tone = toolTone(index)
    val badgeShape = toolBadgeShape(index)
    val scheme = MaterialTheme.colorScheme

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = scheme.surface,
        ),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = scheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.home_featured),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurface,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(badgeShape)
                        .background(tone.badgeBrush),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = iconFor(tool.category),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = tone.onContainer,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = tool.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = tool.category.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_featured_action),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = scheme.primary,
                )
            }
        }
    }
}

@Composable
private fun StatPill(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/* -------------------------------------------------------------------------- */
/* Search + filters                                                           */
/* -------------------------------------------------------------------------- */

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.home_search)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.home_clear_search))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(50),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun CategoryFilterRow(
    selected: ToolCategory?,
    onSelect: (ToolCategory?) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            CategoryChip(
                label = stringResource(R.string.home_all),
                icon = Icons.Filled.Apps,
                selected = selected == null,
                onClick = { onSelect(null) },
            )
        }
        lazyRowItems(ToolCategory.entries.toList()) { category ->
            CategoryChip(
                label = category.label,
                icon = categoryIcon(category),
                selected = selected == category,
                onClick = { onSelect(category) },
            )
        }
    }
}

private fun categoryIcon(category: ToolCategory): ImageVector = when (category) {
    ToolCategory.RECORD -> Icons.AutoMirrored.Filled.Assignment
    ToolCategory.PRODUCTIVITY -> Icons.Filled.Bolt
    ToolCategory.CALCULATOR -> Icons.Filled.Calculate
    ToolCategory.CONVERTER -> Icons.Filled.SwapHoriz
    ToolCategory.UTILITY -> Icons.Filled.Widgets
}

@Composable
private fun CategoryChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

@Composable
private fun SectionHeader(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_section_all),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.home_section_count, count),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/* -------------------------------------------------------------------------- */
/* Tool card                                                                  */
/* -------------------------------------------------------------------------- */

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ToolCard(
    tool: Tool,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
) {
    val index = visualIndexOf(tool.id)
    val tone = toolTone(index)
    val badgeShape = toolBadgeShape(index)
    val scheme = MaterialTheme.colorScheme

    // Springy pop when a tool is (un)favorited.
    val favoriteScale by animateFloatAsState(
        targetValue = if (tool.isFavorite) 1.18f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "favoriteScale",
    )

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier.fillMaxWidth(),
    ) {
        // Each card carries a whisper of its tool's accent colour.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            lerp(tone.container, scheme.surface, 0.60f),
                            scheme.surfaceContainerLow,
                        ),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // A different Expressive polygon + gradient for every tool.
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(badgeShape)
                            .background(tone.badgeBrush),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = iconFor(tool.category),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = tone.onContainer,
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // High-contrast favorite affordance: filled accent disc
                    // when active, subtle neutral disc when not.
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (tool.isFavorite) tone.accent else scheme.surfaceContainerHigh,
                            )
                            .clickable(onClick = onFavorite),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (tool.isFavorite) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Filled.FavoriteBorder
                            },
                            contentDescription = stringResource(
                                if (tool.isFavorite) R.string.action_unfavorite else R.string.action_favorite,
                            ),
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer {
                                    scaleX = favoriteScale
                                    scaleY = favoriteScale
                                },
                            tint = if (tool.isFavorite) {
                                scheme.surface
                            } else {
                                scheme.onSurfaceVariant
                            },
                        )
                    }
                }

                Text(
                    text = tool.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(tone.container)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            text = tool.category.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = tone.onContainer,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = scheme.primary,
                    )
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* Empty state                                                                */
/* -------------------------------------------------------------------------- */

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyState(catalogueEmpty: Boolean, onReset: () -> Unit) {
    val shape = heroAccentShape()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (catalogueEmpty) Icons.Filled.Widgets else Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Text(
            text = stringResource(
                if (catalogueEmpty) R.string.home_empty_catalogue_title else R.string.home_empty_title,
            ),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(
                if (catalogueEmpty) R.string.home_empty_catalogue_desc else R.string.home_empty_desc,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!catalogueEmpty) {
            FilledTonalButton(onClick = onReset) {
                Icon(Icons.Filled.Star, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.home_empty_reset))
            }
        }
    }
}
