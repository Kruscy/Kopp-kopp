package hu.apk.kidsapp.ui.screens.cat

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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.apk.kidsapp.ui.components.BackButton
import hu.apk.kidsapp.ui.theme.KidBackground
import hu.apk.kidsapp.ui.theme.KidBlue
import hu.apk.kidsapp.ui.theme.KidBlueDark
import hu.apk.kidsapp.ui.theme.KidText
import hu.apk.kidsapp.ui.theme.KidYellow
import hu.apk.kidsapp.util.darken
import hu.apk.kidsapp.util.lighten
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private enum class Category(val emoji: String, val label: String) {
    COLOR("🎨", "Szín"),
    TYPE("✨", "Szőr"),
    PAW("🐾", "Mancs"),
    EYE("👀", "Szem"),
    EXTRA("🎀", "Extra"),
}

@Composable
fun CatGameScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var look by remember { mutableStateOf(CatLook()) }
    var category by remember { mutableStateOf(Category.COLOR) }

    // ---- Animációk ----
    val infinite = rememberInfiniteTransition(label = "cat")
    val breathe by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    val tail by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "tail",
    )
    val blink = remember { Animatable(0f) }
    val ear = remember { Animatable(0f) }
    val pop = remember { Animatable(0f) }
    val hop = remember { Animatable(0f) }
    val tilt = remember { Animatable(0f) }
    val heartT = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2200, 5200))
            blink.animateTo(1f, tween(90))
            blink.animateTo(0f, tween(130))
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(3500, 8000))
            repeat(2) {
                ear.animateTo(1f, tween(80))
                ear.animateTo(0f, tween(110))
            }
        }
    }

    fun poke() {
        scope.launch {
            pop.snapTo(1f)
            pop.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 260f))
        }
    }

    fun tapCat() {
        scope.launch {
            hop.snapTo(0f)
            hop.animateTo(1f, tween(150, easing = FastOutSlowInEasing))
            hop.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 300f))
        }
        scope.launch {
            tilt.animateTo(9f, tween(150))
            tilt.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 200f))
        }
        scope.launch {
            heartT.snapTo(0f)
            heartT.animateTo(1f, tween(1100))
        }
    }

    fun choose(newLook: CatLook) {
        look = newLook
        poke()
    }

    Box(modifier = Modifier.fillMaxSize().background(KidBackground)) {
        Row(modifier = Modifier.fillMaxSize()) {
            // ---- Színpad ----
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) { detectTapGestures(onTap = { tapCat() }) },
                ) {
                    val topReserve = 104.dp.toPx()
                    val gy = size.height * 0.85f
                    val s = minOf((gy - topReserve) / 1.08f, size.width / 1.3f)
                    val cx = size.width / 2f - s * 0.04f
                    drawRoom(gy, s, cx)
                    drawCat(
                        look, cx, gy, s,
                        CatAnim(
                            breathe = breathe,
                            tail = tail,
                            blink = blink.value,
                            ear = ear.value,
                            tilt = tilt.value,
                            hop = hop.value,
                            pop = pop.value,
                        ),
                    )
                    // Szívecskék
                    val ht = heartT.value
                    if (ht < 1f) {
                        for (i in 0 until 4) {
                            val dx = (i - 1.5f) * s * 0.16f
                            val rise = ht * s * (0.22f + 0.05f * i)
                            val c = Offset(cx + dx, gy - s * 1.0f - rise)
                            drawHeart(c, s * (0.06f + 0.012f * (i % 2)), Color(0xFFFF6B8B).copy(alpha = (1f - ht).coerceIn(0f, 1f)))
                        }
                    }
                }
                Text(
                    "Állítsd össze a cicád!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = KidText,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 30.dp),
                )
            }

            // ---- Választó panel ----
            Surface(
                modifier = Modifier.width(344.dp).fillMaxHeight(),
                shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
                color = Color.White,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    ) {
                        when (category) {
                            Category.COLOR -> items(furColors.indices.toList()) { i ->
                                OptionCell(look.fur == i, furColors[i].label, { choose(look.copy(fur = i)) }) {
                                    drawMiniCatHead(Offset(size.width / 2f, size.height * 0.58f), size.minDimension * 0.32f, furColors[i].color)
                                }
                            }
                            Category.TYPE -> items(FurType.entries) { t ->
                                OptionCell(look.type == t, t.label, { choose(look.copy(type = t)) }) {
                                    drawFurSwatch(t, furColors[look.fur].color)
                                }
                            }
                            Category.PAW -> items(pawColors.indices.toList()) { i ->
                                OptionCell(look.paw == i, pawColors[i]?.label ?: "ugyanolyan", { choose(look.copy(paw = i)) }) {
                                    drawPawSwatch(furColors[look.fur].color, pawColors[i]?.color)
                                }
                            }
                            Category.EYE -> items(eyeColors.indices.toList()) { i ->
                                OptionCell(look.eye == i, eyeColors[i].label, { choose(look.copy(eye = i)) }) {
                                    drawEyeSwatch(eyeColors[i].color)
                                }
                            }
                            Category.EXTRA -> items(Extra.entries) { e ->
                                OptionCell(look.extra == e, e.label, { choose(look.copy(extra = e)) }, emoji = e.emoji)
                            }
                        }
                    }

                    // Kategóriák
                    Column(
                        modifier = Modifier
                            .width(88.dp)
                            .fillMaxHeight()
                            .background(Color(0xFFF4F6FB))
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Category.entries.forEach { c ->
                            CategoryButton(c.emoji, c.label, category == c) { category = c }
                        }
                        CategoryButton("🎲", "Véletlen", false) {
                            choose(randomLook())
                            tapCat()
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.padding(24.dp)) {
            BackButton(onClick = onBack)
        }
    }
}

private fun randomLook() = CatLook(
    fur = Random.nextInt(furColors.size),
    type = FurType.entries.random(),
    paw = Random.nextInt(pawColors.size),
    eye = Random.nextInt(eyeColors.size),
    extra = Extra.entries.random(),
)

@Composable
private fun CategoryButton(emoji: String, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(if (selected) KidBlue else Color.White, CircleShape)
                .border(if (selected) 0.dp else 2.dp, Color(0xFFD5DAE8), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 26.sp)
        }
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = KidText,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun OptionCell(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    emoji: String? = null,
    preview: (DrawScope.() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) KidYellow.copy(alpha = 0.35f) else Color(0xFFF1F3F8))
            .border(if (selected) 4.dp else 0.dp, if (selected) KidBlueDark else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(74.dp), contentAlignment = Alignment.Center) {
            if (emoji != null) {
                Text(emoji, fontSize = 38.sp)
            } else if (preview != null) {
                Canvas(modifier = Modifier.fillMaxSize()) { preview() }
            }
        }
        Text(label, fontSize = 12.sp, color = KidText, textAlign = TextAlign.Center)
    }
}

// ---- Előnézetek ----

private fun DrawScope.drawFurSwatch(type: FurType, fur: Color) {
    val r = Rect(Offset(size.width * 0.06f, size.height * 0.06f), Size(size.width * 0.88f, size.height * 0.88f))
    val path = Path().apply { addRoundRect(RoundRect(r, CornerRadius(size.minDimension * 0.28f))) }
    drawPath(path, Brush.horizontalGradient(listOf(fur.lighten(0.10f), fur, fur.darken(0.12f)), startX = r.left, endX = r.right))
    drawFurPattern(type, r, path, fur, size.minDimension * 1.5f)
    drawPath(path, fur.darken(0.5f).copy(alpha = 0.5f), style = Stroke(width = 3f))
}

private fun DrawScope.drawPawSwatch(fur: Color, paw: Color?) {
    val pawCol = paw ?: fur
    val w = size.width
    val h = size.height
    val leg = Path().apply {
        addRoundRect(RoundRect(Rect(w * 0.30f, h * 0.02f, w * 0.70f, h * 0.78f), CornerRadius(w * 0.2f)))
    }
    drawPath(leg, Brush.horizontalGradient(listOf(fur.lighten(0.10f), fur, fur.darken(0.12f)), startX = w * 0.3f, endX = w * 0.7f))
    drawPath(leg, fur.darken(0.5f).copy(alpha = 0.5f), style = Stroke(width = 3f))
    val pawRect = Rect(w * 0.20f, h * 0.62f, w * 0.80f, h * 0.94f)
    drawOval(pawCol, topLeft = pawRect.topLeft, size = pawRect.size)
    drawOval(fur.darken(0.5f).copy(alpha = 0.5f), topLeft = pawRect.topLeft, size = pawRect.size, style = Stroke(width = 3f))
    for (k in -1..1) {
        drawLine(
            pawCol.darken(0.35f).copy(alpha = 0.55f),
            Offset(w * 0.5f + k * w * 0.13f, h * 0.70f),
            Offset(w * 0.5f + k * w * 0.13f, h * 0.86f),
            strokeWidth = 4f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawEyeSwatch(iris: Color) {
    val c = Offset(size.width / 2f, size.height / 2f)
    val rx = size.width * 0.34f
    val ry = size.height * 0.40f
    drawOval(Color.White, topLeft = Offset(c.x - rx, c.y - ry), size = Size(rx * 2f, ry * 2f))
    drawCircle(iris, radius = rx * 0.9f, center = c)
    drawCircle(iris.darken(0.35f), radius = rx * 0.9f, center = c, style = Stroke(width = 3f))
    drawOval(Color(0xFF1F1F2B), topLeft = Offset(c.x - rx * 0.34f, c.y - ry * 0.55f), size = Size(rx * 0.68f, ry * 1.1f))
    drawCircle(Color.White, radius = rx * 0.2f, center = Offset(c.x - rx * 0.28f, c.y - ry * 0.35f))
    drawOval(Color(0xFF8E96A8), topLeft = Offset(c.x - rx, c.y - ry), size = Size(rx * 2f, ry * 2f), style = Stroke(width = 3f))
}

// ---- Háttér ----

private fun DrawScope.drawRoom(gy: Float, s: Float, cx: Float) {
    val floorTop = gy - s * 0.17f
    // fal
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFFE9E4FF), Color(0xFFFFEFE3)), startY = 0f, endY = floorTop),
        topLeft = Offset.Zero,
        size = Size(size.width, floorTop),
    )
    // lábazat
    drawRect(Color(0xFFFFFFFF).copy(alpha = 0.7f), topLeft = Offset(0f, floorTop - s * 0.04f), size = Size(size.width, s * 0.04f))
    // padló
    drawRect(Color(0xFFE2B98A), topLeft = Offset(0f, floorTop), size = Size(size.width, size.height - floorTop))
    var y = floorTop + s * 0.09f
    while (y < size.height) {
        drawLine(Color(0xFFCFA070), Offset(0f, y), Offset(size.width, y), strokeWidth = 2f)
        y += s * 0.09f
    }
    // ablak
    val wc = Offset(size.width * 0.80f, floorTop * 0.42f)
    val ww = s * 0.30f
    val wh = s * 0.38f
    drawRoundRect(Color(0xFFFFFFFF), topLeft = Offset(wc.x - ww / 2f - 8f, wc.y - wh / 2f - 8f), size = Size(ww + 16f, wh + 16f), cornerRadius = CornerRadius(14f))
    drawRoundRect(
        Brush.verticalGradient(listOf(Color(0xFF9ED8FF), Color(0xFFE3F5FF)), startY = wc.y - wh / 2f, endY = wc.y + wh / 2f),
        topLeft = Offset(wc.x - ww / 2f, wc.y - wh / 2f),
        size = Size(ww, wh),
        cornerRadius = CornerRadius(8f),
    )
    drawCircle(Color.White.copy(alpha = 0.85f), radius = ww * 0.12f, center = Offset(wc.x - ww * 0.12f, wc.y - wh * 0.14f))
    drawCircle(Color.White.copy(alpha = 0.85f), radius = ww * 0.09f, center = Offset(wc.x + ww * 0.02f, wc.y - wh * 0.10f))
    drawLine(Color.White, Offset(wc.x, wc.y - wh / 2f), Offset(wc.x, wc.y + wh / 2f), strokeWidth = 6f)
    drawLine(Color.White, Offset(wc.x - ww / 2f, wc.y), Offset(wc.x + ww / 2f, wc.y), strokeWidth = 6f)
    // szőnyeg
    val rugRx = s * 0.66f
    val rugRy = s * 0.13f
    drawOval(Color(0xFFFF9EC1), topLeft = Offset(cx - rugRx, gy - rugRy * 0.55f), size = Size(rugRx * 2f, rugRy * 2f))
    drawOval(Color(0xFFFFC6DC), topLeft = Offset(cx - rugRx * 0.82f, gy - rugRy * 0.35f), size = Size(rugRx * 1.64f, rugRy * 1.6f), style = Stroke(width = 5f))
    drawOval(Color(0xFFE5779F), topLeft = Offset(cx - rugRx, gy - rugRy * 0.55f), size = Size(rugRx * 2f, rugRy * 2f), style = Stroke(width = 3f))
}

private fun DrawScope.drawHeart(c: Offset, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y + r)
        cubicTo(c.x - r * 1.6f, c.y - r * 0.2f, c.x - r * 0.7f, c.y - r * 1.4f, c.x, c.y - r * 0.5f)
        cubicTo(c.x + r * 0.7f, c.y - r * 1.4f, c.x + r * 1.6f, c.y - r * 0.2f, c.x, c.y + r)
        close()
    }
    drawPath(p, color)
}
