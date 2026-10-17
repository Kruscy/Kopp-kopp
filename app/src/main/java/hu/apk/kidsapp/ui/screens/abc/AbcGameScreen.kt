package hu.apk.kidsapp.ui.screens.abc

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.apk.kidsapp.ui.components.BackButton
import hu.apk.kidsapp.ui.theme.KidBackground
import hu.apk.kidsapp.ui.theme.KidBlue
import hu.apk.kidsapp.ui.theme.KidBlueDark
import hu.apk.kidsapp.ui.theme.KidText
import hu.apk.kidsapp.ui.screens.writing.WriteItem
import hu.apk.kidsapp.ui.screens.writing.WritingPractice
import hu.apk.kidsapp.util.Step
import hu.apk.kidsapp.util.rememberClipPlayer
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Egy betű: azonosító (a hangfájlhoz), nagy/kis alak, a szó, a kép, a kiemelt rész és a kimondott mondat. */
private class Abc(
    val id: String,
    val upper: String,
    val lower: String,
    val word: String,
    val emoji: String,
    val hlStart: Int,
    val hlLen: Int,
    val spoken: String,
)

private fun abc(id: String, upper: String, lower: String, word: String, emoji: String, spoken: String, hlStart: Int = 0, hlLen: Int = lower.length) =
    Abc(id, upper, lower, word, emoji, hlStart, hlLen, spoken)

// A magyar ábécé 44 betűje, a megszokott sorrendben.
private val letters = listOf(
    abc("a", "A", "a", "alma", "🍎", "A mint alma."),
    abc("a2", "Á", "á", "ágy", "🛏️", "Á mint ágy."),
    abc("b", "B", "b", "banán", "🍌", "Bé mint banán."),
    abc("c", "C", "c", "citrom", "🍋", "Cé mint citrom."),
    abc("cs", "Cs", "cs", "csiga", "🐌", "Csé mint csiga."),
    abc("d", "D", "d", "dinnye", "🍉", "Dé mint dinnye."),
    abc("dz", "Dz", "dz", "madzag", "🧵", "Dzé mint madzag.", hlStart = 2),
    abc("dzs", "Dzs", "dzs", "dzsungel", "🌴", "Dzsé mint dzsungel."),
    abc("e", "E", "e", "egér", "🐭", "E mint egér."),
    abc("e2", "É", "é", "étel", "🍲", "É mint étel."),
    abc("f", "F", "f", "fa", "🌳", "Ef mint fa."),
    abc("g", "G", "g", "gomba", "🍄", "Gé mint gomba."),
    abc("gy", "Gy", "gy", "gyertya", "🕯️", "Gyé mint gyertya."),
    abc("h", "H", "h", "hal", "🐟", "Há mint hal."),
    abc("i", "I", "i", "iskola", "🏫", "I mint iskola."),
    abc("i2", "Í", "í", "íj", "🏹", "Í mint íj."),
    abc("j", "J", "j", "jégkrém", "🍦", "Jé mint jégkrém."),
    abc("k", "K", "k", "kutya", "🐶", "Ká mint kutya."),
    abc("l", "L", "l", "léggömb", "🎈", "El mint léggömb."),
    abc("ly", "Ly", "ly", "lyuk", "🕳️", "Elly mint lyuk."),
    abc("m", "M", "m", "majom", "🐵", "Em mint majom."),
    abc("n", "N", "n", "nap", "☀️", "En mint nap."),
    abc("ny", "Ny", "ny", "nyuszi", "🐰", "Eny mint nyuszi."),
    abc("o", "O", "o", "olló", "✂️", "O mint olló."),
    abc("o2", "Ó", "ó", "óra", "⏰", "Ó mint óra."),
    abc("oe", "Ö", "ö", "ökör", "🐂", "Ö mint ökör."),
    abc("oe2", "Ő", "ő", "őz", "🦌", "Ő mint őz."),
    abc("p", "P", "p", "pillangó", "🦋", "Pé mint pillangó."),
    abc("q", "Q", "q", "quiz", "❓", "Kú mint kvíz."),
    abc("r", "R", "r", "répa", "🥕", "Er mint répa."),
    abc("s", "S", "s", "sajt", "🧀", "Es mint sajt."),
    abc("sz", "Sz", "sz", "szív", "❤️", "Esz mint szív."),
    abc("t", "T", "t", "tehén", "🐄", "Té mint tehén."),
    abc("ty", "Ty", "ty", "tyúk", "🐔", "Tyé mint tyúk."),
    abc("u", "U", "u", "uszoda", "🏊", "U mint uszoda."),
    abc("u2", "Ú", "ú", "újság", "📰", "Ú mint újság."),
    abc("ue", "Ü", "ü", "ünnep", "🎉", "Ü mint ünnep."),
    abc("ue2", "Ű", "ű", "űrhajó", "🚀", "Ű mint űrhajó."),
    abc("v", "V", "v", "vonat", "🚂", "Vé mint vonat."),
    abc("w", "W", "w", "waffle", "🧇", "Dupla vé mint vaffli."),
    abc("x", "X", "x", "xerox", "🖨️", "Iksz mint xerox."),
    abc("y", "Y", "y", "yacht", "⛵", "Ipszilon mint jacht."),
    abc("z", "Z", "z", "zebra", "🦓", "Zé mint zebra."),
    abc("zs", "Zs", "zs", "zsiráf", "🦒", "Zsé mint zsiráf."),
)

private val palette = listOf(
    Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFF9A825), Color(0xFF43A047), Color(0xFF00ACC1),
    Color(0xFF1E88E5), Color(0xFF3949AB), Color(0xFF8E44AD), Color(0xFFEC407A),
)

private enum class Phase { IDLE, LISTEN, ASK, YOUR_TURN, PRAISE }

@Composable
fun AbcGameScreen(onBack: () -> Unit) {
    val player = rememberClipPlayer()
    val scope = rememberCoroutineScope()
    var index by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(Phase.IDLE) }
    var started by remember { mutableStateOf(false) }
    val confetti = remember { Animatable(1f) }
    val pop = remember { Animatable(0f) }

    fun select(i: Int) {
        index = i
        started = true
        val id = letters[i].id
        val praiseClip = "voice_praise_${Random.nextInt(1, 6)}"
        scope.launch {
            pop.snapTo(1f)
            pop.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 260f))
        }
        // 1) kimondja: "A mint alma"  2) megkéri: "Most te mondd vissza!"  3) újra bemondja
        // 4) vár egy kicsit  5) megdicséri
        player.play(
            Step.Mark { phase = Phase.LISTEN },
            Step.Clip("voice_abc_$id"),
            Step.Pause(700),
            Step.Mark { phase = Phase.ASK },
            Step.Clip("voice_say_back"),
            Step.Pause(350),
            Step.Clip("voice_abc_$id"),
            Step.Mark { phase = Phase.YOUR_TURN },
            Step.Pause(4500),
            Step.Mark {
                phase = Phase.PRAISE
                scope.launch {
                    confetti.snapTo(0f)
                    confetti.animateTo(1f, tween(1500))
                }
            },
            Step.Clip(praiseClip),
            Step.Pause(1200),
            Step.Mark { phase = Phase.IDLE },
        )
    }

    LaunchedEffect(Unit) { player.play(Step.Clip("voice_pick_letter")) }

    var writing by remember { mutableStateOf(false) }
    var writeUpper by remember { mutableStateOf(true) }
    if (writing) {
        val writeItems = remember(writeUpper) {
            letters.mapIndexed { i, l ->
                WriteItem(if (writeUpper) l.upper else l.lower, palette[i % palette.size], "voice_abc_${l.id}")
            }
        }
        WritingPractice(
            items = writeItems,
            startIndex = index,
            onIndexChange = { index = it },
            onExit = {
                writing = false
                started = false
                phase = Phase.IDLE
                player.play(Step.Clip("voice_pick_letter"))
            },
            headerExtra = {
                WriteCaseChip("A", writeUpper) { writeUpper = true }
                Spacer(Modifier.width(8.dp))
                WriteCaseChip("a", !writeUpper) { writeUpper = false }
            },
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(KidBackground)) {
        Row(modifier = Modifier.fillMaxSize()) {
            // ---- Színpad ----
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Stage(
                    letter = letters[index],
                    color = palette[index % palette.size],
                    phase = phase,
                    started = started,
                    pop = pop.value,
                    onReplay = { select(index) },
                    onPrev = { select((index + letters.size - 1) % letters.size) },
                    onNext = { select((index + 1) % letters.size) },
                )
                Confetti(confetti.value)
            }

            // ---- A teljes ábécé ----
            Surface(
                modifier = Modifier.width(352.dp).fillMaxHeight(),
                shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
                color = Color.White,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(letters) { i, l ->
                        val sel = i == index && started
                        val c = palette[i % palette.size]
                        Box(
                            modifier = Modifier
                                .height(70.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (sel) c else c.copy(alpha = 0.16f))
                                .border(if (sel) 4.dp else 0.dp, if (sel) KidBlueDark else Color.Transparent, RoundedCornerShape(18.dp))
                                .clickable { select(i) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${l.upper} ${l.lower}",
                                fontSize = (if (l.upper.length + l.lower.length > 4) 17 else if (l.upper.length + l.lower.length > 2) 22 else 28).sp,
                                fontWeight = FontWeight.Black,
                                color = if (sel) Color.White else c,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }
        }
        Box(modifier = Modifier.padding(24.dp)) { BackButton(onClick = onBack) }
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 376.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(KidBlue)
                .clickable {
                    player.stop()
                    writing = true
                }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("✏️", fontSize = 24.sp)
            Spacer(Modifier.width(8.dp))
            Text("Írom", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun WriteCaseChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(if (selected) KidBlue else Color.White, CircleShape)
            .border(3.dp, if (selected) KidBlue else Color(0xFFD5DAE8), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 26.sp, fontWeight = FontWeight.Black, color = if (selected) Color.White else KidText)
    }
}

@Composable
private fun Stage(
    letter: Abc,
    color: Color,
    phase: Phase,
    started: Boolean,
    pop: Float,
    onReplay: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // A két kártya + két nyíl mindig elférjen a színpadon (kisebb kijelzőn kisebb kártyák).
        val arrow = 56.dp
        val gap = 12.dp
        val cardW = ((maxWidth - 32.dp - arrow * 2 - gap * 3) / 2).coerceIn(140.dp, 260.dp)
        val cardH = cardW * 1.12f
        val k = cardW / 260.dp

        Column(
            modifier = Modifier.fillMaxSize().padding(top = 96.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(gap)) {
                ArrowButton("◀", arrow, onPrev)
                // A nagy betű
                Column(
                    modifier = Modifier
                        .scale(1f + 0.07f * pop)
                        .size(width = cardW, height = cardH)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White)
                        .border(6.dp, color.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                        .clickable(onClick = onReplay),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        letter.upper,
                        fontSize = (if (letter.upper.length > 2) 100f else if (letter.upper.length == 2) 130f else 170f).times(k).sp,
                        fontWeight = FontWeight.Black,
                        color = color,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Text(
                        letter.lower,
                        fontSize = (if (letter.lower.length > 2) 44f else 56f).times(k).sp,
                        fontWeight = FontWeight.Bold,
                        color = color.copy(alpha = 0.8f),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                // A kép
                Box(
                    modifier = Modifier
                        .scale(1f + 0.07f * pop)
                        .size(width = cardW, height = cardH)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White)
                        .border(6.dp, color.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                        .clickable(onClick = onReplay),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(letter.emoji, fontSize = (130f * k).sp)
                }
                ArrowButton("▶", arrow, onNext)
            }
            Spacer(Modifier.height(14.dp))
            // A szó, a kezdőbetű kiemelve
            val word = letter.word
            val st = letter.hlStart.coerceIn(0, word.length)
            val en = (st + letter.hlLen).coerceIn(st, word.length)
            Text(
                buildAnnotatedString {
                    append(word.substring(0, st))
                    withStyle(SpanStyle(color = color, fontWeight = FontWeight.Black)) { append(word.substring(st, en)) }
                    append(word.substring(en))
                },
                fontSize = 50.sp,
                fontWeight = FontWeight.Bold,
                color = KidText,
                maxLines = 1,
            )
            Text(letter.spoken.trimEnd('.'), fontSize = 22.sp, color = KidText.copy(alpha = 0.7f), maxLines = 1)
            Spacer(Modifier.height(8.dp))
            PhaseBubble(phase = phase, started = started)
        }
    }
}

@Composable
private fun ArrowButton(label: String, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .background(Color.White, CircleShape)
            .border(3.dp, Color(0xFFD5DAE8), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 26.sp, color = KidText)
    }
}

/** Jelzi, hogy most hallgatni, mondani vagy örülni kell. */
@Composable
private fun PhaseBubble(phase: Phase, started: Boolean) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val k by pulse.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "k",
    )
    val (emoji, text, bg) = when {
        !started -> Triple("👆", "Koppints egy betűre!", Color(0xFFE3F2FD))
        phase == Phase.LISTEN -> Triple("👂", "Hallgasd!", Color(0xFFE3F2FD))
        phase == Phase.ASK -> Triple("🗣️", "Most te mondd!", Color(0xFFFFF3E0))
        phase == Phase.YOUR_TURN -> Triple("🎤", "Most te jössz!", Color(0xFFFFF3E0))
        phase == Phase.PRAISE -> Triple("🌟", "Ügyes vagy!", Color(0xFFE8F5E9))
        else -> Triple("🔁", "Koppints a képre, és újra hallod!", Color(0xFFF1F3F8))
    }
    Row(
        modifier = Modifier
            .scale(if (phase == Phase.YOUR_TURN || phase == Phase.ASK) k else 1f)
            .clip(RoundedCornerShape(30.dp))
            .background(bg)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 34.sp)
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = KidText)
    }
}

@Composable
private fun Confetti(progress: Float) {
    if (progress >= 1f) return
    Canvas(modifier = Modifier.fillMaxSize()) {
        val c = Offset(size.width / 2f, size.height * 0.42f)
        val ease = 1f - (1f - progress) * (1f - progress)
        for (i in 0 until 30) {
            val a = i * (2.0 * Math.PI / 30)
            val dist = ease * size.minDimension * (0.35f + 0.28f * ((i * 7) % 5) / 4f)
            val p = Offset(c.x + dist * cos(a).toFloat(), c.y + dist * sin(a).toFloat() + progress * progress * 110f)
            drawCircle(
                palette[i % palette.size].copy(alpha = (1f - progress).coerceIn(0f, 1f)),
                radius = 10f + (i % 3) * 5f,
                center = p,
            )
        }
    }
}
