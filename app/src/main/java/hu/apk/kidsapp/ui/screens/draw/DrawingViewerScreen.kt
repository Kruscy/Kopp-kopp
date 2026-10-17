package hu.apk.kidsapp.ui.screens.draw

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import hu.apk.kidsapp.data.DrawingStorage
import hu.apk.kidsapp.ui.components.BackButton
import hu.apk.kidsapp.ui.theme.KidBlue
import hu.apk.kidsapp.ui.theme.KidRed
import java.io.File

@Composable
fun DrawingViewerScreen(
    file: File,
    onBack: () -> Unit,
    onRedraw: (File) -> Unit,
    onDeleted: () -> Unit,
) {
    val context = LocalContext.current
    val bitmap = remember(file) { DrawingStorage.loadBitmap(file) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(24.dp),
            )
        }

        Box(modifier = Modifier.padding(24.dp)) {
            BackButton(onClick = onBack)
        }

        Row(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom,
        ) {
            FilledIconButton(
                modifier = Modifier.size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = KidRed),
                onClick = {
                    DrawingStorage.deleteSession(context, file)
                    onDeleted()
                },
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Törlés", tint = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            FilledIconButton(
                modifier = Modifier.size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = KidBlue),
                onClick = { onRedraw(file) },
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Rajzolj rá újra", tint = Color.White)
            }
        }
    }
}
