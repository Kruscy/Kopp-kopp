package hu.apk.kidsapp.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged

/**
 * Több ujj egyszerre: minden ujjnak saját azonosítója van (`id`), így két gyerek egyszerre,
 * egymástól függetlenül rajzolhat ugyanazon a felületen.
 */
@Composable
fun Modifier.multiTouch(
    onDown: (id: Long, pos: Offset) -> Unit,
    onMove: (id: Long, pos: Offset) -> Unit,
    onUp: (id: Long) -> Unit,
): Modifier {
    val down by rememberUpdatedState(onDown)
    val move by rememberUpdatedState(onMove)
    val up by rememberUpdatedState(onUp)
    return pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                event.changes.forEach { change ->
                    val id = change.id.value
                    when {
                        change.changedToDown() -> {
                            change.consume()
                            down(id, change.position)
                        }
                        change.changedToUp() -> {
                            change.consume()
                            up(id)
                        }
                        change.pressed && change.positionChanged() -> {
                            change.consume()
                            move(id, change.position)
                        }
                    }
                }
            }
        }
    }
}
