package com.yiqiu.toolkit.data.repository

import com.yiqiu.toolkit.data.model.Tool
import com.yiqiu.toolkit.data.model.ToolCategory
import com.yiqiu.toolkit.data.model.ToolIds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

interface ToolRepository {
    fun observeTools(): Flow<List<Tool>>
    fun observeTool(id: String): Flow<Tool?>
    suspend fun toggleFavorite(id: String)
}

/**
 * In-memory implementation backed by a [MutableStateFlow]. Exposes cold,
 * reactive streams so the UI layer stays declarative. Replace with Room /
 * DataStore-backed implementation without touching the domain or UI layers.
 *
 * The catalogue only holds tools that are actually implemented, so the app
 * never shows entries that do not work yet.
 */
class InMemoryToolRepository : ToolRepository {

    private val tools = MutableStateFlow(
        listOf(
            Tool(
                id = ToolIds.SwitchRecord,
                title = "现场倒闸操作记录",
                description = "发令人、受令人、操作任务等 8 个板块逐项记录，序号自动编号",
                category = ToolCategory.RECORD,
            ),
        ),
    )

    override fun observeTools(): Flow<List<Tool>> = tools

    override fun observeTool(id: String): Flow<Tool?> =
        tools.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun toggleFavorite(id: String) {
        tools.update { list ->
            list.map { tool ->
                if (tool.id == id) tool.copy(isFavorite = !tool.isFavorite) else tool
            }
        }
    }
}
