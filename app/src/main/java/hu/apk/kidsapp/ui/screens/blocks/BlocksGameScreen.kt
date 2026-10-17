package hu.apk.kidsapp.ui.screens.blocks

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.apk.kidsapp.game.blocks.MAX_UNITS
import hu.apk.kidsapp.game.blocks.Piece
import hu.apk.kidsapp.game.blocks.Shape
import hu.apk.kidsapp.game.blocks.WORLD_COLS
import hu.apk.kidsapp.game.blocks.units
import hu.apk.kidsapp.ui.components.BackButton
import hu.apk.kidsapp.ui.theme.KidGreen
import hu.apk.kidsapp.ui.theme.KidPurple
import hu.apk.kidsapp.ui.theme.KidRed
import hu.apk.kidsapp.ui.theme.KidText
import hu.apk.kidsapp.ui.theme.KidYellow
import hu.apk.kidsapp.util.Voice
import hu.apk.kidsapp.util.darken
import hu.apk.kidsapp.util.lighten
import hu.apk.kidsapp.util.rememberVoicePlayer
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private val blockColors = listOf(
    KidRed,
    Color(0xFF4A90E2), // kék
    KidYellow,
    KidGreen,
    KidPurple,
    Color(0xFFFFA84D), // narancs
)

private val shapeOrder = Shape.entries.toList()

private class Hill(val f: Float, val color: Color, val amp: Float, val base: Float)

@Composable
fun BlocksGameScreen(onBack: () -> Unit) {
    val voice = rememberVoicePlayer()
    val scope = rememberCoroutineScope()

    var stacks by remember { mutableStateOf(mapOf<Int, List<Piece>>()) }
    var curColor by remember { mutableIntStateOf(0) }

    // húzás: új elem a palettáról, vagy meglévő elem áthelyezése
    var dragShape by remember { mutableStateOf<Shape?>(null) }
    var dragColor by remember { mutableIntStateOf(0) }
    var dragSourceColumn by remember { mutableIntStateOf(-1) }
    var dragPos by remember { mutableStateOf<Offset?>(null) }
    var panning by remember { mutableStateOf(false) }

    // kamera
    var camX by remember { mutableFloatStateOf(Float.NaN) }
    var camVel by remember { mutableFloatStateOf(0f) }
    var scrollDir by remember { mutableIntStateOf(0) }
    var dragArmed by remember { mutableStateOf(false) }

    val placeAnim = remember { Animatable(1f) }
    var animatingColumn by remember { mutableIntStateOf(-1) }

    val ghostPulse by rememberInfiniteTransition(label = "ghostPulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(550, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ghostBob",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val availW = with(density) { maxWidth.toPx() }
            val availH = with(density) { maxHeight.toPx() }
            val cell = with(density) { 76.dp.toPx() }
            val pitch = cell * 1.22f
            val depth = cell * 0.26f
            val paletteW = with(density) { 240.dp.toPx() }
            val viewW = availW - paletteW
            val worldW = WORLD_COLS * pitch
            val maxCam = max(0f, worldW - viewW)
            val groundY = availH * 0.80f

            if (camX.isNaN()) camX = maxCam / 2f

            fun clampCam(v: Float) = v.coerceIn(0f, maxCam)
            fun colLeftScreen(col: Int) = paletteW + col * pitch + (pitch - cell) / 2f - camX
            fun colAt(screenX: Float): Int = floor((screenX - paletteW + camX) / pitch).toInt()
            fun stackTop(col: Int): Float = groundY - (stacks[col]?.units() ?: 0f) * cell

            // paletta elrendezése
            val colourX = with(density) { 36.dp.toPx() }
            val colourY0 = with(density) { 122.dp.toPx() }
            val colourDy = with(density) { 62.dp.toPx() }
            val colourR = with(density) { 24.dp.toPx() }
            val shapeX = listOf(with(density) { 108.dp.toPx() }, with(density) { 184.dp.toPx() })
            val shapeY0 = with(density) { 132.dp.toPx() }
            val shapeDy = with(density) { 106.dp.toPx() }
            val iconBox = with(density) { 62.dp.toPx() }
            fun shapeCenter(i: Int) = Offset(shapeX[i % 2], shapeY0 + (i / 2) * shapeDy)

            // minitérkép
            val mapW = min(with(density) { 360.dp.toPx() }, viewW * 0.55f)
            val mapH = with(density) { 22.dp.toPx() }
            val mapLeft = paletteW + (viewW - mapW) / 2f
            val mapTop = availH - with(density) { 34.dp.toPx() }

            // kamera-lendület, nyilak, szélén automatikus görgetés
            LaunchedEffect(maxCam) {
                var last = 0L
                while (true) {
                    withFrameNanos { t ->
                        if (last != 0L) {
                            val dt = ((t - last) / 1_000_000_000f).coerceAtMost(0.05f)
                            var v = camX
                            if (!panning && dragShape == null) {
                                v += camVel * dt
                                camVel *= exp(-3.2f * dt)
                                if (abs(camVel) < 4f) camVel = 0f
                            }
                            if (scrollDir != 0) v += scrollDir * 1100f * dt
                            val dp = dragPos
                            if (dragShape == null) dragArmed = false
                            if (dragShape != null && dp != null && dp.x > paletteW + 170f) dragArmed = true
                            // a szélén automatikusan görget, de csak ha az elem már bent járt a pályán
                            // (különben a palettáról húzva a bal szélen elcsúszna a kamera)
                            if (dragShape != null && dp != null && dragArmed) {
                                if (dp.x > availW - 110f) v += 700f * dt
                                else if (dp.x < paletteW + 110f && dp.x > paletteW) v -= 700f * dt
                            }
                            camX = v.coerceIn(0f, maxCam)
                        }
                        last = t
                    }
                }
            }

            fun placeAt(col: Int, piece: Piece): Boolean {
                if (col !in 0 until WORLD_COLS) return false
                val current = stacks[col] ?: emptyList()
                if (current.units() + piece.shape.units > MAX_UNITS + 0.01f) {
                    voice.play(Voice.BLOCKS_FULL)
                    return false
                }
                stacks = stacks + (col to (current + piece))
                animatingColumn = col
                scope.launch {
                    placeAnim.snapTo(0f)
                    placeAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 180f))
                }
                return true
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(availW, availH) {
                        detectTapGestures(
                            onTap = { tap ->
                                // szín választása
                                for (i in blockColors.indices) {
                                    if ((Offset(colourX, colourY0 + i * colourDy) - tap).getDistance() < colourR * 1.5f) {
                                        curColor = i
                                        return@detectTapGestures
                                    }
                                }
                                // minitérkép: ugrás
                                if (tap.x in mapLeft..(mapLeft + mapW) && tap.y in (mapTop - 16f)..(mapTop + mapH + 16f)) {
                                    val f = ((tap.x - mapLeft) / mapW).coerceIn(0f, 1f)
                                    camX = clampCam(f * worldW - viewW / 2f)
                                    camVel = 0f
                                }
                            },
                        )
                    }
                    .pointerInput(availW, availH) {
                        detectDragGestures(
                            onDragStart = { start ->
                                camVel = 0f
                                // 1) forma a palettáról
                                val si = shapeOrder.indices.firstOrNull { (shapeCenter(it) - start).getDistance() < iconBox * 0.75f }
                                if (si != null) {
                                    dragShape = shapeOrder[si]
                                    dragColor = curColor
                                    dragSourceColumn = -1
                                    dragPos = start
                                    return@detectDragGestures
                                }
                                // 2) a legfelső elem egy oszlopból
                                if (start.x > paletteW) {
                                    val col = colAt(start.x)
                                    val st = stacks[col]
                                    if (!st.isNullOrEmpty()) {
                                        val top = stackTop(col)
                                        val h = st.last().shape.units * cell
                                        val left = colLeftScreen(col)
                                        if (start.x in (left - 10f)..(left + cell + depth + 10f) && start.y in (top - 10f)..(top + h + 10f)) {
                                            dragShape = st.last().shape
                                            dragColor = st.last().color
                                            dragSourceColumn = col
                                            dragPos = start
                                            return@detectDragGestures
                                        }
                                    }
                                    // 3) üres hely: a kamera mozgatása
                                    panning = true
                                }
                            },
                            onDrag = { change, drag ->
                                if (dragShape != null) {
                                    change.consume()
                                    dragPos = change.position
                                } else if (panning) {
                                    change.consume()
                                    camX = clampCam(camX - drag.x)
                                    camVel = -drag.x * 55f
                                }
                            },
                            onDragEnd = {
                                val shape = dragShape
                                val pos = dragPos
                                val source = dragSourceColumn
                                if (shape != null && pos != null) {
                                    val piece = Piece(shape, dragColor)
                                    if (pos.x < paletteW) {
                                        if (source >= 0) {
                                            val cur = stacks[source] ?: emptyList()
                                            if (cur.isNotEmpty()) stacks = stacks + (source to cur.dropLast(1))
                                        } else {
                                            voice.play(Voice.BLOCKS_MISS)
                                        }
                                    } else {
                                        val target = colAt(pos.x)
                                        if (target in 0 until WORLD_COLS) {
                                            if (source >= 0) {
                                                if (target != source) {
                                                    val cur = stacks[source] ?: emptyList()
                                                    // először levesszük, ha nem fér az új helyre, visszatesszük
                                                    stacks = stacks + (source to cur.dropLast(1))
                                                    if (!placeAt(target, piece)) stacks = stacks + (source to cur)
                                                }
                                            } else {
                                                placeAt(target, piece)
                                            }
                                        } else {
                                            voice.play(Voice.BLOCKS_MISS)
                                        }
                                    }
                                }
                                dragShape = null
                                dragSourceColumn = -1
                                dragPos = null
                                panning = false
                            },
                            onDragCancel = {
                                dragShape = null
                                dragSourceColumn = -1
                                dragPos = null
                                panning = false
                            },
                        )
                    },
            ) {
                drawScenery(availW, availH, groundY, camX)

                // talaj-oszlopjelzők (hová lehet építeni)
                for (c in 0..WORLD_COLS) {
                    val x = paletteW + c * pitch - camX
                    if (x < paletteW - 4f || x > availW + 4f) continue
                    drawLine(Color(0xFF6C8F4A).copy(alpha = 0.55f), Offset(x, groundY), Offset(x, groundY + 16f), strokeWidth = 3f)
                }
                // pálya szélei (zászlók)
                for (edge in listOf(0f, worldW)) {
                    val x = paletteW + edge - camX
                    if (x > paletteW - 60f && x < availW + 60f) drawFlag(x, groundY, cell)
                }

                // kiemelés: melyik oszlopra esne a húzott elem
                val dp0 = dragPos
                if (dragShape != null && dp0 != null && dp0.x > paletteW) {
                    val c = colAt(dp0.x)
                    if (c in 0 until WORLD_COLS) {
                        val left = paletteW + c * pitch - camX
                        drawRoundRect(
                            Color(0xFF4A90E2).copy(alpha = 0.16f),
                            topLeft = Offset(left, groundY - MAX_UNITS * cell - 10f),
                            size = Size(pitch, MAX_UNITS * cell + 10f),
                            cornerRadius = CornerRadius(14f),
                        )
                    }
                }

                // elemek
                for (col in 0 until WORLD_COLS) {
                    val st = stacks[col] ?: continue
                    val left = colLeftScreen(col)
                    if (left > availW + cell || left + cell + depth < paletteW - cell) continue
                    var below = 0f
                    st.forEachIndexed { level, piece ->
                        val isTop = level == st.lastIndex
                        if (dragSourceColumn == col && isTop) return@forEachIndexed
                        if (below == 0f) drawContactShadow(left + cell / 2f, groundY, cell)
                        val fall = if (isTop && col == animatingColumn) (1f - placeAnim.value) * cell * 2.2f else 0f
                        val top = groundY - (below + piece.shape.units) * cell - fall
                        drawPiece(piece.shape, Offset(left, top), cell, depth, blockColors[piece.color])
                        below += piece.shape.units
                    }
                }

                // minitérkép
                drawRoundRect(Color.White.copy(alpha = 0.78f), Offset(mapLeft - 8f, mapTop - 8f), Size(mapW + 16f, mapH + 16f), CornerRadius(18f))
                for (c in 0 until WORLD_COLS) {
                    val u = stacks[c]?.units() ?: 0f
                    if (u > 0f) {
                        val x = mapLeft + (c + 0.5f) / WORLD_COLS * mapW
                        drawLine(Color(0xFF8D9AB0), Offset(x, mapTop + mapH), Offset(x, mapTop + mapH - mapH * (u / MAX_UNITS)), strokeWidth = 4f)
                    }
                }
                val vx0 = mapLeft + camX / worldW * mapW
                val vx1 = mapLeft + (camX + viewW) / worldW * mapW
                drawRoundRect(
                    Color(0xFF4A90E2).copy(alpha = 0.30f),
                    Offset(vx0, mapTop - 4f), Size(vx1 - vx0, mapH + 8f), CornerRadius(10f),
                )
                drawRoundRect(
                    Color(0xFF2E6DB4), Offset(vx0, mapTop - 4f), Size(vx1 - vx0, mapH + 8f), CornerRadius(10f),
                    style = Stroke(width = 3f),
                )

                // paletta
                drawRect(Color.White, Offset.Zero, Size(paletteW, availH))
                drawLine(Color(0xFFE0E4EE), Offset(paletteW, 0f), Offset(paletteW, availH), strokeWidth = 3f)
                blockColors.forEachIndexed { i, c ->
                    val center = Offset(colourX, colourY0 + i * colourDy)
                    drawCircle(c, colourR, center)
                    if (i == curColor) {
                        drawCircle(Color(0xFF2A2A3B), colourR + 6f, center, style = Stroke(width = 5f))
                    } else {
                        drawCircle(Color.White, colourR, center, style = Stroke(width = 4f))
                    }
                }
                shapeOrder.forEachIndexed { i, shape ->
                    val ctr = shapeCenter(i)
                    drawRoundRect(
                        Color(0xFFF1F3F8),
                        Offset(ctr.x - iconBox * 0.62f, ctr.y - iconBox * 0.62f),
                        Size(iconBox * 1.24f, iconBox * 1.24f),
                        CornerRadius(22f),
                    )
                    val s = iconBox * 0.78f / max(1f, if (shape.units > 1f) shape.units * 0.62f else 1f)
                    val h = shape.units * s
                    drawPiece(shape, Offset(ctr.x - s / 2f - s * 0.08f, ctr.y - h / 2f + s * 0.08f), s, s * 0.22f, blockColors[curColor])
                }

                // húzott elem
                val gs = dragShape
                val gp = dragPos
                if (gs != null && gp != null) {
                    val h = gs.units * cell
                    val bob = Offset(0f, -6f + ghostPulse * 12f)
                    drawPiece(gs, gp + bob - Offset(cell / 2f, h / 2f), cell, depth, blockColors[dragColor], alpha = 0.8f)
                }
            }

            // nyilak: nyomva tartva görget
            ScrollArrow("◀", Modifier.offset { IntOffset((paletteW + with(density) { 16.dp.toPx() }).toInt(), (availH - with(density) { 112.dp.toPx() }).toInt()) }) { scrollDir = if (it) -1 else 0 }
            ScrollArrow("▶", Modifier.offset { IntOffset((availW - with(density) { 88.dp.toPx() }).toInt(), (availH - with(density) { 112.dp.toPx() }).toInt()) }) { scrollDir = if (it) 1 else 0 }
        }

        BlocksTopBar(onBack = onBack, onClear = { stacks = emptyMap() })
    }
}

@Composable
private fun ScrollArrow(label: String, modifier: Modifier, onHold: (Boolean) -> Unit) {
    Box(
        modifier = modifier
            .size(72.dp)
            .background(Color.White.copy(alpha = 0.85f), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    onHold(true)
                    try {
                        tryAwaitRelease()
                    } finally {
                        onHold(false)
                    }
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 30.sp, color = KidText)
    }
}

@Composable
private fun BlocksTopBar(onBack: () -> Unit, onClear: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        BackButton(onClick = onBack)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Text(
                "Kockaépítő",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = KidText,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
            FilledIconButton(
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = KidRed),
                onClick = onClear,
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Minden törlése", tint = Color.White)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Rajzolás
// ---------------------------------------------------------------------------------------------

private fun DrawScope.drawScenery(w: Float, h: Float, groundY: Float, camX: Float) {
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFF9ED8FF), Color(0xFFEAF6FF)), startY = 0f, endY = groundY),
        Offset.Zero,
        Size(w, groundY),
    )
    // nap
    drawCircle(Color(0xFFFFE066), 54f, Offset(w * 0.86f, groundY * 0.2f))
    drawCircle(Color(0xFFFFF2A8).copy(alpha = 0.5f), 86f, Offset(w * 0.86f, groundY * 0.2f))
    // felhők (lassabban mozognak, mint a talaj)
    for (i in 0 until 8) {
        val span = 2200f
        val x = ((i * 520f - camX * 0.25f) % span + span) % span - 200f
        val y = groundY * (0.12f + 0.08f * ((i * 5) % 4))
        val c = Color.White.copy(alpha = 0.9f)
        drawCircle(c, 34f, Offset(x, y))
        drawCircle(c, 44f, Offset(x + 40f, y - 10f))
        drawCircle(c, 32f, Offset(x + 84f, y))
        drawRoundRect(c, Offset(x - 30f, y), Size(150f, 28f), CornerRadius(14f))
    }
    // dombok (két réteg, parallaxis)
    for (hill in listOf(Hill(0.35f, Color(0xFFB6D9A0), 46f, 62f), Hill(0.6f, Color(0xFF8CC271), 34f, 28f))) {
        val p = Path()
        p.moveTo(0f, groundY)
        var x = 0f
        while (x <= w + 20f) {
            val y = groundY - hill.base - hill.amp * (0.5f + 0.5f * sin((x + camX * hill.f) / 230f)) -
                (hill.amp * 0.35f) * sin((x + camX * hill.f) / 97f)
            p.lineTo(x, y)
            x += 20f
        }
        p.lineTo(w, groundY)
        p.close()
        drawPath(p, hill.color)
    }
    // talaj
    drawRect(Color(0xFF7CB342), Offset(0f, groundY), Size(w, h - groundY))
    drawRect(Color(0xFF9CCC65), Offset(0f, groundY), Size(w, 18f))
    drawRect(Color(0xFF8D6E63), Offset(0f, groundY + 56f), Size(w, h - groundY - 56f))
}

private fun DrawScope.drawFlag(x: Float, groundY: Float, cell: Float) {
    drawLine(Color(0xFF6D4C41), Offset(x, groundY), Offset(x, groundY - cell * 1.4f), strokeWidth = 8f)
    val p = Path().apply {
        moveTo(x, groundY - cell * 1.4f)
        lineTo(x + cell * 0.7f, groundY - cell * 1.2f)
        lineTo(x, groundY - cell * 1.0f)
        close()
    }
    drawPath(p, Color(0xFFE53935))
}

private fun DrawScope.drawContactShadow(centerX: Float, groundY: Float, cell: Float) {
    drawOval(
        Color.Black.copy(alpha = 0.22f),
        Offset(centerX - cell * 0.58f, groundY - 6f),
        Size(cell * 1.16f, 16f),
    )
}

private fun frontPath(shape: Shape, l: Float, t: Float, s: Float): Path {
    val h = shape.units * s
    return when (shape) {
        Shape.ROOF -> Path().apply {
            moveTo(l, t + s)
            lineTo(l + s, t + s)
            lineTo(l + s * 0.5f, t)
            close()
        }
        Shape.STAR -> Path().apply {
            val c = Offset(l + s / 2f, t + s / 2f + s * 0.03f)
            for (i in 0 until 10) {
                val r = if (i % 2 == 0) s * 0.52f else s * 0.23f
                val a = -PI / 2 + i * PI / 5
                val x = c.x + r * cos(a).toFloat()
                val y = c.y + r * sin(a).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        Shape.ARCH -> {
            val body = Path().apply { addRoundRect(RoundRect(l, t, l + s, t + s, CornerRadius(s * 0.09f))) }
            val hole = Path().apply { addOval(Rect(l + s * 0.2f, t + s * 0.36f, l + s * 0.8f, t + s * 1.36f)) }
            Path().apply { op(body, hole, PathOperation.Difference) }
        }
        else -> Path().apply { addRoundRect(RoundRect(l, t, l + s, t + h, CornerRadius(s * 0.09f))) }
    }
}

/** Egy építőelem oldalnézetben, kis térhatású kihúzással. `frontTopLeft` az elülső lap bal felső sarka. */
private fun DrawScope.drawPiece(shape: Shape, frontTopLeft: Offset, s: Float, depth: Float, color: Color, alpha: Float = 1f) {
    val l = frontTopLeft.x
    val t = frontTopLeft.y
    val h = shape.units * s

    when (shape) {
        Shape.BALL -> {
            val c = Offset(l + s / 2f, t + s / 2f)
            drawCircle(
                Brush.radialGradient(
                    listOf(color.lighten(0.45f).copy(alpha = alpha), color.copy(alpha = alpha), color.darken(0.30f).copy(alpha = alpha)),
                    center = Offset(c.x - s * 0.15f, c.y - s * 0.18f),
                    radius = s * 0.62f,
                ),
                radius = s / 2f,
                center = c,
            )
            drawCircle(Color.Black.copy(alpha = 0.10f * alpha), s / 2f, c, style = Stroke(width = 2f))
            drawOval(Color.White.copy(alpha = 0.40f * alpha), Offset(c.x - s * 0.28f, c.y - s * 0.34f), Size(s * 0.24f, s * 0.16f))
        }
        Shape.CYLINDER -> {
            val ry = s * 0.17f
            val body = Path().apply {
                moveTo(l, t + ry)
                lineTo(l, t + h - ry)
                arcTo(Rect(l, t + h - 2f * ry, l + s, t + h), 180f, -180f, false)
                lineTo(l + s, t + ry)
                close()
            }
            drawPath(
                body,
                Brush.horizontalGradient(
                    listOf(color.darken(0.18f).copy(alpha = alpha), color.lighten(0.18f).copy(alpha = alpha), color.darken(0.30f).copy(alpha = alpha)),
                    startX = l,
                    endX = l + s,
                ),
            )
            drawPath(body, Color.Black.copy(alpha = 0.12f * alpha), style = Stroke(width = 2f))
            drawOval(color.lighten(0.40f).copy(alpha = alpha), Offset(l, t), Size(s, 2f * ry))
            drawOval(Color.Black.copy(alpha = 0.12f * alpha), Offset(l, t), Size(s, 2f * ry), style = Stroke(width = 2f))
        }
        else -> {
            val path = frontPath(shape, l, t, s)
            val n = 7
            for (i in n downTo 1) {
                val k = i / n.toFloat()
                translate(depth * k, -depth * k) {
                    drawPath(path, color.darken(0.14f + 0.22f * k).copy(alpha = alpha))
                }
            }
            drawPath(
                path,
                Brush.linearGradient(
                    listOf(color.lighten(0.14f).copy(alpha = alpha), color.copy(alpha = alpha), color.darken(0.08f).copy(alpha = alpha)),
                    start = Offset(l, t),
                    end = Offset(l + s, t + h),
                ),
            )
            drawPath(path, Color.Black.copy(alpha = 0.12f * alpha), style = Stroke(width = 2f))
            if (shape != Shape.ROOF && shape != Shape.STAR) {
                drawOval(
                    Color.White.copy(alpha = 0.30f * alpha),
                    Offset(l + s * 0.14f, t + s * 0.12f),
                    Size(s * 0.30f, s * 0.20f),
                )
            }
        }
    }
}
