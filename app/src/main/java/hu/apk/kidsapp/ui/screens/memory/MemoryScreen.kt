package hu.apk.kidsapp.ui.screens.memory

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.apk.kidsapp.ui.components.BackButton
import hu.apk.kidsapp.ui.screens.language.animals
import hu.apk.kidsapp.ui.theme.KidBackground
import hu.apk.kidsapp.ui.theme.KidText
import hu.apk.kidsapp.util.SoundFx
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class MemCard(val animal: Int) {
    var faceUp by mutableStateOf(false)
    var matched by mutableStateOf(false)
}

// párok száma szintenként: 3, 4, 6, majd mindig 8; (oszlop, sor)
private val levelPairs = intArrayOf(3, 4, 6, 8)

private fun gridFor(pairs: Int): Pair<Int, Int> = when (pairs) {
    3 -> 3 to 2
    4 -> 4 to 2
    6 -> 4 to 3
    else -> 4 to 4
}

private fun deal(pairs: Int): List<MemCard> {
    val picks = animals.indices.shuffled().take(pairs)
    return (picks + picks).shuffled().map { MemCard(it) }
}

private val cardBackTop = Color(0xFF7E57C2)
private val cardBackBottom = Color(0xFF5C6BC0)

/** Memóriajáték (párkereső): állatos kártyákat kell párba találni; a nehézség szintenként nő. */
@Composable
fun MemoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sfx = remember { SoundFx(context.applicationContext) }
    DisposableEffect(sfx) { onDispose { sfx.release() } }
    val scope = rememberCoroutineScope()

    var level by remember { mutableIntStateOf(0) }
    var stars by remember { mutableIntStateOf(0) }
    val cards = remember { mutableStateListOf<MemCard>().apply { addAll(deal(levelPairs[0])) } }
    var first by remember { mutableIntStateOf(-1) }
    var busy by remember { mutableStateOf(false) }
    var won by remember { mutableStateOf(false) }

    fun nextRound() {
        won = false
        first = -1
        busy = false
        level = (level + 1).coerceAtMost(levelPairs.size - 1)
        cards.clear()
        cards.addAll(deal(levelPairs[level]))
    }

    fun tap(i: Int) {
        val c = cards[i]
        if (busy || won || c.faceUp || c.matched) return
        c.faceUp = true
        sfx.play("whoosh", rate = 1.6f, volume = 0.5f)
        if (first < 0) {
            first = i
            return
        }
        val a = cards[first]
        first = -1
        busy = true
        scope.launch {
            if (a.animal == c.animal) {
                delay(500)
                a.matched = true
                c.matched = true
                stars++
                sfx.play("ding")
                if (cards.all { it.matched }) {
                    delay(500)
                    won = true
                    sfx.play("win")
                    delay(2600)
                    nextRound()
                }
            } else {
                delay(1100)
                a.faceUp = false
                c.faceUp = false
                sfx.play("pop", rate = 0.7f, volume = 0.5f)
            }
            busy = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(KidBackground).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            BackButton(onClick = onBack)
            Spacer(Modifier.weight(1f))
            Text("Memória", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = KidText)
            Spacer(Modifier.weight(1f))
            Text(
                "⭐ $stars",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = KidText,
                modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(Color.White).padding(horizontal = 18.dp, vertical = 8.dp),
            )
        }
        Spacer(Modifier.size(16.dp))
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val (cols, rows) = gridFor(cards.size / 2)
            val gap = 16.dp
            val cell = minOf((maxWidth - gap * (cols - 1)) / cols, (maxHeight - gap * (rows - 1)) / rows)
            Column(verticalArrangement = Arrangement.spacedBy(gap), horizontalAlignment = Alignment.CenterHorizontally) {
                for (r in 0 until rows) {
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        for (cc in 0 until cols) {
                            val i = r * cols + cc
                            if (i < cards.size) {
                                CardView(cards[i], Modifier.size(cell)) { tap(i) }
                            }
                        }
                    }
                }
            }
            if (won) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Ügyes vagy! 🌟",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = KidText,
                        modifier = Modifier.clip(RoundedCornerShape(36.dp)).background(Color(0xE6FFFFFF)).padding(horizontal = 40.dp, vertical = 24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CardView(card: MemCard, modifier: Modifier, onClick: () -> Unit) {
    val up = card.faceUp || card.matched
    val rot by animateFloatAsState(if (up) 180f else 0f, tween(380), label = "flip")
    val density = LocalDensity.current.density
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(card.matched) {
        if (card.matched) {
            bounce.animateTo(1.12f, tween(160))
            bounce.animateTo(1f, tween(220))
        }
    }
    Box(
        modifier = modifier
            .scale(bounce.value)
            .graphicsLayer {
                rotationY = rot
                cameraDistance = 14f * density
            }
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (rot <= 90f) {
            Box(
                modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(cardBackTop, cardBackBottom))),
                contentAlignment = Alignment.Center,
            ) {
                Text("?", fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, color = Color.White.copy(alpha = 0.85f))
            }
        } else {
            Image(
                painter = painterResource(animals[card.animal].image),
                contentDescription = animals[card.animal].hu,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
                    .then(if (card.matched) Modifier.border(6.dp, Color(0xFF66BB6A), RoundedCornerShape(22.dp)) else Modifier),
            )
        }
    }
}
