package hu.apk.kidsapp.ui.screens.mosaic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hu.apk.kidsapp.ui.components.BackButton
import hu.apk.kidsapp.ui.theme.KidBackground
import hu.apk.kidsapp.ui.theme.KidRed
import hu.apk.kidsapp.ui.theme.KidText
import hu.apk.kidsapp.util.multiTouch

private val mosaicColors = listOf(
    Color(0xFFE53935), // piros
    Color(0xFFFF8A65), // narancs
    Color(0xFFFFD54F), // sárga
    Color(0xFFA8E063), // világoszöld
    Color(0xFF43A047), // zöld
    Color(0xFF26C6DA), // türkiz
    Color(0xFF4A90E2), // kék
    Color(0xFF3949AB), // sötétkék
    Color(0xFF9B59B6), // lila
    Color(0xFFFF8FD0), // rózsaszín
    Color(0xFF8D6E63), // barna
    Color(0xFF4A2E1E), // sötétbarna
    Color(0xFF212121), // fekete
    Color(0xFF9E9E9E), // szürke
    Color(0xFFFFFFFF), // fehér
)

@Composable
fun MosaicGameScreen(onBack: () -> Unit) {
    var grid by remember { mutableStateOf(mapOf<Pair<Int, Int>, Int>()) }
    var selectedColor by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize().background(KidBackground)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val cellPx = with(density) { 30.dp.toPx() }
            val paletteWidthPx = with(density) { 150.dp.toPx() }
            val availW = with(density) { maxWidth.toPx() }
            val availH = with(density) { maxHeight.toPx() }
            val topMarginPx = with(density) { 96.dp.toPx() }

            val cols = ((availW - paletteWidthPx) / cellPx).toInt().coerceAtLeast(6)
            val rows = ((availH - topMarginPx) / cellPx).toInt().coerceAtLeast(6)
            val gridLeft = paletteWidthPx + ((availW - paletteWidthPx) - cols * cellPx) / 2f

            // A Vissza gomb a bal felső sarokban van (~80dp magas) — a színek alatta kezdődnek,
            // és a sorköz úgy zsugorodik, hogy mind a 8 sor elférjen a kijelzőn.
            val paletteRows = (mosaicColors.size + 1) / 2
            val paletteRadius = with(density) { 26.dp.toPx() }
            val paletteTopY = with(density) { 104.dp.toPx() }
            val paletteColSpacing = with(density) { 70.dp.toPx() }
            val paletteRowSpacing = minOf(
                with(density) { 64.dp.toPx() },
                (availH - paletteTopY - paletteRadius - with(density) { 8.dp.toPx() }) / (paletteRows - 1).coerceAtLeast(1),
            )
            val paletteCenters = mosaicColors.indices.map { i ->
                val col = i % 2
                val row = i / 2
                Offset(paletteWidthPx / 2f - paletteColSpacing / 2f + col * paletteColSpacing, paletteTopY + row * paletteRowSpacing)
            }
            val swatchHitRadius = with(density) { 36.dp.toPx() }

            // Ujjanként külön színnel és külön "utolsó hely"-lel: két gyerek egyszerre festhet,
            // az egyik színváltása nem festi át a másik ujját.
            val fingerColor = remember { HashMap<Long, Int>() }
            val fingerLast = remember { HashMap<Long, Offset>() }
            val paletteFingers = remember { HashSet<Long>() }

            fun selectSwatchAt(pos: Offset) {
                val idx = paletteCenters.indexOfFirst { (it - pos).getDistance() < swatchHitRadius }
                if (idx >= 0) selectedColor = idx
            }

            fun paintCell(pos: Offset, color: Int) {
                val col = ((pos.x - gridLeft) / cellPx).toInt()
                val row = ((pos.y - topMarginPx) / cellPx).toInt()
                if (pos.x >= gridLeft && col in 0 until cols && row in 0 until rows) {
                    if (grid[col to row] != color) grid = grid + ((col to row) to color)
                }
            }

            fun paintLine(from: Offset, to: Offset, color: Int) {
                // Gyors ujjmozgásnál se maradjanak ki cellák: félcellás lépésekkel megyünk végig.
                val steps = (((to - from).getDistance()) / (cellPx / 2f)).toInt().coerceAtLeast(1)
                for (i in 1..steps) paintCell(from + (to - from) * (i / steps.toFloat()), color)
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .multiTouch(
                        onDown = { id, pos ->
                            if (pos.x < paletteWidthPx) {
                                paletteFingers.add(id)
                                selectSwatchAt(pos)
                            } else {
                                val color = selectedColor
                                fingerColor[id] = color
                                fingerLast[id] = pos
                                paintCell(pos, color)
                            }
                        },
                        onMove = { id, pos ->
                            if (id in paletteFingers) {
                                selectSwatchAt(pos)
                            } else {
                                val color = fingerColor[id]
                                val last = fingerLast[id]
                                if (color != null && last != null) paintLine(last, pos, color)
                                fingerLast[id] = pos
                            }
                        },
                        onUp = { id ->
                            paletteFingers.remove(id)
                            fingerColor.remove(id)
                            fingerLast.remove(id)
                        },
                    )
            ) {
                // Paletta háttere
                drawRect(color = Color.White, topLeft = Offset.Zero, size = Size(paletteWidthPx, availH))

                // Rács cellák
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val x = gridLeft + c * cellPx
                        val y = topMarginPx + r * cellPx
                        val colorIdx = grid[c to r]
                        val fill = if (colorIdx != null) mosaicColors[colorIdx] else Color(0xFFFAFAFA)
                        drawRect(fill, topLeft = Offset(x, y), size = Size(cellPx, cellPx))
                        drawRect(
                            Color(0xFFE2E2E2),
                            topLeft = Offset(x, y),
                            size = Size(cellPx, cellPx),
                            style = Stroke(width = 1f),
                        )
                    }
                }

                // Paletta minták
                paletteCenters.forEachIndexed { i, center ->
                    val isSelected = i == selectedColor
                    drawCircle(mosaicColors[i], radius = paletteRadius, center = center)
                    drawCircle(
                        color = if (isSelected) Color.Black else Color(0xFFBFBFBF),
                        radius = paletteRadius,
                        center = center,
                        style = Stroke(width = if (isSelected) 4f else 2f),
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                BackButton(onClick = onBack)
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center) {
                    Text(
                        "Mozaik",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = KidText,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                    FilledIconButton(
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = KidRed),
                        onClick = { grid = emptyMap() },
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Minden törlése", tint = Color.White)
                    }
                }
            }
        }
    }
}
