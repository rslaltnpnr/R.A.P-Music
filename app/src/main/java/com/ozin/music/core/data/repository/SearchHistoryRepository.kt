package com.ozin.music.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.searchHistoryDataStore by preferencesDataStore(name = "ozin_search_history")

/**
 * Persists the last [MAX_ENTRIES] distinct library search queries, most
 * recent first, in the same DataStore-Preferences pattern
 * [com.ozin.music.core.settings.SettingsRepository] uses. A single
 * pipe-joined string (rather than `stringSetPreferencesKey`) is used
 * deliberately because a `Set` has no stable order, and recency order is the
 * whole point here.
 */
@Singleton
class SearchHistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val HISTORY = stringPreferencesKey("recent_queries")
    }

    companion object {
        private const val MAX_ENTRIES = 10
        private const val SEPARATOR = "\u0001"
    }

    val recentQueries: Flow<List<String>> = context.searchHistoryDataStore.data.map { prefs ->
        decode(prefs[Keys.HISTORY])
    }

    suspend fun record(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        context.searchHistoryDataStore.edit { prefs ->
            val current = decode(prefs[Keys.HISTORY])
            val updated = (listOf(trimmed) + current.filterNot { it.equals(trimmed, ignoreCase = true) })
                .take(MAX_ENTRIES)
            prefs[Keys.HISTORY] = updated.joinToString(SEPARATOR)
        }
    }

    suspend fun clear() {
        context.searchHistoryDataStore.edit { prefs -> prefs[Keys.HISTORY] = "" }
    }

    private fun decode(raw: String?): List<String> =
        raw?.takeIf { it.isNotBlank() }?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()
}
