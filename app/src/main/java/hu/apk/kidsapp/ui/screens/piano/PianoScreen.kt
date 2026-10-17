package hu.apk.kidsapp.ui.screens.piano

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import hu.apk.kidsapp.ui.components.BackButton
import hu.apk.kidsapp.ui.theme.KidBackground
import hu.apk.kidsapp.ui.theme.KidText
import hu.apk.kidsapp.util.SoundFx

private const val WHITE_KEYS = 8

// a fehér billentyűk hangjai (p0..p12 a félhangokkal együtt: C, C#, D, D#, E, F, F#, G, G#, A, A#, H, C)
private val whiteSound = intArrayOf(0, 2, 4, 5, 7, 9, 11, 12)

// a fekete billentyűk: a fehér billentyű sorszáma, amelynek a jobb szélén ülnek, és a hangjuk
private val blackKeys = listOf(0 to 1, 1 to 3, 3 to 6, 4 to 8, 5 to 10)

private const val BLACK_HEIGHT = 0.6f
private const val BLACK_WIDTH = 0.58f

/** Egyszerű zongora: egy oktáv (C-től C-ig) fehér és fekete billentyűvel, több ujjal is játszható. */
@Composable
fun PianoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sfx = remember { SoundFx(context.applicationContext) }
    DisposableEffect(sfx) { onDispose { sfx.release() } }

    var size by remember { mutableStateOf(IntSize.Zero) }
    // ujj azonosító -> a lenyomott hang sorszáma
    val pressed = remember { mutableStateMapOf<Long, Int>() }

    fun soundAt(p: Offset): Int? {
        if (size.width <= 0 || p.x < 0f || p.y < 0f || p.x > size.width || p.y > size.height) return null
        val ww = size.width / WHITE_KEYS.toFloat()
        if (p.y < size.height * BLACK_HEIGHT) {
            for ((after, snd) in blackKeys) {
                val cx = ww * (after + 1)
                if (p.x in (cx - ww * BLACK_WIDTH / 2f)..(cx + ww * BLACK_WIDTH / 2f)) return snd
            }
        }
        val i = (p.x / ww).toInt().coerceIn(0, WHITE_KEYS - 1)
        return whiteSound[i]
    }

    Column(modifier = Modifier.fillMaxSize().background(KidBackground).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            BackButton(onClick = onBack)
            Spacer(Modifier.weight(1f))
            Text("Zongora", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = KidText)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(1.dp))
        }
        Spacer(Modifier.size(16.dp))
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { size = it }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val ev = awaitPointerEvent()
                            for (c in ev.changes) {
                                val id = c.id.value
                                if (c.pressed) {
                                    val k = soundAt(c.position)
                                    if (k != pressed[id]) {
                                        if (k != null) {
                                            pressed[id] = k
                                            sfx.play("p$k")
                                        } else {
                                            pressed.remove(id)
                                        }
                                    }
                                } else {
                                    pressed.remove(id)
                                }
                                c.consume()
                            }
                        }
                    }
                },
        ) {
            val ww = size.width / WHITE_KEYS.toFloat()
            val h = size.height.toFloat()
            val down = pressed.values.toSet()
            val gap = 3f
            for (i in 0 until WHITE_KEYS) {
                val isDown = whiteSound[i] in down
                drawRoundRect(
                    color = if (isDown) Color(0xFFBBDEFB) else Color.White,
                    topLeft = Offset(ww * i + gap, 0f),
                    size = Size(ww - gap * 2f, h),
                    cornerRadius = CornerRadius(ww * 0.12f),
                )
                drawRoundRect(
                    color = Color(0x22000000),
                    topLeft = Offset(ww * i + gap, 0f),
                    size = Size(ww - gap * 2f, h),
                    cornerRadius = CornerRadius(ww * 0.12f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
                )
            }
            for ((after, snd) in blackKeys) {
                val cx = ww * (after + 1)
                val bw = ww * BLACK_WIDTH
                drawRoundRect(
                    color = if (snd in down) Color(0xFF546E7A) else Color(0xFF263238),
                    topLeft = Offset(cx - bw / 2f, 0f),
                    size = Size(bw, h * BLACK_HEIGHT),
                    cornerRadius = CornerRadius(bw * 0.18f),
                )
            }
        }
    }
}
