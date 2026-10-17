package hu.apk.kidsapp.ui.screens.car

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.random.Random

private enum class Category(val emoji: String, val label: String) {
    VEHICLE("🚗", "Jármű"),
    BODY("🎨", "Szín"),
    LAMP("💡", "Lámpa"),
    TYRE("⭕", "Kerék"),
    RIM("⚙️", "Felni"),
    RIMCOLOR("✨", "Felni szín"),
    STICKER("⭐", "Matrica"),
    STICKERCOLOR("🖍️", "Matr. szín"),
}

private const val AUTO_ROTATE_DEG_PER_SEC = 10f

@Composable
fun CarGameScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var look by remember { mutableStateOf(CarLook()) }
    var category by remember { mutableStateOf(Category.VEHICLE) }

    // ---- Forgatás, kerékpörgés, fények ----
    var yaw by remember { mutableFloatStateOf(-32f) }
    var pitch by remember { mutableFloatStateOf(15f) }
    var yawVel by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var spin by remember { mutableFloatStateOf(0f) }
    var spinSpeed by remember { mutableFloatStateOf(0f) }
    var lightsOn by remember { mutableStateOf(true) }
    val lamp = remember { Animatable(1f) }
    val pop = remember { Animatable(0f) }
    val bounce = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { t ->
                if (last != 0L) {
                    val dt = ((t - last) / 1_000_000_000f).coerceAtMost(0.05f)
                    if (!dragging) {
                        yaw += (AUTO_ROTATE_DEG_PER_SEC + yawVel) * dt
                        yawVel *= exp(-2.4f * dt)
                        if (kotlin.math.abs(yawVel) < 0.5f) yawVel = 0f
                    }
                    spin += spinSpeed * dt
                    spinSpeed *= exp(-1.0f * dt)
                    if (spinSpeed < 2f) spinSpeed = 0f
                    if (yaw > 360f) yaw -= 360f
                    if (yaw < -360f) yaw += 360f
                    if (spin > 360f) spin -= 360f
                }
                last = t
            }
        }
    }

    fun poke() {
        scope.launch {
            pop.snapTo(1f)
            pop.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 260f))
        }
    }

    fun tapCar() {
        lightsOn = !lightsOn
        spinSpeed = 900f
        scope.launch { lamp.animateTo(if (lightsOn) 1f else 0f, tween(220)) }
        scope.launch {
            bounce.snapTo(0f)
            bounce.animateTo(0.1f, tween(90))
            bounce.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 260f))
        }
    }

    fun choose(newLook: CarLook) {
        val vehicleChanged = newLook.vehicle != look.vehicle
        look = newLook
        poke()
        if (vehicleChanged) {
            yawVel = 420f
            spinSpeed = 500f
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(KidBackground)) {
        Row(modifier = Modifier.fillMaxSize()) {
            // ---- Színpad ----
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) { detectTapGestures(onTap = { tapCar() }) }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    dragging = true
                                    yawVel = 0f
                                },
                                onDragEnd = { dragging = false },
                                onDragCancel = { dragging = false },
                            ) { change, drag ->
                                change.consume()
                                yaw += drag.x * 0.45f
                                pitch = (pitch + drag.y * 0.12f).coerceIn(4f, 40f)
                                yawVel = (drag.x * 0.45f * 55f).coerceIn(-420f, 420f)
                            }
                        },
                ) {
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color(0xFFD6E8FF), Color(0xFFF3F8FF), Color(0xFFE9EEF7)),
                            startY = 0f,
                            endY = size.height,
                        ),
                    )
                    drawCarScene(
                        model = vehicleModel(look.vehicle),
                        look = look,
                        yawDeg = yaw,
                        pitchDeg = pitch,
                        spinDeg = spin,
                        lamp = lamp.value,
                        bounce = bounce.value,
                        popScale = 1f + 0.05f * pop.value,
                        top = 104.dp.toPx(),
                    )
                }
                Text(
                    "Építsd meg az autód!",
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
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    ) {
                        val bodyColor = bodyColors[look.body].color
                        when (category) {
                            Category.VEHICLE -> items(VehicleType.entries) { v ->
                                OptionCell(look.vehicle == v, v.label, { choose(look.copy(vehicle = v)) }) {
                                    drawVehicleIcon(v, bodyColor)
                                }
                            }
                            Category.BODY -> items(bodyColors.indices.toList()) { i ->
                                OptionCell(look.body == i, bodyColors[i].label, { choose(look.copy(body = i)) }) {
                                    drawColorChip(bodyColors[i].color)
                                }
                            }
                            Category.LAMP -> items(lampColors.indices.toList()) { i ->
                                OptionCell(look.lamp == i, lampColors[i].label, { choose(look.copy(lamp = i)) }) {
                                    drawLampChip(lampColors[i].color)
                                }
                            }
                            Category.TYRE -> items(tyreColors.indices.toList()) { i ->
                                OptionCell(look.tyre == i, tyreColors[i].label, { choose(look.copy(tyre = i)) }) {
                                    drawWheelIcon(tyreColors[i].color, look.rimStyle, rimColors[look.rimColor].color)
                                }
                            }
                            Category.RIM -> items(RimStyle.entries) { r ->
                                OptionCell(look.rimStyle == r, r.label, { choose(look.copy(rimStyle = r)) }) {
                                    drawWheelIcon(tyreColors[look.tyre].color, r, rimColors[look.rimColor].color)
                                }
                            }
                            Category.RIMCOLOR -> items(rimColors.indices.toList()) { i ->
                                OptionCell(look.rimColor == i, rimColors[i].label, { choose(look.copy(rimColor = i)) }) {
                                    drawWheelIcon(tyreColors[look.tyre].color, look.rimStyle, rimColors[i].color)
                                }
                            }
                            Category.STICKER -> items(Sticker.entries) { s ->
                                if (s == Sticker.NONE) {
                                    OptionCell(look.sticker == s, s.label, { choose(look.copy(sticker = s)) }, emoji = "🚫")
                                } else {
                                    OptionCell(look.sticker == s, s.label, { choose(look.copy(sticker = s)) }) {
                                        drawStickerIcon(s, bodyColor, look)
                                    }
                                }
                            }
                            Category.STICKERCOLOR -> items(stickerColors.indices.toList()) { i ->
                                OptionCell(look.stickerColor == i, stickerColors[i].label, { choose(look.copy(stickerColor = i)) }) {
                                    drawColorChip(stickerColors[i].color)
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .width(88.dp)
                            .fillMaxHeight()
                            .background(Color(0xFFF4F6FB))
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        Category.entries.forEach { c ->
                            CategoryButton(c.emoji, c.label, category == c) { category = c }
                        }
                        CategoryButton("🎲", "Véletlen", false) {
                            choose(randomLook())
                            tapCar()
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

private fun randomLook() = CarLook(
    vehicle = VehicleType.entries.random(),
    body = Random.nextInt(bodyColors.size),
    lamp = Random.nextInt(lampColors.size),
    tyre = Random.nextInt(tyreColors.size),
    rimStyle = RimStyle.entries.random(),
    rimColor = Random.nextInt(rimColors.size),
    sticker = Sticker.entries.filter { it != Sticker.NONE }.random(),
    stickerColor = Random.nextInt(stickerColors.size),
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
                .size(40.dp)
                .background(if (selected) KidBlue else Color.White, CircleShape)
                .border(if (selected) 0.dp else 2.dp, Color(0xFFD5DAE8), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 20.sp)
        }
        Text(
            label,
            fontSize = 10.sp,
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
        Box(modifier = Modifier.size(width = 84.dp, height = 74.dp), contentAlignment = Alignment.Center) {
            if (emoji != null) {
                Text(emoji, fontSize = 38.sp)
            } else if (preview != null) {
                Canvas(modifier = Modifier.fillMaxSize()) { preview() }
            }
        }
        Text(label, fontSize = 12.sp, color = KidText, textAlign = TextAlign.Center)
    }
}

private fun DrawScope.drawColorChip(color: Color) {
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension * 0.42f
    drawCircle(
        Brush.radialGradient(
            listOf(color.lighten(0.35f), color, color.darken(0.18f)),
            center = Offset(c.x - r * 0.3f, c.y - r * 0.35f),
            radius = r * 1.5f,
        ),
        radius = r,
        center = c,
    )
    drawCircle(Color.Black.copy(alpha = 0.18f), radius = r, center = c, style = Stroke(width = 2.5f))
}

private fun DrawScope.drawLampChip(color: Color) {
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension * 0.46f
    drawCircle(
        Brush.radialGradient(listOf(Color.White, color, color.copy(alpha = 0f)), center = c, radius = r),
        radius = r,
        center = c,
    )
    drawCircle(Color(0xFF2A2C35), radius = r * 0.5f, center = c, style = Stroke(width = 4f))
    drawCircle(color.lighten(0.2f), radius = r * 0.44f, center = c)
}
