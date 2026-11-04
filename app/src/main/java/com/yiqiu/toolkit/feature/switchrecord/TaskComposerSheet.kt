package com.yiqiu.toolkit.feature.switchrecord

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yiqiu.toolkit.R

/**
 * Bottom-sheet task composer.
 *
 * The sheet stays open while composing: tapping a phrase appends a chip to
 * 操作任务, tapping a chip in the preview removes it again. Everything wraps
 * inside the sheet width — there is no horizontal scrolling anywhere.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskComposerSheet(
    tokens: List<TaskToken>,
    customPhrases: List<String>,
    onAddPhrase: (String) -> Unit,
    onStoreCustomPhrase: (String) -> Unit,
    onRemoveToken: (Long) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    var customText by rememberSaveable { mutableStateOf("") }

    // Same guard as the record screen: back with the keyboard open should only
    // collapse the IME, not dismiss the sheet.
    val focusManager = LocalFocusManager.current
    BackHandler(enabled = WindowInsets.isImeVisible) { focusManager.clearFocus() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.switch_record_composer_title),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = stringResource(R.string.switch_record_composer_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (tokens.isNotEmpty()) {
                        TextButton(onClick = onClear) {
                            Text(stringResource(R.string.switch_record_composer_clear))
                        }
                    }
                }

                TaskPreview(tokens = tokens, onRemoveToken = onRemoveToken)

                // 自定义词组 sits at the top of the palette; anything added
                // here is also kept in the temporary group right below, so
                // later rows can reuse it without retyping.
                Row(verticalAlignment = Alignment.Bottom) {
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.switch_record_composer_custom)) },
                        shape = MaterialTheme.shapes.large,
                        maxLines = 3,
                    )
                    Spacer(Modifier.width(10.dp))
                    FilledTonalButton(
                        onClick = {
                            val phrase = customText.trim()
                            if (phrase.isNotEmpty()) {
                                onAddPhrase(phrase)
                                onStoreCustomPhrase(phrase)
                                customText = ""
                            }
                        },
                        modifier = Modifier.height(56.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.switch_record_composer_add))
                    }
                }

                if (customPhrases.isNotEmpty()) {
                    PhraseGroupBlock(
                        group = TaskPhraseGroup(
                            title = stringResource(R.string.switch_record_composer_temp_group),
                            phrases = customPhrases,
                        ),
                        onAddPhrase = onAddPhrase,
                    )
                }

                TaskPhraseGroups.forEach { group ->
                    PhraseGroupBlock(group = group, onAddPhrase = onAddPhrase)
                }
                Spacer(Modifier.height(4.dp))
            }

            // Sticky action: the composer can get long, the primary action
            // should not require scrolling to the end of the phrase palette.
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 3.dp,
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .height(54.dp),
                ) {
                    Text(stringResource(R.string.switch_record_composer_done))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskPreview(
    tokens: List<TaskToken>,
    onRemoveToken: (Long) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.switch_record_composer_preview),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (tokens.isEmpty()) {
                Text(
                    text = stringResource(R.string.switch_record_composer_preview_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    tokens.forEach { token ->
                        InputChip(
                            selected = false,
                            onClick = { onRemoveToken(token.id) },
                            label = { Text(token.text) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.switch_record_token_remove),
                                    modifier = Modifier.size(15.dp),
                                )
                            },
                        )
                    }
                }
                Text(
                    text = tokens.joinToString(separator = "") { it.text },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhraseGroupBlock(
    group: TaskPhraseGroup,
    onAddPhrase: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = group.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            group.phrases.forEach { phrase ->
                SuggestionChip(
                    onClick = { onAddPhrase(phrase) },
                    label = { Text(phrase) },
                )
            }
        }
    }
}
