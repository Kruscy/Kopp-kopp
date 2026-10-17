package hu.apk.kidsapp.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hu.apk.kidsapp.navigation.Routes
import hu.apk.kidsapp.ui.theme.*

data class HomeTile(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val color: Color,
)

val homeTiles = listOf(
    HomeTile(Routes.GAME_DRAW, "Rajzolj magadról!", Icons.Filled.PhotoCamera, KidRed),
    HomeTile(Routes.GAME_POUR, "Színöntögető", Icons.Filled.Colorize, KidBlue),
    HomeTile(Routes.GAME_BLOCKS, "Kockaépítő", Icons.Filled.ViewInAr, KidGreen),
    HomeTile(Routes.GAME_SNAKE, "Kígyó", Icons.Filled.Timeline, KidPurple),
    HomeTile(Routes.GAME_CAKE, "Tortakészítő", Icons.Filled.Cake, KidYellow),
    HomeTile(Routes.GAME_MOSAIC, "Mozaik", Icons.Filled.GridOn, Color(0xFF26C6DA)),
    HomeTile(Routes.GAME_CAT, "Cica-összerakó", Icons.Filled.Pets, Color(0xFFFF7EB6)),
    HomeTile(Routes.GAME_CAR, "Autó-összerakó", Icons.Filled.DirectionsCar, Color(0xFF7E57C2)),
    HomeTile(Routes.GAME_NUMBERS, "Számok", Icons.Filled.LooksOne, Color(0xFFFF7043)),
    HomeTile(Routes.GAME_ABC, "ABC", Icons.Filled.SortByAlpha, Color(0xFF009688)),
    HomeTile(Routes.GAME_HOCKEY, "Léghoki", Icons.Filled.SportsHockey, Color(0xFF546E7A)),
    HomeTile(Routes.GAME_STORIES, "Mesék", Icons.Filled.AutoStories, Color(0xFF8D6E63)),
    HomeTile(Routes.GAME_LANGUAGE, "Nyelvek", Icons.Filled.Translate, Color(0xFF5C6BC0)),
    HomeTile(Routes.GAME_SCENES, "Matricák", Icons.Filled.Interests, Color(0xFF8BC34A)),
    HomeTile(Routes.GAME_MUSIC, "Zene", Icons.Filled.MusicNote, Color(0xFFEC407A)),
    HomeTile(Routes.GAME_COOKING, "Főzés", Icons.Filled.Restaurant, Color(0xFFFF7043)),
    HomeTile(Routes.GAME_CODE, "Kódgyár", Icons.Filled.SmartToy, Color(0xFF00ACC1)),
    HomeTile(Routes.GAME_PIANO, "Zongora", Icons.Filled.Piano, Color(0xFF5E35B1)),
).filter { it.route != Routes.GAME_LANGUAGE || LANGUAGE_GAME_READY }

/**
 * A Nyelvek játék megjelenítése (res/raw/lang_en_*, lang_de_* hangokkal). Hamisra állítva a csempe
 * rejtve marad a főoldalon.
 */
const val LANGUAGE_GAME_READY = true

@Composable
fun HomeScreen(
    isLocked: Boolean,
    onOpenTile: (HomeTile) -> Unit,
    onOpenSettings: () -> Unit,
    onLockToggle: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KidBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Kopp-kopp",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = KidText,
                    modifier = Modifier.weight(1f),
                )
                FilledIconButton(
                    onClick = onLockToggle,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isLocked) KidGreen else KidRed
                    ),
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                        contentDescription = if (isLocked) "Feloldás" else "Zárolás",
                        tint = Color.White,
                    )
                }
                Spacer(Modifier.width(12.dp))
                FilledIconButton(
                    onClick = onOpenSettings,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = KidBlueDark),
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Beállítások", tint = Color.White)
                }
            }

            Spacer(Modifier.height(24.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(homeTiles) { tile ->
                    TileCard(tile = tile, onClick = { onOpenTile(tile) })
                }
            }
        }
    }
}

@Composable
private fun TileCard(tile: HomeTile, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "tileScale",
    )
    val indication = LocalIndication.current

    Column(
        modifier = Modifier
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = indication, onClick = onClick)
            .background(KidSurface, RoundedCornerShape(28.dp))
            .padding(vertical = 24.dp, horizontal = 12.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .background(tile.color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = tile.icon,
                contentDescription = tile.title,
                tint = Color.White,
                modifier = Modifier.size(44.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = tile.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = KidText,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
