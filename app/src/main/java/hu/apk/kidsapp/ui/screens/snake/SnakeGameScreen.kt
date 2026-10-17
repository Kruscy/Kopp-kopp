package hu.apk.kidsapp.ui.screens.snake

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hu.apk.kidsapp.game.snake.Direction
import hu.apk.kidsapp.game.snake.initialSnakeState
import hu.apk.kidsapp.game.snake.stepSnake
import hu.apk.kidsapp.ui.components.BackButton
import hu.apk.kidsapp.ui.theme.KidBackground
import hu.apk.kidsapp.ui.theme.KidBlueDark
import hu.apk.kidsapp.ui.theme.KidGreen
import hu.apk.kidsapp.ui.theme.KidRed
import hu.apk.kidsapp.ui.theme.KidText
import hu.apk.kidsapp.util.Voice
import hu.apk.kidsapp.util.rememberVoicePlayer
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

private const val TICK_MS = 700L
private const val CELL_DP = 28

@Composable
fun SnakeGameScreen(onBack: () -> Unit) {
    val voice = rememberVoicePlayer()
    Box(modifier = Modifier.fillMaxSize().background(KidBackground)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val cellPx = with(density) { CELL_DP.dp.toPx() }
            val availW = with(density) { maxWidth.toPx() }
            val availH = with(density) { maxHeight.toPx() }
            val topMarginPx = with(density) { 110.dp.toPx() }

            val cols = (availW / cellPx).toInt().coerceAtLeast(6)
            val rows = ((availH - topMarginPx) / cellPx).toInt().coerceAtLeast(6)

            var state by remember(cols, rows) { mutableStateOf(initialSnakeState(cols, rows, Random.Default)) }
            var direction by remember(cols, rows) { mutableStateOf(Direction.RIGHT) }

            LaunchedEffect(cols, rows) {
                while (true) {
                    delay(TICK_MS)
                    state = stepSnake(state, direction, cols, rows, Random.Default)
                }
            }

            LaunchedEffect(state.score) {
                if (state.score > 0) voice.play(Voice.SNAKE_NYAM)
            }

            fun turn(newDir: Direction) {
                if (!newDir.isOpposite(direction)) direction = newDir
            }

            var dragAnchor by remember { mutableStateOf<Offset?>(null) }
            val steerThreshold = with(density) { 18.dp.toPx() }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { start -> dragAnchor = start },
                            onDrag = { change, _ ->
                                change.consume()
                                val anchor = dragAnchor ?: change.position
                                val delta = change.position - anchor
                                if (delta.getDistance() > steerThreshold) {
                                    val newDir = if (abs(delta.x) > abs(delta.y)) {
                                        if (delta.x > 0) Direction.RIGHT else Direction.LEFT
                                    } else {
                                        if (delta.y > 0) Direction.DOWN else Direction.UP
                                    }
                                    turn(newDir)
                                    dragAnchor = change.position
                                }
                            },
                            onDragEnd = { dragAnchor = null },
                            onDragCancel = { dragAnchor = null },
                        )
                    }
            ) {
                val gridTop = topMarginPx
                state.snake.forEachIndexed { index, pos ->
                    val center = Offset(pos.x * cellPx + cellPx / 2f, gridTop + pos.y * cellPx + cellPx / 2f)
                    val isHead = index == 0
                    drawCircle(
                        color = if (isHead) KidGreen else KidGreen.copy(alpha = 0.85f),
                        radius = cellPx / 2f * 0.9f,
                        center = center,
                    )
                    if (isHead) {
                        val eyeOffset = cellPx * 0.18f
                        drawCircle(Color.White, radius = cellPx * 0.12f, center = center + Offset(-eyeOffset, -eyeOffset))
                        drawCircle(Color.White, radius = cellPx * 0.12f, center = center + Offset(eyeOffset, -eyeOffset))
                        drawCircle(Color.Black, radius = cellPx * 0.06f, center = center + Offset(-eyeOffset, -eyeOffset))
                        drawCircle(Color.Black, radius = cellPx * 0.06f, center = center + Offset(eyeOffset, -eyeOffset))
                    }
                }
                val fruitCenter = Offset(
                    state.fruit.x * cellPx + cellPx / 2f,
                    gridTop + state.fruit.y * cellPx + cellPx / 2f,
                )
                drawCircle(KidRed, radius = cellPx / 2f * 0.75f, center = fruitCenter)
            }

            Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                BackButton(onClick = onBack)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "Pontszám: ${state.score}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = KidText,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                    FilledIconButton(
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = KidBlueDark),
                        onClick = {
                            state = initialSnakeState(cols, rows, Random.Default)
                            direction = Direction.RIGHT
                        },
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Új játék", tint = Color.White)
                    }
                }
            }
        }
    }
}
