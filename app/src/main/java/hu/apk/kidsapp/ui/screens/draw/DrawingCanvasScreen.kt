package hu.apk.kidsapp.ui.screens.draw

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import hu.apk.kidsapp.data.DrawingStorage
import hu.apk.kidsapp.ui.components.BackButton
import hu.apk.kidsapp.ui.theme.KidBackground
import hu.apk.kidsapp.ui.theme.KidGreen
import hu.apk.kidsapp.util.multiTouch
import hu.apk.kidsapp.util.Voice
import hu.apk.kidsapp.util.rememberVoicePlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private data class DrawnStroke(val points: List<Offset>, val color: Color, val widthPx: Float)

private val palette = listOf(
    Color(0xFF2A2A3B), // fekete
    Color(0xFFFF6B6B), // piros
    Color(0xFFFFA94D), // narancs
    Color(0xFFFFD93D), // sárga
    Color(0xFF6FCF97), // zöld
    Color(0xFF4A90E2), // kék
    Color(0xFFB388FF), // lila
    Color(0xFFFF8FD0), // rózsaszín
)

@Composable
fun DrawingCanvasScreen(photoFile: File, id: String, onBack: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val voice = rememberVoicePlayer()
    var saving by remember { mutableStateOf(false) }
    // Egy többmegapixeles kamerakép teljes méretben könnyen OutOfMemory-t okoz — kisebb méretben
    // töltjük be (rajzoláshoz bőven elég).
    val bitmap = remember(photoFile) { decodeScaled(photoFile.absolutePath, maxSidePx = 1800) }

    if (bitmap == null) {
        Box(modifier = Modifier.fillMaxSize().background(KidBackground), contentAlignment = Alignment.Center) {
            Text("Nem sikerült betölteni a fotót")
        }
        return
    }

    var strokes by remember { mutableStateOf(listOf<DrawnStroke>()) }
    // Ujjanként egy folyamatban lévő vonal: két gyerek egyszerre rajzolhat. A szín és a vastagság
    // akkor rögzül, amikor az ujj leér, így a másik gyerek színváltása nem festi át a vonalat.
    var activeStrokes by remember { mutableStateOf(mapOf<Long, DrawnStroke>()) }
    var currentColor by remember { mutableStateOf(palette.first()) }
    var thickBrush by remember { mutableStateOf(true) }
    val brushWidthPx = if (thickBrush) 28f else 10f

    var canvasWidthPx by remember { mutableStateOf(1f) }
    val scaleToBitmap = bitmap.width / canvasWidthPx

    Box(modifier = Modifier.fillMaxSize()) {
    Row(modifier = Modifier.fillMaxSize().background(KidBackground)) {
        Column(
            // A felső 96dp-t a Vissza gomb foglalja el — a színek alatta kezdődnek, és ha a
            // kijelző alacsony, a sáv görgethető, így a gomb soha nem takarja a színeket.
            modifier = Modifier
                .width(96.dp)
                .fillMaxHeight()
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(top = 96.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            palette.forEach { color ->
                Box(
                    modifier = Modifier
                        .padding(vertical = 6.dp)
                        .size(if (color == currentColor) 52.dp else 44.dp)
                        .clickable { currentColor = color }
                        .background(color, CircleShape),
                )
            }
            Spacer(Modifier.height(16.dp))
            BrushSizeButton(label = "Vékony", selected = !thickBrush, dotSize = 10.dp) { thickBrush = false }
            Spacer(Modifier.height(8.dp))
            BrushSizeButton(label = "Vastag", selected = thickBrush, dotSize = 22.dp) { thickBrush = true }
            Spacer(Modifier.height(16.dp))
            IconButton(onClick = { if (strokes.isNotEmpty()) strokes = strokes.dropLast(1) }) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Visszavonás")
            }
            IconButton(onClick = { strokes = emptyList() }) {
                Icon(Icons.Filled.Clear, contentDescription = "Törlés")
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp), contentAlignment = Alignment.Center) {
            val aspect = bitmap.width.toFloat() / bitmap.height.toFloat()
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(aspect)
            ) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                )
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { canvasWidthPx = it.width.toFloat() }
                        .multiTouch(
                            onDown = { id, pos ->
                                activeStrokes = activeStrokes + (id to DrawnStroke(listOf(pos), currentColor, brushWidthPx))
                            },
                            onMove = { id, pos ->
                                val stroke = activeStrokes[id]
                                if (stroke != null) {
                                    activeStrokes = activeStrokes + (id to stroke.copy(points = stroke.points + pos))
                                }
                            },
                            onUp = { id ->
                                val stroke = activeStrokes[id]
                                if (stroke != null) {
                                    // Egyetlen koppintás is pöttyöt hagy.
                                    val finished = if (stroke.points.size == 1) {
                                        stroke.copy(points = stroke.points + stroke.points.first())
                                    } else stroke
                                    strokes = strokes + finished
                                    activeStrokes = activeStrokes - id
                                }
                            },
                        )
                ) {
                    val allStrokes = strokes + activeStrokes.values
                    allStrokes.forEach { s ->
                        val path = Path().apply {
                            moveTo(s.points.first().x, s.points.first().y)
                            s.points.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(
                            path = path,
                            color = s.color,
                            style = Stroke(width = s.widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        BackButton(onClick = onBack)
    }
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.TopEnd) {
        FilledIconButton(
            modifier = Modifier.size(72.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = KidGreen),
            onClick = {
                if (!saving) {
                    saving = true
                    val strokesSnapshot = strokes
                    val scale = scaleToBitmap
                    scope.launch {
                        withContext(Dispatchers.Default) {
                            val flattened = flattenDrawing(bitmap, strokesSnapshot, scale)
                            DrawingStorage.saveFinished(context, flattened, id)
                        }
                        voice.play(Voice.DRAW_SAVED)
                        onSaved()
                    }
                }
            },
        ) {
            Icon(Icons.Filled.Check, contentDescription = "Kész", tint = Color.White, modifier = Modifier.size(36.dp))
        }
    }
    }
}

@Composable
private fun BrushSizeButton(label: String, selected: Boolean, dotSize: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(if (selected) Color(0xFFE0E0E0) else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.size(dotSize).background(Color(0xFF2A2A3B), CircleShape))
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun decodeScaled(path: String, maxSidePx: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (bounds.outWidth / sample > maxSidePx || bounds.outHeight / sample > maxSidePx) {
        sample *= 2
    }
    return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
}

private fun flattenDrawing(source: Bitmap, strokes: List<DrawnStroke>, scale: Float): Bitmap {
    val result = source.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(result)
    val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    strokes.forEach { s ->
        if (s.points.size < 2) return@forEach
        val path = AndroidPath()
        val first = s.points.first()
        path.moveTo(first.x * scale, first.y * scale)
        s.points.drop(1).forEach { path.lineTo(it.x * scale, it.y * scale) }
        paint.color = s.color.toArgb()
        paint.strokeWidth = s.widthPx * scale
        canvas.drawPath(path, paint)
    }
    return result
}
