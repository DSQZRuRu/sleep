package com.somnia.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Réveil doux : carillon de cloches synthétisé (gamme pentatonique) dont le volume monte
 * sur 60 s, et vibration de plus en plus présente. Joué sur le volume des alarmes.
 */
object GentleAlarm {
    private const val RATE = 22050
    @Volatile private var running = false
    private var thread: Thread? = null
    private var vibrator: Vibrator? = null

    @Synchronized
    fun start(context: Context) {
        if (running) return
        running = true
        thread = Thread({ play() }, "somnia-gentle-alarm").apply { start() }
        vibrate(context)
    }

    @Synchronized
    fun stop() {
        running = false
        thread?.join(500)
        thread = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun vibrate(context: Context) {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator = v
        // Impulsions de plus en plus fortes, puis répétition du dernier motif.
        val timings = longArrayOf(0, 120, 1800, 160, 1500, 220, 1200, 300, 900, 400, 700)
        val amps = intArrayOf(0, 40, 0, 80, 0, 130, 0, 190, 0, 255, 0)
        runCatching { v.vibrate(VibrationEffect.createWaveform(timings, amps, 7)) }
    }

    private fun play() {
        val minBuf = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(maxOf(minBuf, RATE))
                .build()
        }.getOrNull() ?: return
        // Do pentatonique, deux octaves : une mélodie lente qui tourne en boucle.
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 880.0, 1046.5, 880.0, 783.99, 659.25)
        val noteLen = (RATE * 0.9).toInt()
        val buf = ShortArray(noteLen)
        val started = System.currentTimeMillis()
        var n = 0
        try {
            track.play()
            while (running) {
                val f = notes[n % notes.size]
                val ramp = ((System.currentTimeMillis() - started) / 60_000.0).coerceIn(0.08, 1.0)
                for (i in 0 until noteLen) {
                    val t = i.toDouble() / RATE
                    // Cloche : fondamentale + partiels inharmoniques, décroissance exponentielle.
                    val v = (sin(2 * PI * f * t) * exp(-2.2 * t) +
                        0.45 * sin(2 * PI * f * 2.76 * t) * exp(-3.5 * t) +
                        0.2 * sin(2 * PI * f * 5.4 * t) * exp(-5.0 * t)) * 0.45 * ramp
                    buf[i] = (v.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
                }
                track.write(buf, 0, buf.size)
                n++
            }
        } catch (_: Exception) {
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }
}
