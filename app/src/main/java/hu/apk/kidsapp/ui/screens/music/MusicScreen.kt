package hu.apk.kidsapp.ui.screens.music

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
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
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private const val ROWS = 6
private const val COLS = 8
private const val STEP_MS = 520L

private val blobColors = listOf(
    Color(0xFFFF6F91), Color(0xFFFF9F43), Color(0xFFFFD43B),
    Color(0xFF69DB7C), Color(0xFF4DABF7), Color(0xFFA78BFA),
)

// felül a legmagasabb hang, alul a dob
private val sound = listOf("n0", "n1", "n2", "n3", "n4", "n5")

private class Floater(val x: Float, val y: Float, val t0: Float, val color: Color)

/** Zenebuborékok: a bal oldali buborékra koppintva szól a hang, a rácson sorrendbe rakva pedig ismétlődő dallammá áll össze. */
@Composable
fun MusicScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sfx = remember { SoundFx(context.applicationContext) }
    DisposableEffect(sfx) { onDispose { sfx.release() } }

    val grid = remember {
        mutableStateListOf<Boolean>().apply {
            repeat(ROWS * COLS) { add(false) }
            // kis kezdő dallam: C D E G E D C, alatta dobütés
            listOf(4 to 0, 3 to 1, 2 to 2, 1 to 3, 2 to 4, 3 to 5, 4 to 6).forEach { (r, c) -> this[r * COLS + c] = true }
            for (c in 0 until COLS step 2) this[5 * COLS + c] = true
        }
    }
    val hits = remember { FloatArray(ROWS * (COLS + 1)) { -10f } } // 0. oszlop: a bal oldali hangadó buborék
    val floaters = remember { mutableListOf<Floater>() }
    var t by remember { mutableFloatStateOf(0f) }
    var playing by remember { mutableStateOf(true) }
    var step by remember { mutableIntStateOf(-1) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(Unit) {
        var start = 0L
        while (true) {
            withFrameNanos { n ->
                if (start == 0L) start = n
                t = (n - start) / 1e9f
            }
        }
    }

    // rács: 0. oszlop a hangadó buborék, utána 8 lépés
    fun cell() = min(size.width / (COLS + 1.4f), size.height / ROWS.toFloat())
    fun left() = (size.width - cell() * (COLS + 1)) / 2f
    fun top() = (size.height - cell() * ROWS) / 2f

    fun hit(r: Int, c: Int) {
        sfx.play(sound[r])
        hits[r * (COLS + 1) + c] = t
        val cs = cell()
        repeat(2) {
            floaters.add(Floater(left() + cs * (c + 0.5f) + Random.nextFloat() * cs * 0.4f - cs * 0.2f, top() + cs * r, t, blobColors[r]))
        }
    }

    LaunchedEffect(playing) {
        if (!playing) {
            step = -1
            return@LaunchedEffect
        }
        var s = 0
        while (true) {
            step = s
            for (r in 0 until ROWS) if (grid[r * COLS + s]) hit(r, s + 1)
            delay(STEP_MS)
            s = (s + 1) % COLS
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(KidBackground).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            BackButton(onClick = onBack)
            Spacer(Modifier.weight(1f))
            Text("Zene", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = KidText)
            Spacer(Modifier.weight(1f))
            FloatingActionButton(
                onClick = { playing = !playing },
                containerColor = if (playing) Color(0xFFEF5350) else Color(0xFF43A047),
                contentColor = Color.White,
                modifier = Modifier.size(72.dp),
            ) {
                Icon(if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.size(16.dp))
            FloatingActionButton(
                onClick = { for (i in grid.indices) grid[i] = false },
                containerColor = Color(0xFF90A4AE),
                contentColor = Color.White,
                modifier = Modifier.size(72.dp),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(34.dp))
            }
        }
        Spacer(Modifier.size(8.dp))
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { size = it }
                .pointerInput(Unit) {
                    detectTapGestures { p ->
                        val cs = cell()
                        if (p.x < left() || p.y < top()) return@detectTapGestures
                        val c = ((p.x - left()) / cs).toInt()
                        val r = ((p.y - top()) / cs).toInt()
                        if (c !in 0..COLS || r !in 0 until ROWS) return@detectTapGestures
                        if (c == 0) {
                            hit(r, 0)
                        } else {
                            val i = r * COLS + (c - 1)
                            grid[i] = !grid[i]
                            if (grid[i]) hit(r, c)
                        }
                    }
                },
        ) {
            val now = t
            val cs = cell()
            val l = left()
            val tp = top()
            if (step >= 0) {
                drawRoundRect(
                    Color(0x22000000),
                    topLeft = Offset(l + cs * (step + 1) + cs * 0.04f, tp),
                    size = Size(cs * 0.92f, cs * ROWS),
                    cornerRadius = CornerRadius(cs * 0.3f),
                )
            }
            for (r in 0 until ROWS) {
                for (c in 0..COLS) {
                    val on = c == 0 || grid[r * COLS + (c - 1)]
                    drawBlob(
                        center = Offset(l + cs * (c + 0.5f), tp + cs * (r + 0.5f)),
                        radius = cs * (if (c == 0) 0.44f else 0.4f),
                        color = blobColors[r],
                        filled = on,
                        age = now - hits[r * (COLS + 1) + c],
                        now = now,
                        seed = r * 13 + c,
                    )
                }
            }
            // lebegő hangjegyek
            val paint = Paint().apply { isAntiAlias = true; textSize = cs * 0.45f; textAlign = Paint.Align.CENTER }
            val iter = floaters.iterator()
            while (iter.hasNext()) {
                val n = iter.next()
                val a = now - n.t0
                if (a > 1.2f) {
                    iter.remove()
                    continue
                }
                paint.color = android.graphics.Color.argb(
                    ((1f - a / 1.2f) * 255).toInt(), (n.color.red * 255).toInt(), (n.color.green * 255).toInt(), (n.color.blue * 255).toInt(),
                )
                drawIntoCanvas { it.nativeCanvas.drawText("♪", n.x + sin(a * 5f) * cs * 0.1f, n.y - a * cs * 0.9f, paint) }
            }
        }
    }
}

/** Mosolygó buborék: koppintáskor összenyomódik és kinyitja a száját; kikapcsolt állapotban halvány folt. */
private fun DrawScope.drawBlob(center: Offset, radius: Float, color: Color, filled: Boolean, age: Float, now: Float, seed: Int) {
    if (!filled) {
        drawCircle(color.copy(alpha = 0.22f), radius * 0.75f, center)
        return
    }
    val active = age in 0f..0.9f
    val k = if (active) exp(-age * 7f) * cos(age * 26f) else 0f
    val idle = sin(now * 2f + seed) * 0.015f
    val sx = 1f + 0.28f * k + idle
    val sy = 1f - 0.28f * k - idle
    val lift = if (active) -radius * 0.25f * exp(-age * 8f) else 0f
    val cy = center.y + lift + radius * (1f - sy) * 0.5f
    val w = radius * 2f * sx
    val h = radius * 2f * sy
    drawOval(color, Offset(center.x - w / 2f, cy - h / 2f), Size(w, h))
    drawOval(Color.White.copy(alpha = 0.35f), Offset(center.x - w * 0.28f, cy - h * 0.34f), Size(w * 0.22f, h * 0.14f))
    val eye = radius * 0.17f
    val ex = radius * 0.36f * sx
    val ey = cy - h * 0.08f
    for (s in listOf(-1f, 1f)) {
        drawCircle(Color.White, eye, Offset(center.x + s * ex, ey))
        drawCircle(Color(0xFF2A2A3B), eye * 0.55f, Offset(center.x + s * ex, ey + eye * 0.1f))
    }
    val my = cy + h * 0.2f
    if (age in 0f..0.4f) {
        drawOval(Color(0xFF7A2E3A), Offset(center.x - radius * 0.2f, my - radius * 0.12f), Size(radius * 0.4f, radius * 0.3f))
    } else {
        drawArc(
            Color(0xFF2A2A3B), 20f, 140f, false,
            topLeft = Offset(center.x - radius * 0.22f, my - radius * 0.2f), size = Size(radius * 0.44f, radius * 0.34f),
            style = Stroke(width = radius * 0.07f, cap = StrokeCap.Round),
        )
    }
}
