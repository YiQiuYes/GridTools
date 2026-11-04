package com.yiqiu.toolkit.data.repository

import com.yiqiu.toolkit.data.model.Tool
import com.yiqiu.toolkit.data.model.ToolCategory
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
 */
class InMemoryToolRepository : ToolRepository {

    private val tools = MutableStateFlow(SampleTools)

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

private val SampleTools = listOf(
    Tool("qr", "二维码生成", "把文本或链接转换成二维码", ToolCategory.UTILITY, true),
    Tool("unit", "单位换算", "长度、重量、温度等单位互转", ToolCategory.CONVERTER),
    Tool("calc", "科学计算器", "带历史记录的科学计算", ToolCategory.CALCULATOR, true),
    Tool("color", "取色器", "从颜色空间中提取并复制色值", ToolCategory.UTILITY),
    Tool("countdown", "倒计时", "多组可保存的倒计时", ToolCategory.PRODUCTIVITY),
    Tool("password", "密码生成", "可配置强度的随机密码", ToolCategory.UTILITY),
    Tool("text", "文本统计", "字数、字符数与阅读时长", ToolCategory.PRODUCTIVITY),
    Tool("base", "进制转换", "二 / 八 / 十 / 十六进制互转", ToolCategory.CONVERTER),
    Tool("tip", "小费计算", "按人数分摊账单与小费", ToolCategory.CALCULATOR),
    Tool("date", "日期差计算", "计算两个日期之间的间隔", ToolCategory.CALCULATOR),
    Tool("case", "大小写转换", "多种文本命名风格互转", ToolCategory.PRODUCTIVITY),
    Tool("hash", "哈希摘要", "计算文本的 MD5 / SHA 摘要", ToolCategory.UTILITY),
)
