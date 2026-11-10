package com.somnia.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

enum class SoundKind(val label: String, val emoji: String) {
    PINK("Bruit rose", "🌫"),
    BROWN("Bruit brun", "🟤"),
    WAVES("Vagues", "🌊"),
    RAIN("Pluie", "🌧"),
}

data class SoundState(val playing: SoundKind? = null, val endsAt: Long? = null)

/**
 * Sons d'endormissement synthétisés en temps réel (aucun fichier audio) :
 * bruit rose (filtre de Paul Kellet), bruit brun (bruit blanc intégré), vagues (bruit brun
 * modulé lentement) et pluie (bruit rose + gouttes aléatoires). Fondu de sortie en fin de minuteur.
 */
object Soundscape {
    private const val RATE = 22050
    private val _state = MutableStateFlow(SoundState())
    val state: StateFlow<SoundState> = _state.asStateFlow()
    val isPlaying: Boolean get() = _state.value.playing != null

    @Volatile private var thread: Thread? = null
    @Volatile private var stopRequested = false

    @Synchronized
    fun play(kind: SoundKind, minutes: Int) {
        stop()
        stopRequested = false
        val endsAt = System.currentTimeMillis() + minutes * 60_000L
        _state.value = SoundState(kind, endsAt)
        thread = Thread({ run(kind, endsAt) }, "somnia-soundscape").apply { start() }
    }

    @Synchronized
    fun stop() {
        stopRequested = true
        thread?.join(500)
        thread = null
        _state.value = SoundState()
    }

    private fun run(kind: SoundKind, endsAt: Long) {
        val minBuf = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(maxOf(minBuf, RATE))
                .build()
        } catch (e: Exception) {
            _state.value = SoundState(); return
        }
        val gen = Generator(kind)
        val buf = ShortArray(RATE / 10)
        try {
            track.play()
            var fadeIn = 0f
            while (!stopRequested) {
                val remaining = endsAt - System.currentTimeMillis()
                if (remaining <= 0) break
                val fadeOut = (remaining / 60_000f).coerceIn(0f, 1f) // dernière minute
                for (i in buf.indices) {
                    fadeIn = minOf(1f, fadeIn + 1f / (RATE * 3))
                    val v = gen.next() * fadeIn * fadeOut * 0.5f
                    buf[i] = (v.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
                }
                track.write(buf, 0, buf.size)
            }
        } catch (_: Exception) {
        } finally {
            runCatching { track.stop() }
            track.release()
            if (!stopRequested) _state.value = SoundState()
        }
    }

    private class Generator(private val kind: SoundKind) {
        private val rnd = Random(System.nanoTime())
        private var b0 = 0f; private var b1 = 0f; private var b2 = 0f; private var b3 = 0f; private var b4 = 0f; private var b5 = 0f; private var b6 = 0f
        private var brown = 0f
        private var t = 0L
        private var drop = 0f
        private var dropPhase = 0f

        private fun white() = rnd.nextFloat() * 2f - 1f

        private fun pink(): Float {
            val w = white()
            b0 = 0.99886f * b0 + w * 0.0555179f; b1 = 0.99332f * b1 + w * 0.0750759f
            b2 = 0.96900f * b2 + w * 0.1538520f; b3 = 0.86650f * b3 + w * 0.3104856f
            b4 = 0.55000f * b4 + w * 0.5329522f; b5 = -0.7616f * b5 - w * 0.0168980f
            val p = b0 + b1 + b2 + b3 + b4 + b5 + b6 + w * 0.5362f
            b6 = w * 0.115926f
            return p * 0.11f
        }

        private fun brownNoise(): Float {
            brown = (brown + white() * 0.02f) * 0.998f
            return brown * 3.2f
        }

        fun next(): Float {
            t++
            return when (kind) {
                SoundKind.PINK -> pink()
                SoundKind.BROWN -> brownNoise()
                SoundKind.WAVES -> {
                    val s = (t.toDouble() / RATE)
                    val swell = (0.55 + 0.45 * sin(2 * PI * s / 9.0)).toFloat()
                    brownNoise() * swell * swell + pink() * 0.15f * swell
                }
                SoundKind.RAIN -> {
                    if (drop <= 0.001f && rnd.nextInt(RATE / 30) == 0) { drop = 0.4f + rnd.nextFloat() * 0.5f; dropPhase = 0f }
                    var d = 0f
                    if (drop > 0.001f) {
                        dropPhase += 0.9f
                        d = sin(dropPhase) * drop * 0.3f
                        drop *= 0.992f
                    }
                    pink() * 0.7f + d
                }
            }
        }
    }
}
