package com.ozin.music.core.data.mediastore

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.ozin.music.core.data.repository.SongRepository
import com.ozin.music.core.domain.ContentChangeDebouncer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registers a real `ContentObserver` on the MediaStore audio table so new,
 * changed or deleted tracks (e.g. added by another app, or synced in) are
 * picked up without the user having to open the Settings screen and tap
 * "Rescan library". Debounced via [ContentChangeDebouncer] so a burst of
 * per-file notifications (bulk copy/sync) triggers one rescan, not one per
 * file — battery-friendly by design.
 */
@Singleton
class MediaStoreChangeWatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songRepository: SongRepository,
) {
    private val debouncer = ContentChangeDebouncer(debounceMs = 3_000L)
    private val scope = CoroutineScope(Dispatchers.Default)
    private var pollJob: Job? = null
    private var observer: ContentObserver? = null

    fun start() {
        if (observer != null) return
        val obs = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                debouncer.onChange(System.currentTimeMillis())
            }
        }
        observer = obs
        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            /* notifyForDescendants = */ true,
            obs,
        )
        pollJob = scope.launch {
            while (isActive) {
                delay(1_000)
                if (debouncer.shouldFire(System.currentTimeMillis())) {
                    runCatching { songRepository.rescan() }
                }
            }
        }
    }

    fun stop() {
        observer?.let { context.contentResolver.unregisterContentObserver(it) }
        observer = null
        pollJob?.cancel()
        pollJob = null
        debouncer.reset()
    }
}
