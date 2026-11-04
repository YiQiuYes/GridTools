package com.yiqiu.toolkit.core.storage

/**
 * Minimal persistence abstraction for the app.
 *
 * Features persist small pieces of state (phrase libraries, preferences, …)
 * through this interface instead of touching Android storage APIs directly,
 * so the backend can be swapped (SharedPreferences → DataStore → Room)
 * without touching feature or repository code.
 */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
}
