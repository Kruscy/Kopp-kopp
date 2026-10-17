package hu.apk.kidsapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import hu.apk.kidsapp.ui.theme.KidRed

private const val PIN_LENGTH = 4

/**
 * Full numeric-keypad PIN entry dialog for parents. Not kid-optimized on purpose —
 * this is the one screen a 4-year-old should NOT be able to easily pass.
 *
 * [title] explains what the PIN is for. [onSuccess] fires with the entered pin once
 * it reaches PIN_LENGTH digits and [validate] returns true for it; otherwise the
 * dialog shakes/clears and lets the parent retry. [onDismiss] closes without success.
 */
@Composable
fun PinDialog(
    title: String,
    validate: (String) -> Boolean,
    onSuccess: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    fun onDigit(d: String) {
        if (entered.length >= PIN_LENGTH) return
        error = false
        entered += d
        if (entered.length == PIN_LENGTH) {
            if (validate(entered)) {
                onSuccess(entered)
            } else {
                error = true
                entered = ""
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(PIN_LENGTH) { i ->
                        val filled = i < entered.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(
                                    color = if (error) KidRed
                                    else if (filled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape,
                                )
                        )
                    }
                }
                if (error) {
                    Spacer(Modifier.height(8.dp))
                    Text("Helytelen PIN, próbáld újra", color = KidRed, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(20.dp))

                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "back"),
                )
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { key ->
                            when (key) {
                                "" -> Spacer(Modifier.size(56.dp))
                                "back" -> FilledTonalIconButton(
                                    modifier = Modifier.size(56.dp),
                                    onClick = {
                                        error = false
                                        if (entered.isNotEmpty()) entered = entered.dropLast(1)
                                    }
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Törlés")
                                }
                                else -> FilledTonalButton(
                                    modifier = Modifier.size(56.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    onClick = { onDigit(key) }
                                ) {
                                    Text(key, style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                TextButton(onClick = onDismiss) {
                    Text("Mégse")
                }
            }
        }
    }
}

@Composable
fun SetPinDialog(
    title: String = "Adj meg egy új 4 jegyű PIN kódot",
    onConfirmed: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var stage by remember { mutableStateOf(0) } // 0 = first entry, 1 = confirm
    var firstPin by remember { mutableStateOf("") }

    if (stage == 0) {
        PinDialog(
            title = title,
            validate = { true },
            onSuccess = { pin ->
                firstPin = pin
                stage = 1
            },
            onDismiss = onDismiss,
        )
    } else {
        PinDialog(
            title = "Add meg még egyszer megerősítésként",
            validate = { it == firstPin },
            onSuccess = { pin -> onConfirmed(pin) },
            onDismiss = onDismiss,
        )
    }
}
