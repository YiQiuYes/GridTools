package com.yiqiu.toolkit.data.model

/**
 * A named group of phrases rendered as tappable chips in the task composer.
 *
 * Lives in the data layer because it is persisted via the phrase library
 * repository and shared by the composer and the maintenance screens.
 */
data class PhraseGroup(
    val id: String,
    val title: String,
    val phrases: List<String>,
)

/** Built-in seed groups, used until a persisted library overrides them. */
val DefaultPhraseGroups: List<PhraseGroup> = listOf(
    PhraseGroup(
        id = "action",
        title = "操作动作",
        phrases = listOf("断开", "合上", "拉开", "检查", "悬挂", "取下"),
    ),
    PhraseGroup(
        id = "misc",
        title = "其他",
        phrases = listOf("在", "\"禁止合闸，线路有人工作\"标志牌", "分闸", "位置"),
    ),
    PhraseGroup(
        id = "device",
        title = "设备",
        phrases = listOf("开关", "刀闸", "接地刀闸", "开关屏面上"),
    ),
)
