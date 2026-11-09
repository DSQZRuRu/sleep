package com.somnia.tracking

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

/**
 * Actigraphie par l'accéléromètre, téléphone posé sur le matelas.
 * Échantillonnage volontairement faible (~5 Hz) avec regroupement matériel des événements
 * (batching 20 s) : le capteur s'accumule dans le hub sans réveiller le processeur en continu.
 */
class MotionSensorMonitor(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val lock = Any()
    private var lastX = Float.NaN
    private var lastY = 0f
    private var lastZ = 0f
    private var movementSamples = 0

    val available: Boolean get() = accelerometer != null

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SAMPLING_PERIOD_US, MAX_REPORT_LATENCY_US)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    /** Nombre d'échantillons « en mouvement » depuis le dernier appel, puis remise à zéro. */
    fun drain(): Float = synchronized(lock) {
        val v = movementSamples.toFloat()
        movementSamples = 0
        v
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        synchronized(lock) {
            if (!lastX.isNaN()) {
                val delta = abs(x - lastX) + abs(y - lastY) + abs(z - lastZ)
                if (delta > MOVEMENT_THRESHOLD) movementSamples++
            }
            lastX = x; lastY = y; lastZ = z
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        const val SAMPLING_PERIOD_US = 200_000      // 5 Hz
        const val MAX_REPORT_LATENCY_US = 20_000_000 // 20 s de batching
        /** m/s² cumulés sur les 3 axes entre deux échantillons, au-dessus du bruit du capteur. */
        const val MOVEMENT_THRESHOLD = 0.12f
    }
}
