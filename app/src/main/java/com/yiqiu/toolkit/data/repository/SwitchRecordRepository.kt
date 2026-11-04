package com.yiqiu.toolkit.data.repository

import com.yiqiu.toolkit.core.storage.KeyValueStore
import com.yiqiu.toolkit.data.model.OperationEntry
import com.yiqiu.toolkit.data.model.SavedSwitchRecord
import com.yiqiu.toolkit.data.model.TaskToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persisted switching-operation records (历史记录).
 *
 * All records live in one JSON document under a single key — the same
 * document-per-store strategy as [PhraseLibraryRepository]. Writes are
 * synchronous and immediate; the UI observes [records] (newest first).
 */
class SwitchRecordRepository(private val store: KeyValueStore) {

    private val _records = MutableStateFlow<List<SavedSwitchRecord>>(load())
    val records: StateFlow<List<SavedSwitchRecord>> = _records.asStateFlow()

    /** Newest-first snapshot of the saved records. */
    val all: List<SavedSwitchRecord> get() = _records.value

    fun get(id: String): SavedSwitchRecord? = _records.value.firstOrNull { it.id == id }

    /** Insert or update [record]; [SavedSwitchRecord.savedAt] stamps the write. */
    fun save(record: SavedSwitchRecord) {
        val updated = listOf(record) + _records.value.filterNot { it.id == record.id }
        _records.value = updated
        persist()
    }

    fun remove(recordId: String) {
        _records.value = _records.value.filterNot { it.id == recordId }
        persist()
    }

    private fun persist() {
        val json = JSONArray().apply {
            _records.value.forEach { record ->
                val entries = JSONArray().apply {
                    record.entries.forEach { entry ->
                        put(
                            JSONObject()
                                .put(KEY_ISSUER, entry.issuer)
                                .put(KEY_RECEIVER, entry.receiver)
                                .put(KEY_TASK, JSONArray(entry.task.map { it.text }))
                                .put(KEY_COMPLETED_AT, entry.completedAt)
                                .put(KEY_OPERATOR, entry.operator)
                                .put(KEY_SUPERVISOR, entry.supervisor)
                                .put(KEY_NOTE, entry.note),
                        )
                    }
                }
                put(
                    JSONObject()
                        .put(KEY_ID, record.id)
                        .put(KEY_SAVED_AT, record.savedAt)
                        .put(KEY_RECORD_TIME, record.recordTime)
                        .put(KEY_REASON_NO, record.reasonNo)
                        .put(KEY_ENTRIES, entries),
                )
            }
        }
        store.putString(STORE_KEY, json.toString())
    }

    private fun load(): List<SavedSwitchRecord> {
        val raw = store.getString(STORE_KEY) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            val records = ArrayList<SavedSwitchRecord>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val entryArr = obj.optJSONArray(KEY_ENTRIES) ?: JSONArray()
                val entries = ArrayList<OperationEntry>(entryArr.length())
                // Fresh ids on load: they are UI-scoped, uniqueness only has to
                // hold inside the editing session.
                var entryId = 1L
                var tokenId = 1L
                for (j in 0 until entryArr.length()) {
                    val entryObj = entryArr.getJSONObject(j)
                    val taskArr = entryObj.optJSONArray(KEY_TASK) ?: JSONArray()
                    val tokens = ArrayList<TaskToken>(taskArr.length())
                    for (k in 0 until taskArr.length()) {
                        tokens += TaskToken(id = tokenId++, text = taskArr.getString(k))
                    }
                    entries += OperationEntry(
                        id = entryId++,
                        issuer = entryObj.optString(KEY_ISSUER),
                        receiver = entryObj.optString(KEY_RECEIVER),
                        task = tokens,
                        completedAt = entryObj.optString(KEY_COMPLETED_AT),
                        operator = entryObj.optString(KEY_OPERATOR),
                        supervisor = entryObj.optString(KEY_SUPERVISOR),
                        note = entryObj.optString(KEY_NOTE),
                    )
                }
                records += SavedSwitchRecord(
                    id = obj.getString(KEY_ID),
                    savedAt = obj.optLong(KEY_SAVED_AT),
                    recordTime = obj.optString(KEY_RECORD_TIME),
                    reasonNo = obj.optString(KEY_REASON_NO),
                    entries = entries,
                )
            }
            records
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val STORE_KEY = "switch_records_v1"
        const val KEY_ID = "id"
        const val KEY_SAVED_AT = "savedAt"
        const val KEY_RECORD_TIME = "recordTime"
        const val KEY_REASON_NO = "reasonNo"
        const val KEY_ENTRIES = "entries"
        const val KEY_ISSUER = "issuer"
        const val KEY_RECEIVER = "receiver"
        const val KEY_TASK = "task"
        const val KEY_COMPLETED_AT = "completedAt"
        const val KEY_OPERATOR = "operator"
        const val KEY_SUPERVISOR = "supervisor"
        const val KEY_NOTE = "note"
    }
}
