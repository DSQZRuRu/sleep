package com.somnia.tracking

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

data class SoundMinute(val avgDb: Float, val loudEvents: Int, val snoreLike: Boolean)

/**
 * Mesure du niveau sonore — AUCUN enregistrement :
 * chaque bloc audio de 0,5 s est réduit à un seul nombre (niveau RMS en dB) puis le tampon
 * est immédiatement réécrit par le bloc suivant. Rien n'est stocké ni transmis.
 */
class SoundLevelMonitor(private val context: Context) {

    @Volatile private var running = false
    private var thread: Thread? = null

    private val lock = Any()
    private var sumDb = 0.0
    private var blocks = 0
    private val eventTimes = mutableListOf<Long>()
    private var baselineDb = -50f

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (running || !hasPermission()) return false
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuf <= 0) return false
        val record = try {
            AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, max(minBuf, BLOCK_SAMPLES * 2))
        } catch (e: Exception) {
            return false
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return false
        }
        running = true
        thread = Thread({
            val buffer = ShortArray(BLOCK_SAMPLES)
            try {
                record.startRecording()
                while (running) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) onBlock(buffer, read)
                }
            } catch (_: Exception) {
            } finally {
                runCatching { record.stop() }
                record.release()
            }
        }, "somnia-sound").apply { priority = Thread.MIN_PRIORITY; start() }
        return true
    }

    fun stop() {
        running = false
        thread?.join(1000)
        thread = null
    }

    private fun onBlock(buffer: ShortArray, read: Int) {
        var sum = 0.0
        for (i in 0 until read) { val s = buffer[i].toDouble(); sum += s * s }
        val rms = sqrt(sum / read)
        val db = (20 * log10(max(rms, 1.0) / 32768.0)).toFloat()
        synchronized(lock) {
            sumDb += db; blocks++
            // Fond sonore : descend vite, monte lentement.
            baselineDb = if (db < baselineDb) baselineDb + (db - baselineDb) * 0.2f else baselineDb + (db - baselineDb) * 0.005f
            if (db > baselineDb + LOUD_MARGIN_DB) eventTimes += System.currentTimeMillis()
        }
    }

    fun drain(): SoundMinute = synchronized(lock) {
        val avg = if (blocks > 0) (sumDb / blocks).toFloat() else 0f
        val events = eventTimes.toList()
        sumDb = 0.0; blocks = 0; eventTimes.clear()
        SoundMinute(avg, events.size, isSnoreLike(events))
    }

    /** Ronflement probable : au moins 4 pics réguliers espacés de 2 à 7 secondes. */
    private fun isSnoreLike(times: List<Long>): Boolean {
        if (times.size < 4) return false
        var regular = 0
        for (i in 1 until times.size) {
            val gap = times[i] - times[i - 1]
            if (gap in 2_000..7_000) regular++
        }
        return regular >= 3
    }

    companion object {
        const val SAMPLE_RATE = 8000
        const val BLOCK_SAMPLES = 4000 // 0,5 s
        const val LOUD_MARGIN_DB = 15f
    }
}
