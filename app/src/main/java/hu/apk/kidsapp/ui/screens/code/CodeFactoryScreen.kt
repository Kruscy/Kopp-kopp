package hu.apk.kidsapp.ui.screens.code

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hu.apk.kidsapp.ui.components.BackButton
import hu.apk.kidsapp.ui.theme.KidText
import hu.apk.kidsapp.util.SoundFx
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private const val COLS = 6
private const val ROWS = 4
private const val MAX_STEPS = 10

private val dx = intArrayOf(1, -1, 0, 0)
private val dy = intArrayOf(0, 0, -1, 1)
private val dirColors = listOf(Color(0xFF43A047), Color(0xFFFB8C00), Color(0xFF1E88E5), Color(0xFF8E24AA))

private class Level(val start: Pair<Int, Int>, val stars: List<Pair<Int, Int>>, val rocks: List<Pair<Int, Int>>)

private val levels = listOf(
    Level(0 to 1, listOf(2 to 1), emptyList()),
    Level(0 to 1, listOf(4 to 1), emptyList()),
    Level(0 to 0, listOf(3 to 2), emptyList()),
    Level(0 to 3, listOf(3 to 3, 3 to 1), emptyList()),
    Level(0 to 0, listOf(5 to 2), listOf(1 to 0, 3 to 1)),
    Level(0 to 0, listOf(5 to 2, 5 to 3), listOf(1 to 0, 1 to 1, 3 to 2, 3 to 3)),
)

private class Confetti(var x: Float, var y: Float, val vx: Float, var vy: Float, val color: Color, val r: Float)

/** Kódgyár: nyilakból összerakott programmal vezetjük a robotot a csillagokhoz; hibázni nem lehet, a robot csak visszatér az elejére. */
@Composable
fun CodeFactoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sfx = remember { SoundFx(context.applicationContext) }
    DisposableEffect(sfx) { onDispose { sfx.release() } }
    val scope = rememberCoroutineScope()

    var level by remember { mutableIntStateOf(0) }
    val lv = levels[level % levels.size]
    val program = remember { mutableStateListOf<Int>() }
    val collected = remember { mutableStateListOf<Boolean>() }
    var execIdx by remember { mutableIntStateOf(-1) }
    var running by remember { mutableStateOf(false) }
    var won by remember { mutableStateOf(false) }
    var rx by remember { mutableFloatStateOf(0f) }
    var ry by remember { mutableFloatStateOf(0f) }
    var facing by remember { mutableIntStateOf(0) }
    var t by remember { mutableFloatStateOf(0f) }
    val confetti = remember { mutableListOf<Confetti>() }
    var job by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(level) {
        job?.cancel()
        program.clear()
        collected.clear()
        lv.stars.forEach { collected.add(false) }
        rx = lv.start.first.toFloat()
        ry = lv.start.second.toFloat()
        facing = 0
        execIdx = -1
        running = false
        won = false
    }

    LaunchedEffect(Unit) {
        var start = 0L
        var last = 0f
        while (true) {
            withFrameNanos { n ->
                if (start == 0L) start = n
                val now = (n - start) / 1e9f
                val dt = (now - last).coerceIn(0f, 0.05f)
                last = now
                val it = confetti.iterator()
                while (it.hasNext()) {
                    val c = it.next()
                    c.x += c.vx * dt
                    c.y += c.vy * dt
                    c.vy += 600f * dt
                    if (c.y > 4000f) it.remove()
                }
                t = now
            }
        }
    }

    suspend fun anim(dur: Float, block: (Float) -> Unit) {
        var start = 0L
        while (true) {
            var done = false
            withFrameNanos { n ->
                if (start == 0L) start = n
                val a = ((n - start) / 1e9f / dur).coerceIn(0f, 1f)
                block(a)
                done = a >= 1f
            }
            if (done) break
        }
    }

    fun runProgram() {
        if (running || program.isEmpty()) return
        val steps = program.toList()
        job = scope.launch {
            running = true
            for (i in collected.indices) collected[i] = false
            var cx = lv.start.first
            var cy = lv.start.second
            rx = cx.toFloat(); ry = cy.toFloat()
            delay(250)
            for ((i, d) in steps.withIndex()) {
                execIdx = i
                facing = d
                val nx = cx + dx[d]
                val ny = cy + dy[d]
                val blocked = nx !in 0 until COLS || ny !in 0 until ROWS || (nx to ny) in lv.rocks
                if (blocked) {
                    sfx.play("wall")
                    val ox = cx.toFloat(); val oy = cy.toFloat()
                    anim(0.4f) { a ->
                        val k = sin(a * PI.toFloat()) * 0.22f
                        rx = ox + dx[d] * k
                        ry = oy + dy[d] * k
                    }
                    rx = ox; ry = oy
                } else {
                    sfx.play("paddle", rate = 0.8f + 0.06f * i)
                    val ox = cx.toFloat(); val oy = cy.toFloat()
                    anim(0.5f) { a ->
                        val e = a * a * (3f - 2f * a)
                        rx = ox + dx[d] * e
                        ry = oy + dy[d] * e
                    }
                    cx = nx; cy = ny
                    rx = cx.toFloat(); ry = cy.toFloat()
                    val si = lv.stars.indexOf(cx to cy)
                    if (si >= 0 && !collected[si]) {
                        collected[si] = true
                        sfx.play("ding")
                    }
                }
                delay(120)
            }
            execIdx = -1
            if (collected.all { it }) {
                won = true
                sfx.play("win")
                repeat(60) {
                    confetti.add(
                        Confetti(
                            Random.nextFloat() * 1800f, -50f - Random.nextFloat() * 300f, (Random.nextFloat() - 0.5f) * 200f, 100f + Random.nextFloat() * 200f,
                            listOf(Color(0xFFFF6F91), Color(0xFFFFD43B), Color(0xFF69DB7C), Color(0xFF4DABF7), Color(0xFFA78BFA)).random(), 8f + Random.nextFloat() * 8f,
                        ),
                    )
                }
                delay(3200)
                level++
            } else {
                sfx.play("boing", rate = 1.2f)
                delay(500)
                val ox = rx; val oy = ry
                anim(0.7f) { a ->
                    rx = ox + (lv.start.first - ox) * a
                    ry = oy + (lv.start.second - oy) * a
                }
                for (i in collected.indices) collected[i] = false
                facing = 0
            }
            running = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFE8F1F8)).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            BackButton(onClick = onBack)
            Spacer(Modifier.weight(1f))
            Text(
                "Kódgyár · ${level % levels.size + 1}. szint",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = KidText,
            )
            Spacer(Modifier.weight(1f))
            FloatingActionButton(
                onClick = { runProgram() },
                containerColor = if (running || program.isEmpty()) Color(0xFFA5D6A7) else Color(0xFF43A047),
                contentColor = Color.White,
                modifier = Modifier.size(72.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.size(16.dp))
            FloatingActionButton(
                onClick = {
                    job?.cancel()
                    program.clear()
                    for (i in collected.indices) collected[i] = false
                    rx = lv.start.first.toFloat(); ry = lv.start.second.toFloat()
                    facing = 0; execIdx = -1; running = false
                },
                containerColor = Color(0xFF90A4AE),
                contentColor = Color.White,
                modifier = Modifier.size(72.dp),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(34.dp))
            }
        }
        Spacer(Modifier.size(8.dp))
        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val cell = min(size.width / COLS, size.height / ROWS)
            val ox = (size.width - cell * COLS) / 2f
            val oy = (size.height - cell * ROWS) / 2f
            drawFloor(lv, cell, ox, oy, t)
            // csillagok
            lv.stars.forEachIndexed { i, s ->
                if (!collected.getOrElse(i) { false }) {
                    val sc = 1f + 0.12f * sin(t * 3f + i)
                    drawStar(Offset(ox + cell * (s.first + 0.5f), oy + cell * (s.second + 0.5f)), cell * 0.32f * sc, t)
                }
            }
            drawRobot(Offset(ox + cell * (rx + 0.5f), oy + cell * (ry + 0.5f)), cell, facing, t, running, won)
            for (c in confetti) drawCircle(c.color, c.r, Offset(c.x * size.width / 1800f, c.y))
        }
        Spacer(Modifier.size(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            for (d in 0 until 4) {
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(76.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(dirColors[d])
                        .clickable(enabled = !running && program.size < MAX_STEPS) {
                            program.add(d)
                            sfx.play("pop", rate = 1.1f)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    ArrowGlyph(d, Color.White, Modifier.size(44.dp))
                }
            }
            Spacer(Modifier.size(14.dp))
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                for (i in 0 until MAX_STEPS) {
                    val d = program.getOrNull(i)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 3.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    d == null -> Color(0xFFCFD8DC)
                                    execIdx == i -> dirColors[d].copy(alpha = 1f)
                                    else -> dirColors[d].copy(alpha = if (execIdx >= 0 && i < execIdx) 0.45f else 0.85f)
                                },
                            )
                            .clickable(enabled = d != null && !running) { program.removeAt(i) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (d != null) ArrowGlyph(d, Color.White, Modifier.fillMaxSize(0.62f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ArrowGlyph(dir: Int, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val deg = when (dir) { 0 -> 0f; 1 -> 180f; 2 -> -90f; else -> 90f }
        rotate(deg, Offset(size.width / 2f, size.height / 2f)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            drawRoundRect(color, Offset(cx - s * 0.42f, cy - s * 0.11f), Size(s * 0.55f, s * 0.22f), CornerRadius(s * 0.08f))
            val p = Path().apply {
                moveTo(cx + s * 0.46f, cy)
                lineTo(cx + s * 0.04f, cy - s * 0.34f)
                lineTo(cx + s * 0.04f, cy + s * 0.34f)
                close()
            }
            drawPath(p, color)
        }
    }
}

private fun DrawScope.drawFloor(lv: Level, cell: Float, ox: Float, oy: Float, t: Float) {
    drawRoundRect(Color(0xFF546E7A), Offset(ox - cell * 0.08f, oy - cell * 0.08f), Size(cell * COLS + cell * 0.16f, cell * ROWS + cell * 0.16f), CornerRadius(cell * 0.2f))
    for (cx in 0 until COLS) {
        for (cy in 0 until ROWS) {
            val c = if ((cx + cy) % 2 == 0) Color(0xFFCFD8DC) else Color(0xFFECEFF1)
            drawRect(c, Offset(ox + cell * cx, oy + cell * cy), Size(cell, cell))
        }
    }
    // töltőpad az induló mezőn
    val s = lv.start
    val sc = Offset(ox + cell * (s.first + 0.5f), oy + cell * (s.second + 0.5f))
    drawCircle(Color(0xFF80DEEA), cell * 0.4f, sc)
    drawCircle(Color(0xFFB2EBF2), cell * 0.3f, sc)
    for (r in lv.rocks) {
        val c = Offset(ox + cell * (r.first + 0.5f), oy + cell * (r.second + 0.55f))
        drawOval(Color(0xFF78909C), Offset(c.x - cell * 0.36f, c.y - cell * 0.26f), Size(cell * 0.72f, cell * 0.56f))
        drawOval(Color(0xFF90A4AE), Offset(c.x - cell * 0.28f, c.y - cell * 0.22f), Size(cell * 0.4f, cell * 0.24f))
    }
}

private fun DrawScope.drawStar(c: Offset, r: Float, t: Float) {
    val p = Path()
    for (i in 0 until 10) {
        val rad = if (i % 2 == 0) r else r * 0.45f
        val a = -PI.toFloat() / 2f + i * PI.toFloat() / 5f
        val x = c.x + cos(a) * rad
        val y = c.y + sin(a) * rad
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawCircle(Color(0x33FFD54F), r * 1.35f, c)
    drawPath(p, Color(0xFFFFD54F))
    drawPath(p, Color(0xFFFFA000), style = Stroke(width = r * 0.1f))
}

private fun DrawScope.drawRobot(c: Offset, cell: Float, facing: Int, t: Float, moving: Boolean, won: Boolean) {
    val u = cell * 0.5f
    val bob = if (won) abs(sin(t * 8f)) * u * 0.35f else if (moving) abs(sin(t * 14f)) * u * 0.04f else sin(t * 2f) * u * 0.03f
    val cx = c.x
    val cy = c.y - bob
    drawOval(Color(0x33000000), Offset(cx - u * 0.75f, c.y + u * 0.55f), Size(u * 1.5f, u * 0.3f))
    // kerekek
    for (s in listOf(-1f, 1f)) {
        drawRoundRect(Color(0xFF37474F), Offset(cx + s * u * 0.42f - u * 0.18f, cy + u * 0.3f), Size(u * 0.36f, u * 0.4f), CornerRadius(u * 0.14f))
    }
    // test
    drawRoundRect(Color(0xFF42A5F5), Offset(cx - u * 0.56f, cy - u * 0.12f), Size(u * 1.12f, u * 0.8f), CornerRadius(u * 0.2f))
    drawCircle(if (won) Color(0xFF69F0AE) else Color(0xFFFF5252), u * 0.1f, Offset(cx, cy + u * 0.4f))
    // fej
    drawRoundRect(Color(0xFFB0BEC5), Offset(cx - u * 0.5f, cy - u * 0.78f), Size(u * 1.0f, u * 0.62f), CornerRadius(u * 0.18f))
    drawRoundRect(Color(0xFF263238), Offset(cx - u * 0.4f, cy - u * 0.7f), Size(u * 0.8f, u * 0.46f), CornerRadius(u * 0.12f))
    val lx = dx[facing] * u * 0.06f
    val ly = dy[facing] * u * 0.05f
    for (s in listOf(-1f, 1f)) {
        drawCircle(Color(0xFF00E5FF), u * 0.1f, Offset(cx + s * u * 0.2f + lx, cy - u * 0.47f + ly))
    }
    // antenna
    drawLine(Color(0xFF78909C), Offset(cx, cy - u * 0.78f), Offset(cx, cy - u * 0.98f), strokeWidth = u * 0.06f)
    drawCircle(if (moving || won) Color(0xFFFFEB3B) else Color(0xFFFF8A80), u * (0.1f + 0.02f * sin(t * 6f)), Offset(cx, cy - u * 1.02f))
    // karok
    for (s in listOf(-1f, 1f)) {
        val up = if (won) -abs(sin(t * 8f)) * u * 0.4f else 0f
        drawLine(Color(0xFF90A4AE), Offset(cx + s * u * 0.56f, cy + u * 0.1f), Offset(cx + s * u * 0.74f, cy + u * 0.3f + up), strokeWidth = u * 0.12f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}
