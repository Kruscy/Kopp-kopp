package hu.apk.kidsapp.ui.screens.stories

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.apk.kidsapp.ui.components.BackButton
import hu.apk.kidsapp.ui.theme.KidBackground
import hu.apk.kidsapp.ui.theme.KidText
import hu.apk.kidsapp.util.Step
import hu.apk.kidsapp.util.rememberClipPlayer

private val Paper = Color(0xFFFFF3DC)

/** Mesekönyv: a polcról választott mese oldalról oldalra felolvasva, képekkel, magától lapozva. */
@Composable
fun StoryBookScreen(onBack: () -> Unit) {
    var open by remember { mutableStateOf<Int?>(null) }
    val idx = open
    if (idx == null) {
        StoryShelf(onBack = onBack, onOpen = { open = it })
    } else {
        StoryReader(story = stories[idx], onClose = { open = null })
    }
}

@Composable
private fun StoryShelf(onBack: () -> Unit, onOpen: (Int) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(KidBackground)) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                BackButton(onClick = onBack)
                Spacer(Modifier.weight(1f))
                Text(
                    "Mesék",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = KidText,
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(1.dp))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            ) {
                stories.withIndex().toList().chunked(3).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        rowItems.forEach { (i, s) ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(Color.White)
                                    .clickable { onOpen(i) }
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Image(
                                    painter = painterResource(s.cover),
                                    contentDescription = s.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(2f)
                                        .clip(RoundedCornerShape(20.dp)),
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "${s.emoji} ${s.title}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KidText,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                )
                            }
                        }
                        repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryReader(story: Story, onClose: () -> Unit) {
    val player = rememberClipPlayer()
    var page by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(true) }
    var ended by remember { mutableStateOf(false) }
    val last = story.pages.lastIndex

    fun go(p: Int) {
        if (p < 0 || p > last) return
        ended = false
        playing = true
        page = p
    }

    // Az aktuális oldal felolvasása; a végén (kis szünet után) magától lapoz.
    LaunchedEffect(page, playing, ended) {
        if (!playing || ended) {
            player.stop()
        } else {
            player.play(
                Step.Pause(600),
                Step.Clip(story.pages[page].clip),
                Step.Pause(1100),
                Step.Mark {
                    if (page < last) {
                        page++
                    } else {
                        ended = true
                        playing = false
                    }
                },
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .pointerInput(story) {
                var dx = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dx = 0f },
                    onHorizontalDrag = { _, d -> dx += d },
                    onDragEnd = {
                        if (dx < -90f) go(page + 1) else if (dx > 90f) go(page - 1)
                    },
                )
            },
    ) {
        AnimatedContent(
            targetState = page,
            transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(500)) },
            label = "storyPage",
            modifier = Modifier.fillMaxSize(),
        ) { p ->
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(story.pages[p].image),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 96.dp, end = 96.dp, bottom = 16.dp)
                        .widthIn(max = 900.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.White.copy(alpha = 0.88f))
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                ) {
                    Text(
                        story.pages[p].text,
                        fontSize = 22.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.Medium,
                        color = KidText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // Felső sor: vissza, oldalpontok, szünet
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onClick = onClose)
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.8f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (i in 0..last) {
                    Box(
                        modifier = Modifier
                            .size(if (i == page) 16.dp else 11.dp)
                            .background(if (i <= page) Color(0xFF8D6E63) else Color(0xFFD7CCC8), CircleShape),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            RoundButton(if (playing) "⏸" else "▶") {
                if (ended) go(0) else playing = !playing
            }
        }

        // Nyilak a két szélen
        if (page > 0) {
            RoundButton("◀", Modifier.align(Alignment.CenterStart).padding(start = 12.dp)) { go(page - 1) }
        }
        if (page < last) {
            RoundButton("▶", Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)) { go(page + 1) }
        }

        if (ended) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White)
                        .padding(horizontal = 40.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Vége! 🌟", fontSize = 40.sp, fontWeight = FontWeight.Black, color = KidText)
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        EndButton("🔁 Újra", Color(0xFF43A047)) { go(0) }
                        EndButton("📚 Másik mese", Color(0xFF8D6E63)) { onClose() }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(64.dp)
            .background(Color.White.copy(alpha = 0.85f), CircleShape)
            .border(3.dp, Color(0xFFD7CCC8), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 26.sp, color = KidText)
    }
}

@Composable
private fun EndButton(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(26.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
