package com.yiqiu.toolkit.feature.phraselibrary

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yiqiu.toolkit.R
import com.yiqiu.toolkit.core.di.AppContainer
import com.yiqiu.toolkit.core.di.LocalAppContainer
import com.yiqiu.toolkit.data.model.PhraseGroup
import com.yiqiu.toolkit.data.repository.PhraseLibraryRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * 操作任务维护 — add/remove phrase groups and phrases used by the task
 * composer. Every change goes through [PhraseLibraryRepository] and is
 * persisted immediately.
 */
class PhraseLibraryViewModel(private val repository: PhraseLibraryRepository) : ViewModel() {

    val groups: StateFlow<List<PhraseGroup>> = repository.groups

    fun addPhrase(groupId: String, phrase: String) = repository.addPhrase(groupId, phrase)

    fun removePhrase(groupId: String, phrase: String) = repository.removePhrase(groupId, phrase)

    fun addGroup(title: String) = repository.addGroup(title)

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PhraseLibraryViewModel(container.phraseLibraryRepository) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhraseLibraryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PhraseLibraryViewModel = viewModel(
        factory = PhraseLibraryViewModel.Factory(LocalAppContainer.current),
    ),
) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    var addingGroup by remember { mutableStateOf(false) }
    var addPhraseTarget by remember { mutableStateOf<PhraseGroup?>(null) }
    val focusManager = LocalFocusManager.current

    // Tap any empty area to drop the keyboard, matching the record screen.
    Scaffold(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.phrase_lib_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "desc") {
                Text(
                    text = stringResource(R.string.phrase_lib_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item(key = "add_group") {
                FilledTonalButton(
                    onClick = { addingGroup = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.phrase_lib_add_group))
                }
            }

            items(groups, key = { it.id }) { group ->
                PhraseGroupCard(
                    group = group,
                    onRemovePhrase = { viewModel.removePhrase(group.id, it) },
                    onAddPhrase = { addPhraseTarget = group },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    if (addingGroup) {
        TextEntryDialog(
            title = stringResource(R.string.phrase_lib_add_group),
            label = stringResource(R.string.phrase_lib_group_name),
            onConfirm = { title ->
                viewModel.addGroup(title)
                addingGroup = false
            },
            onDismiss = { addingGroup = false },
        )
    }

    addPhraseTarget?.let { target ->
        TextEntryDialog(
            title = stringResource(R.string.phrase_lib_add_phrase),
            label = stringResource(R.string.phrase_lib_phrase_text),
            onConfirm = { phrase ->
                viewModel.addPhrase(target.id, phrase)
                addPhraseTarget = null
            },
            onDismiss = { addPhraseTarget = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhraseGroupCard(
    group: PhraseGroup,
    onRemovePhrase: (String) -> Unit,
    onAddPhrase: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = group.phrases.size.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (group.phrases.isEmpty()) {
                Text(
                    text = stringResource(R.string.phrase_lib_empty_group),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                group.phrases.forEach { phrase ->
                    InputChip(
                        selected = false,
                        onClick = { onRemovePhrase(phrase) },
                        label = { Text(phrase) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.switch_record_token_remove),
                                modifier = Modifier.size(15.dp),
                            )
                        },
                    )
                }
                InputChip(
                    selected = false,
                    onClick = onAddPhrase,
                    label = { Text(stringResource(R.string.phrase_lib_add_phrase)) },
                    leadingIcon = {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                )
            }
        }
    }
}

/** Small dialog with one text input, used for both group and phrase creation. */
@Composable
private fun TextEntryDialog(
    title: String,
    label: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
            )
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = { onConfirm(text.trim()) },
            ) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
