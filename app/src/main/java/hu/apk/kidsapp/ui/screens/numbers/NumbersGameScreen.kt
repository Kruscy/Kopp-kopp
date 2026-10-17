package hu.apk.kidsapp.ui.screens.numbers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.apk.kidsapp.ui.components.BackButton
import hu.apk.kidsapp.ui.theme.KidBackground
import hu.apk.kidsapp.ui.theme.KidBlue
import hu.apk.kidsapp.ui.theme.KidText
import hu.apk.kidsapp.ui.screens.writing.WriteItem
import hu.apk.kidsapp.ui.screens.writing.WritingPractice
import hu.apk.kidsapp.util.Step
import hu.apk.kidsapp.util.rememberClipPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val numberColors = listOf(
    Color(0xFFE53935), Color(0xFFFF8F00), Color(0xFFF9A825), Color(0xFF43A047), Color(0xFF00ACC1),
    Color(0xFF1E88E5), Color(0xFF3949AB), Color(0xFF8E44AD), Color(0xFFEC407A), Color(0xFF8D6E63),
)
private val numberWords = listOf("egy", "kettő", "három", "négy", "öt", "hat", "hét", "nyolc", "kilenc", "tíz")
private val objectEmoji = listOf("🍎", "🐟", "🐥", "🚗", "🌸", "⭐", "🐞", "🎈", "🍓", "🦋")
private val findTexts = listOf(
    "Melyik az egy?", "Melyik a kettő?", "Melyik a három?", "Melyik a négy?", "Melyik az öt?",
    "Melyik a hat?", "Melyik a hét?", "Melyik a nyolc?", "Melyik a kilenc?", "Melyik a tíz?",
)

private enum class Mode { LEARN, FIND, WRITE }

private fun praise() = Step.Clip("voice_praise_${Random.nextInt(1, 6)}")

@Composable
fun NumbersGameScreen(onBack: () -> Unit) {
    val player = rememberClipPlayer()
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(Mode.LEARN) }
    var number by remember { mutableIntStateOf(1) }
    val counted = remember { mutableStateListOf<Int>() }
    val confetti = remember { Animatable(1f) }

    // "Keresd meg" játék
    var target by remember { mutableIntStateOf(1) }
    var options by remember { mutableStateOf(listOf(1, 2, 3)) }
    var wrongNumber by remember { mutableIntStateOf(0) }
    var solved by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }

    fun burst() {
        scope.launch {
            confetti.snapTo(0f)
            confetti.animateTo(1f, tween(1400))
        }
    }

    fun newQuestion() {
        val t = Random.nextInt(1, 11)
        val distractors = (1..10).filter { it != t }.shuffled().take(2)
        target = t
        options = (listOf(t) + distractors).shuffled()
        solved = false
        wrongNumber = 0
        player.play(Step.Clip("voice_find_$t"))
    }

    fun selectNumber(n: Int) {
        number = n
        counted.clear()
        player.play(Step.Clip("voice_num_$n"))
    }

    fun tapItem(i: Int) {
        val pos = counted.indexOf(i)
        if (pos >= 0) {
            player.play(Step.Clip("voice_num_${pos + 1}"))
            return
        }
        counted.add(i)
        val n = counted.size
        if (n == number) {
            player.play(Step.Clip("voice_num_$n"), Step.Pause(250), Step.Clip("voice_counted"))
            burst()
        } else {
            player.play(Step.Clip("voice_num_$n"))
        }
    }

    fun answer(n: Int) {
        if (solved) return
        if (n == target) {
            solved = true
            burst()
            player.play(Step.Clip("voice_num_$n"), Step.Pause(200), praise())
            scope.launch {
                delay(2800)
                if (mode == Mode.FIND) newQuestion()
            }
        } else {
            wrongNumber = n
            player.play(Step.Clip("voice_try_again"))
            scope.launch {
                repeat(3) {
                    shake.animateTo(14f, tween(50))
                    shake.animateTo(-14f, tween(50))
                }
                shake.animateTo(0f, tween(50))
                wrongNumber = 0
            }
        }
    }

    LaunchedEffect(Unit) { player.play(Step.Clip("voice_pick_number")) }

    if (mode == Mode.WRITE) {
        val writeItems = remember { (1..10).map { WriteItem(it.toString(), numberColors[it - 1], "voice_num_$it") } }
        WritingPractice(
            items = writeItems,
            startIndex = number - 1,
            onIndexChange = { number = it + 1 },
            onExit = {
                mode = Mode.LEARN
                counted.clear()
                player.play(Step.Clip("voice_pick_number"))
            },
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(KidBackground)) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                BackButton(onClick = onBack)
                Spacer(Modifier.weight(1f))
                Text(
                    "Számok",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = KidText,
                )
                Spacer(Modifier.weight(1f))
                ModeChip("📖", "Tanulom", mode == Mode.LEARN) {
                    mode = Mode.LEARN
                    counted.clear()
                    player.play(Step.Clip("voice_pick_number"))
                }
                Spacer(Modifier.width(10.dp))
                ModeChip("🎯", "Keresd!", mode == Mode.FIND) {
                    mode = Mode.FIND
                    newQuestion()
                }
                Spacer(Modifier.width(10.dp))
                ModeChip("✏️", "Írom", false) {
                    player.stop()
                    mode = Mode.WRITE
                }
            }

            Spacer(Modifier.height(8.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (mode) {
                    Mode.LEARN -> LearnContent(
                        number = number,
                        counted = counted,
                        onTapNumeral = { player.play(Step.Clip("voice_num_$number")) },
                        onTapItem = { tapItem(it) },
                        onReset = { counted.clear() },
                    )
                    Mode.WRITE -> Unit
                    Mode.FIND -> FindContent(
                        target = target,
                        options = options,
                        wrongNumber = wrongNumber,
                        shake = shake.value,
                        solved = solved,
                        onReplay = { player.play(Step.Clip("voice_find_$target")) },
                        onAnswer = { answer(it) },
                    )
                }
                Confetti(progress = confetti.value)
            }

            if (mode == Mode.LEARN) {
                Spacer(Modifier.height(8.dp))
                NumberStrip(selected = number, onSelect = { selectNumber(it) })
            }
        }
    }
}

@Composable
private fun ModeChip(emoji: String, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(26.dp))
            .background(if (selected) KidBlue else Color.White)
            .border(2.dp, if (selected) KidBlue else Color(0xFFD5DAE8), RoundedCornerShape(26.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 24.sp)
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else KidText)
    }
}

@Composable
private fun LearnContent(
    number: Int,
    counted: List<Int>,
    onTapNumeral: () -> Unit,
    onTapItem: (Int) -> Unit,
    onReset: () -> Unit,
) {
    val color = numberColors[number - 1]
    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        // A nagy számjegy
        Column(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White)
                .clickable(onClick = onTapNumeral)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                number.toString(),
                fontSize = 200.sp,
                fontWeight = FontWeight.Black,
                color = color,
            )
            Text(numberWords[number - 1], fontSize = 38.sp, fontWeight = FontWeight.Bold, color = color)
        }

        // Tárgyak: koppintva megszámolhatók + számkép
        Column(
            modifier = Modifier
                .weight(1.5f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val rows = (0 until number).chunked(5)
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { i ->
                                CountItem(
                                    emoji = objectEmoji[number - 1],
                                    ordinal = counted.indexOf(i) + 1,
                                    color = color,
                                    index = i,
                                    onClick = { onTapItem(i) },
                                )
                            }
                        }
                    }
                }
            }
            TenFrame(number = number, counted = counted.size, color = color)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFFF1F3F8))
                    .clickable(onClick = onReset)
                    .padding(horizontal = 18.dp, vertical = 6.dp),
            ) {
                Text("🔄", fontSize = 22.sp)
            }
        }
    }
}

@Composable
private fun CountItem(emoji: String, ordinal: Int, color: Color, index: Int, onClick: () -> Unit) {
    val isCounted = ordinal > 0
    val scale by animateFloatAsState(
        targetValue = if (isCounted) 1.0f else 0.92f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 300f),
        label = "itemScale",
    )
    val wobble = rememberInfiniteTransition(label = "wobble")
    val angle by wobble.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            tween(700 + index * 37, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "angle",
    )
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (isCounted) color.copy(alpha = 0.18f) else Color(0xFFF6F7FB))
            .border(if (isCounted) 4.dp else 0.dp, if (isCounted) color else Color.Transparent, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            emoji,
            fontSize = 54.sp,
            modifier = Modifier.scale(if (isCounted) 1.12f else scale).rotate(if (isCounted) 0f else angle),
        )
        if (isCounted) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(34.dp)
                    .background(color, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(ordinal.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

/** Tízes keret (számkép): a mennyiséget pontokkal is mutatja. */
@Composable
private fun TenFrame(number: Int, counted: Int, color: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF1F3F8))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (r in 0 until 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (c in 0 until 5) {
                    val idx = r * 5 + c
                    val filled = idx < number
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(
                                when {
                                    !filled -> Color.White
                                    idx < counted -> color
                                    else -> color.copy(alpha = 0.45f)
                                },
                                CircleShape,
                            )
                            .border(2.dp, Color(0xFFD5DAE8), CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberStrip(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (n in 1..10) {
            val isSel = n == selected
            val scale by animateFloatAsState(if (isSel) 1.08f else 1f, label = "stripScale")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(78.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(22.dp))
                    .background(numberColors[n - 1])
                    .border(if (isSel) 5.dp else 0.dp, if (isSel) Color.White else Color.Transparent, RoundedCornerShape(22.dp))
                    .clickable { onSelect(n) },
                contentAlignment = Alignment.Center,
            ) {
                Text(n.toString(), fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
    }
}

@Composable
private fun FindContent(
    target: Int,
    options: List<Int>,
    wrongNumber: Int,
    shake: Float,
    solved: Boolean,
    onReplay: () -> Unit,
    onAnswer: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .clickable(onClick = onReplay)
                .padding(horizontal = 28.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🔊", fontSize = 40.sp)
            Spacer(Modifier.width(14.dp))
            Text(findTexts[target - 1], fontSize = 40.sp, fontWeight = FontWeight.Bold, color = KidText)
        }
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            options.forEach { n ->
                val isTarget = n == target
                val showCorrect = solved && isTarget
                val isWrong = wrongNumber == n
                Box(
                    modifier = Modifier
                        .offset(x = if (isWrong) shake.dp else 0.dp)
                        .size(width = 220.dp, height = 250.dp)
                        .scale(if (showCorrect) 1.08f else 1f)
                        .clip(RoundedCornerShape(36.dp))
                        .background(Color.White)
                        .border(
                            6.dp,
                            when {
                                showCorrect -> Color(0xFF43A047)
                                isWrong -> Color(0xFFE53935)
                                else -> numberColors[n - 1].copy(alpha = 0.35f)
                            },
                            RoundedCornerShape(36.dp),
                        )
                        .clickable { onAnswer(n) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        n.toString(),
                        fontSize = 150.sp,
                        fontWeight = FontWeight.Black,
                        color = numberColors[n - 1],
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** Színes konfetti a sikerhez. */
@Composable
private fun Confetti(progress: Float) {
    if (progress >= 1f) return
    Canvas(modifier = Modifier.fillMaxSize()) {
        val c = Offset(size.width / 2f, size.height * 0.45f)
        val ease = 1f - (1f - progress) * (1f - progress)
        for (i in 0 until 28) {
            val a = i * (2.0 * Math.PI / 28)
            val dist = ease * size.minDimension * (0.35f + 0.25f * ((i * 7) % 5) / 4f)
            val p = Offset(c.x + dist * cos(a).toFloat(), c.y + dist * sin(a).toFloat() + progress * progress * 120f)
            drawCircle(
                numberColors[i % numberColors.size].copy(alpha = (1f - progress).coerceIn(0f, 1f)),
                radius = 10f + (i % 3) * 5f,
                center = p,
            )
        }
    }
}
