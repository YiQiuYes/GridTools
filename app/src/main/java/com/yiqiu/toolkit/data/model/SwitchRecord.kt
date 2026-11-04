package com.yiqiu.toolkit.data.model

/**
 * One phrase chip inside 操作任务 — tapped to remove it.
 */
data class TaskToken(
    val id: Long,
    val text: String,
)

/**
 * A single row of the switching-operation record.
 *
 * The eight sections of the paper form map onto the fields below; 序号 is not
 * stored because it is derived from the row position, so it renumbers itself
 * whenever a row is removed.
 */
data class OperationEntry(
    val id: Long,
    val issuer: String = "",
    val receiver: String = "",
    val task: List<TaskToken> = emptyList(),
    val completedAt: String = "",
    val operator: String = "",
    val supervisor: String = "",
    val note: String = "",
)

/** The free-text fields of an entry, addressed by [SwitchRecordUiEvent.FieldChanged]. */
enum class EntryField {
    ISSUER,
    RECEIVER,
    COMPLETED_AT,
    OPERATOR,
    SUPERVISOR,
    NOTE,
}

/**
 * A persisted switching-operation record.
 *
 * [TaskToken.id]/[OperationEntry.id] are UI-scoped only — the repository
 * serializes the text content and assigns fresh ids when a record is loaded
 * back for editing.
 */
data class SavedSwitchRecord(
    val id: String,
    val savedAt: Long,
    val recordTime: String = "",
    val reasonNo: String = "",
    val entries: List<OperationEntry> = emptyList(),
)
