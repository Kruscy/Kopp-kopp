package hu.apk.kidsapp.ui.screens.cooking

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import hu.apk.kidsapp.R
import hu.apk.kidsapp.ui.components.BackButton
import hu.apk.kidsapp.ui.theme.KidText
import hu.apk.kidsapp.util.SoundFx
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private class Ing(val name: String, val color: Color, val res: Int)

private val ingredients = listOf(
    Ing("Alma", Color(0xFFE53935), R.drawable.st_apple),
    Ing("Banán", Color(0xFFFDD835), R.drawable.st_banana),
    Ing("Narancs", Color(0xFFFB8C00), R.drawable.st_orange),
    Ing("Sajt", Color(0xFFFFCA28), R.drawable.st_cheese),
    Ing("Kenyér", Color(0xFFD7A86E), R.drawable.st_bread),
    Ing("Tej", Color(0xFFF5F5F5), R.drawable.st_milk),
    Ing("Kifli", Color(0xFFE0A458), R.drawable.st_croissant),
    Ing("Torta", Color(0xFFF48FB1), R.drawable.st_cake),
    Ing("Keksz", Color(0xFF8D6E63), R.drawable.st_cookie),
)

private const val MAX_IN_POT = 6
private val waterColor = Color(0xFF9AD0F5)
private val monsterColor = Color(0xFF9575CD)

private class InPot(val ing: Int, var ang: Float, val rad: Float, val t0: Float)

// kind: 0 fröccsenő csepp, 1 gőz, 2 szív, 3 szikra
private class Part(var x: Float, var y: Float, var vx: Float, var vy: Float, val t0: Float, val life: Float, val color: Color, val r: Float, val kind: Int)

private class Flyer(val from: Offset, val to: Offset, val t0: Float, val dur: Float, val color: Color, val arc: Float)

private class Bubble(val x: Float, val y: Float, val r: Float, val t0: Float)

private class Sim {
    val pot = mutableListOf<InPot>()
    val parts = mutableListOf<Part>()
    val flyers = mutableListOf<Flyer>()
    val bubbles = mutableListOf<Bubble>()
    var liquid = waterColor
    var stir = 0f
    var ready = false
    var stirring = false
    var spoon = Offset.Zero
    var stirSpeed = 0f
    var dragIdx = -1
    var dragPos = Offset.Zero
    var look = Offset.Zero
    var serveT0 = -10f
    var ateAt = -10f
    var label: String? = null
    var labelT0 = -10f
    var steamAcc = 0f
    var bubbleAcc = 0f
    var readyT0 = -10f
}

private class Geo(val w: Float, val h: Float) {
    val trayH = h * 0.2f
    val stageH = h - trayH
    val potH = min(stageH * 0.5f, w * 0.24f)
    val potW = potH * 1.4f
    val potCx = w * 0.36f
    val potBottom = stageH * 0.93f
    val rimY = potBottom - potH
    val rimRy = potW * 0.5f * 0.62f // ~38°-os rálátás: a fazék nyílása jól látszik
    val monsterR = min(stageH * 0.2f, w * 0.12f)
    val monsterX = w * 0.78f
    val monsterY = potBottom - monsterR * 1.05f
    val cellW = w / ingredients.size
    val iconS = min(cellW * 0.86f, trayH * 0.84f)
    fun trayCenter(i: Int) = Offset(cellW * (i + 0.5f), stageH + trayH * 0.5f)
    fun trayIndexAt(p: Offset): Int {
        if (p.y < stageH) return -1
        val i = (p.x / cellW).toInt()
        return if (i in ingredients.indices) i else -1
    }
    fun inPotZone(p: Offset) = abs(p.x - potCx) < potW * 0.62f && p.y > rimY - rimRy * 1.6f && p.y < potBottom
    fun inMonster(p: Offset) = hypot(p.x - monsterX, p.y - monsterY) < monsterR * 1.3f
    val mouth = Offset(monsterX, monsterY + monsterR * 0.28f)
}

/** Főzés: hozzávalókat húzunk a fazékba, megkeverjük, és a megéhezett szörnyecske megeszi. */
@Composable
fun CookingScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sfx = remember { SoundFx(context.applicationContext) }
    DisposableEffect(sfx) { onDispose { sfx.release() } }
    val bitmaps: List<ImageBitmap> = ingredients.map { ImageBitmap.imageResource(it.res) }

    val sim = remember { Sim() }
    var t by remember { mutableFloatStateOf(0f) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    fun serving(now: Float) = now - sim.serveT0 < 3.0f

    LaunchedEffect(Unit) {
        var start = 0L
        var last = 0f
        while (true) {
            withFrameNanos { n ->
                if (start == 0L) start = n
                val now = (n - start) / 1e9f
                val dt = (now - last).coerceIn(0f, 0.05f)
                last = now
                if (size.width > 0) stepSim(sim, Geo(size.width.toFloat(), size.height.toFloat()), now, dt, sfx)
                t = now
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFFF3E0)).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            BackButton(onClick = onBack)
            Spacer(Modifier.weight(1f))
            Text("Főzés", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = KidText)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(1.dp))
        }
        Spacer(Modifier.size(8.dp))
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { size = it }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val g = Geo(size.width.toFloat(), size.height.toFloat())
                        val now0 = t
                        if (serving(now0)) {
                            // közben nem nyúlunk semmihez
                            do {
                                val ev = awaitPointerEvent()
                            } while (ev.changes.any { it.pressed })
                            return@awaitEachGesture
                        }
                        val start = down.position
                        var mode = 0
                        var moved = 0f
                        val idx = g.trayIndexAt(start)
                        if (idx >= 0) {
                            mode = 1
                            sim.dragIdx = idx
                            sim.dragPos = start
                            sfx.play("wall")
                        } else if (g.inMonster(start) && sim.ready) {
                            mode = 3
                        } else if (g.inPotZone(start)) {
                            if (sim.ready) {
                                mode = 3
                            } else if (sim.pot.isNotEmpty()) {
                                mode = 2
                                sim.stirring = true
                                sim.spoon = start
                            }
                        }
                        while (true) {
                            val ev = awaitPointerEvent()
                            val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                            if (!ch.pressed) break
                            val d = ch.position - ch.previousPosition
                            moved += hypot(d.x, d.y)
                            when (mode) {
                                1 -> sim.dragPos = ch.position
                                2 -> {
                                    sim.spoon = ch.position
                                    if (abs(ch.position.x - g.potCx) < g.potW * 0.5f) {
                                        sim.stir += hypot(d.x, d.y) / (g.potW * 3.5f)
                                    }
                                    sim.stirSpeed = (hypot(d.x, d.y) / 14f).coerceAtMost(1f)
                                }
                            }
                            sim.look = ch.position
                            ch.consume()
                        }
                        val end = sim.dragPos
                        when (mode) {
                            1 -> {
                                val tap = moved < 24f
                                if ((tap || g.inPotZone(end)) && sim.pot.size < MAX_IN_POT) {
                                    addToPot(sim, g, idx, t, sfx)
                                }
                                sim.dragIdx = -1
                            }
                            2 -> sim.stirring = false
                            3 -> if (moved < 60f) serve(sim, g, t, sfx)
                        }
                    }
                },
        ) {
            val now = t
            val g = Geo(size.width.toFloat(), size.height.toFloat())
            drawKitchen(g)
            drawMonster(g, sim, now)
            drawPot(g, sim, now, bitmaps)
            drawParts(sim, now, g)
            drawTray(g, bitmaps, sim)
            drawLabel(g, sim, now)
            if (sim.dragIdx >= 0) {
                val bm = bitmaps[sim.dragIdx]
                drawFit(bm, sim.dragPos, g.iconS * 1.25f)
            }
        }
    }
}

private fun addToPot(sim: Sim, g: Geo, idx: Int, now: Float, sfx: SoundFx) {
    val ing = ingredients[idx]
    val n = sim.pot.size
    sim.pot.add(InPot(idx, Random.nextFloat() * 6.28f, 0.25f + 0.2f * (n % 3), now))
    sfx.play("pop", rate = 0.8f + 0.1f * n)
    repeat(10) {
        val a = Random.nextFloat() * 6.28f
        val sp = g.potW * (0.2f + Random.nextFloat() * 0.5f)
        sim.parts.add(Part(g.potCx, g.rimY, cos(a) * sp * 0.6f, -abs(sin(a)) * sp - g.potH * 0.8f, now, 0.8f, ing.color, g.potH * 0.035f, 0))
    }
    sim.label = ing.name
    sim.labelT0 = now
    sim.stir = (sim.stir - 0.2f).coerceAtLeast(0f)
}

private fun serve(sim: Sim, g: Geo, now: Float, sfx: SoundFx) {
    if (!sim.ready) return
    sim.serveT0 = now
    sfx.play("whoosh", rate = 1.3f)
    val colors = sim.pot.map { ingredients[it.ing].color } + sim.liquid
    repeat(16) { i ->
        sim.flyers.add(
            Flyer(
                from = Offset(g.potCx + (Random.nextFloat() - 0.5f) * g.potW * 0.5f, g.rimY),
                to = g.mouth,
                t0 = now + i * 0.04f,
                dur = 0.75f,
                color = colors[i % colors.size],
                arc = g.potH * (0.5f + Random.nextFloat() * 0.35f),
            ),
        )
    }
}

private fun stepSim(sim: Sim, g: Geo, now: Float, dt: Float, sfx: SoundFx) {
    // folyadék színe
    var r = waterColor.red * 0.5f
    var gr = waterColor.green * 0.5f
    var b = waterColor.blue * 0.5f
    for (p in sim.pot) {
        val c = ingredients[p.ing].color
        r += c.red; gr += c.green; b += c.blue
    }
    val n = sim.pot.size + 0.5f
    val target = Color(r / n, gr / n, b / n)
    sim.liquid = lerp(sim.liquid, target, (dt * 4f).coerceAtMost(1f))

    sim.stirSpeed = if (sim.stirring) sim.stirSpeed * (1f - dt * 3f) else sim.stirSpeed * (1f - dt * 6f)
    val omega = 0.8f + 9f * sim.stirSpeed
    for (p in sim.pot) p.ang += omega * dt

    // keverés kész?
    if (!sim.ready && sim.pot.isNotEmpty() && sim.stir >= 1f && now - sim.serveT0 > 3f) {
        sim.ready = true
        sim.readyT0 = now
        sfx.play("ding")
        repeat(14) {
            val a = Random.nextFloat() * 6.28f
            sim.parts.add(Part(g.potCx + cos(a) * g.potW * 0.4f, g.rimY + sin(a) * g.rimRy, 0f, -g.potH * 0.3f, now, 1.0f, Color(0xFFFFE082), g.potH * 0.05f, 3))
        }
    }

    // szörnyecske eszik: a repülő falatok megérkeznek
    if (sim.flyers.isNotEmpty()) {
        val done = sim.flyers.all { now - it.t0 > it.dur }
        if (done) {
            sim.flyers.clear()
            sim.ateAt = now
            sfx.play("goal")
            repeat(8) {
                sim.parts.add(Part(g.monsterX + (Random.nextFloat() - 0.5f) * g.monsterR, g.monsterY - g.monsterR * 0.6f, (Random.nextFloat() - 0.5f) * g.monsterR * 0.6f, -g.monsterR * 0.9f, now, 1.6f, Color(0xFFFF6F91), g.monsterR * 0.14f, 2))
            }
        }
    }
    // ennyi kör után új főzés
    if (sim.ateAt > 0f && now - sim.ateAt > 1.6f && (sim.ready || sim.pot.isNotEmpty())) {
        sim.pot.clear()
        sim.ready = false
        sim.stir = 0f
        sim.ateAt = -10f
    }

    // buborékok és gőz
    if (sim.pot.isNotEmpty() && !sim.ready || sim.pot.isNotEmpty() && sim.ready) {
        sim.bubbleAcc += dt * (4f + 18f * sim.stirSpeed)
        while (sim.bubbleAcc >= 1f) {
            sim.bubbleAcc -= 1f
            val a = Random.nextFloat() * 6.28f
            val rr = sqrtRand()
            sim.bubbles.add(Bubble(g.potCx + cos(a) * rr * g.potW * 0.38f, g.rimY + sin(a) * rr * g.rimRy * 0.75f, g.potH * (0.025f + Random.nextFloat() * 0.03f), now))
        }
        sim.steamAcc += dt * (2f + (if (sim.ready) 3f else 0f))
        while (sim.steamAcc >= 1f) {
            sim.steamAcc -= 1f
            sim.parts.add(Part(g.potCx + (Random.nextFloat() - 0.5f) * g.potW * 0.6f, g.rimY - g.rimRy * 0.6f, 0f, -g.potH * 0.35f, now, 1.8f, Color.White, g.potH * 0.1f, 1))
        }
    }
    sim.bubbles.removeAll { now - it.t0 > 0.6f }
    val it = sim.parts.iterator()
    while (it.hasNext()) {
        val p = it.next()
        if (now - p.t0 > p.life) {
            it.remove()
            continue
        }
        p.x += p.vx * dt
        p.y += p.vy * dt
        if (p.kind == 0) p.vy += g.potH * 3.2f * dt
    }
}

private fun sqrtRand() = kotlin.math.sqrt(Random.nextFloat())

private fun DrawScope.drawFit(bm: ImageBitmap, center: Offset, box: Float) {
    val s = box / maxOf(bm.width, bm.height)
    val w = (bm.width * s).toInt().coerceAtLeast(1)
    val h = (bm.height * s).toInt().coerceAtLeast(1)
    drawImage(bm, dstOffset = IntOffset((center.x - w / 2f).toInt(), (center.y - h / 2f).toInt()), dstSize = IntSize(w, h))
}

private fun DrawScope.drawKitchen(g: Geo) {
    // fal csempével, pult, tálca
    drawRect(Color(0xFFFFF3E0), size = Size(g.w, g.stageH))
    val tile = g.stageH * 0.12f
    var y = 0f
    var row = 0
    while (y < g.potBottom - g.potH * 0.1f) {
        var x = if (row % 2 == 0) 0f else -tile / 2f
        while (x < g.w) {
            drawRoundRect(Color(0x14000000), Offset(x + 2f, y + 2f), Size(tile - 4f, tile - 4f), CornerRadius(6f))
            x += tile
        }
        y += tile
        row++
    }
    drawRect(Color(0xFFBCAAA4), Offset(0f, g.potBottom - g.potH * 0.1f), Size(g.w, g.stageH - g.potBottom + g.potH * 0.1f))
    drawRect(Color(0xFFA1887F), Offset(0f, g.potBottom + g.potH * 0.03f), Size(g.w, g.stageH - g.potBottom - g.potH * 0.03f))
    drawRect(Color(0xFF8D6E63), Offset(0f, g.potBottom - g.potH * 0.1f), Size(g.w, g.stageH * 0.008f))
    drawRect(Color(0xFFFFFFFF), Offset(0f, g.stageH), Size(g.w, g.trayH))
    drawRect(Color(0x14000000), Offset(0f, g.stageH), Size(g.w, 3f))
}

private fun DrawScope.drawPot(g: Geo, sim: Sim, now: Float, bitmaps: List<ImageBitmap>) {
    val wob = if (sim.ready && now - sim.serveT0 > 3f) sin((now - sim.readyT0) * 9f) * g.potH * 0.012f else 0f
    val cx = g.potCx + wob
    // fogantyúk
    for (s in listOf(-1f, 1f)) {
        drawRoundRect(Color(0xFF455A64), Offset(cx + s * g.potW * 0.5f - (if (s < 0) g.potW * 0.1f else 0f), g.rimY + g.potH * 0.06f), Size(g.potW * 0.1f, g.potH * 0.1f), CornerRadius(g.potH * 0.05f))
    }
    val bottomRy = g.rimRy * 0.8f
    drawOval(Color(0x33000000), Offset(cx - g.potW * 0.56f, g.potBottom - bottomRy * 1.2f), Size(g.potW * 1.12f, bottomRy * 1.7f))
    drawRect(Color(0xFF607D8B), Offset(cx - g.potW / 2f, g.rimY), Size(g.potW, g.potH - bottomRy))
    drawOval(Color(0xFF607D8B), Offset(cx - g.potW / 2f, g.potBottom - bottomRy * 2f), Size(g.potW, bottomRy * 2f))
    drawRoundRect(Color(0x33FFFFFF), Offset(cx - g.potW * 0.4f, g.rimY + g.potH * 0.18f), Size(g.potW * 0.06f, g.potH * 0.5f), CornerRadius(g.potH * 0.04f))
    // peremtől a folyadékig
    drawOval(Color(0xFF90A4AE), Offset(cx - g.potW / 2f, g.rimY - g.rimRy), Size(g.potW, g.rimRy * 2f))
    drawOval(Color(0xFF37474F), Offset(cx - g.potW * 0.46f, g.rimY - g.rimRy * 0.84f), Size(g.potW * 0.92f, g.rimRy * 1.68f))
    val liqW = g.potW * 0.86f
    val liqH = g.rimRy * 1.6f
    val liqTop = g.rimY - liqH / 2f + g.rimRy * 0.06f
    drawOval(sim.liquid, Offset(cx - liqW / 2f, liqTop), Size(liqW, liqH))
    // kavargó csíkok
    if (sim.pot.isNotEmpty()) {
        val sw = now * (1.2f + 6f * sim.stirSpeed)
        for (k in 0 until 3) {
            val a = sw + k * 2.1f
            drawArc(
                Color.White.copy(alpha = 0.35f), Math.toDegrees(a.toDouble()).toFloat(), 70f, false,
                Offset(cx - liqW * (0.34f - 0.08f * k), liqTop + liqH * (0.22f + 0.07f * k)), Size(liqW * (0.68f - 0.16f * k), liqH * (0.56f - 0.14f * k)),
                style = Stroke(width = g.potH * 0.018f, cap = StrokeCap.Round),
            )
        }
    }
    for (b in sim.bubbles) {
        val a = (now - b.t0) / 0.6f
        drawCircle(Color.White.copy(alpha = 0.7f * (1f - a)), b.r * (0.4f + a * 0.8f), Offset(b.x, b.y), style = Stroke(width = g.potH * 0.012f))
    }
    // hozzávalók a folyadék tetején
    for (p in sim.pot) {
        val age = now - p.t0
        val drop = if (age < 0.35f) (1f - age / 0.35f) * g.potH * 0.7f else 0f
        val bob = sin(now * 3f + p.t0 * 5f) * g.potH * 0.015f
        val pos = Offset(
            cx + cos(p.ang) * p.rad * liqW * 0.45f,
            g.rimY + sin(p.ang) * p.rad * liqH * 0.42f - g.potH * 0.08f + bob - drop,
        )
        drawFit(bitmaps[p.ing], pos, g.potH * 0.3f)
    }
    // kanál
    if (sim.stirring || (sim.pot.isNotEmpty() && !sim.ready)) {
        val sp = if (sim.stirring) sim.spoon else Offset(cx + liqW * 0.15f + sin(now * 3f) * liqW * 0.12f, g.rimY + cos(now * 3f) * liqH * 0.2f)
        val head = Offset(sp.x.coerceIn(cx - liqW * 0.4f, cx + liqW * 0.4f), sp.y.coerceIn(g.rimY - liqH * 0.3f, g.rimY + liqH * 0.3f))
        drawLine(Color(0xFF8D6E63), head, Offset(head.x + g.potW * 0.18f, head.y - g.potH * 0.95f), strokeWidth = g.potH * 0.05f, cap = StrokeCap.Round)
        drawOval(Color(0xFFA1887F), Offset(head.x - g.potH * 0.07f, head.y - g.potH * 0.045f), Size(g.potH * 0.14f, g.potH * 0.09f))
    }
}

private fun DrawScope.drawMonster(g: Geo, sim: Sim, now: Float) {
    val r = g.monsterR
    val cx = g.monsterX
    val eating = sim.flyers.isNotEmpty()
    val chew = now - sim.ateAt in 0f..1.5f
    val bounce = if (chew) abs(sin((now - sim.ateAt) * 9f)) * r * 0.08f else if (sim.ready) abs(sin(now * 5f)) * r * 0.05f else 0f
    val cy = g.monsterY - bounce
    // test
    drawOval(Color(0x22000000), Offset(cx - r * 0.9f, g.potBottom - r * 0.08f), Size(r * 1.8f, r * 0.2f))
    drawOval(monsterColor, Offset(cx - r, cy - r), Size(r * 2f, r * 2.1f))
    drawOval(Color.White.copy(alpha = 0.25f), Offset(cx - r * 0.55f, cy - r * 0.8f), Size(r * 0.5f, r * 0.3f))
    // fülek
    for (s in listOf(-1f, 1f)) {
        drawCircle(monsterColor, r * 0.22f, Offset(cx + s * r * 0.72f, cy - r * 0.88f))
        drawCircle(Color(0xFFB39DDB), r * 0.12f, Offset(cx + s * r * 0.72f, cy - r * 0.88f))
    }
    // szemek: a mutatott irányba néznek
    val lookDir = (sim.look - Offset(cx, cy)).let { d -> val l = hypot(d.x, d.y).coerceAtLeast(1f); Offset(d.x / l, d.y / l) }
    for (s in listOf(-1f, 1f)) {
        val e = Offset(cx + s * r * 0.34f, cy - r * 0.3f)
        drawCircle(Color.White, r * 0.2f, e)
        drawCircle(Color(0xFF2A2A3B), r * 0.1f, Offset(e.x + lookDir.x * r * 0.06f, e.y + lookDir.y * r * 0.06f))
    }
    // száj: éhesen tátog, evéskor rág, utána mosolyog
    val m = g.mouth
    val mw = r * 0.7f
    val mh = when {
        eating -> r * (0.55f + 0.1f * sin(now * 14f))
        chew -> r * (0.15f + 0.2f * abs(sin((now - sim.ateAt) * 9f)))
        sim.ready -> r * (0.38f + 0.08f * sin(now * 6f))
        else -> r * 0.24f
    }
    if (chew && now - sim.ateAt > 1.2f) {
        drawArc(Color(0xFF2A2A3B), 15f, 150f, false, Offset(m.x - mw * 0.5f, m.y - mh * 0.7f), Size(mw, r * 0.4f), style = Stroke(width = r * 0.06f, cap = StrokeCap.Round))
    } else {
        drawOval(Color(0xFF5D1F3A), Offset(m.x - mw / 2f, m.y - mh / 2f), Size(mw, mh))
        drawOval(Color(0xFFFF8A9B), Offset(m.x - mw * 0.28f, m.y + mh * 0.05f), Size(mw * 0.56f, mh * 0.4f))
    }
    for (s in listOf(-1f, 1f)) drawCircle(Color(0x55FF6F91), r * 0.12f, Offset(cx + s * r * 0.6f, cy + r * 0.12f))
}

private fun DrawScope.drawParts(sim: Sim, now: Float, g: Geo) {
    for (p in sim.parts) {
        val a = ((now - p.t0) / p.life).coerceIn(0f, 1f)
        when (p.kind) {
            0 -> drawCircle(p.color.copy(alpha = 1f - a * 0.6f), p.r, Offset(p.x, p.y))
            1 -> drawCircle(Color.White.copy(alpha = 0.55f * (1f - a)), p.r * (0.5f + a), Offset(p.x + sin(a * 6f + p.t0) * p.r * 0.5f, p.y))
            2 -> drawHeart(Offset(p.x, p.y), p.r * (1f + a * 0.3f), p.color.copy(alpha = 1f - a))
            3 -> drawStar(Offset(p.x, p.y), p.r * (1f - a * 0.5f), p.color.copy(alpha = 1f - a))
        }
    }
    for (f in sim.flyers) {
        val a = ((now - f.t0) / f.dur).coerceIn(0f, 1f)
        if (now < f.t0) continue
        val x = f.from.x + (f.to.x - f.from.x) * a
        val y = f.from.y + (f.to.y - f.from.y) * a - sin(a * PI.toFloat()) * f.arc
        drawCircle(f.color, g.potH * 0.07f * (1f - a * 0.4f), Offset(x, y))
    }
}

private fun DrawScope.drawHeart(c: Offset, r: Float, color: Color) {
    drawCircle(color, r * 0.55f, Offset(c.x - r * 0.5f, c.y - r * 0.2f))
    drawCircle(color, r * 0.55f, Offset(c.x + r * 0.5f, c.y - r * 0.2f))
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(c.x - r * 1.0f, c.y)
        lineTo(c.x + r * 1.0f, c.y)
        lineTo(c.x, c.y + r * 1.1f)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawStar(c: Offset, r: Float, color: Color) {
    drawLine(color, Offset(c.x - r, c.y), Offset(c.x + r, c.y), strokeWidth = r * 0.35f, cap = StrokeCap.Round)
    drawLine(color, Offset(c.x, c.y - r), Offset(c.x, c.y + r), strokeWidth = r * 0.35f, cap = StrokeCap.Round)
}

private fun DrawScope.drawTray(g: Geo, bitmaps: List<ImageBitmap>, sim: Sim) {
    for (i in ingredients.indices) {
        val c = g.trayCenter(i)
        drawRoundRect(Color(0xFFFFF3E0), Offset(c.x - g.cellW * 0.46f, c.y - g.trayH * 0.45f), Size(g.cellW * 0.92f, g.trayH * 0.9f), CornerRadius(g.trayH * 0.2f))
        val dim = if (sim.dragIdx == i) 0.3f else 1f
        val bm = bitmaps[i]
        val s = g.iconS / maxOf(bm.width, bm.height)
        val w = (bm.width * s).toInt().coerceAtLeast(1)
        val h = (bm.height * s).toInt().coerceAtLeast(1)
        drawImage(bm, dstOffset = IntOffset((c.x - w / 2f).toInt(), (c.y - h / 2f).toInt()), dstSize = IntSize(w, h), alpha = dim)
    }
}

private fun DrawScope.drawLabel(g: Geo, sim: Sim, now: Float) {
    val l = sim.label ?: return
    val a = now - sim.labelT0
    if (a > 1.4f) return
    val paint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = g.stageH * 0.09f
        isFakeBoldText = true
        color = android.graphics.Color.argb(((1f - a / 1.4f).coerceIn(0f, 1f) * 255).toInt(), 0x2A, 0x2A, 0x3B)
    }
    drawIntoCanvas { it.nativeCanvas.drawText(l, g.potCx, g.rimY - g.rimRy - g.potH * 0.1f - a * g.potH * 0.2f, paint) }
}
