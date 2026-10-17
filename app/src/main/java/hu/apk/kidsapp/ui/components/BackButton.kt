package hu.apk.kidsapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import hu.apk.kidsapp.ui.theme.KidBlueDark

/** Big, thumb-friendly back button reused by every non-home screen. */
@Composable
fun BackButton(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = KidBlueDark,
        contentColor = Color.White,
        icon = { Icon(Icons.Filled.ArrowBackIosNew, contentDescription = null) },
        text = { Text("Vissza") },
    )
}
