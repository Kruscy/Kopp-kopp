package hu.apk.kidsapp.ui.screens.draw

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hu.apk.kidsapp.data.DrawingStorage
import hu.apk.kidsapp.ui.components.BackButton
import hu.apk.kidsapp.ui.theme.KidBackground
import hu.apk.kidsapp.ui.theme.KidText
import java.io.File

@Composable
fun GalleryScreen(onBack: () -> Unit, onOpenDrawing: (File) -> Unit) {
    val context = LocalContext.current
    val drawings = remember { DrawingStorage.listFinished(context) }

    Box(modifier = Modifier.fillMaxSize().background(KidBackground).padding(24.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            BackButton(onClick = onBack)
            Spacer(Modifier.height(16.dp))
            Text(
                "Galéria",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = KidText,
            )
            Spacer(Modifier.height(16.dp))

            if (drawings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Még nincs mentett rajzod, készíts egy új fotót!", color = KidText)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(drawings) { file ->
                        GalleryThumbnail(file = file, onClick = { onOpenDrawing(file) })
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryThumbnail(file: File, onClick: () -> Unit) {
    val thumb = remember(file) { DrawingStorage.loadThumbnail(file, 300) }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
            .background(Color.White, RoundedCornerShape(20.dp))
            .padding(8.dp),
    ) {
        if (thumb != null) {
            Image(
                bitmap = thumb.asImageBitmap(),
                contentDescription = file.name,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
