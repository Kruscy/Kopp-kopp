package hu.apk.kidsapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val KidBlue = Color(0xFF4A90E2)
val KidBlueDark = Color(0xFF2E6DB4)
val KidYellow = Color(0xFFFFD93D)
val KidGreen = Color(0xFF6FCF97)
val KidRed = Color(0xFFFF6B6B)
val KidPurple = Color(0xFFB388FF)
val KidBackground = Color(0xFFF4F8FF)
val KidSurface = Color(0xFFFFFFFF)
val KidOnPrimary = Color(0xFFFFFFFF)
val KidText = Color(0xFF2A2A3B)

private val KidColorScheme = lightColorScheme(
    primary = KidBlue,
    onPrimary = KidOnPrimary,
    secondary = KidYellow,
    background = KidBackground,
    surface = KidSurface,
    onBackground = KidText,
    onSurface = KidText,
    error = KidRed,
)

@Composable
fun KolyokJatekokTheme(content: @Composable () -> Unit) {
    // Always the same bright, kid-friendly light palette regardless of system dark mode.
    MaterialTheme(
        colorScheme = KidColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
