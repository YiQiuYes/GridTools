package com.yiqiu.toolkit.feature.switchrecord

/** One phrase chip inside 操作任务 — tapped to remove it. */
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

data class SwitchRecordUiState(
    val recordTime: String = "",
    val reasonNo: String = "",
    val entries: List<OperationEntry> = emptyList(),
    /** 自定义词组临时分组：页面生命周期内暂存，供各行拼接复用。 */
    val customPhrases: List<String> = emptyList(),
    /** 已填写过的人名（发令人/受令人/操作人/监护人），页面生命周期内暂存。 */
    val people: List<String> = emptyList(),
)

sealed interface SwitchRecordUiEvent {
    data class RecordTimeChanged(val value: String) : SwitchRecordUiEvent
    data class ReasonNoChanged(val value: String) : SwitchRecordUiEvent
    data class FieldChanged(
        val entryId: Long,
        val field: EntryField,
        val value: String,
    ) : SwitchRecordUiEvent
    data object EntryAdded : SwitchRecordUiEvent
    data class EntryRemoved(val entryId: Long) : SwitchRecordUiEvent
    data class TaskPhraseAdded(val entryId: Long, val text: String) : SwitchRecordUiEvent
    /** 加入自定义词组：追加进临时分组，供本页后续拼接复用。 */
    data class CustomPhraseStored(val text: String) : SwitchRecordUiEvent
    /** 暂存一个已填写的人名，供四个人员字段的弹窗选择。 */
    data class PersonStored(val name: String) : SwitchRecordUiEvent
    data class TaskTokenRemoved(val entryId: Long, val tokenId: Long) : SwitchRecordUiEvent
    data class TaskCleared(val entryId: Long) : SwitchRecordUiEvent
}

/**
 * The phrase library itself (groups + persistence) lives in
 * `data/repository/PhraseLibraryRepository.kt`; the composer receives its
 * groups as state and merges in the page-scoped temporary group.

 */

/** 操作事由编号的可选值。 */
val ReasonNoOptions: List<String> = listOf(
    "a 配网事故紧急处理",
    "b 单一操作",
    "c 低压配电网操作",
    "d 二次设备开关、压板、连接片的操作",
)
