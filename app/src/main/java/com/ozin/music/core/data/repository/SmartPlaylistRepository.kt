package com.ozin.music.core.data.repository

import com.ozin.music.core.data.local.SmartPlaylistDao
import com.ozin.music.core.data.model.SmartPlaylist
import com.ozin.music.core.data.model.Song
import com.ozin.music.core.domain.BuiltInSmartPlaylists
import com.ozin.music.core.domain.SmartPlaylistEngine
import com.ozin.music.core.domain.SmartPlaylistRule
import com.ozin.music.core.domain.SmartPlaylistRuleCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartPlaylistRepository @Inject constructor(
    private val smartPlaylistDao: SmartPlaylistDao,
    private val songRepository: SongRepository,
) {
    val playlists: Flow<List<SmartPlaylist>> = smartPlaylistDao.observeAll()

    init {
        // First-run seed of the 3 built-in example smart playlists, expressed
        // through the same rule engine used for user-created ones.
        CoroutineScope(Dispatchers.IO).launch { seedBuiltInsIfNeeded() }
    }

    private suspend fun seedBuiltInsIfNeeded() {
        if (smartPlaylistDao.count() > 0) return
        listOf(
            BuiltInSmartPlaylists.RECENTLY_ADDED_NAME to BuiltInSmartPlaylists.recentlyAdded(),
            BuiltInSmartPlaylists.POPULAR_THIS_WEEK_NAME to BuiltInSmartPlaylists.popularThisWeek(),
            BuiltInSmartPlaylists.NEGLECTED_FAVORITES_NAME to BuiltInSmartPlaylists.neglectedFavorites(),
        ).forEach { (name, rules) ->
            smartPlaylistDao.insert(
                SmartPlaylist(name = name, rulesEncoded = SmartPlaylistRuleCodec.encode(rules), isBuiltIn = true)
            )
        }
    }

    suspend fun create(name: String, rules: List<SmartPlaylistRule>): Long =
        smartPlaylistDao.insert(SmartPlaylist(name = name, rulesEncoded = SmartPlaylistRuleCodec.encode(rules)))

    suspend fun updateRules(playlist: SmartPlaylist, name: String, rules: List<SmartPlaylistRule>) =
        smartPlaylistDao.update(playlist.copy(name = name, rulesEncoded = SmartPlaylistRuleCodec.encode(rules)))

    suspend fun delete(playlist: SmartPlaylist) = smartPlaylistDao.delete(playlist)

    fun rulesOf(playlist: SmartPlaylist): List<SmartPlaylistRule> = SmartPlaylistRuleCodec.decode(playlist.rulesEncoded)

    /** Live-evaluated song list: re-derives whenever the underlying library
     * (favorites, ratings, play counts, rescans, ...) changes. */
    fun evaluate(playlist: SmartPlaylist): Flow<List<Song>> =
        songRepository.songs.map { songs -> SmartPlaylistEngine.evaluate(songs, rulesOf(playlist)) }
}
