package hu.apk.kidsapp.ui.screens.cake

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hu.apk.kidsapp.util.Voice
import hu.apk.kidsapp.util.rememberVoicePlayer
import hu.apk.kidsapp.game.cake.CakeAction
import hu.apk.kidsapp.game.cake.Decoration
import hu.apk.kidsapp.game.cake.DecorationType
import hu.apk.kidsapp.game.cake.PaletteItem
import hu.apk.kidsapp.game.cake.starPath
import hu.apk.kidsapp.ui.components.BackButton
import hu.apk.kidsapp.ui.theme.KidBackground
import hu.apk.kidsapp.ui.theme.KidGreen
import hu.apk.kidsapp.ui.theme.KidPurple
import hu.apk.kidsapp.ui.theme.KidRed
import hu.apk.kidsapp.ui.theme.KidText
import hu.apk.kidsapp.ui.theme.KidYellow
import hu.apk.kidsapp.util.darken
import hu.apk.kidsapp.util.lighten
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val chocolateBrown = Color(0xFF6D4C29)
private val vanillaCream = Color(0xFFFFE9B3)
private val strawberryPink = Color(0xFFFF9EC4)
private val blueberryPurple = Color(0xFF9B7FD4)
private val matchaGreen = Color(0xFF8BC34A)

private val whiteGlaze = Color(0xFFFFFBF2)
private val pinkGlaze = Color(0xFFFF6FA5)
private val lemonGlaze = Color(0xFFFFD54F)
private val chocolateGlaze = Color(0xFF8B5A2B)

private val layerColorCycle = listOf(chocolateBrown, vanillaCream, strawberryPink, blueberryPurple, matchaGreen)
private val icingColorCycle = listOf(whiteGlaze, pinkGlaze, lemonGlaze, chocolateGlaze)

private val decorationDefs = listOf(
    DecorationType.CHERRY to KidRed,
    DecorationType.STAR to KidYellow,
    DecorationType.SPRINKLE to KidPurple,
    DecorationType.CANDLE to Color.White,
    DecorationType.HEART to Color(0xFFFF5A8A),
    DecorationType.FLOWER to Color(0xFFFF8FD0),
    DecorationType.CHOC_CHIP to Color(0xFF4A2E1E),
    DecorationType.STRAWBERRY to Color(0xFFE0334D),
)

private const val ELLIPSE_RATIO = 0.42f

@Composable
fun CakeGameScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val voice = rememberVoicePlayer()

    var layerColors by remember { mutableStateOf(listOf<Color>()) }
    var icingByLayer by remember { mutableStateOf(mapOf<Int, Color>()) }
    var decorations by remember { mutableStateOf(listOf<Decoration>()) }
    var history by remember { mutableStateOf(listOf<CakeAction>()) }

    var layerSwatchColor by remember { mutableStateOf(layerColorCycle.first()) }
    var icingSwatchColor by remember { mutableStateOf(icingColorCycle.first()) }

    var draggingItem by remember { mutableStateOf<PaletteItem?>(null) }
    var dragPos by remember { mutableStateOf<Offset?>(null) }

    val layerAnim = remember { Animatable(1f) }
    var animatingLayerIndex by remember { mutableStateOf<Int?>(null) }

    val icingAnim = remember { Animatable(1f) }
    var animatingIcingLayer by remember { mutableStateOf<Int?>(null) }
    var animatingIcingColor by remember { mutableStateOf<Color?>(null) }

    val paletteItems = remember(layerSwatchColor, icingSwatchColor) {
        listOf(PaletteItem.LayerColor(layerSwatchColor), PaletteItem.Icing(icingSwatchColor)) +
            decorationDefs.map { (type, color) -> PaletteItem.Deco(type, color) }
    }

    fun animateLayerDrop(index: Int) {
        animatingLayerIndex = index
        scope.launch {
            layerAnim.snapTo(0f)
            layerAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 170f))
        }
    }

    // null = nincs nyitva színválasztó; false = a réteg (forma) színei; true = a máz színei
    var menuForIcing by remember { mutableStateOf<Boolean?>(null) }

    fun placeLayer(color: Color) {
        layerSwatchColor = color
        layerColors = layerColors + color
        history = history + CakeAction.Layer(color)
        animateLayerDrop(layerColors.lastIndex)
    }

    fun pourIcing(color: Color) {
        icingSwatchColor = color
        // Mindig a legfelső réteget mázazzuk — az alatta lévők mázán nem változtatunk.
        val target = layerColors.lastIndex
        if (target < 0) {
            voice.play(Voice.CAKE_NEED_LAYER)
            return
        }
        if (layerColors.size >= 2) voice.play(Voice.CAKE_DONE)
        val previousColor = icingByLayer[target]
        animatingIcingLayer = target
        animatingIcingColor = color
        history = history + CakeAction.IcingStep(target, previousColor)
        // Az állapot azonnal rögzül; az animáció csak látvány (gyors egymás utáni mázolásnál az
        // újabb animáció megszakítja a régit, így a máz nem veszhet el).
        icingByLayer = icingByLayer + (target to color)
        scope.launch {
            icingAnim.snapTo(0f)
            icingAnim.animateTo(1f, tween(1300, easing = FastOutSlowInEasing))
        }
    }

    fun clearAll() {
        layerColors = emptyList()
        icingByLayer = emptyMap()
        decorations = emptyList()
        history = emptyList()
        animatingIcingLayer = null
        animatingIcingColor = null
        scope.launch { icingAnim.snapTo(1f) }
    }

    fun undo() {
        val last = history.lastOrNull() ?: return
        history = history.dropLast(1)
        when (last) {
            is CakeAction.Layer -> layerColors = layerColors.dropLast(1)
            is CakeAction.Deco -> decorations = decorations.dropLast(1)
            is CakeAction.IcingStep -> {
                icingByLayer = if (last.previousColor != null) {
                    icingByLayer + (last.layerIndex to last.previousColor)
                } else {
                    icingByLayer - last.layerIndex
                }
                animatingIcingLayer = null
                animatingIcingColor = null
                scope.launch { icingAnim.snapTo(1f) }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(KidBackground)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val paletteWidthPx = with(density) { 196.dp.toPx() }
            val availW = with(density) { maxWidth.toPx() }
            val availH = with(density) { maxHeight.toPx() }

            val cakeCenterX = paletteWidthPx + (availW - paletteWidthPx) / 2f
            val plateY = availH * 0.82f
            val layerHeight = with(density) { 58.dp.toPx() }
            val baseRadiusX = with(density) { 116.dp.toPx() }
            val layerTaper = with(density) { 8.dp.toPx() }
            // A tányér ugyanazzal a perspektívával (ELLIPSE_RATIO) készül, mint a torta, és a
            // tortánál szélesebb, így a torta alsó íve végig a tányér lapján belül marad.
            val plateRx = baseRadiusX * 1.3f
            val plateRy = plateRx * ELLIPSE_RATIO
            val plateThickness = with(density) { 11.dp.toPx() }

            // Két oszlopos paletta: nagyobb ikonok, nagyobb találati felület. A felső sorban a
            // forma és a máz van (ezekre koppintva nyílik a színválasztó).
            val paletteTopY = with(density) { 132.dp.toPx() }
            val paletteSpacing = min(with(density) { 88.dp.toPx() }, (availH - paletteTopY - with(density) { 36.dp.toPx() }) / 4f)
            val paletteRadius = with(density) { 27.dp.toPx() }
            val paletteColDx = with(density) { 50.dp.toPx() }
            val paletteCenters = paletteItems.indices.map { i ->
                Offset(paletteWidthPx / 2f + (if (i % 2 == 0) -paletteColDx else paletteColDx), paletteTopY + (i / 2) * paletteSpacing)
            }
            val swatchHitRadius = with(density) { 50.dp.toPx() }
            val decoPaletteScale = 1.35f
            val decoPlacedScale = 1.6f

            fun layerRadiusX(index: Int): Float =
                (baseRadiusX - index * layerTaper).coerceAtLeast(baseRadiusX * 0.55f)

            fun layerTopY(index: Int): Float = plateY - (index + 1) * layerHeight
            fun layerBottomY(index: Int): Float = plateY - index * layerHeight

            // Színválasztó menü geometriája (a megnyitott paletta-elem mellett jelenik meg)
            val menuR = with(density) { 26.dp.toPx() }
            val menuGap = with(density) { 14.dp.toPx() }
            val menuLeft = paletteWidthPx + with(density) { 12.dp.toPx() }
            fun menuColorsFor(icing: Boolean): List<Color> = if (icing) icingColorCycle else layerColorCycle
            fun menuAnchorY(icing: Boolean): Float = paletteCenters[if (icing) 1 else 0].y
            fun menuCenter(i: Int, icing: Boolean): Offset =
                Offset(menuLeft + menuGap + menuR + i * (2 * menuR + menuGap), menuAnchorY(icing))

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    // Keyed on the swatch colors (not just Unit) so this coroutine restarts and
                    // picks up the CURRENT paletteItems — otherwise the closures below would keep
                    // dragging out the colors from app launch.
                    .pointerInput(layerSwatchColor, icingSwatchColor) {
                        detectTapGestures(
                            onTap = { tap ->
                                val open = menuForIcing
                                if (open != null) {
                                    val colors = menuColorsFor(open)
                                    val hit = colors.indices.firstOrNull { i ->
                                        (menuCenter(i, open) - tap).getDistance() < menuR * 1.25f
                                    }
                                    menuForIcing = null
                                    if (hit != null) {
                                        if (open) pourIcing(colors[hit]) else placeLayer(colors[hit])
                                        return@detectTapGestures
                                    }
                                }
                                val idx = paletteCenters.indexOfFirst { (it - tap).getDistance() < swatchHitRadius }
                                if (idx >= 0) {
                                    when (paletteItems[idx]) {
                                        is PaletteItem.LayerColor -> menuForIcing = if (open == false) null else false
                                        is PaletteItem.Icing -> menuForIcing = if (open == true) null else true
                                        is PaletteItem.Deco -> {}
                                    }
                                }
                            },
                        )
                    }
                    .pointerInput(layerSwatchColor, icingSwatchColor) {
                        detectDragGestures(
                            onDragStart = { start ->
                                menuForIcing = null
                                val idx = paletteCenters.indexOfFirst { (it - start).getDistance() < swatchHitRadius }
                                if (idx >= 0) {
                                    draggingItem = paletteItems[idx]
                                    dragPos = start
                                }
                            },
                            onDrag = { change, _ ->
                                if (draggingItem != null) {
                                    change.consume()
                                    dragPos = change.position
                                }
                            },
                            onDragEnd = {
                                val item = draggingItem
                                val pos = dragPos
                                if (item != null && pos != null && pos.x > paletteWidthPx) {
                                    when (item) {
                                        is PaletteItem.LayerColor -> placeLayer(item.color)
                                        is PaletteItem.Icing -> pourIcing(item.color)
                                        is PaletteItem.Deco -> {
                                            if (layerColors.isEmpty()) {
                                                voice.play(Voice.CAKE_NEED_LAYER)
                                            } else {
                                                val deco = Decoration(item.type, pos, item.color)
                                                decorations = decorations + deco
                                                history = history + CakeAction.Deco(deco)
                                            }
                                        }
                                    }
                                } else if (item != null && pos != null) {
                                    // Kihúzta, de visszaejtette a palettára — segítünk neki.
                                    voice.play(Voice.CAKE_DRAG_OUT)
                                }
                                draggingItem = null
                                dragPos = null
                            },
                            onDragCancel = {
                                draggingItem = null
                                dragPos = null
                            },
                        )
                    }
            ) {
                // A pult éle a tányér mögött van, így a tányér a pult lapján áll.
                drawKitchenBackground(availW, availH, counterY = plateY - plateRy * 1.5f)

                // Paletta háttere
                drawRect(color = Color.White, topLeft = Offset.Zero, size = Size(paletteWidthPx, availH))

                drawPlate(cakeCenterX, plateY, plateRx, plateRy, plateThickness)

                // Torta rétegei alulról felfelé, hengeres 3D hatással; minden réteg a saját mázával
                // együtt rajzolódik, mielőtt a FELETTE lévő réteg jönne — így az mindig helyesen
                // eltakarja az alatta lévőt (és annak mázát), sosem fordítva.
                layerColors.forEachIndexed { index, color ->
                    val rx = layerRadiusX(index)
                    val ry = rx * ELLIPSE_RATIO
                    val isAnimatingLayer = index == animatingLayerIndex
                    val layerProg = if (isAnimatingLayer) layerAnim.value else 1f
                    val fallOffset = (1f - layerProg) * layerHeight * 2f
                    val topY = layerTopY(index) - fallOffset
                    val bottomY = layerBottomY(index) - fallOffset

                    // Minden réteg kerek aljjal rajzolódik (nem csak a legalsó) — a korábbi
                    // forgásirány-hiba javítása óta ez már nem "harap ki" csúnyán, csak szépen
                    // legömbölyíti a hengert, ahogy egy valódi tortaréteg alja is kerekded.
                    drawCylinderLayer(cakeCenterX, topY, bottomY, rx, ry, color, roundedBottom = true)

                    val committedIcingColor = icingByLayer[index]
                    val isAnimatingIcing = index == animatingIcingLayer
                    val coverage = if (isAnimatingIcing) icingAnim.value else if (committedIcingColor != null) 1f else 0f
                    val icingColorToUse = if (isAnimatingIcing) animatingIcingColor else committedIcingColor
                    if (icingColorToUse != null && coverage > 0.001f) {
                        drawIcing(cakeCenterX, topY, rx, ry, layerHeight, icingColorToUse, coverage)
                    }
                }

                // Díszek
                decorations.forEach { deco -> drawDecoration(deco.type, deco.pos, deco.color, scale = decoPlacedScale) }

                // Paletta minták
                paletteItems.forEachIndexed { i, item ->
                    val center = paletteCenters[i]
                    when (item) {
                        is PaletteItem.LayerColor -> drawLayerSwatch(center, paletteRadius, item.color)
                        is PaletteItem.Icing -> drawIcingSwatch(center, paletteRadius * 1.7f, item.color)
                        is PaletteItem.Deco -> drawDecoration(item.type, center + Offset(0f, paletteRadius * 0.25f), item.color, scale = decoPaletteScale)
                    }
                }

                // Húzott elem előnézete
                val di = draggingItem
                val dp = dragPos
                if (di != null && dp != null) {
                    when (di) {
                        is PaletteItem.LayerColor -> drawLayerSwatch(dp, paletteRadius, di.color)
                        is PaletteItem.Icing -> drawIcingSwatch(dp, paletteRadius * 1.7f, di.color)
                        is PaletteItem.Deco -> drawDecoration(di.type, dp, di.color, scale = decoPlacedScale, alpha = 0.85f)
                    }
                }

                // Színválasztó menü: koppintásra nyílik a forma / máz mintán
                val openMenu = menuForIcing
                if (openMenu != null) {
                    val colors = menuColorsFor(openMenu)
                    val anchorY = menuAnchorY(openMenu)
                    val panelW = colors.size * 2 * menuR + (colors.size + 1) * menuGap
                    val panelH = 2 * menuR + 2 * menuGap
                    val panelTopLeft = Offset(menuLeft, anchorY - panelH / 2f)
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.18f),
                        topLeft = panelTopLeft + Offset(4f, 6f),
                        size = Size(panelW, panelH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(menuR, menuR),
                    )
                    drawRoundRect(
                        color = Color.White,
                        topLeft = panelTopLeft,
                        size = Size(panelW, panelH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(menuR, menuR),
                    )
                    val selected = if (openMenu) icingSwatchColor else layerSwatchColor
                    colors.forEachIndexed { i, color ->
                        val c = menuCenter(i, openMenu)
                        if (color == selected) {
                            drawCircle(Color(0xFF4A90E2), radius = menuR + 6f, center = c, style = Stroke(width = 5f))
                        }
                        if (openMenu) drawIcingSwatch(c, menuR * 1.7f, color) else drawLayerSwatch(c, menuR, color)
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                BackButton(onClick = onBack)
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center) {
                    Text(
                        "Tortakészítő",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = KidText,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                    Row {
                        FilledIconButton(
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = KidGreen),
                            onClick = { undo() },
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Visszavonás", tint = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        FilledIconButton(
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = KidRed),
                            onClick = { clearAll() },
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Minden törlése", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawPlate(cx: Float, cy: Float, rx: Float, ry: Float, thickness: Float) {
    // Árnyék a pulton
    drawOval(
        Color.Black.copy(alpha = 0.16f),
        topLeft = Offset(cx - rx * 1.04f, cy + thickness - ry * 0.80f),
        size = Size(rx * 2.08f, ry * 2f * 1.0f),
    )
    // Tányér oldala (vastagság): alsó ellipszis + összekötő sáv
    val side = Color(0xFFB9BCC9)
    drawOval(side, topLeft = Offset(cx - rx, cy + thickness - ry), size = Size(rx * 2f, ry * 2f))
    drawRect(side, topLeft = Offset(cx - rx, cy), size = Size(rx * 2f, thickness))
    // Felső lap
    drawOval(
        Brush.radialGradient(
            listOf(Color(0xFFFFFFFF), Color(0xFFE8E9F0)),
            center = Offset(cx - rx * 0.2f, cy - ry * 0.3f),
            radius = rx * 1.2f,
        ),
        topLeft = Offset(cx - rx, cy - ry),
        size = Size(rx * 2f, ry * 2f),
    )
    drawOval(
        Color(0xFFA9ADBD),
        topLeft = Offset(cx - rx, cy - ry),
        size = Size(rx * 2f, ry * 2f),
        style = Stroke(width = 2.5f),
    )
    // Mélyedés a tányér közepén, ide kerül a torta
    drawOval(
        Color(0xFFD9DBE6),
        topLeft = Offset(cx - rx * 0.82f, cy - ry * 0.82f),
        size = Size(rx * 2f * 0.82f, ry * 2f * 0.82f),
    )
    drawOval(
        Color(0xFFBFC2D1),
        topLeft = Offset(cx - rx * 0.82f, cy - ry * 0.82f),
        size = Size(rx * 2f * 0.82f, ry * 2f * 0.82f),
        style = Stroke(width = 1.5f),
    )
}

private fun DrawScope.drawCatFrame(center: Offset, w: Float) {
    val h = w * 1.18f
    val left = center.x - w / 2f
    val top = center.y - h / 2f
    val frameThick = w * 0.075f
    val outer = Size(w, h)

    // Akasztózsineg és szög
    val nail = Offset(center.x, top - w * 0.2f)
    drawLine(Color(0xFF8A6A4A), nail, Offset(left + w * 0.12f, top + 2f), strokeWidth = 2.5f)
    drawLine(Color(0xFF8A6A4A), nail, Offset(left + w * 0.88f, top + 2f), strokeWidth = 2.5f)
    drawCircle(Color(0xFF6B5136), radius = w * 0.025f, center = nail)

    // Árnyék a falon
    drawRoundRect(
        Color.Black.copy(alpha = 0.12f),
        topLeft = Offset(left + w * 0.03f, top + w * 0.04f),
        size = outer,
        cornerRadius = CornerRadius(w * 0.05f),
    )
    // Fa keret
    drawRoundRect(
        Brush.linearGradient(
            listOf(Color(0xFFB9773F), Color(0xFF8E5A2B)),
            start = Offset(left, top),
            end = Offset(left + w, top + h),
        ),
        topLeft = Offset(left, top),
        size = outer,
        cornerRadius = CornerRadius(w * 0.05f),
    )
    drawRoundRect(
        Color(0xFFD9A06A).copy(alpha = 0.7f),
        topLeft = Offset(left + 2f, top + 2f),
        size = Size(w - 4f, h - 4f),
        cornerRadius = CornerRadius(w * 0.05f),
        style = Stroke(width = 2f),
    )
    // Paszpartu + égszínkék háttér
    val inLeft = left + frameThick
    val inTop = top + frameThick
    val inW = w - frameThick * 2f
    val inH = h - frameThick * 2f
    drawRect(Color(0xFFFFF6E6), topLeft = Offset(inLeft, inTop), size = Size(inW, inH))
    val pad = inW * 0.09f
    val picLeft = inLeft + pad
    val picTop = inTop + pad
    val picW = inW - pad * 2f
    val picH = inH - pad * 2f
    drawRect(
        Brush.verticalGradient(
            listOf(Color(0xFFBFE3F5), Color(0xFFE8F6FC)),
            startY = picTop,
            endY = picTop + picH,
        ),
        topLeft = Offset(picLeft, picTop),
        size = Size(picW, picH),
    )

    // A cica (narancssárga, ülő, szemből)
    val cx = picLeft + picW / 2f
    val headR = picW * 0.27f
    val headC = Offset(cx, picTop + picH * 0.46f)
    val fur = Color(0xFFF2A154)
    val furDark = Color(0xFFD9803A)

    // Test
    drawOval(
        fur,
        topLeft = Offset(cx - picW * 0.28f, headC.y + headR * 0.55f),
        size = Size(picW * 0.56f, picH * 0.46f),
    )
    drawOval(
        Color(0xFFFFE3C4),
        topLeft = Offset(cx - picW * 0.14f, headC.y + headR * 0.95f),
        size = Size(picW * 0.28f, picH * 0.30f),
    )
    // Farok
    val tail = Path().apply {
        moveTo(cx + picW * 0.24f, picTop + picH * 0.92f)
        cubicTo(
            cx + picW * 0.5f, picTop + picH * 0.92f,
            cx + picW * 0.5f, picTop + picH * 0.62f,
            cx + picW * 0.36f, picTop + picH * 0.66f,
        )
    }
    drawPath(tail, fur, style = Stroke(width = picW * 0.09f, cap = StrokeCap.Round))
    // Fülek
    fun ear(sign: Float) {
        val outerEar = Path().apply {
            moveTo(cx + sign * headR * 0.95f, headC.y - headR * 0.25f)
            lineTo(cx + sign * headR * 0.80f, headC.y - headR * 1.25f)
            lineTo(cx + sign * headR * 0.15f, headC.y - headR * 0.85f)
            close()
        }
        drawPath(outerEar, fur)
        val innerEar = Path().apply {
            moveTo(cx + sign * headR * 0.78f, headC.y - headR * 0.45f)
            lineTo(cx + sign * headR * 0.72f, headC.y - headR * 1.0f)
            lineTo(cx + sign * headR * 0.32f, headC.y - headR * 0.78f)
            close()
        }
        drawPath(innerEar, Color(0xFFFFB3BE))
    }
    ear(-1f)
    ear(1f)
    // Fej
    drawCircle(fur, radius = headR, center = headC)
    // Csíkok a homlokon
    for (dx in listOf(-0.28f, 0f, 0.28f)) {
        drawLine(
            furDark,
            Offset(cx + dx * headR, headC.y - headR * 0.98f),
            Offset(cx + dx * headR * 0.9f, headC.y - headR * 0.62f),
            strokeWidth = headR * 0.09f,
            cap = StrokeCap.Round,
        )
    }
    // Pofa
    drawOval(
        Color(0xFFFFE3C4),
        topLeft = Offset(cx - headR * 0.62f, headC.y + headR * 0.05f),
        size = Size(headR * 1.24f, headR * 0.85f),
    )
    // Szemek
    for (sign in listOf(-1f, 1f)) {
        val ec = Offset(cx + sign * headR * 0.42f, headC.y - headR * 0.12f)
        drawOval(Color(0xFF2B2B3A), topLeft = Offset(ec.x - headR * 0.14f, ec.y - headR * 0.19f), size = Size(headR * 0.28f, headR * 0.38f))
        drawCircle(Color.White, radius = headR * 0.06f, center = Offset(ec.x - headR * 0.04f, ec.y - headR * 0.08f))
        // Pirospozsgás arc
        drawCircle(Color(0xFFFF8FA3).copy(alpha = 0.45f), radius = headR * 0.14f, center = Offset(cx + sign * headR * 0.66f, headC.y + headR * 0.28f))
        // Bajusz
        for (k in -1..1) {
            drawLine(
                Color(0xFF7A5A44),
                Offset(cx + sign * headR * 0.55f, headC.y + headR * (0.30f + k * 0.08f)),
                Offset(cx + sign * headR * 1.18f, headC.y + headR * (0.22f + k * 0.2f)),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
            )
        }
    }
    // Orr és száj
    val nose = Path().apply {
        moveTo(cx - headR * 0.11f, headC.y + headR * 0.18f)
        lineTo(cx + headR * 0.11f, headC.y + headR * 0.18f)
        lineTo(cx, headC.y + headR * 0.30f)
        close()
    }
    drawPath(nose, Color(0xFFFF6F8E))
    val mouth = Path().apply {
        moveTo(cx, headC.y + headR * 0.30f)
        quadraticTo(cx - headR * 0.04f, headC.y + headR * 0.50f, cx - headR * 0.24f, headC.y + headR * 0.44f)
        moveTo(cx, headC.y + headR * 0.30f)
        quadraticTo(cx + headR * 0.04f, headC.y + headR * 0.50f, cx + headR * 0.24f, headC.y + headR * 0.44f)
    }
    drawPath(mouth, Color(0xFF7A4A3A), style = Stroke(width = 2.5f, cap = StrokeCap.Round))
    // Mancsok
    for (sign in listOf(-1f, 1f)) {
        drawOval(
            Color(0xFFFFE3C4),
            topLeft = Offset(cx + sign * picW * 0.14f - picW * 0.07f, picTop + picH * 0.86f),
            size = Size(picW * 0.14f, picH * 0.10f),
        )
    }
    // Üveg-csillanás a kereten belül
    drawLine(Color.White.copy(alpha = 0.35f), Offset(picLeft + picW * 0.12f, picTop + picH * 0.10f), Offset(picLeft + picW * 0.30f, picTop + picH * 0.02f), strokeWidth = picW * 0.05f, cap = StrokeCap.Round)
    // Belső keret-él
    drawRect(Color(0xFF8E5A2B).copy(alpha = 0.5f), topLeft = Offset(picLeft, picTop), size = Size(picW, picH), style = Stroke(width = 2f))
}

private fun DrawScope.drawKitchenBackground(width: Float, height: Float, counterY: Float) {
    // Csempés fal
    drawRect(color = Color(0xFFFDF1E0), topLeft = Offset.Zero, size = Size(width, counterY))
    val tile = 64f
    var x = 0f
    while (x < width) {
        drawLine(Color(0xFFE9D6B8), Offset(x, 0f), Offset(x, counterY), strokeWidth = 1.5f)
        x += tile
    }
    var y = 0f
    while (y < counterY) {
        drawLine(Color(0xFFE9D6B8), Offset(0f, y), Offset(width, y), strokeWidth = 1.5f)
        y += tile
    }
    // Pár apró csempe-dísz (szív), hogy barátságosabb legyen
    val heartSpots = listOf(Offset(width * 0.30f, tile * 0.5f), Offset(width * 0.68f, tile * 1.5f))
    heartSpots.forEach { spot -> drawDecoration(DecorationType.HEART, spot, Color(0xFFF2B6C6).copy(alpha = 0.6f), scale = 0.7f) }

    // Képkeretes cica a falon
    val frameW = minOf(width * 0.13f, counterY * 0.36f)
    drawCatFrame(Offset(width * 0.34f, counterY * 0.46f), frameW)

    // Munkapult
    drawRect(color = Color(0xFFD9AF80), topLeft = Offset(0f, counterY), size = Size(width, height - counterY))
    drawRect(
        color = Color(0xFFC79764),
        topLeft = Offset(0f, counterY),
        size = Size(width, (height - counterY) * 0.18f),
    )
    drawLine(Color(0xFFB07F4E), Offset(0f, counterY), Offset(width, counterY), strokeWidth = 5f)
}

private fun DrawScope.drawCylinderLayer(
    cx: Float,
    topY: Float,
    bottomY: Float,
    rx: Float,
    ry: Float,
    color: Color,
    roundedBottom: Boolean = true,
) {
    val wallPath = Path().apply {
        moveTo(cx - rx, topY)
        lineTo(cx - rx, bottomY)
        if (roundedBottom) {
            // Negative sweep: from the left point (180°), go counter-clockwise THROUGH the
            // bottom (90°) to the right point (0°/360°) — the front-facing curve. A positive
            // sweep here would arc through the top instead, biting a notch out of the front.
            arcTo(Rect(cx - rx, bottomY - ry, cx + rx, bottomY + ry), 180f, -180f, false)
        } else {
            lineTo(cx + rx, bottomY)
        }
        lineTo(cx + rx, topY)
        close()
    }
    drawPath(
        wallPath,
        Brush.horizontalGradient(listOf(color.lighten(0.10f), color.darken(0.08f), color.darken(0.32f))),
    )
    drawPath(
        wallPath,
        Color.Black.copy(alpha = 0.12f),
        style = Stroke(width = 1.5f, join = androidx.compose.ui.graphics.StrokeJoin.Round, cap = StrokeCap.Round),
    )

    drawOval(
        Brush.radialGradient(
            listOf(color.lighten(0.4f), color),
            center = Offset(cx - rx * 0.25f, topY - ry * 0.25f),
            radius = rx * 1.4f,
        ),
        topLeft = Offset(cx - rx, topY - ry),
        size = Size(rx * 2f, ry * 2f),
    )
    drawOval(
        color.darken(0.18f).copy(alpha = 0.35f),
        topLeft = Offset(cx - rx, topY - ry),
        size = Size(rx * 2f, ry * 2f),
        style = Stroke(width = 1.5f),
    )
}

private fun DrawScope.drawLayerSwatch(center: Offset, radius: Float, color: Color) {
    val rx = radius
    val ry = rx * ELLIPSE_RATIO
    val height = radius * 1.1f
    drawCylinderLayer(center.x, center.y - height / 2f, center.y + height / 2f, rx, ry, color)
}

private fun DrawScope.drawIcingSwatch(center: Offset, size: Float, color: Color) {
    val w = size
    val h = size * 1.25f
    val path = Path().apply {
        moveTo(center.x - w * 0.5f, center.y - h * 0.35f)
        quadraticTo(center.x - w * 0.55f, center.y - h * 0.6f, center.x - w * 0.22f, center.y - h * 0.55f)
        lineTo(center.x + w * 0.22f, center.y - h * 0.55f)
        quadraticTo(center.x + w * 0.55f, center.y - h * 0.6f, center.x + w * 0.5f, center.y - h * 0.35f)
        lineTo(center.x + w * 0.14f, center.y + h * 0.32f)
        lineTo(center.x, center.y + h * 0.5f)
        lineTo(center.x - w * 0.14f, center.y + h * 0.32f)
        close()
    }
    drawPath(
        path,
        Brush.linearGradient(
            listOf(color.lighten(0.3f), color),
            start = Offset(center.x - w / 2f, center.y - h / 2f),
            end = Offset(center.x + w / 2f, center.y + h / 2f),
        ),
    )
    drawPath(path, Color(0xFFBFBFBF), style = Stroke(width = 2f))
    drawCircle(color, radius = size * 0.09f, center = Offset(center.x, center.y + h * 0.55f))
}

private fun DrawScope.drawIcing(cx: Float, topY: Float, rx: Float, ry: Float, wallHeight: Float, color: Color, coverage: Float) {
    if (coverage <= 0.001f) return
    val sweepDeg = (coverage * 360f).coerceIn(0f, 360f)
    val bulge = 1.0f

    // A máz végigfolyik a torta oldalán: a palást a felső lap ellipszisének ELSŐ (látható) íve alól
    // indul, és hullámos alsó széllel ér véget. A hátsó félen nincs palást (az a torta mögött lenne).
    // A palást a sweep mögött "folyik le" (a hossza fokozatosan nő), így a rajzolás közben is szép.
    val sweepEndAbs = -90f + sweepDeg
    val frontEnd = min(180f, sweepEndAbs)
    if (frontEnd > 0.5f) {
        val steps = 72
        val top = ArrayList<Offset>(steps + 1)
        val bottom = ArrayList<Offset>(steps + 1)
        for (k in 0..steps) {
            val aDeg = frontEnd * k / steps
            val a = Math.toRadians(aDeg.toDouble())
            val x = cx + rx * cos(a).toFloat()
            val y = topY + ry * sin(a).toFloat()
            val wave = 0.5f + 0.5f * sin(a * 12.0 + 0.6).toFloat()
            val slow = sin(a * 5.0 + 1.3).toFloat()
            val fullLen = wallHeight * (0.50f + 0.34f * wave + 0.07f * slow).coerceIn(0.4f, 0.93f)
            val grow = ((sweepEndAbs - aDeg) / 35f).coerceIn(0f, 1f)
            top.add(Offset(x, y))
            bottom.add(Offset(x, y + fullLen * grow))
        }
        val curtain = Path().apply {
            moveTo(top[0].x, top[0].y)
            for (k in 1..steps) lineTo(top[k].x, top[k].y)
            for (k in steps downTo 0) lineTo(bottom[k].x, bottom[k].y)
            close()
        }
        drawPath(
            curtain,
            Brush.verticalGradient(
                listOf(color, color.darken(0.06f)),
                startY = topY,
                endY = topY + wallHeight,
            ),
        )
        // finom árnyék a szegély alatt és csillanás a palástban
        val edge = Path().apply {
            moveTo(bottom[0].x, bottom[0].y)
            for (k in 1..steps) lineTo(bottom[k].x, bottom[k].y)
        }
        drawPath(edge, color.darken(0.18f), style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    drawArc(
        brush = Brush.radialGradient(
            listOf(color.lighten(0.35f), color, color.darken(0.05f)),
            center = Offset(cx - rx * 0.2f, topY - ry * 0.2f),
            radius = rx * 1.3f,
        ),
        startAngle = -90f,
        sweepAngle = sweepDeg,
        useCenter = true,
        topLeft = Offset(cx - rx * bulge, topY - ry * bulge),
        size = Size(rx * 2f * bulge, ry * 2f * bulge),
    )

    // Fényes csillanás a tetején, ha már eléggé kész van
    if (coverage > 0.5f) {
        drawOval(
            color = Color.White.copy(alpha = 0.35f * ((coverage - 0.5f) / 0.5f).coerceIn(0f, 1f)),
            topLeft = Offset(cx - rx * 0.5f, topY - ry * 0.55f),
            size = Size(rx * 0.6f, ry * 0.5f),
        )
    }
}

private fun DrawScope.drawDecoration(type: DecorationType, center: Offset, color: Color, scale: Float, alpha: Float = 1f) {
    when (type) {
        DecorationType.CHERRY -> {
            drawCircle(color.copy(alpha = alpha), radius = 15f * scale, center = center)
            drawLine(
                color = Color(0xFF6D4C29).copy(alpha = alpha),
                start = center + Offset(0f, -15f * scale),
                end = center + Offset(7f * scale, -28f * scale),
                strokeWidth = 3f * scale,
            )
        }
        DecorationType.STAR -> drawPath(starPath(center, 20f * scale, 9f * scale), color.copy(alpha = alpha))
        DecorationType.SPRINKLE -> drawRoundRect(
            color = color.copy(alpha = alpha),
            topLeft = center + Offset(-14f * scale, -6f * scale),
            size = Size(28f * scale, 12f * scale),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * scale, 6f * scale),
        )
        DecorationType.CANDLE -> {
            drawRect(
                color = Color.White.copy(alpha = alpha),
                topLeft = center + Offset(-4f * scale, -32f * scale),
                size = Size(8f * scale, 32f * scale),
            )
            drawCircle(
                color = Color(0xFFFFA84D).copy(alpha = alpha),
                radius = 7f * scale,
                center = center + Offset(0f, -38f * scale),
            )
        }
        DecorationType.HEART -> {
            val s = 15f * scale
            val path = Path().apply {
                moveTo(center.x, center.y + s * 0.85f)
                cubicTo(
                    center.x - s * 1.3f, center.y - s * 0.2f,
                    center.x - s * 0.6f, center.y - s * 1.2f,
                    center.x, center.y - s * 0.4f,
                )
                cubicTo(
                    center.x + s * 0.6f, center.y - s * 1.2f,
                    center.x + s * 1.3f, center.y - s * 0.2f,
                    center.x, center.y + s * 0.85f,
                )
                close()
            }
            drawPath(path, color.copy(alpha = alpha))
        }
        DecorationType.FLOWER -> {
            val petalR = 8f * scale
            val orbit = 9f * scale
            for (i in 0 until 5) {
                val angle = Math.toRadians((i * 72).toDouble())
                val px = center.x + (orbit * cos(angle)).toFloat()
                val py = center.y + (orbit * sin(angle)).toFloat()
                drawCircle(color.copy(alpha = alpha), radius = petalR, center = Offset(px, py))
            }
            drawCircle(Color(0xFFFFD54F).copy(alpha = alpha), radius = petalR * 0.75f, center = center)
        }
        DecorationType.CHOC_CHIP -> {
            drawCircle(color.copy(alpha = alpha), radius = 9f * scale, center = center)
            drawCircle(Color.White.copy(alpha = 0.25f * alpha), radius = 3f * scale, center = center + Offset(-3f * scale, -3f * scale))
        }
        DecorationType.STRAWBERRY -> {
            drawCircle(color.copy(alpha = alpha), radius = 15f * scale, center = center)
            val seedPositions = listOf(
                Offset(-0.5f, 0f), Offset(0.5f, 0f), Offset(0f, -0.55f),
                Offset(0f, 0.55f), Offset(-0.35f, -0.4f), Offset(0.35f, 0.4f),
            )
            seedPositions.forEach { rel ->
                drawCircle(
                    Color(0xFFFFD54F).copy(alpha = alpha),
                    radius = 2f * scale,
                    center = center + Offset(rel.x * 13f * scale, rel.y * 13f * scale),
                )
            }
            val leaf = Path().apply {
                moveTo(center.x, center.y - 15f * scale)
                lineTo(center.x - 6f * scale, center.y - 22f * scale)
                lineTo(center.x + 6f * scale, center.y - 22f * scale)
                close()
            }
            drawPath(leaf, Color(0xFF6FCF97).copy(alpha = alpha))
        }
    }
}
