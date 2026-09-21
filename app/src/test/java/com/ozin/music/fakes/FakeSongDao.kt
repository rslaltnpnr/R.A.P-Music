package com.ozin.music.fakes

import com.ozin.music.core.data.local.SongDao
import com.ozin.music.core.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory fake standing in for Room's SongDao in JVM unit tests. */
class FakeSongDao : SongDao {

    private val songs = mutableMapOf<Long, Song>()
    private val _all = MutableStateFlow<List<Song>>(emptyList())

    private fun emit() {
        _all.value = songs.values.sortedBy { it.title }
    }

    override fun observeAll(): StateFlow<List<Song>> = _all.asStateFlow()
    override fun observeFavorites() = MutableStateFlow(songs.values.filter { it.isFavorite }).asStateFlow()
    override fun observeRecentlyAdded(limit: Int) =
        MutableStateFlow(songs.values.sortedByDescending { it.dateAdded }.take(limit)).asStateFlow()
    override fun observeRecentlyPlayed(limit: Int) =
        MutableStateFlow(songs.values.filter { it.lastPlayedAt > 0 }.sortedByDescending { it.lastPlayedAt }.take(limit)).asStateFlow()
    override fun observeMostPlayed(limit: Int) =
        MutableStateFlow(songs.values.filter { it.playCount > 0 }.sortedByDescending { it.playCount }.take(limit)).asStateFlow()
    override fun search(query: String) =
        MutableStateFlow(songs.values.filter { it.title.contains(query, true) }).asStateFlow()

    override suspend fun getById(id: Long): Song? = songs[id]
    override suspend fun getAllIds(): List<Long> = songs.keys.toList()

    override suspend fun upsertAll(songs: List<Song>) {
        songs.forEach { this.songs[it.id] = it }
        emit()
    }

    override suspend fun upsert(song: Song) {
        songs[song.id] = song
        emit()
    }

    override suspend fun update(song: Song) {
        songs[song.id] = song
        emit()
    }

    override suspend fun deleteByIds(ids: List<Long>) {
        ids.forEach { songs.remove(it) }
        emit()
    }

    override suspend fun setFavorite(id: Long, favorite: Boolean) {
        songs[id]?.let { songs[id] = it.copy(isFavorite = favorite) }
        emit()
    }

    override suspend fun recordPlay(id: Long, timestamp: Long) {
        songs[id]?.let { songs[id] = it.copy(playCount = it.playCount + 1, lastPlayedAt = timestamp) }
        emit()
    }

    override suspend fun setRating(id: Long, rating: Int) {
        songs[id]?.let { songs[id] = it.copy(rating = rating) }
        emit()
    }

    override suspend fun setMoodTags(id: Long, moodTags: String) {
        songs[id]?.let { songs[id] = it.copy(moodTags = moodTags) }
        emit()
    }

    override suspend fun delete(song: Song) {
        songs.remove(song.id)
        emit()
    }
}
