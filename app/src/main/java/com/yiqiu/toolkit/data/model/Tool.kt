package com.yiqiu.toolkit.data.model

/**
 * Core domain entity. The data layer deliberately holds no Compose/Android
 * references so it can be reused by any feature (and replaced by a real
 * database or network source later).
 */
data class Tool(
    val id: String,
    val title: String,
    val description: String,
    val category: ToolCategory,
    val isFavorite: Boolean = false,
)

enum class ToolCategory(val label: String) {
    PRODUCTIVITY("效率"),
    CALCULATOR("计算"),
    CONVERTER("转换"),
    UTILITY("实用工具"),
}
