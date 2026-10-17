package hu.apk.kidsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import hu.apk.kidsapp.data.DrawGameState
import hu.apk.kidsapp.data.DrawingStorage
import hu.apk.kidsapp.data.KioskLock
import hu.apk.kidsapp.data.PinManager
import hu.apk.kidsapp.navigation.Routes
import hu.apk.kidsapp.ui.components.PinDialog
import hu.apk.kidsapp.ui.components.SetPinDialog
import hu.apk.kidsapp.ui.screens.HomeScreen
import hu.apk.kidsapp.ui.screens.HomeTile
import hu.apk.kidsapp.ui.screens.SettingsScreen
import hu.apk.kidsapp.ui.screens.draw.CameraCaptureScreen
import hu.apk.kidsapp.ui.screens.draw.DrawHomeScreen
import hu.apk.kidsapp.ui.screens.draw.DrawingCanvasScreen
import hu.apk.kidsapp.ui.screens.draw.DrawingViewerScreen
import hu.apk.kidsapp.ui.screens.draw.GalleryScreen
import hu.apk.kidsapp.ui.screens.blocks.BlocksGameScreen
import hu.apk.kidsapp.ui.screens.cake.CakeGameScreen
import hu.apk.kidsapp.ui.screens.abc.AbcGameScreen
import hu.apk.kidsapp.ui.screens.car.CarGameScreen
import hu.apk.kidsapp.ui.screens.numbers.NumbersGameScreen
import hu.apk.kidsapp.ui.screens.cat.CatGameScreen
import hu.apk.kidsapp.ui.screens.hockey.HockeyGameScreen
import hu.apk.kidsapp.ui.screens.language.LanguageGameScreen
import hu.apk.kidsapp.ui.screens.code.CodeFactoryScreen
import hu.apk.kidsapp.ui.screens.cooking.CookingScreen
import hu.apk.kidsapp.ui.screens.music.MusicScreen
import hu.apk.kidsapp.ui.screens.scene.SceneWorldScreen
import hu.apk.kidsapp.ui.screens.stories.StoryBookScreen
import hu.apk.kidsapp.ui.screens.mosaic.MosaicGameScreen
import hu.apk.kidsapp.ui.screens.pour.PourGameScreen
import hu.apk.kidsapp.ui.screens.snake.SnakeGameScreen
import hu.apk.kidsapp.ui.theme.*

class MainActivity : ComponentActivity() {

    private lateinit var pinManager: PinManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pinManager = PinManager(this)

        setContent {
            KolyokJatekokTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(activity = this, pinManager = pinManager)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-assert the lock if the OS ever kicked us out of lock-task mode
        // (e.g. after a reboot) while auto-lock is meant to be active.
        if (pinManager.isPinSet() && pinManager.autoLockEnabled && !KioskLock.isPinned(this)) {
            KioskLock.lock(this)
        }
    }
}

@Composable
private fun AppRoot(activity: ComponentActivity, pinManager: PinManager) {
    val navController = rememberNavController()
    val drawGameState = remember { DrawGameState() }

    var isLocked by remember { mutableStateOf(KioskLock.isPinned(activity)) }
    var showInitialSetup by remember { mutableStateOf(!pinManager.isPinSet()) }
    var showUnlockPin by remember { mutableStateOf(false) }
    var showSettingsPinGate by remember { mutableStateOf(false) }

    // First launch: force a PIN to be created before anything can be locked/configured.
    LaunchedEffect(Unit) {
        if (pinManager.isPinSet() && pinManager.autoLockEnabled) {
            isLocked = KioskLock.lock(activity)
        }
    }

    fun engageLock() {
        isLocked = KioskLock.lock(activity)
    }

    fun disengageLock() {
        KioskLock.unlock(activity)
        isLocked = KioskLock.isPinned(activity)
    }

    // A gyerek gyakran sokszor egymás után rányom egy gombra. A nyers popBackStack() ilyenkor
    // a kezdőlapot is kipattintaná, és az üres verem miatt fehér képernyő maradna. Ezért minden
    // navigáció csak akkor fut le, ha az aktuális képernyő éppen nincs átmenetben (RESUMED).
    fun isIdle(): Boolean =
        navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED

    fun back() {
        if (isIdle() && navController.previousBackStackEntry != null) navController.popBackStack()
    }

    fun backTo(route: String) {
        if (isIdle()) navController.popBackStack(route, inclusive = false)
    }

    fun go(route: String, builder: androidx.navigation.NavOptionsBuilder.() -> Unit = {}) {
        if (isIdle()) navController.navigate(route) {
            launchSingleTop = true
            builder()
        }
    }

    // Végső védőháló: ha valamiért mégis kiürülne a verem, térjünk vissza a kezdőlapra.
    val currentEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(currentEntry) {
        if (currentEntry == null) {
            navController.navigate(Routes.HOME) { launchSingleTop = true }
        }
    }

    // Az app edge-to-edge fut: a tartalom a rendszersáv (állapotsor, Android navigációs sáv) alá
    // csúszna. Ezért MINDEN képernyőt a rendszersávok közé szorítunk; a sáv mögötti rész színe a
    // képernyőé (a léghokié sötét).
    val route = currentEntry?.destination?.route
    val rootBackground = if (route == Routes.GAME_HOCKEY) Color(0xFF263238) else KidBackground
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(rootBackground)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                isLocked = isLocked,
                onOpenTile = { tile: HomeTile -> go(tile.route) },
                onOpenSettings = {
                    if (pinManager.isPinSet()) showSettingsPinGate = true
                    else go(Routes.SETTINGS)
                },
                onLockToggle = {
                    if (isLocked) {
                        if (pinManager.isPinSet()) showUnlockPin = true else disengageLock()
                    } else {
                        engageLock()
                    }
                },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                pinManager = pinManager,
                onBack = { back() },
            )
        }
        composable(Routes.GAME_DRAW) {
            DrawHomeScreen(
                onBack = { back() },
                onNewPhoto = { go(Routes.GAME_DRAW_CAMERA) },
                onOpenGallery = { go(Routes.GAME_DRAW_GALLERY) },
            )
        }
        composable(Routes.GAME_DRAW_CAMERA) {
            val id = remember { DrawingStorage.newSessionId() }
            CameraCaptureScreen(
                id = id,
                onBack = { back() },
                onPhotoTaken = { file ->
                    drawGameState.pendingPhotoFile.value = file
                    drawGameState.pendingId.value = id
                    go(Routes.GAME_DRAW_CANVAS) { popUpTo(Routes.GAME_DRAW) }
                },
            )
        }
        composable(Routes.GAME_DRAW_CANVAS) {
            val photo = drawGameState.pendingPhotoFile.value
            val id = drawGameState.pendingId.value
            if (photo != null && id != null) {
                DrawingCanvasScreen(
                    photoFile = photo,
                    id = id,
                    onBack = { backTo(Routes.GAME_DRAW) },
                    onSaved = { backTo(Routes.GAME_DRAW) },
                )
            } else {
                // Soha ne maradjon üres képernyő: ha hiányzik a fotó, lépjünk vissza.
                LaunchedEffect(Unit) { backTo(Routes.GAME_DRAW) }
            }
        }
        composable(Routes.GAME_DRAW_GALLERY) {
            GalleryScreen(
                onBack = { back() },
                onOpenDrawing = { file ->
                    drawGameState.viewingFile.value = file
                    go(Routes.GAME_DRAW_VIEW)
                },
            )
        }
        composable(Routes.GAME_DRAW_VIEW) {
            val file = drawGameState.viewingFile.value
            if (file != null) {
                DrawingViewerScreen(
                    file = file,
                    onBack = { back() },
                    onRedraw = {
                        drawGameState.pendingPhotoFile.value = file
                        drawGameState.pendingId.value = DrawingStorage.idOf(file)
                        go(Routes.GAME_DRAW_CANVAS) { popUpTo(Routes.GAME_DRAW) }
                    },
                    onDeleted = { backTo(Routes.GAME_DRAW_GALLERY) },
                )
            } else {
                LaunchedEffect(Unit) { back() }
            }
        }
        composable(Routes.GAME_POUR) {
            PourGameScreen(onBack = { back() })
        }
        composable(Routes.GAME_BLOCKS) {
            BlocksGameScreen(onBack = { back() })
        }
        composable(Routes.GAME_SNAKE) {
            SnakeGameScreen(onBack = { back() })
        }
        composable(Routes.GAME_CAKE) {
            CakeGameScreen(onBack = { back() })
        }
        composable(Routes.GAME_MOSAIC) {
            MosaicGameScreen(onBack = { back() })
        }
        composable(Routes.GAME_CAT) {
            CatGameScreen(onBack = { back() })
        }
        composable(Routes.GAME_CAR) {
            CarGameScreen(onBack = { back() })
        }
        composable(Routes.GAME_NUMBERS) {
            NumbersGameScreen(onBack = { back() })
        }
        composable(Routes.GAME_ABC) {
            AbcGameScreen(onBack = { back() })
        }
        composable(Routes.GAME_HOCKEY) {
            HockeyGameScreen(onBack = { back() })
        }
        composable(Routes.GAME_STORIES) {
            StoryBookScreen(onBack = { back() })
        }
        composable(Routes.GAME_LANGUAGE) {
            LanguageGameScreen(onBack = { back() })
        }
        composable(Routes.GAME_SCENES) {
            SceneWorldScreen(onBack = { back() })
        }
        composable(Routes.GAME_MUSIC) {
            MusicScreen(onBack = { back() })
        }
        composable(Routes.GAME_COOKING) {
            CookingScreen(onBack = { back() })
        }
        composable(Routes.GAME_CODE) {
            CodeFactoryScreen(onBack = { back() })
        }
    }
    }

    if (showInitialSetup) {
        SetPinDialog(
            title = "Üdv! Állíts be egy 4 jegyű PIN kódot a zároláshoz",
            onConfirmed = { pin ->
                pinManager.setPin(pin)
                showInitialSetup = false
                if (pinManager.autoLockEnabled) engageLock()
            },
            onDismiss = { /* PIN is mandatory on first run; keep the dialog up. */ },
        )
    }

    if (showUnlockPin) {
        PinDialog(
            title = "Add meg a PIN kódot a feloldáshoz",
            validate = { pinManager.verifyPin(it) },
            onSuccess = {
                showUnlockPin = false
                disengageLock()
            },
            onDismiss = { showUnlockPin = false },
        )
    }

    if (showSettingsPinGate) {
        PinDialog(
            title = "Add meg a PIN kódot a beállításokhoz",
            validate = { pinManager.verifyPin(it) },
            onSuccess = {
                showSettingsPinGate = false
                go(Routes.SETTINGS)
            },
            onDismiss = { showSettingsPinGate = false },
        )
    }
}
