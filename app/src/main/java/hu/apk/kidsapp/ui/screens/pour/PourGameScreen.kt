package hu.apk.kidsapp.ui.screens.pour

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import hu.apk.kidsapp.data.PourGameProgress
import hu.apk.kidsapp.game.pour.generateSolvableLevel
import hu.apk.kidsapp.game.pour.isSolved
import hu.apk.kidsapp.util.Voice
import hu.apk.kidsapp.util.rememberVoicePlayer
import hu.apk.kidsapp.game.pour.levelSpecFor
import hu.apk.kidsapp.game.pour.levelSpecs
import hu.apk.kidsapp.game.pour.pour
import hu.apk.kidsapp.game.pour.pourAmount
import hu.apk.kidsapp.game.pour.top
import hu.apk.kidsapp.ui.components.BackButton
import hu.apk.kidsapp.ui.theme.*
import hu.apk.kidsapp.util.darken
import hu.apk.kidsapp.util.lighten
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val pourColors = listOf(
    KidRed,
    KidBlue,
    KidYellow,
    KidGreen,
    KidPurple,
    Color(0xFFFF8FD0), // rózsaszín
)

private const val TUBE_WIDTH_DP = 56
private const val TUBE_SPACING_DP = 16
private const val TUBE_STEP_DP = TUBE_WIDTH_DP + TUBE_SPACING_DP

private data class PourAnim(
    val from: Int,
    val to: Int,
    val amount: Int,
    val color: Int,
    val progress: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
)

/** 0→1 over [start,end], eased; used to time the float/pour/return sub-phases within one pour. */
private fun phase(p: Float, start: Float, end: Float): Float {
    val t = ((p - start) / (end - start)).coerceIn(0f, 1f)
    return FastOutSlowInEasing.transform(t)
}

@Composable
fun PourGameScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val progress = remember { PourGameProgress(context) }
    val scope = rememberCoroutineScope()
    val voice = rememberVoicePlayer()

    var levelIndex by remember { mutableStateOf(progress.currentLevel) }
    var seed by remember { mutableStateOf(System.currentTimeMillis()) }
    var tubes by remember(levelIndex, seed) {
        mutableStateOf(generateSolvableLevel(levelSpecFor(levelIndex), seed))
    }
    var selected by remember(levelIndex, seed) { mutableStateOf<Int?>(null) }
    var pouring by remember(levelIndex, seed) { mutableStateOf<PourAnim?>(null) }

    val solved = tubes.isSolved()
    var pourJob by remember { mutableStateOf<Job?>(null) }

    fun reshuffle() {
        // A folyamatban lévő öntést le kell állítani, különben a lefutása már az ÚJ táblára írna.
        pourJob?.cancel()
        selected = null
        pouring = null
        seed = System.currentTimeMillis()
    }

    fun goNext() {
        pourJob?.cancel()
        levelIndex = (levelIndex + 1).coerceAtMost(levelSpecs.lastIndex)
        progress.currentLevel = levelIndex
        selected = null
        pouring = null
        seed = System.currentTimeMillis()
    }

    fun onTubeTap(index: Int) {
        if (solved || pouring != null) return
        val from = selected
        when {
            from == null -> {
                if (tubes[index].segments.isNotEmpty()) selected = index else voice.play(Voice.POUR_EMPTY)
            }
            from == index -> selected = null
            else -> {
                val amount = pourAmount(tubes[from], tubes[index])
                if (amount > 0) {
                    val color = tubes[from].top()!!
                    val anim = Animatable(0f)
                    selected = null
                    pouring = PourAnim(from, index, amount, color, anim)
                    pourJob = scope.launch {
                        anim.animateTo(1f, tween(1400, easing = androidx.compose.animation.core.LinearEasing))
                        if (from < tubes.size && index < tubes.size) {
                            val (newFrom, newTo) = pour(tubes[from], tubes[index])
                            tubes = tubes.toMutableList().also {
                                it[from] = newFrom
                                it[index] = newTo
                            }
                        }
                        pouring = null
                    }
                } else {
                    voice.play(Voice.POUR_CANT)
                    selected = if (tubes[index].segments.isNotEmpty()) index else null
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(KidBackground)) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                BackButton(onClick = onBack)
                Spacer(Modifier.weight(1f))
                Text(
                    "${levelIndex + 1}. pálya",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = KidText,
                )
                Spacer(Modifier.weight(1f))
                FilledIconButton(
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = KidBlueDark),
                    onClick = { reshuffle() },
                ) {
                    Icon(Icons.Filled.Shuffle, contentDescription = "Új keverés", tint = Color.White)
                }
            }

            Box(
                modifier = Modifier.fillMaxSize().padding(top = 64.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    // A horizontalScroll a saját keretére vágja a tartalmát, ezért a lebegő/megdőlt
                    // pohár teteje levágódna. A padding a vágókereten BELÜL ad helyet felül.
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 110.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(TUBE_SPACING_DP.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    val maxCapacity = tubes.maxOf { it.capacity }
                    val pouringNow = pouring

                    // Egyetlen "jelenlét" tényező: 0→1 a lebegés alatt, 1 marad öntés közben,
                    // majd 1→0 amíg a forrás visszatér a helyére.
                    val floatAmount = pouringNow?.let { p ->
                        val rise = phase(p.progress.value, 0f, 0.28f)
                        val fall = phase(p.progress.value, 0.82f, 1f)
                        rise * (1f - fall)
                    } ?: 0f
                    val pourFraction = pouringNow?.let { phase(it.progress.value, 0.28f, 0.82f) } ?: 0f

                    tubes.forEachIndexed { index, tube ->
                        val (baseSegments, partialUnits, partialColor) = when (index) {
                            pouringNow?.from -> Triple(
                                tube.segments.dropLast(pouringNow.amount),
                                pouringNow.amount * (1f - pourFraction),
                                pouringNow.color,
                            )
                            pouringNow?.to -> Triple(
                                tube.segments,
                                pouringNow.amount * pourFraction,
                                pouringNow.color,
                            )
                            else -> Triple(tube.segments, 0f, 0)
                        }

                        val isFloatingSource = pouringNow != null && index == pouringNow.from
                        val dxDp = if (isFloatingSource) {
                            ((pouringNow.to - pouringNow.from) * TUBE_STEP_DP * 0.62f * floatAmount).dp
                        } else 0.dp
                        val liftDp = if (isFloatingSource) (-42f * floatAmount).dp else 0.dp
                        val tiltDeg = if (isFloatingSource) -26f * floatAmount else 0f

                        TubeView(
                            capacity = tube.capacity,
                            baseSegments = baseSegments,
                            partialTopUnits = partialUnits,
                            partialColor = partialColor,
                            maxCapacity = maxCapacity,
                            selected = selected == index,
                            glowing = pouringNow != null && (index == pouringNow.from || index == pouringNow.to),
                            floatOffsetX = dxDp,
                            floatOffsetY = liftDp,
                            tiltDeg = tiltDeg,
                            floating = isFloatingSource,
                            onClick = { onTubeTap(index) },
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(solved) {
        if (solved) voice.play(Voice.POUR_WIN)
    }

    if (solved) {
        WinDialog(onNext = { goNext() })
    }
}

@Composable
private fun TubeView(
    capacity: Int,
    baseSegments: List<Int>,
    partialTopUnits: Float,
    partialColor: Int,
    maxCapacity: Int,
    selected: Boolean,
    glowing: Boolean,
    floatOffsetX: Dp,
    floatOffsetY: Dp,
    tiltDeg: Float,
    floating: Boolean,
    onClick: () -> Unit,
) {
    val unitHeight = 34.dp
    val width = TUBE_WIDTH_DP.dp
    val lift by animateDpAsState(targetValue = if (selected) (-18).dp else 0.dp, label = "tubeLift")
    val tubeShape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 24.dp, bottomEnd = 24.dp)

    val filledUnits = baseSegments.size + partialTopUnits
    val emptyUnits = (maxCapacity - filledUnits).coerceAtLeast(0f)

    Box(
        modifier = Modifier
            .zIndex(if (floating) 10f else 0f)
            .offset(x = floatOffsetX, y = lift + floatOffsetY)
            .rotate(tiltDeg)
            .clickable(onClick = onClick)
            .width(width)
            .height(unitHeight * maxCapacity)
            .clip(tubeShape)
            .background(
                Brush.horizontalGradient(listOf(Color(0xFFF3F8FF), Color(0xFFE2ECF7), Color(0xFFF3F8FF))),
            )
            .border(
                width = if (selected || glowing) 4.dp else 2.dp,
                color = if (glowing) KidYellow else if (selected) KidBlueDark else Color(0xFFCBD3DC),
                shape = tubeShape,
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(unitHeight * emptyUnits))

            if (partialTopUnits > 0.01f) {
                LiquidBand(
                    color = pourColors.getOrElse(partialColor) { Color.Gray },
                    height = unitHeight * partialTopUnits,
                    isSurface = true,
                )
            }

            baseSegments.asReversed().forEachIndexed { i, colorIndex ->
                val isSurface = i == 0 && partialTopUnits <= 0.01f
                LiquidBand(
                    color = pourColors.getOrElse(colorIndex) { Color.Gray },
                    height = unitHeight,
                    isSurface = isSurface,
                )
            }
        }

        // Üveg-fény csík
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(8.dp)
                .align(Alignment.CenterStart)
                .padding(start = 5.dp)
                .background(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.05f))),
                    RoundedCornerShape(50),
                ),
        )
    }
}

@Composable
private fun LiquidBand(color: Color, height: Dp, isSurface: Boolean) {
    val shape = if (isSurface) RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp) else RoundedCornerShape(0.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.horizontalGradient(
                    listOf(color.lighten(0.30f), color, color.darken(0.12f)),
                ),
                shape,
            ),
    )
}

@Composable
private fun WinDialog(onNext: () -> Unit) {
    Dialog(onDismissRequest = {}) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Szuper! Kész a pálya!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = KidGreen,
                )
                Spacer(Modifier.height(20.dp))
                Button(onClick = onNext) {
                    Text("Következő pálya")
                }
            }
        }
    }
}
