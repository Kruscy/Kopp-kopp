package hu.apk.kidsapp.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Egyszerű, programból generált hangeffektek (pattogás, ütés, gól), hangfájlok nélkül.
 * A rövid hangokat 16 bites WAV-ként a gyorsítótárba írja, és SoundPool-lal játssza le.
 */
class SoundFx(context: Context) {
    private val pool: SoundPool? = try {
        SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
    } catch (_: Exception) {
        null
    }
    private val ids = HashMap<String, Int>()

    init {
        try {
            for ((name, notes) in SOUNDS) {
                val file = File(context.cacheDir, "sfx_$name.wav")
                if (!file.exists() || file.length() == 0L) file.writeBytes(wav(notes))
                pool?.load(file.absolutePath, 1)?.let { ids[name] = it }
            }
        } catch (_: Exception) {
        }
    }

    fun play(name: String, rate: Float = 1f, volume: Float = 1f) {
        val id = ids[name] ?: return
        try {
            pool?.play(id, volume, volume, 1, 0, rate.coerceIn(0.5f, 2f))
        } catch (_: Exception) {
        }
    }

    fun release() {
        try {
            pool?.release()
        } catch (_: Exception) {
        }
    }

    /** (frekvencia Hz, hossz ms, hangerő) hangjegyek egymás után; exponenciálisan lecsengő szinusz. */
    private class Note(
        val freq: Double,
        val ms: Int,
        val gain: Double,
        val harmonic: Double = 0.35,
        val sweepTo: Double = freq,
        val flat: Boolean = false,
        val h3: Double = 0.0,
    )

    private companion object {
        const val RATE = 22050

        val SOUNDS = mapOf(
            "paddle" to listOf(Note(740.0, 110, 0.9)),
            "wall" to listOf(Note(420.0, 70, 0.7, 0.2)),
            "ding" to listOf(Note(880.0, 110, 0.8), Note(1319.0, 260, 0.9)),
            "pop" to listOf(Note(900.0, 90, 0.8, 0.2, sweepTo = 450.0)),
            "boing" to listOf(Note(320.0, 320, 0.8, 0.25, sweepTo = 760.0)),
            "honk" to listOf(Note(390.0, 170, 0.8, 0.5, flat = true), Note(310.0, 260, 0.8, 0.5, flat = true)),
            "chirp" to listOf(Note(1800.0, 80, 0.6, 0.1, sweepTo = 2700.0), Note(2500.0, 80, 0.6, 0.1, sweepTo = 1900.0)),
            "roar" to listOf(Note(150.0, 700, 0.9, 0.9, sweepTo = 70.0, flat = true)),
            "whoosh" to listOf(Note(250.0, 700, 0.7, 0.3, sweepTo = 1500.0, flat = true)),
            "n0" to listOf(Note(880.0, 520, 0.8, 0.3)),
            "n1" to listOf(Note(784.0, 520, 0.8, 0.3)),
            "n2" to listOf(Note(659.3, 560, 0.8, 0.3)),
            "n3" to listOf(Note(587.3, 560, 0.8, 0.3)),
            "n4" to listOf(Note(523.3, 600, 0.9, 0.3)),
            "n5" to listOf(Note(150.0, 260, 1.0, 0.4, sweepTo = 60.0)),
            "p0" to listOf(Note(261.63, 1500, 0.9, 0.5, h3 = 0.25)),
            "p1" to listOf(Note(277.18, 1500, 0.9, 0.5, h3 = 0.25)),
            "p2" to listOf(Note(293.66, 1500, 0.9, 0.5, h3 = 0.25)),
            "p3" to listOf(Note(311.13, 1500, 0.9, 0.5, h3 = 0.25)),
            "p4" to listOf(Note(329.63, 1500, 0.9, 0.5, h3 = 0.25)),
            "p5" to listOf(Note(349.23, 1500, 0.9, 0.5, h3 = 0.25)),
            "p6" to listOf(Note(369.99, 1500, 0.9, 0.5, h3 = 0.25)),
            "p7" to listOf(Note(392.0, 1500, 0.9, 0.5, h3 = 0.25)),
            "p8" to listOf(Note(415.3, 1500, 0.9, 0.5, h3 = 0.25)),
            "p9" to listOf(Note(440.0, 1500, 0.9, 0.5, h3 = 0.25)),
            "p10" to listOf(Note(466.16, 1500, 0.9, 0.5, h3 = 0.25)),
            "p11" to listOf(Note(493.88, 1500, 0.9, 0.5, h3 = 0.25)),
            "p12" to listOf(Note(523.25, 1500, 0.9, 0.5, h3 = 0.25)),
            "goal" to listOf(Note(523.0, 120, 0.8), Note(659.0, 120, 0.8), Note(784.0, 120, 0.8), Note(1047.0, 320, 0.9)),
            "win" to listOf(
                Note(523.0, 140, 0.8), Note(523.0, 140, 0.8), Note(784.0, 140, 0.8),
                Note(659.0, 140, 0.8), Note(784.0, 140, 0.8), Note(1047.0, 520, 0.9),
            ),
        )

        fun wav(notes: List<Note>): ByteArray {
            val pcm = ByteArrayOutputStream()
            for (n in notes) {
                val count = RATE * n.ms / 1000
                val decay = 7.0 / (n.ms / 1000.0)
                var phase = 0.0
                for (i in 0 until count) {
                    val t = i / RATE.toDouble()
                    // a hang magassága a hossz mentén csúszhat (n.sweepTo), a fázist összegezzük
                    val f = n.freq + (n.sweepTo - n.freq) * (i / count.toDouble())
                    phase += 2 * PI * f / RATE
                    val env = if (n.flat) minOf(1.0, i / 60.0) * minOf(1.0, (count - i) / 220.0) else exp(-decay * t) * minOf(1.0, i / 60.0)
                    val v = (sin(phase) + n.harmonic * sin(2 * phase) + n.h3 * sin(3 * phase)) * env * n.gain * 0.55
                    val sample = (v * 32767).toInt().coerceIn(-32768, 32767)
                    pcm.write(sample and 0xFF)
                    pcm.write((sample shr 8) and 0xFF)
                }
            }
            val data = pcm.toByteArray()
            val out = ByteArrayOutputStream()
            fun int(v: Int) { out.write(v and 0xFF); out.write((v shr 8) and 0xFF); out.write((v shr 16) and 0xFF); out.write((v shr 24) and 0xFF) }
            fun short(v: Int) { out.write(v and 0xFF); out.write((v shr 8) and 0xFF) }
            out.write("RIFF".toByteArray()); int(36 + data.size); out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray()); int(16); short(1); short(1); int(RATE); int(RATE * 2); short(2); short(16)
            out.write("data".toByteArray()); int(data.size)
            out.write(data)
            return out.toByteArray()
        }
    }
}
