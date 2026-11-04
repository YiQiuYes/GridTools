package com.yiqiu.toolkit.data.repository

import com.yiqiu.toolkit.core.storage.KeyValueStore
import com.yiqiu.toolkit.data.model.DefaultPhraseGroups
import com.yiqiu.toolkit.data.model.PhraseGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * The persisted library of task-composer phrase groups.
 *
 * The whole library is stored as one JSON document under a single key — the
 * payload is tiny, and document-level writes keep the repository free of any
 * database dependency. UI layers observe [groups]; every mutation persists
 * immediately, so edits survive process death.
 */
class PhraseLibraryRepository(private val store: KeyValueStore) {

    private val _groups = MutableStateFlow(load())
    val groups: StateFlow<List<PhraseGroup>> = _groups.asStateFlow()

    /** Append [phrase] to the group with [groupId]; duplicates are ignored. */
    fun addPhrase(groupId: String, phrase: String) {
        val text = phrase.trim()
        if (text.isEmpty()) return
        updateGroup(groupId) { group ->
            if (text in group.phrases) group else group.copy(phrases = group.phrases + text)
        }
    }

    fun removePhrase(groupId: String, phrase: String) {
        updateGroup(groupId) { group -> group.copy(phrases = group.phrases - phrase) }
    }

    /** Create a new empty group; duplicate titles are ignored. */
    fun addGroup(title: String) {
        val name = title.trim()
        if (name.isEmpty()) return
        if (_groups.value.any { it.title == name }) return
        _groups.value = _groups.value + PhraseGroup(
            id = "group_${System.currentTimeMillis()}",
            title = name,
            phrases = emptyList(),
        )
        persist()
    }

    private fun updateGroup(groupId: String, transform: (PhraseGroup) -> PhraseGroup) {
        _groups.value = _groups.value.map { if (it.id == groupId) transform(it) else it }
        persist()
    }

    private fun persist() {
        val json = JSONArray().apply {
            _groups.value.forEach { group ->
                put(
                    JSONObject()
                        .put(KEY_ID, group.id)
                        .put(KEY_TITLE, group.title)
                        .put(KEY_PHRASES, JSONArray(group.phrases)),
                )
            }
        }
        store.putString(STORE_KEY, json.toString())
    }

    private fun load(): List<PhraseGroup> {
        val raw = store.getString(STORE_KEY) ?: return DefaultPhraseGroups
        return runCatching {
            val arr = JSONArray(raw)
            val groups = ArrayList<PhraseGroup>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val phraseArr = obj.getJSONArray(KEY_PHRASES)
                val phrases = ArrayList<String>(phraseArr.length())
                for (j in 0 until phraseArr.length()) {
                    phrases += phraseArr.getString(j)
                }
                groups += PhraseGroup(
                    id = obj.getString(KEY_ID),
                    title = obj.getString(KEY_TITLE),
                    phrases = phrases,
                )
            }
            groups
        }.getOrDefault(DefaultPhraseGroups)
    }

    private companion object {
        const val STORE_KEY = "phrase_library_v1"
        const val KEY_ID = "id"
        const val KEY_TITLE = "title"
        const val KEY_PHRASES = "phrases"
    }
}
