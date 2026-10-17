package hu.apk.kidsapp.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed interface Step {
    /** A `res/raw` hangfájl neve (kiterjesztés nélkül), pl. "voice_num_3". */
    data class Clip(val name: String) : Step

    data class Pause(val ms: Long) : Step

    /** A képernyő állapotának léptetése a sorozat közben (a főszálon fut). */
    data class Mark(val action: () -> Unit) : Step
}

/**
 * Hangok és szünetek sorozatban. Új sorozat indításakor a régi leáll, így a gyerek gyors
 * koppintgatása nem csinál hanghalmazt.
 */
class ClipPlayer(private val context: Context, private val scope: CoroutineScope) {
    private var job: Job? = null
    private var current: MediaPlayer? = null

    fun play(vararg steps: Step) {
        stop()
        job = scope.launch {
            for (s in steps) {
                when (s) {
                    is Step.Clip -> playClip(s.name)
                    is Step.Pause -> delay(s.ms)
                    is Step.Mark -> s.action()
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        try {
            current?.release()
        } catch (_: Exception) {
        }
        current = null
    }

    private suspend fun playClip(name: String) {
        val id = context.resources.getIdentifier(name, "raw", context.packageName)
        if (id == 0) return
        suspendCancellableCoroutine<Unit> { cont ->
            val p = MediaPlayer()
            try {
                p.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                context.resources.openRawResourceFd(id).use { afd ->
                    p.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
                p.setOnCompletionListener {
                    try {
                        it.release()
                    } catch (_: Exception) {
                    }
                    if (current === it) current = null
                    if (cont.isActive) cont.resume(Unit)
                }
                p.setOnErrorListener { mp, _, _ ->
                    try {
                        mp.release()
                    } catch (_: Exception) {
                    }
                    if (current === mp) current = null
                    if (cont.isActive) cont.resume(Unit)
                    true
                }
                p.prepare()
                current = p
                p.start()
            } catch (_: Exception) {
                try {
                    p.release()
                } catch (_: Exception) {
                }
                if (cont.isActive) cont.resume(Unit)
            }
            cont.invokeOnCancellation {
                try {
                    p.release()
                } catch (_: Exception) {
                }
                if (current === p) current = null
            }
        }
    }
}

@Composable
fun rememberClipPlayer(): ClipPlayer {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val player = remember { ClipPlayer(context, scope) }
    DisposableEffect(player) { onDispose { player.stop() } }
    return player
}
