package com.ozin.music.core.domain

import com.ozin.music.core.data.model.Song

/**
 * Pure in-memory queue manipulation logic, independent of ExoPlayer's
 * Timeline, so it can be unit tested directly. [com.ozin.music.core.player]
 * mirrors these operations onto the real MediaController/ExoPlayer timeline.
 */
class QueueManager {
    private val _queue = mutableListOf<Song>()
    val queue: List<Song> get() = _queue.toList()

    var currentIndex: Int = -1
        private set

    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        _queue.clear()
        _queue.addAll(songs)
        currentIndex = startIndex.coerceIn(0, (songs.size - 1).coerceAtLeast(0))
    }

    fun playNext(song: Song) {
        val insertAt = (currentIndex + 1).coerceIn(0, _queue.size)
        _queue.add(insertAt, song)
    }

    fun addToQueue(song: Song) {
        _queue.add(song)
    }

    fun removeAt(index: Int) {
        if (index !in _queue.indices) return
        _queue.removeAt(index)
        if (index < currentIndex) currentIndex--
        else if (index == currentIndex) currentIndex = currentIndex.coerceAtMost(_queue.size - 1)
    }

    fun move(from: Int, to: Int) {
        if (from !in _queue.indices || to !in _queue.indices) return
        val item = _queue.removeAt(from)
        _queue.add(to, item)
        currentIndex = when (currentIndex) {
            from -> to
            in (from + 1)..to -> currentIndex - 1
            in to until from -> currentIndex + 1
            else -> currentIndex
        }
    }

    fun current(): Song? = _queue.getOrNull(currentIndex)

    fun next(repeatAll: Boolean): Song? {
        if (_queue.isEmpty()) return null
        val nextIndex = currentIndex + 1
        return if (nextIndex < _queue.size) {
            currentIndex = nextIndex
            _queue[currentIndex]
        } else if (repeatAll) {
            currentIndex = 0
            _queue[currentIndex]
        } else {
            null
        }
    }

    fun previous(): Song? {
        if (_queue.isEmpty()) return null
        val prevIndex = currentIndex - 1
        if (prevIndex < 0) return null
        currentIndex = prevIndex
        return _queue[currentIndex]
    }
}
