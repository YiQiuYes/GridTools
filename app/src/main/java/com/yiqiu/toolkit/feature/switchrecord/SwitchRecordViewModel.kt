package com.yiqiu.toolkit.feature.switchrecord

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * State holder for the switching-operation record.
 *
 * State is kept in Compose snapshot state rather than a [kotlinx.coroutines.flow.StateFlow]
 * on purpose: every keystroke round-trips through [SwitchRecordUiEvent.FieldChanged]
 * and must land synchronously, otherwise the text field cursor jumps while typing.
 *
 * The record lives in memory for now, exactly like the tool catalogue itself.
 */
class SwitchRecordViewModel : ViewModel() {

    var uiState by mutableStateOf(SwitchRecordUiState(recordTime = currentDateTime()))
        private set

    private var nextEntryId = 1L
    private var nextTokenId = 1L

    init {
        // Start with one empty row: the form is the point of the screen, and an
        // empty screen would make "添加一项" the only affordance.
        addEntry()
    }

    fun onEvent(event: SwitchRecordUiEvent) {
        when (event) {
            is SwitchRecordUiEvent.RecordTimeChanged ->
                uiState = uiState.copy(recordTime = event.value)

            is SwitchRecordUiEvent.ReasonNoChanged ->
                uiState = uiState.copy(reasonNo = event.value)

            is SwitchRecordUiEvent.FieldChanged -> updateEntry(event.entryId) { entry ->
                entry.withField(event.field, event.value)
            }

            SwitchRecordUiEvent.EntryAdded -> addEntry()

            is SwitchRecordUiEvent.EntryRemoved ->
                uiState = uiState.copy(entries = uiState.entries.filterNot { it.id == event.entryId })

            is SwitchRecordUiEvent.TaskPhraseAdded -> {
                val text = event.text.trim()
                if (text.isNotEmpty()) {
                    updateEntry(event.entryId) { entry ->
                        entry.copy(task = entry.task + TaskToken(nextTokenId++, text))
                    }
                }
            }

            is SwitchRecordUiEvent.CustomPhraseStored -> {
                val text = event.text.trim()
                if (text.isNotEmpty() && text !in uiState.customPhrases) {
                    uiState = uiState.copy(customPhrases = uiState.customPhrases + text)
                }
            }

            is SwitchRecordUiEvent.TaskTokenRemoved -> updateEntry(event.entryId) { entry ->
                entry.copy(task = entry.task.filterNot { it.id == event.tokenId })
            }

            is SwitchRecordUiEvent.TaskCleared -> updateEntry(event.entryId) { entry ->
                entry.copy(task = emptyList())
            }
        }
    }

    private fun addEntry() {
        uiState = uiState.copy(entries = uiState.entries + OperationEntry(id = nextEntryId++))
    }

    private fun updateEntry(entryId: Long, transform: (OperationEntry) -> OperationEntry) {
        uiState = uiState.copy(
            entries = uiState.entries.map { if (it.id == entryId) transform(it) else it },
        )
    }
}

private fun OperationEntry.withField(field: EntryField, value: String): OperationEntry =
    when (field) {
        EntryField.ISSUER -> copy(issuer = value)
        EntryField.RECEIVER -> copy(receiver = value)
        EntryField.COMPLETED_AT -> copy(completedAt = value)
        EntryField.OPERATOR -> copy(operator = value)
        EntryField.SUPERVISOR -> copy(supervisor = value)
        EntryField.NOTE -> copy(note = value)
    }

/** `yyyy-MM-dd HH:mm` in the device locale, used for every timestamp on screen. */
internal fun currentDateTime(): String =
    SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault()).format(Date())

internal const val DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm"
