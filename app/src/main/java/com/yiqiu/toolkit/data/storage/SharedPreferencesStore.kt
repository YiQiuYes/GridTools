package com.yiqiu.toolkit.data.storage

import android.content.Context
import com.yiqiu.toolkit.core.storage.KeyValueStore

/**
 * [KeyValueStore] backed by [SharedPreferences].
 *
 * Writes are async (`apply`); the payloads stored through this layer are tiny
 * documents, so read-modify-write races within one process are not a concern.
 */
class SharedPreferencesStore(context: Context) : KeyValueStore {

    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    private companion object {
        const val FILE_NAME = "toolkit_store"
    }
}
