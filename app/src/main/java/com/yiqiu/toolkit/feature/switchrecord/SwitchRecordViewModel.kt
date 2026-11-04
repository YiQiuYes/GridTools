package com.yiqiu.toolkit.feature.switchrecord

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.yiqiu.toolkit.core.di.AppContainer
import com.yiqiu.toolkit.data.model.EntryField
import com.yiqiu.toolkit.data.model.OperationEntry
import com.yiqiu.toolkit.data.model.PhraseGroup
import com.yiqiu.toolkit.data.model.SavedSwitchRecord
import com.yiqiu.toolkit.data.model.TaskToken
import com.yiqiu.toolkit.data.repository.PhraseLibraryRepository
import com.yiqiu.toolkit.data.repository.SwitchRecordRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * State holder for the switching-operation record.
 *
 * State is kept in Compose snapshot state rather than a StateFlow on purpose:
 * every keystroke round-trips through [SwitchRecordUiEvent.FieldChanged] and
 * must land synchronously, otherwise the text field cursor jumps while typing.
 *
 * Records persist through [SwitchRecordRepository]. The screen starts blank
 * ([recordId] == null) or loads an existing record for editing; [save] then
 * updates that same record. [lastSavedAt] flips on every successful save so
 * the UI can confirm with a snackbar.
 */
class SwitchRecordViewModel(
    phraseLibrary: PhraseLibraryRepository,
    private val recordRepository: SwitchRecordRepository,
    recordId: String? = null,
) : ViewModel() {

    var uiState by mutableStateOf(SwitchRecordUiState(recordTime = currentDate()))
        private set

    /** Persisted phrase groups shown by the task composer. */
    val phraseGroups: StateFlow<List<PhraseGroup>> = phraseLibrary.groups

    /** Timestamp of the last successful save; null until the first one. */
    var lastSavedAt by mutableStateOf<Long?>(null)
        private set

    /** The record being edited; assigned on first save for new records. */
    private var currentRecordId: String? = recordId

    private var nextEntryId = 1L
    private var nextTokenId = 1L

    init {
        val saved = recordId?.let { recordRepository.get(it) }
        if (saved != null) {
            load(saved)
        } else {
            // Start with one empty row: the form is the point of the screen,
            // and an empty screen would make "添加一项" the only affordance.
            addEntry()
        }
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

            is SwitchRecordUiEvent.PersonStored -> {
                val name = event.name.trim()
                if (name.isNotEmpty() && name !in uiState.people) {
                    uiState = uiState.copy(people = uiState.people + name)
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

    /**
     * Persist the current form. New records get a fresh id on first save;
     * afterwards every save updates the same record in place.
     */
    fun save() {
        val id = currentRecordId ?: newRecordId().also { currentRecordId = it }
        val savedAt = System.currentTimeMillis()
        recordRepository.save(
            SavedSwitchRecord(
                id = id,
                savedAt = savedAt,
                recordTime = uiState.recordTime,
                reasonNo = uiState.reasonNo,
                entries = uiState.entries,
            ),
        )
        lastSavedAt = savedAt
    }

    private fun load(record: SavedSwitchRecord) {
        uiState = SwitchRecordUiState(
            recordTime = record.recordTime.ifBlank { currentDate() },
            reasonNo = record.reasonNo,
        )
        record.entries.forEach { entry ->
            val restored = entry.copy(
                id = nextEntryId++,
                task = entry.task.map { TaskToken(id = nextTokenId++, text = it.text) },
            )
            uiState = uiState.copy(entries = uiState.entries + restored)
        }
        if (uiState.entries.isEmpty()) addEntry()
    }

    private fun addEntry() {
        uiState = uiState.copy(entries = uiState.entries + OperationEntry(id = nextEntryId++))
    }

    private fun updateEntry(entryId: Long, transform: (OperationEntry) -> OperationEntry) {
        uiState = uiState.copy(
            entries = uiState.entries.map { if (it.id == entryId) transform(it) else it },
        )
    }

    class Factory(
        private val container: AppContainer,
        private val recordId: String? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SwitchRecordViewModel(
                container.phraseLibraryRepository,
                container.switchRecordRepository,
                recordId,
            ) as T
    }
}

private fun newRecordId(): String = "rec_${System.currentTimeMillis()}"

private fun OperationEntry.withField(field: EntryField, value: String): OperationEntry =
    when (field) {
        EntryField.ISSUER -> copy(issuer = value)
        EntryField.RECEIVER -> copy(receiver = value)
        EntryField.COMPLETED_AT -> copy(completedAt = value)
        EntryField.OPERATOR -> copy(operator = value)
        EntryField.SUPERVISOR -> copy(supervisor = value)
        EntryField.NOTE -> copy(note = value)
    }
