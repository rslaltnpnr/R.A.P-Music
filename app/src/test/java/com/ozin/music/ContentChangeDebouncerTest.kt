package com.ozin.music

import com.ozin.music.core.domain.ContentChangeDebouncer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentChangeDebouncerTest {

    @Test
    fun `does not fire before the debounce window elapses`() {
        val debouncer = ContentChangeDebouncer(debounceMs = 1000)
        debouncer.onChange(0)
        assertFalse(debouncer.shouldFire(500))
    }

    @Test
    fun `fires once the debounce window has elapsed since the last change`() {
        val debouncer = ContentChangeDebouncer(debounceMs = 1000)
        debouncer.onChange(0)
        assertTrue(debouncer.shouldFire(1000))
    }

    @Test
    fun `a burst of changes resets the quiet window each time`() {
        val debouncer = ContentChangeDebouncer(debounceMs = 1000)
        debouncer.onChange(0)
        debouncer.onChange(500)
        debouncer.onChange(900)
        // Only 400ms of quiet since the last change at t=900.
        assertFalse(debouncer.shouldFire(1300))
        assertTrue(debouncer.shouldFire(1900))
    }

    @Test
    fun `fires only once per burst`() {
        val debouncer = ContentChangeDebouncer(debounceMs = 1000)
        debouncer.onChange(0)
        assertTrue(debouncer.shouldFire(1000))
        assertFalse(debouncer.shouldFire(1001))
    }

    @Test
    fun `no pending change never fires`() {
        val debouncer = ContentChangeDebouncer(debounceMs = 1000)
        assertFalse(debouncer.shouldFire(10_000))
        assertFalse(debouncer.hasPending())
    }

    @Test
    fun `reset clears pending state`() {
        val debouncer = ContentChangeDebouncer(debounceMs = 1000)
        debouncer.onChange(0)
        debouncer.reset()
        assertFalse(debouncer.hasPending())
        assertFalse(debouncer.shouldFire(1000))
    }
}
