package hu.apk.kidsapp.ui.screens.scene

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.apk.kidsapp.ui.components.BackButton
import hu.apk.kidsapp.ui.theme.KidBackground
import hu.apk.kidsapp.ui.theme.KidText
import hu.apk.kidsapp.util.SoundFx
import hu.apk.kidsapp.util.Step
import hu.apk.kidsapp.util.multiTouch
import hu.apk.kidsapp.util.rememberClipPlayer
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private enum class React { NONE, HOP, DRIVE, ROCKET, WOBBLE }

/** Egy letett matrica. Minden mező egyszerű (a rajzoló ciklus képkockánként újrarajzol). */
private class Placed(var def: StickerDef, var x: Float, var y: Float, val phase: Float) {
    var react = React.NONE
    var reactT0 = 0f
    var driveDir = 1f
    var lamp = -1
    var dragging = false
}

private class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, val glyph: String, val size: Float)

private class Pose(var dx: Float = 0f, var dy: Float = 0f, var sx: Float = 1f, var sy: Float = 1f, var rot: Float = 0f, var flip: Boolean = false)

private class Drag(val p: Placed, val offX: Float, val offY: Float, val startX: Float, val startY: Float, val t0: Float) {
    var moved = false
}

private val lampNames = listOf("Piros", "Sárga", "Zöld")
private val lampColors = listOf(Color(0xFFE53935), Color(0xFFFDD835), Color(0xFF43A047))
private val dinoRoarers = setOf("trex", "triceratops", "brontosaurus", "stegosaurus")

/** Matricás világ: a jelenetválasztóból megnyíló, matricákkal játszható képes jelenetek. */
@Composable
fun SceneWorldScreen(onBack: () -> Unit) {
    var open by remember { mutableStateOf<Int?>(null) }
    val saved = remember { HashMap<String, MutableList<Placed>>() }
    val idx = open
    if (idx == null) {
        ScenePicker(onBack = onBack, onOpen = { open = it })
    } else {
        val scene = sceneWorlds[idx]
        SceneStage(scene = scene, placed = saved.getOrPut(scene.id) { mutableListOf() }, onPicker = { open = null })
    }
}

@Composable
private fun ScenePicker(onBack: () -> Unit, onOpen: (Int) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(KidBackground)) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                BackButton(onClick = onBack)
                Spacer(Modifier.weight(1f))
                Text(
                    "Matricák",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = KidText,
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(1.dp))
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            ) {
                sceneWorlds.withIndex().toList().chunked(3).forEach { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        rowItems.forEach { (i, s) ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(Color.White)
                                    .clickable { onOpen(i) }
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Image(
                                    painter = painterResource(s.bg),
                                    contentDescription = s.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(2f).clip(RoundedCornerShape(20.dp)),
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "${s.emoji} ${s.title}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KidText,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                )
                            }
                        }
                        repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SceneStage(scene: SceneDef, placed: MutableList<Placed>, onPicker: () -> Unit) {
    val context = LocalContext.current
    val sfx = remember { SoundFx(context.applicationContext) }
    DisposableEffect(sfx) { onDispose { sfx.release() } }
    val player = rememberClipPlayer()
    val density = LocalDensity.current
    val dp = density.density

    val bitmaps: Map<String, ImageBitmap> = scene.stickers.associate { it.id to ImageBitmap.imageResource(it.image) }
    var t by remember { mutableFloatStateOf(0f) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var label by remember { mutableStateOf<String?>(null) }
    var labelSeq by remember { mutableIntStateOf(0) }
    val particles = remember { ArrayList<Particle>() }
    val drags = remember { HashMap<Long, Drag>() }
    val glyphPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    val trayH = 112f * dp
    val W = size.width.toFloat()
    val H = size.height.toFloat()

    fun sizeOf(d: StickerDef): Pair<Float, Float> {
        val bmp = bitmaps[d.id]!!
        val h = d.h * H
        return Pair(h * bmp.width / bmp.height, h)
    }

    fun say(text: String) {
        label = text
        labelSeq++
    }

    fun burst(x: Float, y: Float, glyph: String, n: Int, up: Boolean) {
        repeat(n) {
            val a = Random.nextFloat() * 2f * PI.toFloat()
            val v = (80f + Random.nextFloat() * 220f) * dp
            particles.add(
                Particle(
                    x, y, cos(a) * v, if (up) -abs(sin(a)) * v - 80f * dp else sin(a) * v,
                    0.9f + Random.nextFloat() * 0.6f, glyph, (14f + Random.nextFloat() * 14f) * dp,
                ),
            )
        }
    }

    fun pose(p: Placed): Pose {
        val d = p.def
        val (w, h) = sizeOf(d)
        val o = Pose()
        if (p.dragging) {
            o.sx = 1.12f
            o.sy = 1.12f
            return o
        }
        when (d.kind) {
            Kind.WALKER -> {
                val ph = t * 0.55f + p.phase
                o.dx = W * 0.07f * sin(ph)
                o.flip = cos(ph) < 0f
                o.dy = -abs(sin(t * 5f + p.phase)) * 5f * dp
            }
            Kind.FLYER -> {
                val ph = t * 0.5f + p.phase
                o.dx = W * 0.09f * sin(ph)
                o.dy = 28f * dp * sin(t * 0.9f + p.phase)
                o.flip = cos(ph) < 0f
                o.rot = 6f * sin(t * 2f + p.phase)
            }
            else -> {
                val b = 0.015f * sin(t * 1.6f + p.phase)
                o.sx = 1f + b
                o.sy = 1f - b
            }
        }
        val age = t - p.reactT0
        when (p.react) {
            React.HOP -> if (age < 0.9f) {
                val k = age / 0.9f
                o.dy -= abs(sin(k * PI.toFloat() * 2.5f)) * (1f - k) * H * 0.09f
                o.sy *= 1f + 0.10f * sin(k * PI.toFloat() * 5f)
                o.sx *= 1f - 0.07f * sin(k * PI.toFloat() * 5f)
            }
            React.WOBBLE -> if (age < 1.2f) o.rot = 14f * sin(age * 28f)
            React.DRIVE -> if (age < 3.6f) {
                val k = age / 3.6f
                val total = W + 2f * w
                val xs = (((p.x + p.driveDir * k * total + w) % total) + total) % total - w
                o.dx = xs - p.x
                o.flip = p.driveDir < 0f
                o.dy = -abs(sin(t * 12f)) * 2.5f * dp
            }
            React.ROCKET -> if (age < 3.4f) {
                val k = age / 3.4f
                val total = H + 2f * h
                val ys = (((p.y - k * total + h) % total) + total) % total - h
                o.dy = ys - p.y
                o.dx = 3f * dp * sin(t * 40f)
            }
            else -> Unit
        }
        return o
    }

    fun hit(pos: Offset): Placed? {
        for (i in placed.indices.reversed()) {
            val p = placed[i]
            val (w, h) = sizeOf(p.def)
            val o = pose(p)
            val cx = p.x + o.dx
            val cy = p.y + o.dy
            if (abs(pos.x - cx) <= w / 2f * 1.05f && abs(pos.y - cy) <= h / 2f * 1.05f) return p
        }
        return null
    }

    fun sfxFor(d: StickerDef): String = when {
        d.kind == Kind.DRIVER -> "honk"
        d.kind == Kind.ROCKET -> "whoosh"
        d.kind == Kind.FLYER -> "chirp"
        d.kind == Kind.LAMP -> "ding"
        d.id in dinoRoarers -> "roar"
        d.id == "babydino" -> "chirp"
        d.kind == Kind.WALKER -> "pop"
        else -> "boing"
    }

    fun tap(p: Placed) {
        val d = p.def
        say(d.name)
        player.play(Step.Clip("st_${d.id}"))
        p.reactT0 = t
        when (d.kind) {
            Kind.DRIVER -> {
                p.react = React.DRIVE
                p.driveDir = if (p.x < W / 2f) 1f else -1f
            }
            Kind.ROCKET -> p.react = React.ROCKET
            Kind.LAMP -> {
                p.lamp = (p.lamp + 1) % 3
                p.react = React.HOP
                say(lampNames[p.lamp])
            }
            Kind.EGG -> p.react = React.WOBBLE
            else -> p.react = React.HOP
        }
        sfx.play(sfxFor(d))
        burst(p.x, p.y - sizeOf(d).second * 0.4f, "⭐", 5, up = true)
    }

    fun drop(p: Placed, fingerY: Float) {
        p.dragging = false
        if (fingerY > H - trayH) { // a tálcára dobva: törlés
            placed.remove(p)
            sfx.play("pop")
            burst(p.x, p.y, "💨", 6, up = false)
            return
        }
        val (w, h) = sizeOf(p.def)
        p.x = p.x.coerceIn(w * 0.3f, W - w * 0.3f)
        p.y = p.y.coerceIn(h * 0.35f, H - trayH - h * 0.35f)
        // üdvözlés: ha egy másik matricához közel tettük le
        val other = placed.firstOrNull { q ->
            q !== p && hypot(q.x - p.x, q.y - p.y) < (sizeOf(q.def).first + w) * 0.35f
        }
        if (other != null) {
            p.react = React.HOP
            p.reactT0 = t
            other.react = React.HOP
            other.reactT0 = t + 0.1f
            sfx.play("ding")
            burst((p.x + other.x) / 2f, (p.y + other.y) / 2f - h * 0.3f, "💖", 7, up = true)
        } else {
            sfx.play("pop")
        }
        say(p.def.name)
        player.play(Step.Clip("st_${p.def.id}"))
    }

    fun spawn(d: StickerDef, x: Float, y: Float): Placed {
        val p = Placed(d, x, y, Random.nextFloat() * 6.28f)
        placed.add(p)
        return p
    }

    // játékciklus
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { n ->
                if (last != 0L) {
                    val dt = ((n - last) / 1e9f).coerceAtMost(0.05f)
                    t += dt
                    val iter = particles.iterator()
                    while (iter.hasNext()) {
                        val q = iter.next()
                        q.life -= dt
                        q.x += q.vx * dt
                        q.y += q.vy * dt
                        q.vy += 300f * dp * dt
                        if (q.life <= 0f) iter.remove()
                    }
                    // a tojásból kikel a kisdínó
                    for (p in placed) {
                        if (p.react == React.WOBBLE && t - p.reactT0 > 1.2f) {
                            val baby = scene.stickers.firstOrNull { it.id == "babydino" }
                            if (baby != null) {
                                p.def = baby
                                p.react = React.HOP
                                p.reactT0 = t
                                sfx.play("chirp")
                                burst(p.x, p.y, "⭐", 10, up = true)
                                say(baby.name)
                                player.play(Step.Clip("st_${baby.id}"))
                            } else {
                                p.react = React.NONE
                            }
                        }
                    }
                }
                last = n
            }
        }
    }
    LaunchedEffect(labelSeq) {
        if (label != null) {
            delay(1600)
            label = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                size = it.size
                rootOrigin = it.positionInRoot()
            },
    ) {
        Image(
            painter = painterResource(scene.bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .multiTouch(
                    onDown = { id, pos ->
                        val p = hit(pos) ?: return@multiTouch
                        placed.remove(p)
                        placed.add(p) // előre hozzuk
                        val o = pose(p)
                        drags[id] = Drag(p, p.x + o.dx - pos.x, p.y + o.dy - pos.y, pos.x, pos.y, t)
                    },
                    onMove = { id, pos ->
                        val d = drags[id] ?: return@multiTouch
                        if (!d.moved && hypot(pos.x - d.startX, pos.y - d.startY) > 14f * dp) {
                            d.moved = true
                            d.p.dragging = true
                            d.p.react = React.NONE
                        }
                        if (d.moved) {
                            d.p.x = (pos.x + d.offX).coerceIn(0f, W)
                            d.p.y = (pos.y + d.offY).coerceIn(0f, H)
                        }
                    },
                    onUp = { id ->
                        val d = drags.remove(id) ?: return@multiTouch
                        if (!d.moved && t - d.t0 < 0.6f) {
                            tap(d.p)
                        } else if (d.moved) {
                            drop(d.p, d.p.y)
                        }
                    },
                ),
        ) {
            val frame = t // az újrarajzoláshoz
            placed.forEach { p ->
                val bmp = bitmaps[p.def.id] ?: return@forEach
                val (w, h) = sizeOf(p.def)
                val o = pose(p)
                val cx = p.x + o.dx
                val cy = p.y + o.dy
                if (p.def.kind == Kind.LAMP && p.lamp >= 0) {
                    val pulse = 0.85f + 0.15f * sin(frame * 8f)
                    drawCircle(lampColors[p.lamp].copy(alpha = 0.28f * pulse), radius = h * 0.55f, center = Offset(cx, cy))
                }
                if (p.def.kind != Kind.FLYER && p.def.kind != Kind.ROCKET) {
                    val lift = if (p.dragging) 6f * dp else 0f
                    drawOval(
                        Color.Black.copy(alpha = if (p.dragging) 0.10f else 0.15f),
                        topLeft = Offset(cx - w * 0.33f, p.y + h * 0.5f - h * 0.04f + lift),
                        size = Size(w * 0.66f, h * 0.08f),
                    )
                }
                withTransform({
                    translate(cx, cy)
                    rotate(o.rot, Offset.Zero)
                    scale(if (o.flip) -o.sx else o.sx, o.sy, Offset.Zero)
                }) {
                    drawImage(
                        bmp,
                        dstOffset = IntOffset((-w / 2f).toInt(), (-h / 2f).toInt()),
                        dstSize = IntSize(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1)),
                    )
                }
            }
            drawIntoCanvas { c ->
                particles.forEach { q ->
                    glyphPaint.textSize = q.size
                    glyphPaint.alpha = (255 * (q.life / 0.9f).coerceIn(0f, 1f)).toInt()
                    c.nativeCanvas.drawText(q.glyph, q.x - q.size / 2f, q.y, glyphPaint)
                }
            }
        }

        // felső sor
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onClick = onPicker)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.85f))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text("${scene.emoji} ${scene.title}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = KidText)
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.85f), CircleShape)
                    .clickable {
                        placed.clear()
                        sfx.play("pop")
                    },
                contentAlignment = Alignment.Center,
            ) { Text("🗑️", fontSize = 26.sp) }
        }

        // a név szalag
        label?.let { l ->
            Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 88.dp)) {
                Text(
                    l,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    color = KidText,
                    modifier = Modifier
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White.copy(alpha = 0.92f))
                        .padding(horizontal = 32.dp, vertical = 10.dp),
                )
            }
        }

        // matrica-tálca alul
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(112.dp)
                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(scene.stickers) { d ->
                    var itemOrigin by remember { mutableStateOf(Offset.Zero) }
                    var dragging by remember { mutableStateOf<Placed?>(null) }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF1F3F8))
                            .onGloballyPositioned { itemOrigin = it.positionInRoot() }
                            .clickable {
                                val p = spawn(d, W * (0.2f + 0.5f * Random.nextFloat()), H * (0.35f + 0.25f * Random.nextFloat()))
                                p.react = React.HOP
                                p.reactT0 = t
                                sfx.play("pop")
                                say(d.name)
                                player.play(Step.Clip("st_${d.id}"))
                            }
                            .pointerInput(d) {
                                detectVerticalDragGestures(
                                    onDragStart = { off ->
                                        val local = itemOrigin - rootOrigin + off
                                        val p = spawn(d, local.x, local.y)
                                        p.dragging = true
                                        dragging = p
                                        sfx.play("pop")
                                    },
                                    onVerticalDrag = { change, _ ->
                                        val p = dragging ?: return@detectVerticalDragGestures
                                        val local = itemOrigin - rootOrigin + change.position
                                        p.x = local.x.coerceIn(0f, W)
                                        p.y = local.y.coerceIn(0f, H)
                                    },
                                    onDragEnd = {
                                        dragging?.let { drop(it, it.y) }
                                        dragging = null
                                    },
                                    onDragCancel = {
                                        dragging?.let { drop(it, it.y) }
                                        dragging = null
                                    },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(d.image),
                            contentDescription = d.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                        )
                    }
                }
            }
        }
    }
}
