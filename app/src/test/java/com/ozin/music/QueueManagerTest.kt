package com.ozin.music

import com.ozin.music.core.data.model.Song
import com.ozin.music.core.domain.QueueManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class QueueManagerTest {

    private fun song(id: Long) = Song(
        id = id, title = "Song $id", artist = "Artist", album = "Album", albumId = 1,
        durationMs = 1000, path = "/music/$id.mp3", sizeBytes = 10, year = 2020,
        trackNumber = 1, dateAdded = 0,
    )

    private lateinit var manager: QueueManager

    @Before
    fun setUp() {
        manager = QueueManager()
        manager.setQueue((1..5).map { song(it.toLong()) }, startIndex = 0)
    }

    @Test
    fun `next advances through queue`() {
        assertEquals(1L, manager.current()?.id)
        assertEquals(2L, manager.next(repeatAll = false)?.id)
        assertEquals(3L, manager.next(repeatAll = false)?.id)
    }

    @Test
    fun `next returns null at end without repeat`() {
        repeat(4) { manager.next(repeatAll = false) }
        assertNull(manager.next(repeatAll = false))
    }

    @Test
    fun `next wraps around with repeat all`() {
        repeat(4) { manager.next(repeatAll = true) }
        assertEquals(5L, manager.current()?.id)
        assertEquals(1L, manager.next(repeatAll = true)?.id)
    }

    @Test
    fun `previous moves back`() {
        manager.next(repeatAll = false)
        manager.next(repeatAll = false)
        assertEquals(2L, manager.previous()?.id)
    }

    @Test
    fun `playNext inserts right after current`() {
        manager.playNext(song(99))
        assertEquals(99L, manager.queue[1].id)
    }

    @Test
    fun `addToQueue appends at end`() {
        manager.addToQueue(song(99))
        assertEquals(99L, manager.queue.last().id)
    }

    @Test
    fun `removeAt before current shifts current index back`() {
        manager.setQueue((1..5).map { song(it.toLong()) }, startIndex = 2)
        manager.removeAt(0)
        assertEquals(3L, manager.current()?.id)
    }

    @Test
    fun `move reorders and tracks current index`() {
        manager.setQueue((1..5).map { song(it.toLong()) }, startIndex = 0)
        manager.move(0, 3)
        assertEquals(1L, manager.current()?.id)
        assertEquals(3, manager.currentIndex)
    }
}
