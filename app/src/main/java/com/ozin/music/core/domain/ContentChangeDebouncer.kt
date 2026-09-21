package com.ozin.music.core.domain

/**
 * Pure debounce policy for `ContentObserver` change notifications on the
 * MediaStore audio table. `MediaStoreChangeWatcher` (Android-specific, not
 * unit-testable) feeds every observed change timestamp into [onChange] and
 * calls the resulting decision's `shouldFire` action once, after
 * [debounceMs] of quiet, no matter how many change events arrived in that
 * window. This keeps a burst of per-file MediaStore notifications (e.g. a
 * bulk copy/sync) from triggering one incremental rescan per file.
 */
class ContentChangeDebouncer(private val debounceMs: Long = 3_000L) {

    private var pendingSinceMs: Long? = null
    private var lastChangeAtMs: Long = 0L

    /** Call on every raw observer callback with the current wall-clock time. */
    fun onChange(nowMs: Long) {
        if (pendingSinceMs == null) pendingSinceMs = nowMs
        lastChangeAtMs = nowMs
    }

    /**
     * Call periodically (e.g. every second) with the current wall-clock
     * time. Returns true at most once per burst, exactly when [debounceMs]
     * has elapsed since the last observed change, and resets afterward.
     */
    fun shouldFire(nowMs: Long): Boolean {
        val pending = pendingSinceMs ?: return false
        if (nowMs - lastChangeAtMs < debounceMs) return false
        pendingSinceMs = null
        return true
    }

    fun hasPending(): Boolean = pendingSinceMs != null

    fun reset() {
        pendingSinceMs = null
        lastChangeAtMs = 0L
    }
}
