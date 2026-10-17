package hu.apk.kidsapp.ui.screens.writing

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.onSizeChanged
import hu.apk.kidsapp.ui.components.BackButton
import hu.apk.kidsapp.ui.theme.KidBackground
import hu.apk.kidsapp.ui.theme.KidText
import hu.apk.kidsapp.util.SoundFx
import hu.apk.kidsapp.util.Step
import hu.apk.kidsapp.util.rememberClipPlayer
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Egy leírandó jel (szám vagy betű): a szöveg, a színe és a hangfájl, ami kimondja. */
class WriteItem(val text: String, val color: Color, val sayClip: String)

private enum class Stage { LARGE1, LARGE2, FIVE, DONE }

private const val BOXES = 5

/**
 * Íráskészség-gyakorló: balra a minta, jobbra nagyban a hely, ahová az ujjal rá kell húzni a halvány
 * alakra. Nagyon megbocsátó: a vonalvezetés, a sorrend és a pontosság nem számít, csak az, hogy az
 * alak nagy része be legyen húzva. Menete: 1) nagyban egyszer, 2) nagyban még egyszer,
 * 3) kicsiben ötször egymás után, aztán jön a következő.
 */
@Composable
fun WritingPractice(
    items: List<WriteItem>,
    startIndex: Int,
    onIndexChange: (Int) -> Unit,
    onExit: () -> Unit,
    headerExtra: @Composable RowScope.() -> Unit = {},
) {
    val player = rememberClipPlayer()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sfx = remember { SoundFx(context.applicationContext) }
    DisposableEffect(sfx) { onDispose { sfx.release() } }

    var index by remember { mutableIntStateOf(startIndex.coerceIn(0, items.lastIndex)) }
    var stage by remember { mutableStateOf(Stage.LARGE1) }
    var box by remember { mutableIntStateOf(0) }
    var round by remember { mutableIntStateOf(0) } // a rajzolófelület újraindításához
    var locked by remember { mutableStateOf(false) } // dicséret közben nincs bevitel
    val confetti = remember { Animatable(1f) }
    val item = items[index.coerceIn(0, items.lastIndex)]

    fun burst() {
        scope.launch {
            confetti.snapTo(0f)
            confetti.animateTo(1f, tween(1500))
        }
    }

    fun praise() = Step.Clip("voice_praise_${Random.nextInt(1, 6)}")

    // Új jel (vagy átváltott nagy/kis betű): elölről kezdjük, és kimondjuk, mit kell írni.
    LaunchedEffect(index, item.text) {
        stage = Stage.LARGE1
        box = 0
        round++
        locked = false
        onIndexChange(index)
        player.play(Step.Clip(item.sayClip), Step.Pause(300), Step.Clip("voice_write_it"))
    }

    fun complete() {
        if (locked) return
        locked = true
        when (stage) {
            Stage.LARGE1 -> {
                burst()
                player.play(
                    praise(), Step.Pause(250),
                    Step.Mark { stage = Stage.LARGE2; round++; locked = false },
                    Step.Clip("voice_write_again"),
                )
            }
            Stage.LARGE2 -> {
                burst()
                player.play(
                    praise(), Step.Pause(250),
                    Step.Mark { stage = Stage.FIVE; box = 0; round++; locked = false },
                    Step.Clip("voice_write_five"),
                )
            }
            Stage.FIVE -> {
                if (box < BOXES - 1) {
                    sfx.play("ding")
                    player.play(Step.Pause(550), Step.Mark { box++; locked = false })
                } else {
                    stage = Stage.DONE
                    burst()
                    player.play(
                        praise(), Step.Pause(200), Step.Clip("voice_write_next"), Step.Pause(500),
                        Step.Mark {
                            if (index < items.lastIndex) index++ else onExit()
                        },
                    )
                }
            }
            Stage.DONE -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(KidBackground)) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                BackButton(onClick = onExit)
                Spacer(Modifier.weight(1f))
                Text(
                    "Írd le!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = KidText,
                )
                Spacer(Modifier.width(16.dp))
                ProgressDots(stage, box, item.color)
                Spacer(Modifier.weight(1f))
                headerExtra()
                Spacer(Modifier.width(10.dp))
                RoundChip("◀") { if (index > 0) index-- }
                Spacer(Modifier.width(8.dp))
                RoundChip("▶") { if (index < items.lastIndex) index++ }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                // A minta
                Column(
                    modifier = Modifier
                        .weight(if (stage == Stage.FIVE) 0.7f else 0.9f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White)
                        .clickable { player.play(Step.Clip(item.sayClip)) }
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Minta", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = KidText.copy(alpha = 0.6f))
                    GlyphCanvas(item.text, item.color, solid = true, modifier = Modifier.weight(1f).fillMaxWidth())
                }
                // A hely, ahová írni kell
                Box(modifier = Modifier.weight(1.6f).fillMaxHeight()) {
                    if (stage == Stage.FIVE || (stage == Stage.DONE)) {
                        FiveBoxes(item, box, round, locked, stage == Stage.DONE) { complete() }
                    } else {
                        key(item.text, stage, round) {
                            TraceBox(
                                text = item.text,
                                color = item.color,
                                active = !locked,
                                onComplete = { complete() },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
        Confetti(confetti.value)
    }
}

@Composable
private fun FiveBoxes(item: WriteItem, box: Int, round: Int, locked: Boolean, allDone: Boolean, onComplete: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        listOf(0..2, 3..4).forEach { rowRange ->
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
            ) {
                for (i in rowRange) {
                    key(item.text, i, round) {
                        TraceBox(
                            text = item.text,
                            color = item.color,
                            active = !allDone && !locked && i == box,
                            pending = i > box && !allDone,
                            onComplete = onComplete,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
                if (rowRange.last == 4) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProgressDots(stage: Stage, box: Int, color: Color) {
    val done = when (stage) {
        Stage.LARGE1 -> 0
        Stage.LARGE2 -> 1
        Stage.FIVE -> 2 + box
        Stage.DONE -> 2 + BOXES
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until 2 + BOXES) {
            val big = i < 2
            Box(
                modifier = Modifier
                    .size(if (big) 26.dp else 18.dp)
                    .background(if (i < done) color else Color.White, CircleShape)
                    .border(3.dp, color.copy(alpha = 0.6f), CircleShape),
            )
            if (i == 1) Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun RoundChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(Color.White, CircleShape)
            .border(3.dp, Color(0xFFD5DAE8), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 22.sp, color = KidText)
    }
}

// ---------------------------------------------------------------------------------------------
// Betű-rajzolás és az "alak" maszkja

/** Az alak vastagítása (a betűmérethez képest), hogy könnyebb legyen követni. */
private const val FAT = 0.035f

private class GlyphLayout(val textSize: Float, val x: Float, val y: Float)

private fun glyphPaint(color: Int, style: Paint.Style, strokeWidth: Float = 0f) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    this.color = color
    this.style = style
    this.strokeWidth = strokeWidth
    strokeJoin = Paint.Join.ROUND
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
}

/** A szöveget a dobozba illeszti: a látható alak a doboz ~80%-át kitölti, középen. */
private fun layoutGlyph(text: String, w: Float, h: Float): GlyphLayout {
    val p = glyphPaint(0, Paint.Style.FILL)
    p.textSize = 100f
    val b = Rect()
    p.getTextBounds(text, 0, text.length, b)
    val bw = max(1, b.width()).toFloat()
    val bh = max(1, b.height()).toFloat()
    val k = min(w * 0.78f / bw, h * 0.78f / bh)
    val size = 100f * k
    val x = (w - bw * k) / 2f - b.left * k
    val y = h / 2f - (b.top + b.bottom) / 2f * k
    return GlyphLayout(size, x, y)
}

@Composable
private fun GlyphCanvas(text: String, color: Color, solid: Boolean, modifier: Modifier) {
    Canvas(modifier = modifier) { drawGlyph(text, color, solid) }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGlyph(text: String, color: Color, solid: Boolean) {
    val l = layoutGlyph(text, size.width, size.height)
    drawIntoCanvas { c ->
        if (solid) {
            val fill = glyphPaint(color.toArgb(), Paint.Style.FILL_AND_STROKE).apply { textSize = l.textSize; strokeWidth = l.textSize * FAT }
            c.nativeCanvas.drawText(text, l.x, l.y, fill)
        } else {
            // halvány, vastag alak: "ezen kell végigmenni" (körvonal nincs, mert az átfedő
            // kontúrú betűknél (A, a, ...) belső vonalakat rajzolna)
            val fill = glyphPaint(color.copy(alpha = 0.34f).toArgb(), Paint.Style.FILL_AND_STROKE).apply { textSize = l.textSize; strokeWidth = l.textSize * FAT }
            c.nativeCanvas.drawText(text, l.x, l.y, fill)
        }
    }
}

/** Az alak pontjai (képernyő-pixelben), amiket be kell húzni. Kisebb bitmapről mintavételezve. */
private fun buildMask(text: String, w: Int, h: Int): FloatArray {
    if (w < 8 || h < 8) return FloatArray(0)
    val scale = 4
    val bw = max(8, w / scale)
    val bh = max(8, h / scale)
    val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val l = layoutGlyph(text, w.toFloat(), h.toFloat())
    canvas.scale(bw / w.toFloat(), bh / h.toFloat())
    val p = glyphPaint(0xFF000000.toInt(), Paint.Style.FILL_AND_STROKE).apply { textSize = l.textSize; strokeWidth = l.textSize * FAT }
    canvas.drawText(text, l.x, l.y, p)
    val px = IntArray(bw * bh)
    bmp.getPixels(px, 0, bw, 0, 0, bw, bh)
    bmp.recycle()
    val out = ArrayList<Float>()
    var y = 0
    while (y < bh) {
        var x = 0
        while (x < bw) {
            if ((px[y * bw + x] ushr 24) > 128) {
                out.add((x + 0.5f) * w / bw)
                out.add((y + 0.5f) * h / bh)
            }
            x += 2
        }
        y += 2
    }
    return out.toFloatArray()
}

// ---------------------------------------------------------------------------------------------
// A rajzolófelület

private const val COVER_NEEDED = 0.85f // az alak ennyi részét kell betakarni
private const val ON_SHAPE_MIN = 0.40f // a húzott pontok ennyi része legyen az alak közelében

@Composable
private fun TraceBox(
    text: String,
    color: Color,
    active: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    pending: Boolean = false,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val ink = remember { mutableStateListOf<Offset>() } // Offset.Unspecified = vonalszakadás
    var finished by remember { mutableStateOf(false) }
    val mask = remember(text, size) { buildMask(text, size.width, size.height) }
    val covered = remember(mask) { BooleanArray(mask.size / 2) }
    val stats = remember(mask) { IntArray(3) } // 0: betakart, 1: összes pont, 2: az alak közelében
    val inkWidth = min(size.width, size.height) * 0.085f
    val reach = min(size.width, size.height) * 0.10f

    fun mark(q: Offset) {
        val r2 = reach * reach
        val far2 = (reach * 2f) * (reach * 2f)
        var near = false
        var i = 0
        while (i < covered.size) {
            val dx = mask[2 * i] - q.x
            val dy = mask[2 * i + 1] - q.y
            val d2 = dx * dx + dy * dy
            if (d2 <= r2) {
                if (!covered[i]) {
                    covered[i] = true
                    stats[0]++
                }
                near = true
            } else if (!near && d2 <= far2) near = true
            i++
        }
        stats[1]++
        if (near) stats[2]++
    }

    fun addSegment(from: Offset?, to: Offset) {
        if (from == null) {
            mark(to)
            return
        }
        val d = (to - from).getDistance()
        val steps = max(1, (d / (reach * 0.5f)).toInt() + 1)
        for (s in 1..steps) mark(from + (to - from) * (s / steps.toFloat()))
    }

    fun check(): Boolean {
        if (finished || covered.isEmpty()) return false
        val cov = stats[0] / covered.size.toFloat()
        val ratio = if (stats[1] == 0) 0f else stats[2] / stats[1].toFloat()
        return cov >= COVER_NEEDED && ratio >= ON_SHAPE_MIN
    }

    fun clearInk() {
        ink.clear()
        covered.fill(false)
        stats.fill(0)
    }

    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (finished) Color(0xFFE8F5E9) else Color.White)
            .border(
                if (active) 6.dp else 3.dp,
                when {
                    finished -> Color(0xFF43A047)
                    active -> color.copy(alpha = 0.7f)
                    else -> Color(0xFFD5DAE8)
                },
                shape,
            )
            .onSizeChanged { size = it }
            .pointerInput(active, finished, mask) {
                if (!active || finished) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var last: Offset? = down.position
                    ink.add(Offset.Unspecified)
                    ink.add(down.position)
                    addSegment(null, down.position)
                    down.consume()
                    var done = false
                    while (!done) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!ch.pressed) {
                            done = true
                        } else {
                            ink.add(ch.position)
                            addSegment(last, ch.position)
                            last = ch.position
                        }
                        ch.consume()
                        if (check()) {
                            finished = true
                            onComplete()
                            break
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawGlyph(text, color, solid = false)
            // a húzott vonal
            val path = Path()
            var open = false
            for (p in ink) {
                if (p == Offset.Unspecified) {
                    open = false
                } else if (!open) {
                    path.moveTo(p.x, p.y)
                    path.lineTo(p.x + 0.1f, p.y) // egy koppintás is pötty
                    open = true
                } else {
                    path.lineTo(p.x, p.y)
                }
            }
            drawPath(
                path,
                if (finished) Color(0xFF43A047) else color,
                style = Stroke(width = inkWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            if (pending) drawRect(Color.White.copy(alpha = 0.45f))
        }
        if (active && !finished && ink.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(46.dp)
                    .background(Color(0xFFF1F3F8), CircleShape)
                    .clickable { clearInk() },
                contentAlignment = Alignment.Center,
            ) { Text("🔄", fontSize = 20.sp) }
        }
    }
}

@Composable
private fun Confetti(progress: Float) {
    if (progress >= 1f) return
    val colors = listOf(Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFF9A825), Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFF8E44AD), Color(0xFFEC407A))
    Canvas(modifier = Modifier.fillMaxSize()) {
        val c = Offset(size.width * 0.6f, size.height * 0.5f)
        val ease = 1f - (1f - progress) * (1f - progress)
        for (i in 0 until 30) {
            val a = i * (2.0 * Math.PI / 30)
            val dist = ease * size.minDimension * (0.35f + 0.28f * ((i * 7) % 5) / 4f)
            val p = Offset(c.x + dist * cos(a).toFloat(), c.y + dist * sin(a).toFloat() + progress * progress * 110f)
            drawCircle(colors[i % colors.size].copy(alpha = (1f - progress).coerceIn(0f, 1f)), radius = 10f + (i % 3) * 5f, center = p)
        }
    }
}
