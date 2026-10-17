package hu.apk.kidsapp.ui.screens.language

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.apk.kidsapp.ui.components.BackButton
import hu.apk.kidsapp.ui.theme.KidBackground
import hu.apk.kidsapp.ui.theme.KidBlueDark
import hu.apk.kidsapp.ui.theme.KidText
import hu.apk.kidsapp.util.Step
import hu.apk.kidsapp.util.rememberClipPlayer
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** A tanulható nyelvek. [code] a hangfájlok előtagja (lang_en_..., lang_de_...). */
private enum class Lang(val code: String, val title: String, val nativeName: String, val huName: String, val startClip: String) {
    EN("en", "English", "English", "Angol", "voice_lang_en_start"),
    DE("de", "Deutsch", "Deutsch", "Német", "voice_lang_de_start"),
}

private val palette = listOf(
    Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFF9A825), Color(0xFF43A047), Color(0xFF00ACC1),
    Color(0xFF1E88E5), Color(0xFF3949AB), Color(0xFF8E44AD), Color(0xFFEC407A),
)

private val derColor = Color(0xFF1E88E5)
private val dieColor = Color(0xFFE53935)
private val dasColor = Color(0xFF43A047)

private fun articleColor(article: String) = when (article) {
    "der" -> derColor
    "die" -> dieColor
    else -> dasColor
}

/** Nyelvtanulás: először zászlóval nyelvet választunk, utána állatokat tanulunk az adott nyelven. */
@Composable
fun LanguageGameScreen(onBack: () -> Unit) {
    var lang by remember { mutableStateOf<Lang?>(null) }
    val l = lang
    if (l == null) {
        LanguagePicker(onBack = onBack, onPick = { lang = it })
    } else {
        AnimalLearn(lang = l, onChangeLanguage = { lang = null })
    }
}

@Composable
private fun LanguagePicker(onBack: () -> Unit, onPick: (Lang) -> Unit) {
    val player = rememberClipPlayer()
    LaunchedEffect(Unit) { player.play(Step.Clip("voice_lang_pick_flag")) }

    Box(modifier = Modifier.fillMaxSize().background(KidBackground)) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                BackButton(onClick = onBack)
                Spacer(Modifier.weight(1f))
                Text(
                    "Nyelvek",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = KidText,
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(1.dp))
            }
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Lang.entries.forEach { l ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(36.dp))
                            .background(Color.White)
                            .clickable {
                                player.play(Step.Clip(l.startClip))
                                onPick(l)
                            }
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Flag(l, Modifier.fillMaxWidth().aspectRatio(1.5f).clip(RoundedCornerShape(18.dp)).border(3.dp, Color(0xFFD5DAE8), RoundedCornerShape(18.dp)))
                        Spacer(Modifier.height(14.dp))
                        Text(l.nativeName, fontSize = 40.sp, fontWeight = FontWeight.Black, color = KidText)
                        Text(l.huName, fontSize = 24.sp, color = KidText.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

/** Zászlók kirajzolva (nem emoji, hogy minden eszközön ugyanúgy látsszon). */
@Composable
private fun Flag(lang: Lang, modifier: Modifier) {
    Canvas(modifier = modifier) {
        when (lang) {
            Lang.DE -> drawGermanFlag()
            Lang.EN -> drawUkFlag()
        }
    }
}

private fun DrawScope.drawGermanFlag() {
    val h = size.height / 3f
    drawRect(Color(0xFF000000), Offset.Zero, Size(size.width, h))
    drawRect(Color(0xFFDD0000), Offset(0f, h), Size(size.width, h))
    drawRect(Color(0xFFFFCE00), Offset(0f, 2 * h), Size(size.width, h + 1f))
}

private fun DrawScope.drawUkFlag() {
    val w = size.width
    val h = size.height
    val blue = Color(0xFF012169)
    val red = Color(0xFFC8102E)
    drawRect(blue)
    // fehér átlók, majd vékonyabb piros átlók
    val tl = Offset.Zero
    val br = Offset(w, h)
    val tr = Offset(w, 0f)
    val bl = Offset(0f, h)
    drawLine(Color.White, tl, br, strokeWidth = h * 0.22f)
    drawLine(Color.White, tr, bl, strokeWidth = h * 0.22f)
    drawLine(red, tl, br, strokeWidth = h * 0.09f)
    drawLine(red, tr, bl, strokeWidth = h * 0.09f)
    // fehér, majd piros kereszt
    drawRect(Color.White, Offset(0f, h * 0.5f - h * 0.17f), Size(w, h * 0.34f))
    drawRect(Color.White, Offset(w * 0.5f - h * 0.17f, 0f), Size(h * 0.34f, h))
    drawRect(red, Offset(0f, h * 0.5f - h * 0.1f), Size(w, h * 0.2f))
    drawRect(red, Offset(w * 0.5f - h * 0.1f, 0f), Size(h * 0.2f, h))
}

private enum class Phase { IDLE, LISTEN, ASK, YOUR_TURN, PRAISE }

@Composable
private fun AnimalLearn(lang: Lang, onChangeLanguage: () -> Unit) {
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
        val clip = "lang_${lang.code}_${animals[i].id}"
        val praiseClip = "voice_praise_${Random.nextInt(1, 6)}"
        scope.launch {
            pop.snapTo(1f)
            pop.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 260f))
        }
        // 1) kimondja az állatot  2) "Most te mondd vissza!"  3) újra bemondja  4) vár  5) megdicséri
        player.play(
            Step.Mark { phase = Phase.LISTEN },
            Step.Clip(clip),
            Step.Pause(700),
            Step.Mark { phase = Phase.ASK },
            Step.Clip("voice_say_back"),
            Step.Pause(350),
            Step.Clip(clip),
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

    LaunchedEffect(lang) {
        player.play(Step.Clip(lang.startClip), Step.Pause(200), Step.Clip("voice_lang_pick_animal"))
    }

    Box(modifier = Modifier.fillMaxSize().background(KidBackground)) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Stage(
                    animal = animals[index],
                    lang = lang,
                    color = palette[index % palette.size],
                    phase = phase,
                    started = started,
                    pop = pop.value,
                    onReplay = { select(index) },
                    onPrev = { select((index + animals.size - 1) % animals.size) },
                    onNext = { select((index + 1) % animals.size) },
                )
                Confetti(confetti.value)
            }

            Surface(
                modifier = Modifier.width(352.dp).fillMaxHeight(),
                shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
                color = Color.White,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(animals) { i, a ->
                        val sel = i == index && started
                        Image(
                            painter = painterResource(a.image),
                            contentDescription = a.hu,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .border(
                                    if (sel) 5.dp else 1.dp,
                                    if (sel) KidBlueDark else Color(0xFFE3E7F1),
                                    RoundedCornerShape(18.dp),
                                )
                                .clickable { select(i) },
                        )
                    }
                }
            }
        }
        // felül balra: vissza a zászlókhoz
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BackButton(onClick = onChangeLanguage)
            Flag(
                lang,
                Modifier
                    .size(width = 84.dp, height = 56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(3.dp, Color.White, RoundedCornerShape(10.dp))
                    .clickable(onClick = onChangeLanguage),
            )
        }
    }
}

@Composable
private fun Stage(
    animal: Animal,
    lang: Lang,
    color: Color,
    phase: Phase,
    started: Boolean,
    pop: Float,
    onReplay: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val arrow = 56.dp
        val gap = 12.dp
        // A kártya oldala a szélességhez ÉS a magassághoz is igazodik: alatta még a szó és a buborék is elférjen.
        val byWidth = maxWidth - 32.dp - arrow * 2 - gap * 2
        val byHeight = maxHeight - 96.dp - 12.dp - 190.dp
        val cardW = minOf(byWidth, byHeight, 420.dp).coerceAtLeast(150.dp)
        val k = cardW / 380.dp

        Column(
            modifier = Modifier.fillMaxSize().padding(top = 96.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(gap)) {
                ArrowButton("◀", arrow, onPrev)
                Image(
                    painter = painterResource(animal.image),
                    contentDescription = animal.hu,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .scale(1f + 0.06f * pop)
                        .size(cardW)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White)
                        .border(6.dp, color.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                        .clickable(onClick = onReplay),
                )
                ArrowButton("▶", arrow, onNext)
            }
            Spacer(Modifier.height(14.dp))
            // a szó: németnél a névelő színezve (der = kék, die = piros, das = zöld)
            when (lang) {
                Lang.EN -> Text(
                    animal.en,
                    fontSize = (64f * k).coerceAtLeast(40f).sp,
                    fontWeight = FontWeight.Black,
                    color = color,
                    maxLines = 1,
                )
                Lang.DE -> Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = articleColor(animal.article), fontWeight = FontWeight.Black)) {
                            append(animal.article)
                        }
                        append(" ")
                        withStyle(SpanStyle(color = KidText, fontWeight = FontWeight.Black)) { append(animal.de) }
                    },
                    fontSize = (56f * k).coerceAtLeast(34f).sp,
                    maxLines = 1,
                )
            }
            Text(animal.hu, fontSize = 24.sp, color = KidText.copy(alpha = 0.6f), maxLines = 1)
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
        !started -> Triple("👆", "Koppints egy állatra!", Color(0xFFE3F2FD))
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
        Text(text, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = KidText, textAlign = TextAlign.Center)
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
