package hu.apk.kidsapp.ui.screens.draw

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hu.apk.kidsapp.ui.components.BackButton
import hu.apk.kidsapp.ui.theme.KidBackground
import hu.apk.kidsapp.ui.theme.KidBlue
import hu.apk.kidsapp.ui.theme.KidRed
import hu.apk.kidsapp.ui.theme.KidText

@Composable
fun DrawHomeScreen(onBack: () -> Unit, onNewPhoto: () -> Unit, onOpenGallery: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(KidBackground).padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BackButton(onClick = onBack)
            Spacer(Modifier.height(16.dp))
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "Rajzolj magadról!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = KidText,
            )
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                BigChoiceCard("Új fénykép", Icons.Filled.PhotoCamera, KidRed, onNewPhoto)
                BigChoiceCard("Galéria", Icons.Filled.Collections, KidBlue, onOpenGallery)
            }
        }
    }
}

@Composable
private fun BigChoiceCard(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(Color.White, RoundedCornerShape(28.dp))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(96.dp).background(color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(52.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = KidText)
    }
}
