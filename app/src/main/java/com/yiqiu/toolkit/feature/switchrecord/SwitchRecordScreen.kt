package com.yiqiu.toolkit.feature.switchrecord

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yiqiu.toolkit.R
import com.yiqiu.toolkit.ui.theme.toolBadgeShape
import com.yiqiu.toolkit.ui.theme.toolTone
import com.yiqiu.toolkit.ui.theme.visualIndexOf

/**
 * 现场倒闸操作记录.
 *
 * Layout rule: everything lives inside one column of the page width. Fields
 * wrap onto the next line as the text grows — nothing scrolls horizontally, and
 * no field is off to the right.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SwitchRecordScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SwitchRecordViewModel = viewModel(),
) {
    val state = viewModel.uiState
    var composingEntryId by rememberSaveable { mutableStateOf<Long?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    // On predictive-back devices the back press that collapses the keyboard
    // can also reach the app's back dispatcher and pop this screen. While the
    // IME is visible, consume the back press and just drop focus instead.
    val imeVisible = WindowInsets.isImeVisible
    val focusManager = LocalFocusManager.current
    BackHandler(enabled = imeVisible) { focusManager.clearFocus() }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.switch_record_title),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 840.dp)
                    .fillMaxWidth()
                    .consumeWindowInsets(innerPadding)
                    .imePadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 12.dp,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "header") {
                    RecordHeaderCard(
                        recordTime = state.recordTime,
                        reasonNo = state.reasonNo,
                        onRecordTimeChange = {
                            viewModel.onEvent(SwitchRecordUiEvent.RecordTimeChanged(it))
                        },
                        onReasonNoChange = {
                            viewModel.onEvent(SwitchRecordUiEvent.ReasonNoChanged(it))
                        },
                    )
                }

                item(key = "section") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.switch_record_section),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.switch_record_count, state.entries.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                if (state.entries.isEmpty()) {
                    item(key = "empty") {
                        EmptyEntries(
                            onAdd = { viewModel.onEvent(SwitchRecordUiEvent.EntryAdded) },
                        )
                    }
                } else {
                    itemsIndexed(state.entries, key = { _, entry -> entry.id }) { index, entry ->
                        OperationEntryCard(
                            sequence = index + 1,
                            entry = entry,
                            onFieldChange = { field, value ->
                                viewModel.onEvent(SwitchRecordUiEvent.FieldChanged(entry.id, field, value))
                            },
                            onRemove = { viewModel.onEvent(SwitchRecordUiEvent.EntryRemoved(entry.id)) },
                            onComposeTask = { composingEntryId = entry.id },
                            modifier = Modifier.animateItem(),
                        )
                    }

                    item(key = "add") {
                        FilledTonalButton(
                            onClick = { viewModel.onEvent(SwitchRecordUiEvent.EntryAdded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.switch_record_add))
                        }
                    }
                }
            }
        }
    }

    val composingEntry = state.entries.firstOrNull { it.id == composingEntryId }
    if (composingEntry != null) {
        TaskComposerSheet(
            tokens = composingEntry.task,
            customPhrases = state.customPhrases,
            onAddPhrase = { phrase ->
                viewModel.onEvent(SwitchRecordUiEvent.TaskPhraseAdded(composingEntry.id, phrase))
            },
            onStoreCustomPhrase = { phrase ->
                viewModel.onEvent(SwitchRecordUiEvent.CustomPhraseStored(phrase))
            },
            onRemoveToken = { tokenId ->
                viewModel.onEvent(SwitchRecordUiEvent.TaskTokenRemoved(composingEntry.id, tokenId))
            },
            onClear = { viewModel.onEvent(SwitchRecordUiEvent.TaskCleared(composingEntry.id)) },
            onDismiss = { composingEntryId = null },
        )
    }
}

/* -------------------------------------------------------------------------- */
/* Header                                                                     */
/* -------------------------------------------------------------------------- */

@Composable
private fun RecordHeaderCard(
    recordTime: String,
    reasonNo: String,
    onRecordTimeChange: (String) -> Unit,
    onReasonNoChange: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val brush = Brush.linearGradient(
        listOf(
            scheme.primaryContainer,
            lerp(scheme.primaryContainer, scheme.tertiaryContainer, 0.55f),
            scheme.tertiaryContainer,
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(brush)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Assignment,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = scheme.onPrimaryContainer,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.switch_record_eyebrow),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onPrimaryContainer,
            )
        }

        DateTimeField(
            label = stringResource(R.string.switch_record_time),
            value = recordTime,
            onValueChange = onRecordTimeChange,
            containerColor = scheme.surface.copy(alpha = 0.72f),
            modifier = Modifier.fillMaxWidth(),
        )

        ReasonNoField(
            value = reasonNo,
            onValueChange = onReasonNoChange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 操作事由编号 — picked from [ReasonNoOptions] instead of free text, so the
 * printed form always carries one of the standard codes. Rendered as a
 * read-only field; a transparent overlay forwards taps to the dropdown.
 */
@Composable
private fun ReasonNoField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme

    Box(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.switch_record_reason_no)) },
            placeholder = {
                Text(
                    text = stringResource(R.string.switch_record_reason_no_hint),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            shape = MaterialTheme.shapes.large,
            maxLines = 2,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = scheme.surface.copy(alpha = 0.72f),
                unfocusedContainerColor = scheme.surface.copy(alpha = 0.72f),
                focusedBorderColor = scheme.primary,
                unfocusedBorderColor = scheme.outlineVariant,
            ),
        )
        // The read-only field swallows taps itself, so an invisible layer on
        // top forwards the click to open the option menu instead.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(MaterialTheme.shapes.large)
                .clickable { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ReasonNoOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* Entry card                                                                 */
/* -------------------------------------------------------------------------- */

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OperationEntryCard(
    sequence: Int,
    entry: OperationEntry,
    onFieldChange: (EntryField, String) -> Unit,
    onRemove: () -> Unit,
    onComposeTask: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val seed = visualIndexOf("switch-record-$sequence")
    val tone = toolTone(seed)
    val badgeShape = toolBadgeShape(seed)

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(badgeShape)
                        .background(tone.badgeBrush),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = sequence.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = tone.onContainer,
                    )
                }
                Spacer(Modifier.width(12.dp))
                DateTimeField(
                    label = stringResource(R.string.switch_record_completed_at),
                    value = entry.completedAt,
                    onValueChange = { onFieldChange(EntryField.COMPLETED_AT, it) },
                    containerColor = MaterialTheme.colorScheme.surface,
                    compact = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(R.string.switch_record_delete_entry),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            TwoFieldRow(
                first = { fieldModifier ->
                    LabelledField(
                        label = stringResource(R.string.switch_record_issuer),
                        value = entry.issuer,
                        onValueChange = { onFieldChange(EntryField.ISSUER, it) },
                        modifier = fieldModifier,
                    )
                },
                second = { fieldModifier ->
                    LabelledField(
                        label = stringResource(R.string.switch_record_receiver),
                        value = entry.receiver,
                        onValueChange = { onFieldChange(EntryField.RECEIVER, it) },
                        modifier = fieldModifier,
                    )
                },
            )

            TaskField(
                tokens = entry.task,
                onCompose = onComposeTask,
            )

            TwoFieldRow(
                first = { fieldModifier ->
                    LabelledField(
                        label = stringResource(R.string.switch_record_operator),
                        value = entry.operator,
                        onValueChange = { onFieldChange(EntryField.OPERATOR, it) },
                        modifier = fieldModifier,
                    )
                },
                second = { fieldModifier ->
                    LabelledField(
                        label = stringResource(R.string.switch_record_supervisor),
                        value = entry.supervisor,
                        onValueChange = { onFieldChange(EntryField.SUPERVISOR, it) },
                        modifier = fieldModifier,
                    )
                },
            )

            LabelledField(
                label = stringResource(R.string.switch_record_note),
                value = entry.note,
                onValueChange = { onFieldChange(EntryField.NOTE, it) },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Two short fields per line on phones that can afford it, stacked when the
 * window is too narrow to give both a usable width.
 */
@Composable
private fun TwoFieldRow(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 300.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                first(Modifier.fillMaxWidth())
                second(Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                first(Modifier.weight(1f))
                second(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LabelledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxLines: Int = 3,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        shape = MaterialTheme.shapes.large,
        maxLines = maxLines,
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}

/* -------------------------------------------------------------------------- */
/* 操作任务 — plain text on the card; tap to open the composer and edit         */
/* -------------------------------------------------------------------------- */

@Composable
private fun TaskField(
    tokens: List<TaskToken>,
    onCompose: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.switch_record_task),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                // The whole field is the edit affordance: tapping it opens the
                // composer sheet where tokens are added and removed.
                .clickable(onClick = onCompose),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
            ) {
                Text(
                    text = tokens.joinToString(separator = "") { it.text }
                        .ifEmpty { stringResource(R.string.switch_record_task_hint) },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (tokens.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* Empty state                                                                */
/* -------------------------------------------------------------------------- */

@Composable
private fun EmptyEntries(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Assignment,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.switch_record_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.switch_record_empty_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilledTonalButton(onClick = onAdd, modifier = Modifier.height(48.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.switch_record_add))
        }
    }
}
