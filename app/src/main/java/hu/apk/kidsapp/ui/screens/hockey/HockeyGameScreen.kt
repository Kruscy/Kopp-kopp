package hu.apk.kidsapp.ui.screens.hockey

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.apk.kidsapp.ui.components.BackButton
import hu.apk.kidsapp.ui.theme.KidText
import hu.apk.kidsapp.util.SoundFx
import hu.apk.kidsapp.util.Step
import hu.apk.kidsapp.util.lighten
import hu.apk.kidsapp.util.darken
import hu.apk.kidsapp.util.multiTouch
import hu.apk.kidsapp.util.rememberClipPlayer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private val RED = Color(0xFFE53935)
private val BLUE = Color(0xFF1E88E5)
private const val WIN_SCORE = 5

private enum class Phase { SERVE, PLAY, GOAL, WON }

private enum class Event { GOAL, WIN }

private class Burst(val x: Float, val y: Float, val color: Color, var age: Float = 0f)

/** A tábla méretei képpontban. A hossza a képernyő hosszában fekszik, a kapuk a két végén vannak. */
private class Field(w: Float, h: Float, d: Float) {
    val x0 = 60f * d
    val x1 = w - 60f * d
    val y0 = 96f * d
    val y1 = h - 22f * d
    val cx = (x0 + x1) / 2f
    val cy = (y0 + y1) / 2f
    val width = x1 - x0
    val height = y1 - y0
    val rp = height * 0.056f             // korong sugara (nagyobb, könnyebb eltalálni)
    val rm = height * 0.092f             // ütő sugara
    val goalHalf = height * 0.215f       // a kapu fél szélessége
    val zoneDepth = width * 0.30f        // meddig jöhet előre az ütő a saját kapujától
}

private class Game {
    var phase = Phase.SERVE
    var timer = 1.4f
    var scoreL = 0
    var scoreR = 0
    var serveTo = 1   // -1: bal, 1: jobb
    var lastGoalBy = 0
    var winner = 0
    var winTime = 0f
    var event: Event? = null
    val bursts = ArrayList<Burst>()
    var nextBurst = 0f

    var px = 0f; var py = 0f; var vx = 0f; var vy = 0f
    val trail = ArrayList<Offset>()

    // ütők: pozíció, sebesség, cél (az ujj helye)
    var lx = 0f; var ly = 0f; var lvx = 0f; var lvy = 0f; var ltx = 0f; var lty = 0f
    var rx = 0f; var ry = 0f; var rvx = 0f; var rvy = 0f; var rtx = 0f; var rty = 0f
    var leftPointer = -1L
    var rightPointer = -1L
    var fingerSeen = false

    var celebration = 0f   // 0..1 konfetti
    var shake = 0f

    fun resetPositions(f: Field) {
        lx = f.x0 + f.rm * 2.2f; ly = f.cy; ltx = lx; lty = ly; lvx = 0f; lvy = 0f
        rx = f.x1 - f.rm * 2.2f; ry = f.cy; rtx = rx; rty = ry; rvx = 0f; rvy = 0f
    }

    fun serve(f: Field, toward: Int) {
        px = f.cx; py = f.cy
        val speed = f.width * 0.15f
        val ang = (Random.nextFloat() - 0.5f) * 0.9f
        vx = toward * speed * cos(ang)
        vy = speed * sin(ang)
        trail.clear()
    }
}

@Composable
fun HockeyGameScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clips = rememberClipPlayer()
    val sfx = remember { SoundFx(context.applicationContext) }
    DisposableEffect(sfx) { onDispose { sfx.release() } }

    val game = remember { Game() }
    var frame by remember { mutableLongStateOf(0L) }
    var started by remember { mutableStateOf(false) }
    var winnerUi by remember { mutableIntStateOf(0) }

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFF263238))) {
        val density = LocalDensity.current
        val d = density.density
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        val field = remember(w, h) { Field(w, h, d) }

        fun newGame() {
            game.scoreL = 0; game.scoreR = 0
            game.winner = 0
            game.resetPositions(field)
            game.phase = Phase.SERVE
            game.timer = 1.2f
            game.serveTo = if (Random.nextBoolean()) 1 else -1
            game.px = field.cx; game.py = field.cy; game.vx = 0f; game.vy = 0f
            game.trail.clear()
            game.bursts.clear()
            game.celebration = 0f
            game.winTime = 0f
            winnerUi = 0
            clips.play(Step.Clip("voice_game_start"))
        }

        LaunchedEffect(field) {
            game.resetPositions(field)
            game.px = field.cx; game.py = field.cy
            if (!started) {
                started = true
                clips.play(Step.Clip("voice_game_start"))
            }
            var last = 0L
            while (true) {
                withFrameNanos { t ->
                    if (last != 0L) {
                        val dt = ((t - last) / 1_000_000_000f).coerceIn(0.001f, 0.04f)
                        step(game, field, dt, sfx)
                        when (game.event) {
                            Event.GOAL -> clips.play(Step.Clip("voice_goal"))
                            Event.WIN -> {
                                winnerUi = game.winner
                                clips.play(
                                // a nyertes színe, gratuláció, ünneplés, majd újraindul a játék
                                Step.Clip(if (game.winner < 0) "voice_red_won" else "voice_blue_won"),
                                Step.Pause(300),
                                Step.Clip("voice_winner"),
                                Step.Pause(4200),
                                Step.Mark { newGame() },
                                )
                            }
                            null -> {}
                        }
                        game.event = null
                        frame++
                    }
                    last = t
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .multiTouch(
                    onDown = { id, pos ->
                        game.fingerSeen = true
                        // Az ÚJ érintés mindig átveszi a saját oldal ütőjét. Régen az első ujj
                        // "lefoglalta" az oldalt, és ha az (pl. tenyér, vagy elveszett felengedés miatt)
                        // elakadt, az adott oldal lefagyott, és a valódi ujjat már nem vette figyelembe.
                        if (pos.x < field.cx) {
                            game.leftPointer = id
                            setTarget(game, field, true, pos)
                        } else {
                            game.rightPointer = id
                            setTarget(game, field, false, pos)
                        }
                    },
                    onMove = { id, pos ->
                        if (id == game.leftPointer) setTarget(game, field, true, pos)
                        else if (id == game.rightPointer) setTarget(game, field, false, pos)
                    },
                    onUp = { id ->
                        if (id == game.leftPointer) game.leftPointer = -1
                        if (id == game.rightPointer) game.rightPointer = -1
                    },
                ),
        ) {
            @Suppress("UNUSED_VARIABLE") val tick = frame
            drawRink(field, game, pulse)
        }

        // felső sáv
        Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            BackButton(onClick = onBack)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Text(
                    "Léghoki",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                FilledIconButton(
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF455A64)),
                    onClick = { newGame() },
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Új játék", tint = Color.White)
                }
            }
        }

        // győzelem
        if (winnerUi != 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White.copy(alpha = 0.94f))
                        .clickable { newGame() }
                        .padding(horizontal = 44.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("🏆", fontSize = 64.sp)
                    Text(
                        if (winnerUi < 0) "A piros nyert!" else "A kék nyert!",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = if (winnerUi < 0) RED else BLUE,
                    )
                    Text("Gratulálok!", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = KidText)
                    Spacer(Modifier.width(8.dp))
                    Row {
                        Text("🔄 ", fontSize = 20.sp)
                        Text("Mindjárt új játék indul…", fontSize = 18.sp, color = KidText.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

private fun setTarget(g: Game, f: Field, left: Boolean, pos: Offset) {
    if (left) {
        g.ltx = pos.x.coerceIn(f.x0 + f.rm + 4f, f.cx - f.rm - 4f)
        g.lty = pos.y.coerceIn(f.y0 + f.rm + 2f, f.y1 - f.rm - 2f)
    } else {
        g.rtx = pos.x.coerceIn(f.cx + f.rm + 4f, f.x1 - f.rm - 4f)
        g.rty = pos.y.coerceIn(f.y0 + f.rm + 2f, f.y1 - f.rm - 2f)
    }
}

// ---------------------------------------------------------------------------------------------
// Fizika
// ---------------------------------------------------------------------------------------------

private fun step(g: Game, f: Field, dtFrame: Float, sfx: SoundFx) {
    g.celebration = max(0f, g.celebration - dtFrame * 0.7f)
    if (g.phase != Phase.WON && g.bursts.isNotEmpty()) {
        g.bursts.forEach { it.age += dtFrame }
        g.bursts.removeAll { it.age > 1.5f }
    }
    g.shake = max(0f, g.shake - dtFrame * 3f)
    val sub = 4
    val dt = dtFrame / sub
    repeat(sub) {
        movePaddles(g, f, dt)
        when (g.phase) {
            Phase.SERVE -> {
                if (it == 0) g.timer -= dtFrame
                if (g.timer <= 0f) {
                    g.serve(f, g.serveTo)
                    g.phase = Phase.PLAY
                }
            }
            Phase.GOAL -> {
                if (it == 0) g.timer -= dtFrame
                if (g.timer <= 0f) {
                    g.resetPositions(f)
                    g.px = f.cx; g.py = f.cy; g.vx = 0f; g.vy = 0f
                    g.trail.clear()
                    g.phase = Phase.SERVE
                    g.timer = 1.0f
                }
            }
            Phase.WON -> if (it == 0) stepFireworks(g, f, dtFrame)
            Phase.PLAY -> stepPuck(g, f, dt, sfx)
        }
    }
    if (g.phase == Phase.PLAY) {
        g.trail.add(Offset(g.px, g.py))
        if (g.trail.size > 10) g.trail.removeAt(0)
    }
}

private fun stepFireworks(g: Game, f: Field, dt: Float) {
    g.winTime += dt
    g.nextBurst -= dt
    if (g.nextBurst <= 0f) {
        g.nextBurst = 0.28f
        val col = if (g.winner < 0) RED else BLUE
        val colors = listOf(col, Color(0xFFFFD54F), col.lighten(0.3f), Color.White)
        g.bursts.add(
            Burst(
                f.x0 + f.width * (0.08f + 0.84f * Random.nextFloat()),
                f.y0 + f.height * (0.12f + 0.7f * Random.nextFloat()),
                colors[Random.nextInt(colors.size)],
            ),
        )
    }
    g.bursts.forEach { it.age += dt }
    g.bursts.removeAll { it.age > 1.5f }
}

private fun movePaddles(g: Game, f: Field, dt: Float) {
    val maxSpeed = f.width * 1.6f // px/s (az ütő sebessége)
    fun move(x: Float, y: Float, tx: Float, ty: Float): FloatArray {
        val dx = tx - x
        val dy = ty - y
        val dist = hypot(dx, dy)
        val maxMove = maxSpeed * dt
        return if (dist <= maxMove || dist < 0.001f) floatArrayOf(tx, ty, dx / dt, dy / dt)
        else floatArrayOf(x + dx / dist * maxMove, y + dy / dist * maxMove, dx / dist * maxSpeed, dy / dist * maxSpeed)
    }
    val l = move(g.lx, g.ly, g.ltx, g.lty)
    g.lx = l[0]; g.ly = l[1]; g.lvx = l[2]; g.lvy = l[3]
    val r = move(g.rx, g.ry, g.rtx, g.rty)
    g.rx = r[0]; g.ry = r[1]; g.rvx = r[2]; g.rvy = r[3]
}

private fun stepPuck(g: Game, f: Field, dt: Float, sfx: SoundFx) {
    g.px += g.vx * dt
    g.py += g.vy * dt
    // kis súrlódás
    val damp = 1f - 0.35f * dt
    g.vx *= damp; g.vy *= damp

    val rp = f.rp
    // alsó / felső fal
    if (g.py - rp < f.y0) { g.py = f.y0 + rp; g.vy = abs(g.vy) * 0.96f; wallSound(g, f, sfx) }
    if (g.py + rp > f.y1) { g.py = f.y1 - rp; g.vy = -abs(g.vy) * 0.96f; wallSound(g, f, sfx) }

    // oldalfalak: a kapunyílás kivételével
    val inGoalY = kotlin.math.abs(g.py - f.cy) < f.goalHalf - rp * 0.3f
    if (g.px - rp < f.x0 && !inGoalY) { g.px = f.x0 + rp; g.vx = abs(g.vx) * 0.96f; wallSound(g, f, sfx) }
    if (g.px + rp > f.x1 && !inGoalY) { g.px = f.x1 - rp; g.vx = -abs(g.vx) * 0.96f; wallSound(g, f, sfx) }

    // kapufák
    for (postX in listOf(f.x0, f.x1)) {
        for (sgn in listOf(-1f, 1f)) {
            val postY = f.cy + sgn * f.goalHalf
            val dx = g.px - postX
            val dy = g.py - postY
            val dist = hypot(dx, dy)
            if (dist < rp + 4f && dist > 0.001f) {
                val nx = dx / dist; val ny = dy / dist
                g.px = postX + nx * (rp + 4f); g.py = postY + ny * (rp + 4f)
                val dot = g.vx * nx + g.vy * ny
                if (dot < 0) { g.vx -= 1.9f * dot * nx; g.vy -= 1.9f * dot * ny; sfx.play("wall", 1.2f, 0.7f) }
            }
        }
    }

    // ütők
    collide(g, f, g.lx, g.ly, g.lvx, g.lvy, sfx)
    collide(g, f, g.rx, g.ry, g.rvx, g.rvy, sfx)

    // sebesség korlátozása: soha ne álljon meg, és ne legyen túl gyors
    val sp = hypot(g.vx, g.vy)
    val minSp = f.width * 0.08f
    val maxSp = f.width * 0.36f
    if (sp < minSp && sp > 0.001f) { g.vx *= minSp / sp; g.vy *= minSp / sp }
    else if (sp > maxSp) { g.vx *= maxSp / sp; g.vy *= maxSp / sp }
    else if (sp <= 0.001f) { g.vx = minSp; g.vy = 0f }

    // gól?
    if (g.px < f.x0 - rp * 0.6f && inGoalY) goal(g, scorerRight = true, sfx = sfx)
    else if (g.px > f.x1 + rp * 0.6f && inGoalY) goal(g, scorerRight = false, sfx = sfx)
}

private fun abs(v: Float) = kotlin.math.abs(v)

private fun wallSound(g: Game, f: Field, sfx: SoundFx) {
    val sp = hypot(g.vx, g.vy) / f.width
    if (sp > 0.25f) sfx.play("wall", 0.9f + sp * 0.4f, min(1f, sp))
}

private fun collide(g: Game, f: Field, mx: Float, my: Float, mvx: Float, mvy: Float, sfx: SoundFx) {
    val dx = g.px - mx
    val dy = g.py - my
    val dist = hypot(dx, dy)
    val minDist = f.rp + f.rm
    if (dist < minDist && dist > 0.001f) {
        val nx = dx / dist; val ny = dy / dist
        // kitolás az ütőből
        g.px = mx + nx * minDist
        g.py = my + ny * minDist
        // relatív sebesség az ütőhöz képest
        val rvx = g.vx - mvx
        val rvy = g.vy - mvy
        val dot = rvx * nx + rvy * ny
        if (dot < 0) {
            val e = 1.0f
            g.vx = g.vx - (1f + e) * dot * nx
            g.vy = g.vy - (1f + e) * dot * ny
            // az ütő mozgása is lendületet ad ("vissza lehet ütni")
            val push = (mvx * nx + mvy * ny).coerceAtLeast(0f) * 0.12f
            g.vx += nx * push
            g.vy += ny * push
            val sp = hypot(g.vx, g.vy) / f.width
            sfx.play("paddle", 0.85f + min(0.6f, sp * 0.5f), 1f)
            g.shake = 0.35f
        }
    }
}

private fun goal(g: Game, scorerRight: Boolean, sfx: SoundFx) {
    if (scorerRight) g.scoreR++ else g.scoreL++
    g.lastGoalBy = if (scorerRight) 1 else -1
    g.celebration = 1f
    g.vx = 0f; g.vy = 0f
    g.phase = Phase.GOAL
    g.timer = 1.7f
    // a kapott gól után a korong a kapott fél felé indul
    g.serveTo = if (scorerRight) -1 else 1
    val win = if (g.scoreR >= WIN_SCORE) 1 else if (g.scoreL >= WIN_SCORE) -1 else 0
    if (win != 0) {
        g.winner = win // -1: piros (bal) nyert, 1: kék (jobb)
        g.phase = Phase.WON
        g.winTime = 0f
        g.bursts.clear()
        g.event = Event.WIN
        sfx.play("win")
    } else {
        g.event = Event.GOAL
        sfx.play("goal")
    }
}

// ---------------------------------------------------------------------------------------------
// Rajzolás
// ---------------------------------------------------------------------------------------------

private fun DrawScope.drawRink(f: Field, g: Game, pulse: Float) {
    val shakeX = if (g.shake > 0f) (Random.nextFloat() - 0.5f) * 6f * g.shake else 0f
    // asztal
    drawRoundRect(Color(0xFF37474F), Offset(f.x0 - 38f, f.y0 - 26f), Size(f.width + 76f, f.height + 52f), CornerRadius(46f))
    drawRoundRect(
        Brush.verticalGradient(listOf(Color(0xFFE1F5FE), Color(0xFFB3E5FC)), startY = f.y0, endY = f.y1),
        Offset(f.x0, f.y0), Size(f.width, f.height), CornerRadius(26f),
    )
    // jégcsillogás
    for (i in 0 until 6) {
        val x = f.x0 + f.width * (0.1f + i * 0.16f)
        drawLine(Color.White.copy(alpha = 0.35f), Offset(x, f.y0 + 10f), Offset(x - 120f, f.y1 - 10f), strokeWidth = 14f)
    }
    // középvonal, kör, kapuk
    drawLine(Color(0xFF90A4AE), Offset(f.cx, f.y0), Offset(f.cx, f.y1), strokeWidth = 6f)
    drawCircle(Color(0xFF90A4AE), f.height * 0.2f, Offset(f.cx, f.cy), style = Stroke(width = 6f))
    drawCircle(Color(0xFF90A4AE), 10f, Offset(f.cx, f.cy))
    for ((side, color) in listOf(-1 to RED, 1 to BLUE)) {
        val gx = if (side < 0) f.x0 else f.x1
        // kapu-félkör a jégen
        drawCircle(color.copy(alpha = 0.35f), f.goalHalf, Offset(gx, f.cy), style = Stroke(width = 6f))
        // a kapu nyílása (sötét zseb)
        drawRoundRect(
            Color(0xFF263238),
            Offset(if (side < 0) gx - 52f else gx - 6f, f.cy - f.goalHalf),
            Size(58f, f.goalHalf * 2f),
            CornerRadius(20f),
        )
        drawLine(color, Offset(gx, f.cy - f.goalHalf), Offset(gx, f.cy + f.goalHalf), strokeWidth = 10f)
        // kapufák
        drawCircle(color, 14f, Offset(gx, f.cy - f.goalHalf))
        drawCircle(color, 14f, Offset(gx, f.cy + f.goalHalf))
    }
    // fal a kapu nyílásán kívül (két szakasz)
    for (gx in listOf(f.x0, f.x1)) {
        drawLine(Color(0xFF90A4AE), Offset(gx, f.y0), Offset(gx, f.cy - f.goalHalf), strokeWidth = 8f)
        drawLine(Color(0xFF90A4AE), Offset(gx, f.cy + f.goalHalf), Offset(gx, f.y1), strokeWidth = 8f)
    }

    // pontok: a saját kapu mellett, a játékos felé fordítva
    drawScore(f, g, left = true)
    drawScore(f, g, left = false)

    // korong nyoma
    if (g.trail.size > 1) {
        g.trail.forEachIndexed { i, p ->
            val a = (i + 1f) / g.trail.size
            drawCircle(Color(0xFF263238).copy(alpha = 0.12f * a), f.rp * (0.55f + 0.45f * a), p)
        }
    }
    // korong
    if (g.phase == Phase.SERVE || g.phase == Phase.PLAY) {
        val px = g.px + shakeX
        drawCircle(Color.Black.copy(alpha = 0.22f), f.rp * 1.05f, Offset(px + 5f, g.py + 7f))
        drawCircle(Brush.radialGradient(listOf(Color(0xFF546E7A), Color(0xFF1C2A31)), center = Offset(px - f.rp * 0.3f, g.py - f.rp * 0.3f), radius = f.rp * 1.3f), f.rp, Offset(px, g.py))
        drawCircle(Color(0xFFFFD54F), f.rp * 0.55f, Offset(px, g.py), style = Stroke(width = 4f))
        drawCircle(Color.White.copy(alpha = 0.55f), f.rp * 0.18f, Offset(px - f.rp * 0.35f, g.py - f.rp * 0.38f))
    }
    // szervánál: a korong a közepén villog
    if (g.phase == Phase.SERVE) {
        drawCircle(Color(0xFFFFD54F).copy(alpha = 0.6f), f.rp * 1.6f * pulse, Offset(f.cx, f.cy), style = Stroke(width = 6f))
    }
    // ütők
    drawMallet(f, g.lx, g.ly, RED, g.leftPointer >= 0)
    drawMallet(f, g.rx, g.ry, BLUE, g.rightPointer >= 0)

    // győzelmi tűzijáték
    g.bursts.forEach { b ->
        val t = b.age / 1.5f
        val radius = f.height * 0.30f * (1f - (1f - t) * (1f - t))
        val alpha = (1f - t).coerceIn(0f, 1f)
        for (i in 0 until 18) {
            val ang = i * (2f * PI.toFloat() / 18f)
            val c = Offset(b.x + cos(ang) * radius, b.y + sin(ang) * radius + t * t * 40f)
            drawCircle(b.color.copy(alpha = alpha), 11f * (1f - 0.5f * t), c)
        }
        drawCircle(Color.White.copy(alpha = alpha * 0.8f), 14f * (1f - t), Offset(b.x, b.y))
    }

    // gól-konfetti: abból a kapuból szóródik, ahová a korong beesett
    if (g.celebration > 0f) {
        val t = 1f - g.celebration
        val ox = if (g.lastGoalBy > 0) f.x0 else f.x1
        val dirX = if (ox < f.cx) 1f else -1f
        val color = if (g.lastGoalBy > 0) BLUE else RED
        for (i in 0 until 26) {
            val a = (i / 26f) * 2f * PI.toFloat()
            val dist = t * f.height * (0.5f + 0.5f * ((i * 7) % 5) / 4f)
            val c = Offset(ox + dirX * kotlin.math.abs(cos(a)) * dist, f.cy + sin(a) * dist)
            val col = if (i % 2 == 0) color else Color(0xFFFFD54F)
            drawCircle(col.copy(alpha = g.celebration), 9f + (i % 3) * 4f, c)
        }
    }
}

private fun DrawScope.drawMallet(f: Field, x: Float, y: Float, color: Color, active: Boolean) {
    val r = f.rm
    drawCircle(Color.Black.copy(alpha = 0.25f), r * 1.05f, Offset(x + 6f, y + 9f))
    drawCircle(Brush.radialGradient(listOf(color.lighten(0.25f), color.darken(0.22f)), center = Offset(x - r * 0.3f, y - r * 0.3f), radius = r * 1.5f), r, Offset(x, y))
    drawCircle(color.darken(0.4f), r, Offset(x, y), style = Stroke(width = 5f))
    drawCircle(color.lighten(0.35f), r * 0.52f, Offset(x, y))
    drawCircle(color.darken(0.25f), r * 0.52f, Offset(x, y), style = Stroke(width = 4f))
    drawCircle(Color.White.copy(alpha = 0.55f), r * 0.14f, Offset(x - r * 0.18f, y - r * 0.2f))
    if (active) drawCircle(Color.White.copy(alpha = 0.55f), r * 1.18f, Offset(x, y), style = Stroke(width = 5f))
}

private val scorePaint = android.graphics.Paint().apply {
    isAntiAlias = true
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
}

private fun DrawScope.drawScore(f: Field, g: Game, left: Boolean) {
    val score = if (left) g.scoreL else g.scoreR
    val color = if (left) RED else BLUE
    // a pontok pöttyei a kapu mellett (függőlegesen), a szám a játékos felé fordítva
    val x = if (left) f.x0 + f.width * 0.17f else f.x1 - f.width * 0.17f
    val dotR = f.height * 0.026f
    val total = WIN_SCORE
    val gap = dotR * 3.2f
    val startY = f.cy - (total - 1) * gap / 2f
    for (i in 0 until total) {
        val c = Offset(x, startY + i * gap)
        if (i < score) drawCircle(color, dotR, c) else drawCircle(color.copy(alpha = 0.35f), dotR, c, style = Stroke(width = 3f))
    }
    val size = f.height * 0.30f
    scorePaint.textSize = size
    scorePaint.color = color.copy(alpha = 0.28f).toArgb()
    val tx = if (left) f.x0 + f.width * 0.07f else f.x1 - f.width * 0.07f
    rotate(if (left) 90f else -90f, Offset(tx, f.cy)) {
        drawContext.canvas.nativeCanvas.drawText(score.toString(), tx, f.cy + size * 0.35f, scorePaint)
    }
}

