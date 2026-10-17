package hu.apk.kidsapp.data

import androidx.compose.runtime.mutableStateOf
import java.io.File

/**
 * Hands a File between the Game-1 screens (camera -> canvas, gallery -> viewer)
 * without threading it through Navigation Compose route arguments.
 */
class DrawGameState {
    var pendingPhotoFile = mutableStateOf<File?>(null)
    var pendingId = mutableStateOf<String?>(null)
    var viewingFile = mutableStateOf<File?>(null)
}
