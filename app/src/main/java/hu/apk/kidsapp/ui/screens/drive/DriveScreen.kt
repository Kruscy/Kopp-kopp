package hu.apk.kidsapp.ui.screens.drive

import android.content.Context
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.apk.kidsapp.ui.components.BackButton
import hu.apk.kidsapp.util.SoundFx
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private val carColors = listOf(
    Color(0xFF42A5F5), Color(0xFFFFCA28), Color(0xFF66BB6A), Color(0xFFAB47BC),
    Color(0xFFFF7043), Color(0xFFECEFF1), Color(0xFF26C6DA), Color(0xFFEC407A),
)

private class Other(
    var x: Float, // az út szélességének törtrésze (0..1)
    var y: Float,
    val color: Color,
    val speedFactor: Float,
) {
    var hit = false
    var vx = 0f
    var spin = 0f
    var passed = false
}

private class Tree(var x: Float, var y: Float, val r: Float, val flower: Boolean)

private class Sim {
    var inited = false
    var playerX = 0.5f
    var tilt = 0f
    var tiltRaw = 0f // a telefon/tablet döntése (m/s², jobbra dőlés = pozitív)
    var touchDir = 0
    var speedMul = 1f
    var markOffset = 0f
    var passed = 0
    var spawnIn = 1.2f
    var invuln = 0f
    var shake = 0f
    var cheerT = -10f
    val others = mutableListOf<Other>()
    val trees = mutableListOf<Tree>()
}

/** Autóvezetés: a tablet döntésével kormányozzuk a piros autót az úton, és ki kell kerülni a többi autót. */
@Composable
fun DriveScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val sfx = remember { SoundFx(context.applicationContext) }
    DisposableEffect(sfx) { onDispose { sfx.release() } }
    val sim = remember { Sim() }
    var t by remember { mutableFloatStateOf(0f) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var stars by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val rot = view.display?.rotation ?: Surface.ROTATION_0
                val ax = e.values[0]
                val ay = e.values[1]
                // a képernyő jobbra mutató tengelye mentén mért gyorsulás
                val sx = when (rot) {
                    Surface.ROTATION_90 -> -ay
                    Surface.ROTATION_180 -> -ax
                    Surface.ROTATION_270 -> ay
                    else -> ax
                }
                sim.tiltRaw = -sx // jobbra dőlő eszköznél a mért érték negatív
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (acc != null) sm.registerListener(l, acc, SensorManager.SENSOR_DELAY_GAME)
        view.keepScreenOn = true
        onDispose {
            sm.unregisterListener(l)
            view.keepScreenOn = false
        }
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
                if (size.width > 0) {
                    stars = step(sim, size.width.toFloat(), size.height.toFloat(), now, dt, sfx)
                }
                t = now
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF7CB342))) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { size = it }
                    .pointerInput(Unit) {
                        // tartalék kormányzás: a képernyő bal vagy jobb felének nyomása
                        awaitPointerEventScope {
                            while (true) {
                                val ev = awaitPointerEvent()
                                val down = ev.changes.filter { it.pressed }
                                sim.touchDir = when {
                                    down.isEmpty() -> 0
                                    down.first().position.x < size.width / 2f -> -1
                                    else -> 1
                                }
                            }
                        }
                    },
            ) {
                drawWorld(sim, t)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackButton(onClick = onBack)
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("⭐", fontSize = 32.sp)
                    Spacer(Modifier.size(10.dp))
                    Text("$stars", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2A2A3B))
                }
            }
        }
    }
}

private class Geo(val w: Float, val h: Float) {
    val roadW = min(w * 0.6f, h * 0.95f)
    val roadL = (w - roadW) / 2f
    val carW = roadW / 3f * 0.52f
    val carLen = carW * 1.9f
    val playerY = h - carLen * 1.5f
    val lanes = floatArrayOf(1f / 6f, 0.5f, 5f / 6f)
    fun px(x: Float) = roadL + roadW * x
}

private fun step(sim: Sim, w: Float, h: Float, now: Float, dt: Float, sfx: SoundFx): Int {
    val g = Geo(w, h)
    if (!sim.inited) {
        sim.inited = true
        repeat(14) {
            val left = Random.nextBoolean()
            sim.trees.add(
                Tree(
                    if (left) Random.nextFloat() * (g.roadL - 40f) else g.roadL + g.roadW + 40f + Random.nextFloat() * (w - g.roadL - g.roadW - 60f),
                    Random.nextFloat() * h, 28f + Random.nextFloat() * 30f, Random.nextInt(3) == 0,
                ),
            )
        }
    }
    val diff = min(1.7f, 1f + sim.passed * 0.012f)
    val vBase = g.roadW * 0.5f * diff
    sim.speedMul += (1f - sim.speedMul) * min(1f, dt * 1.2f)
    val v = vBase * sim.speedMul

    // kormányzás: érintés felülírja a döntést
    val dead = 0.35f
    val raw = if (abs(sim.tiltRaw) < dead) 0f else sim.tiltRaw - dead * (if (sim.tiltRaw > 0) 1f else -1f)
    val tiltN = (raw / 3.2f).coerceIn(-1f, 1f)
    val target = if (sim.touchDir != 0) sim.touchDir.toFloat() else tiltN
    sim.tilt += (target - sim.tilt) * min(1f, dt * 9f)
    val margin = g.carW / g.roadW * 0.5f - 0.015f
    sim.playerX = (sim.playerX + sim.tilt * dt * 1.0f).coerceIn(margin, 1f - margin)

    sim.markOffset = (sim.markOffset + v * dt) % (g.carLen * 1.8f)
    sim.invuln = max(0f, sim.invuln - dt)
    sim.shake = max(0f, sim.shake - dt)

    for (tr in sim.trees) {
        tr.y += v * dt
        if (tr.y > h + 80f) {
            tr.y = -80f - Random.nextFloat() * 160f
            val left = Random.nextBoolean()
            tr.x = if (left) Random.nextFloat() * (g.roadL - 40f) else g.roadL + g.roadW + 40f + Random.nextFloat() * (w - g.roadL - g.roadW - 60f)
        }
    }

    // új autók
    // Az új sor csak akkor jön, ha az előző már jó messze van: így mindig marad idő kikerülni.
    sim.spawnIn -= dt
    val gap = g.carLen * max(3.0f, 4.6f - sim.passed * 0.03f)
    val nearest = sim.others.minOfOrNull { it.y } ?: gap
    if (sim.spawnIn <= 0f && nearest > gap - g.carLen) {
        sim.spawnIn = 0.4f
        val count = if (Random.nextInt(10) < 1) 2 else 1
        val free = (0..2).filter { l -> sim.others.none { abs(it.x - g.lanes[l]) < 0.1f && it.y < g.carLen * 2.4f } }.shuffled().take(count)
        for (l in free) {
            sim.others.add(Other(g.lanes[l], -g.carLen, carColors.random(), 0.40f + Random.nextFloat() * 0.05f))
        }
    }

    val it = sim.others.iterator()
    while (it.hasNext()) {
        val o = it.next()
        val rel = max(vBase * 0.15f, v - vBase * o.speedFactor)
        o.y += rel * dt
        if (o.hit) {
            o.x += o.vx * dt
            o.spin += 280f * dt * (if (o.vx > 0) 1f else -1f)
        }
        // elkerült autó
        if (!o.hit && !o.passed && o.y > g.playerY + g.carLen * 0.8f) {
            o.passed = true
            sim.passed++
            sfx.play("pop", rate = 1.3f)
            if (sim.passed % 10 == 0) {
                sim.cheerT = now
                sfx.play("ding")
            }
        }
        if (o.y > h + g.carLen * 2f || o.x < -0.6f || o.x > 1.6f) {
            it.remove()
            continue
        }
        // ütközés
        if (!o.hit && sim.invuln <= 0f) {
            val dx = abs(g.px(o.x) - g.px(sim.playerX))
            val dy = abs(o.y - g.playerY)
            if (dx < g.carW * 0.9f && dy < g.carLen * 0.92f) {
                o.hit = true
                o.vx = if (g.px(o.x) >= g.px(sim.playerX)) 1.4f else -1.4f
                sim.speedMul = 0.25f
                sim.invuln = 1.3f
                sim.shake = 0.5f
                sfx.play("boing")
                sfx.play("honk", rate = 1.2f, volume = 0.6f)
            }
        }
    }
    return sim.passed
}

private fun DrawScope.drawWorld(sim: Sim, now: Float) {
    if (!sim.inited) return
    val g = Geo(size.width, size.height)
    val sh = if (sim.shake > 0f) sin(now * 70f) * 8f * sim.shake else 0f
    translate(sh, 0f) {
        // fű, világosabb csíkokkal, amik a sebességgel mozognak
        drawRect(Color(0xFF7CB342), size = size)
        val stripe = g.carLen * 3f
        var y = (sim.markOffset * 1.0f) % (stripe * 2f) - stripe * 2f
        while (y < size.height) {
            drawRect(Color(0x14FFFFFF), Offset(0f, y), Size(size.width, stripe))
            y += stripe * 2f
        }
        // út
        drawRect(Color(0xFFB0BEC5), Offset(g.roadL - 14f, 0f), Size(g.roadW + 28f, size.height))
        drawRect(Color(0xFF546E7A), Offset(g.roadL, 0f), Size(g.roadW, size.height))
        drawRect(Color.White, Offset(g.roadL + 6f, 0f), Size(8f, size.height))
        drawRect(Color.White, Offset(g.roadL + g.roadW - 14f, 0f), Size(8f, size.height))
        // szaggatott sávjelzések
        val dash = g.carLen * 0.9f
        val period = g.carLen * 1.8f
        for (k in 1..2) {
            val x = g.roadL + g.roadW * k / 3f
            var yy = sim.markOffset - period
            while (yy < size.height) {
                drawRoundRect(Color(0xFFFFF59D), Offset(x - 5f, yy), Size(10f, dash), CornerRadius(5f))
                yy += period
            }
        }
        // fák, virágok
        for (tr in sim.trees) {
            if (tr.flower) {
                drawCircle(Color(0xFFFFEB3B), 7f, Offset(tr.x, tr.y))
                drawCircle(Color(0xFFFF80AB), 6f, Offset(tr.x + 14f, tr.y + 6f))
                drawCircle(Color.White, 6f, Offset(tr.x - 12f, tr.y + 8f))
            } else {
                drawCircle(Color(0x33000000), tr.r, Offset(tr.x + 8f, tr.y + 10f))
                drawCircle(Color(0xFF2E7D32), tr.r, Offset(tr.x, tr.y))
                drawCircle(Color(0xFF43A047), tr.r * 0.7f, Offset(tr.x - tr.r * 0.15f, tr.y - tr.r * 0.15f))
                drawCircle(Color(0xFF81C784), tr.r * 0.3f, Offset(tr.x - tr.r * 0.3f, tr.y - tr.r * 0.3f))
            }
        }
        // többi autó
        for (o in sim.others) drawCar(g.px(o.x), o.y, g.carW, g.carLen, o.color, o.spin, false)
        // játékos
        val blink = sim.invuln > 0f && ((now * 12f).toInt() % 2 == 0)
        if (!blink) drawCar(g.px(sim.playerX), g.playerY, g.carW, g.carLen, Color(0xFFE53935), sim.tilt * 12f, true)
    }
    // gratuláció
    val a = now - sim.cheerT
    if (a in 0f..1.6f) {
        val paint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = size.height * 0.12f * (1f + 0.1f * sin(a * 12f))
            isFakeBoldText = true
            color = android.graphics.Color.argb(((1f - a / 1.6f) * 255).toInt(), 255, 255, 255)
            setShadowLayer(8f, 0f, 4f, android.graphics.Color.argb(160, 0, 0, 0))
        }
        drawIntoCanvas { it.nativeCanvas.drawText("Ügyes! 🌟", size.width / 2f, size.height * 0.3f - a * 40f, paint) }
    }
}

/** Felülnézeti autó: a motorháztető felfelé néz. */
private fun DrawScope.drawCar(cx: Float, cy: Float, w: Float, len: Float, color: Color, angle: Float, player: Boolean) {
    rotate(angle, Offset(cx, cy)) {
        val l = cx - w / 2f
        val t = cy - len / 2f
        // árnyék
        drawRoundRect(Color(0x33000000), Offset(l + 6f, t + 8f), Size(w, len), CornerRadius(w * 0.3f))
        // kerekek
        for (sx in listOf(-1f, 1f)) for (fy in listOf(0.2f, 0.76f)) {
            drawRoundRect(Color(0xFF263238), Offset(cx + sx * w * 0.5f - w * 0.09f, t + len * fy), Size(w * 0.18f, len * 0.17f), CornerRadius(8f))
        }
        // karosszéria
        drawRoundRect(color, Offset(l, t), Size(w, len), CornerRadius(w * 0.3f))
        drawRoundRect(Color(0x22000000), Offset(l, t + len * 0.55f), Size(w, len * 0.45f), CornerRadius(w * 0.3f))
        // szélvédő, tető, hátsó ablak
        drawRoundRect(Color(0xFF90CAF9), Offset(l + w * 0.12f, t + len * 0.2f), Size(w * 0.76f, len * 0.17f), CornerRadius(8f))
        drawRoundRect(Color(0x33FFFFFF), Offset(l + w * 0.14f, t + len * 0.38f), Size(w * 0.72f, len * 0.26f), CornerRadius(10f))
        drawRoundRect(Color(0xFF78909C), Offset(l + w * 0.16f, t + len * 0.68f), Size(w * 0.68f, len * 0.1f), CornerRadius(8f))
        // fényszórók elöl, féklámpák hátul
        for (sx in listOf(-1f, 1f)) {
            drawCircle(Color(0xFFFFF59D), w * 0.08f, Offset(cx + sx * w * 0.3f, t + len * 0.06f))
            drawCircle(Color(0xFFFF5252), w * 0.06f, Offset(cx + sx * w * 0.3f, t + len * 0.95f))
        }
        if (player) {
            drawRect(Color(0x33FFF59D), Offset(l + w * 0.1f, t - len * 0.35f), Size(w * 0.8f, len * 0.35f))
        }
    }
}
