package hu.apk.kidsapp.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hu.apk.kidsapp.data.PinManager
import hu.apk.kidsapp.ui.components.BackButton
import hu.apk.kidsapp.ui.components.PinDialog
import hu.apk.kidsapp.ui.components.SetPinDialog
import hu.apk.kidsapp.ui.theme.KidBackground
import hu.apk.kidsapp.ui.theme.KidText

@Composable
fun SettingsScreen(pinManager: PinManager, onBack: () -> Unit) {
    var autoLock by remember { mutableStateOf(pinManager.autoLockEnabled) }
    var showChangePinFlow by remember { mutableStateOf(false) }
    var showOldPinCheck by remember { mutableStateOf(false) }

    // A telepített verzió (a Play frissítések ellenőrzéséhez).
    val context = LocalContext.current
    val versionText = remember {
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (Build.VERSION.SDK_INT >= 28) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
            "${info.versionName} ($code. kiadás)"
        } catch (_: Exception) {
            "ismeretlen"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KidBackground)
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BackButton(onClick = onBack)
            Spacer(Modifier.height(24.dp))
            Text(
                "Beállítások",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = KidText,
            )
            Spacer(Modifier.height(24.dp))

            SettingsCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Automatikus zárolás", fontWeight = FontWeight.SemiBold)
                        Text(
                            "A kezdőlap megnyitásakor azonnal zárolja a képernyőt",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = autoLock,
                        onCheckedChange = {
                            autoLock = it
                            pinManager.autoLockEnabled = it
                        },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            SettingsCard {
                Column {
                    Text("PIN kód módosítása", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        if (pinManager.isPinSet()) showOldPinCheck = true else showChangePinFlow = true
                    }) {
                        Text("PIN megváltoztatása")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            SettingsCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Verzió", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(versionText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showOldPinCheck) {
        PinDialog(
            title = "Add meg a jelenlegi PIN kódot",
            validate = { pinManager.verifyPin(it) },
            onSuccess = {
                showOldPinCheck = false
                showChangePinFlow = true
            },
            onDismiss = { showOldPinCheck = false },
        )
    }

    if (showChangePinFlow) {
        SetPinDialog(
            onConfirmed = { newPin ->
                pinManager.setPin(newPin)
                showChangePinFlow = false
            },
            onDismiss = { showChangePinFlow = false },
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(20.dp))
            .padding(20.dp),
        content = content,
    )
}
