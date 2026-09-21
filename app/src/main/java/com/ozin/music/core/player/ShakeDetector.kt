package com.ozin.music.core.player

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Real accelerometer-based shake detector: computes the gravity-subtracted
 * acceleration magnitude and fires [onShake] when it crosses
 * [SHAKE_THRESHOLD_G] with at least [MIN_INTERVAL_MS] since the last trigger
 * (debounce, so a single shake gesture - several accelerometer samples in a
 * row above threshold - fires once, not repeatedly).
 *
 * The listener is only registered while both "shake to pause" is enabled in
 * settings AND a song is actually playing (see [PlaybackService]), and is
 * unregistered the rest of the time, so an idle/disabled state costs
 * nothing - accelerometer sampling is otherwise a real, continuous battery
 * drain if left running unconditionally.
 */
class ShakeDetector(
    context: Context,
    private val onShake: () -> Unit,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var lastShakeAtMs = 0L

    fun start() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val gX = event.values[0] / SensorManager.GRAVITY_EARTH
        val gY = event.values[1] / SensorManager.GRAVITY_EARTH
        val gZ = event.values[2] / SensorManager.GRAVITY_EARTH
        // Magnitude of acceleration in units of g, with gravity (the
        // stationary 1g baseline) already implicitly subtracted out by
        // comparing the combined magnitude against ~1g + threshold rather
        // than against zero.
        val magnitude = sqrt(gX * gX + gY * gY + gZ * gZ)

        if (magnitude > SHAKE_THRESHOLD_G) {
            val now = System.currentTimeMillis()
            if (now - lastShakeAtMs > MIN_INTERVAL_MS) {
                lastShakeAtMs = now
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        /** Combined acceleration, in units of g, that counts as a shake.
         * 1g is the stationary baseline, so this requires roughly 1.7g of
         * extra force - a deliberate, noticeably forceful shake rather than
         * everyday handling/walking vibration. */
        private const val SHAKE_THRESHOLD_G = 2.7f

        /** Minimum time between two accepted shakes, so one physical shake
         * gesture (which produces many samples above threshold) triggers the
         * action once. */
        private const val MIN_INTERVAL_MS = 1000L
    }
}
