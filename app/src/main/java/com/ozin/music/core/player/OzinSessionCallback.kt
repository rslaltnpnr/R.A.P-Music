package com.ozin.music.core.player

import androidx.media3.session.MediaSession

/**
 * Default-accepting session callback: every connecting controller (system
 * media UI, Bluetooth headset, Android Auto, lockscreen, notification) gets
 * full playback command access. Kept as an explicit class so behavior can be
 * tightened per-controller later without touching [PlaybackService].
 */
class OzinSessionCallback : MediaSession.Callback
