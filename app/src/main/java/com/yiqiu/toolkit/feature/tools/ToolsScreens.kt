package com.yiqiu.toolkit.feature.tools

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.window.core.layout.WindowSizeClass
import com.yiqiu.toolkit.R
import com.yiqiu.toolkit.core.di.AppContainer
import com.yiqiu.toolkit.core.di.LocalAppContainer
import com.yiqiu.toolkit.data.model.Tool
import com.yiqiu.toolkit.domain.usecase.GetToolUseCase
import com.yiqiu.toolkit.domain.usecase.GetToolsUseCase
import com.yiqiu.toolkit.domain.usecase.ToggleFavoriteUseCase
import com.yiqiu.toolkit.ui.theme.heroDecorationShape
import com.yiqiu.toolkit.ui.theme.iconFor
import com.yiqiu.toolkit.ui.theme.toolBadgeShape
import com.yiqiu.toolkit.ui.theme.toolTone
import com.yiqiu.toolkit.ui.theme.visualIndexOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/* -------------------------------------------------------------------------- */
/* List pane                                                                  */
/* -------------------------------------------------------------------------- */

class ToolListViewModel(getTools: GetToolsUseCase) : ViewModel() {
    val tools: StateFlow<List<Tool>> = getTools().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ToolListViewModel(container.toolUseCases.getTools) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ToolListScreen(
    onToolClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ToolListViewModel = viewModel(
        factory = ToolListViewModel.Factory(LocalAppContainer.current),
    ),
) {
    val tools by viewModel.tools.collectAsStateWithLifecycle()
    val grouped = tools.groupBy { it.category }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.tools_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = stringResource(R.string.tools_section_count, tools.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 4.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            grouped.forEach { (category, categoryTools) ->
                item(key = "header_${category.name}", span = { GridItemSpan(maxLineSpan) }) {
                    CategoryHeader(label = category.label, count = categoryTools.size)
                }
                gridItems(
                    items = categoryTools,
                    key = { it.id },
                    span = { GridItemSpan(1) },
                ) { tool ->
                    ToolCard(
                        tool = tool,
                        onClick = { onToolClick(tool.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            if (tools.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) { ToolListEmptyState() }
            }
        }
    }
}

/** Shown when no tool has been registered yet. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ToolListEmptyState() {
    val shape = heroDecorationShape()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 64.dp),
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
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(
            text = stringResource(R.string.tools_empty_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.tools_empty_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CategoryHeader(label: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ToolCard(
    tool: Tool,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val index = visualIndexOf(tool.id)
    val tone = toolTone(index)
    val badgeShape = toolBadgeShape(index)

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(badgeShape)
                        .background(tone.badgeBrush),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = iconFor(tool.category),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = tone.onContainer,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (tool.isFavorite) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = stringResource(R.string.action_favorite),
                        modifier = Modifier.size(18.dp),
                        tint = tone.accent,
                    )
                }
            }
            Text(
                text = tool.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* Detail pane                                                                */
/* -------------------------------------------------------------------------- */

/**
 * Scoped to a single [ToolDetailRoute] by the
 * `rememberViewModelStoreNavEntryDecorator`: each tool id gets its own
 * ViewModel instance, cleared when the entry leaves the back stack.
 */
class ToolDetailViewModel(
    getTool: GetToolUseCase,
    getTools: GetToolsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val toolId: String,
) : ViewModel() {
    private val toolFlow = getTool(toolId)

    val tool: StateFlow<Tool?> = toolFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    val relatedTools: StateFlow<List<Tool>> = combine(toolFlow, getTools()) { current, all ->
        if (current == null) emptyList()
        else all.filter { it.category == current.category && it.id != toolId }.take(5)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun toggleFavorite() {
        viewModelScope.launch { toggleFavoriteUseCase(toolId) }
    }

    class Factory(
        private val container: AppContainer,
        private val toolId: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ToolDetailViewModel(
                container.toolUseCases.getTool,
                container.toolUseCases.getTools,
                container.toolUseCases.toggleFavorite,
                toolId,
            ) as T
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun ToolDetailScreen(
    toolId: String,
    onBack: () -> Unit,
    onRelatedTool: (String) -> Unit,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier,
    onMaintain: (() -> Unit)? = null,
    viewModel: ToolDetailViewModel = viewModel(
        factory = ToolDetailViewModel.Factory(LocalAppContainer.current, toolId),
    ),
) {
    val tool by viewModel.tool.collectAsStateWithLifecycle()
    val related by viewModel.relatedTools.collectAsStateWithLifecycle()

    // On wide screens the list pane stays visible, so a back arrow would be
    // redundant. Show it only when the layout is single-pane (compact width).
    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
    val showBackButton = !windowAdaptiveInfo.windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = tool?.title ?: stringResource(R.string.tools_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        val current = tool
        if (current == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.tools_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            ToolDetailContent(
                tool = current,
                related = related,
                onToggleFavorite = viewModel::toggleFavorite,
                onRelatedTool = onRelatedTool,
                onLaunch = onLaunch,
                onMaintain = onMaintain,
                contentPadding = innerPadding,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ToolDetailContent(
    tool: Tool,
    related: List<Tool>,
    onToggleFavorite: () -> Unit,
    onRelatedTool: (String) -> Unit,
    onLaunch: () -> Unit,
    onMaintain: (() -> Unit)?,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DetailHero(tool = tool)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onLaunch,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Bolt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.tools_detail_open))
            }
            // Maintenance entry (e.g. the phrase library), rendered between
            // the launch button and the favorite toggle when the tool
            // advertises one.
            if (onMaintain != null) {
                FilledTonalIconButton(onClick = onMaintain) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = stringResource(R.string.tools_detail_maintain),
                    )
                }
            }
            FilledTonalIconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (tool.isFavorite) {
                        Icons.Filled.Favorite
                    } else {
                        Icons.Filled.FavoriteBorder
                    },
                    contentDescription = stringResource(
                        if (tool.isFavorite) R.string.action_unfavorite else R.string.action_favorite,
                    ),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoCard(
                icon = Icons.Filled.Info,
                label = stringResource(R.string.tools_detail_eyebrow),
                value = tool.category.label,
                modifier = Modifier.weight(1f),
            )
            InfoCard(
                icon = Icons.Filled.Favorite,
                label = stringResource(R.string.action_favorite),
                value = if (tool.isFavorite) "已收藏" else "未收藏",
                modifier = Modifier.weight(1f),
            )
        }

        if (related.isNotEmpty()) {
            Text(
                text = stringResource(R.string.tools_detail_related),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(related, key = { it.id }) { relatedTool ->
                    RelatedToolChip(tool = relatedTool, onClick = { onRelatedTool(relatedTool.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DetailHero(tool: Tool) {
    val index = visualIndexOf(tool.id)
    val tone = toolTone(index)
    val badgeShape = toolBadgeShape(index)
    val decoShape = heroDecorationShape()
    val scheme = MaterialTheme.colorScheme
    val heroBrush = Brush.linearGradient(
        listOf(
            tone.container,
            lerp(tone.container, scheme.tertiaryContainer, 0.5f),
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(heroBrush)
            .padding(24.dp),
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.TopEnd)
                .offset(x = 56.dp, y = (-52).dp)
                .rotate(20f)
                .clip(decoShape)
                .background(tone.accent.copy(alpha = 0.20f)),
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(badgeShape)
                    .background(tone.badgeBrush),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconFor(tool.category),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = tone.onContainer,
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = tool.category.label,
                style = MaterialTheme.typography.labelLarge,
                color = tone.onContainer,
            )
            Text(
                text = tool.title,
                style = MaterialTheme.typography.displaySmall,
                color = tone.onContainer,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodyLarge,
                color = tone.onContainer.copy(alpha = 0.82f),
            )
        }
    }
}

@Composable
private fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RelatedToolChip(tool: Tool, onClick: () -> Unit) {
    val index = visualIndexOf(tool.id)
    val tone = toolTone(index)
    val badgeShape = toolBadgeShape(index)

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier.width(170.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(badgeShape)
                    .background(tone.badgeBrush),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconFor(tool.category),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = tone.onContainer,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = tool.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Shown in the detail pane on large screens before a tool is selected. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ToolDetailPlaceholder() {
    val shape = heroDecorationShape()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(132.dp)
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(
            text = stringResource(R.string.tools_detail_placeholder),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
