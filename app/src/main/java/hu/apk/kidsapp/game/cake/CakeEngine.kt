package hu.apk.kidsapp.game.cake

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.cos
import kotlin.math.sin

enum class DecorationType { CHERRY, STAR, SPRINKLE, CANDLE, HEART, FLOWER, CHOC_CHIP, STRAWBERRY }

data class Decoration(val type: DecorationType, val pos: Offset, val color: Color)

sealed class PaletteItem {
    data class LayerColor(val color: Color) : PaletteItem()
    data class Icing(val color: Color) : PaletteItem()
    data class Deco(val type: DecorationType, val color: Color) : PaletteItem()
}

/**
 * One undo-able step, kept in insertion order so "Visszavonás" always removes the latest change.
 * [IcingStep.previousColor] remembers what (if anything) was on that layer before, so undoing a
 * re-coat restores it instead of just wiping the icing.
 */
sealed class CakeAction {
    data class Layer(val color: Color) : CakeAction()
    data class IcingStep(val layerIndex: Int, val previousColor: Color?) : CakeAction()
    data class Deco(val decoration: Decoration) : CakeAction()
}

/** Five-pointed star outline centered at [center]. */
fun starPath(center: Offset, outerRadius: Float, innerRadius: Float): Path {
    val path = Path()
    val points = 5
    val angleStep = Math.PI / points
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val angle = -Math.PI / 2 + i * angleStep
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
