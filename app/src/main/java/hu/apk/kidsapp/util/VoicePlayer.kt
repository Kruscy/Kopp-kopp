package hu.apk.kidsapp.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.SystemClock
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import hu.apk.kidsapp.R

/** A felmondható hangos mondatok (ElevenLabs, "Ana Rita" hang). */
enum class Voice(@RawRes val res: Int, val cooldownMs: Long) {
    POUR_WIN(R.raw.voice_pour_win, 0),
    POUR_CANT(R.raw.voice_pour_cant, 6_000),
    POUR_EMPTY(R.raw.voice_pour_empty, 6_000),
    CAKE_DONE(R.raw.voice_cake_done, 8_000),
    CAKE_NEED_LAYER(R.raw.voice_cake_need_layer, 8_000),
    CAKE_DRAG_OUT(R.raw.voice_cake_drag_out, 8_000),
    BLOCKS_FULL(R.raw.voice_blocks_full, 6_000),
    BLOCKS_MISS(R.raw.voice_blocks_miss, 8_000),
    SNAKE_NYAM(R.raw.voice_snake_nyam, 1_500),
    CAMERA_READY(R.raw.voice_camera_ready, 10_000),
    CAMERA_HELP(R.raw.voice_camera_help, 12_000),
    DRAW_SAVED(R.raw.voice_draw_saved, 0),
    GOAL(R.raw.voice_goal, 0),
    WINNER(R.raw.voice_winner, 0),
    GAME_START(R.raw.voice_game_start, 0),
}

/**
 * Egyszerre egy mondat szól: ha épp beszél, az új kérés elmarad (a gyerek össze-vissza nyomkodása
 * se csináljon belőle hanghalmazt). Minden mondatnak van saját várakozási ideje is.
 */
class VoicePlayer(private val context: Context) {
    private var player: MediaPlayer? = null
    private var playing = false
    private val lastPlayed = mutableMapOf<Voice, Long>()

    fun play(voice: Voice) {
        if (playing) return
        val now = SystemClock.elapsedRealtime()
        val last = lastPlayed[voice]
        if (last != null && now - last < voice.cooldownMs) return
        try {
            val p = MediaPlayer()
            p.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            context.resources.openRawResourceFd(voice.res).use { afd ->
                p.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            p.setOnCompletionListener {
                playing = false
                it.release()
                if (player === it) player = null
            }
            p.setOnErrorListener { mp, _, _ ->
                playing = false
                mp.release()
                if (player === mp) player = null
                true
            }
            p.prepare()
            player = p
            playing = true
            lastPlayed[voice] = now
            p.start()
        } catch (_: Exception) {
            playing = false
            player?.release()
            player = null
        }
    }

    fun release() {
        playing = false
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null
    }
}

@Composable
fun rememberVoicePlayer(): VoicePlayer {
    val context = LocalContext.current.applicationContext
    val player = remember { VoicePlayer(context) }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}
