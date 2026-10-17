package hu.apk.kidsapp.ui.screens.draw

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Rect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import hu.apk.kidsapp.data.DrawingStorage
import hu.apk.kidsapp.ui.components.BackButton
import hu.apk.kidsapp.ui.theme.KidGreen
import hu.apk.kidsapp.ui.theme.KidRed
import hu.apk.kidsapp.util.Voice
import hu.apk.kidsapp.util.rememberVoicePlayer
import kotlinx.coroutines.delay
import java.io.File

private const val STABLE_MS = 550L

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun CameraCaptureScreen(id: String, onBack: () -> Unit, onPhotoTaken: (File) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val imageCapture = remember { ImageCapture.Builder().build() }
    var faceReady by remember { mutableStateOf(false) }
    var captured by remember { mutableStateOf(false) }
    val voice = rememberVoicePlayer()

    // Ha egy darabig nem látszik teljes arc, szóban segítünk; ha megvan, megdicsérjük.
    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        while (true) {
            delay(6_000)
            if (!faceReady && !captured) voice.play(Voice.CAMERA_HELP)
        }
    }
    LaunchedEffect(faceReady) {
        if (faceReady) voice.play(Voice.CAMERA_READY)
    }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    val faceDetector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build()
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            faceDetector.close()
            cameraProvider?.unbindAll()
        }
    }

    fun takePhoto() {
        if (captured) return
        captured = true
        val photoFile = DrawingStorage.originalFileFor(context, id)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onPhotoTaken(photoFile)
                }
                override fun onError(exc: ImageCaptureException) {
                    // Kid-facing app: silently let them try again rather than showing a scary error.
                    captured = false
                }
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val provider = cameraProviderFuture.get()
                        cameraProvider = provider
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        var goodSinceMs = 0L
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage == null || captured) {
                                imageProxy.close()
                            } else {
                                val rotation = imageProxy.imageInfo.rotationDegrees
                                val (effectiveW, effectiveH) = if (rotation == 90 || rotation == 270) {
                                    mediaImage.height to mediaImage.width
                                } else {
                                    mediaImage.width to mediaImage.height
                                }
                                val inputImage = InputImage.fromMediaImage(mediaImage, rotation)
                                faceDetector.process(inputImage)
                                    .addOnSuccessListener { faces ->
                                        val face = faces.firstOrNull()
                                        val fullyVisible = face != null &&
                                            isFullyInFrame(face.boundingBox, effectiveW, effectiveH)
                                        val now = System.currentTimeMillis()
                                        if (fullyVisible) {
                                            if (goodSinceMs == 0L) goodSinceMs = now
                                            faceReady = true
                                            if (!captured && now - goodSinceMs > STABLE_MS) {
                                                takePhoto()
                                            }
                                        } else {
                                            goodSinceMs = 0L
                                            faceReady = false
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            }
                        }

                        provider.unbindAll()
                        try {
                            provider.bindToLifecycle(
                                lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageCapture, analysis
                            )
                        } catch (e: Exception) {
                            try {
                                provider.bindToLifecycle(
                                    lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture, analysis
                                )
                            } catch (e2: Exception) {
                                // No usable camera; leave preview blank, the manual button will just fail silently.
                            }
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
            )

            // Arc-keret: zölden jelzi, ha az egész arc látszik, és ekkor automatikusan fotóz.
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .border(
                            width = 5.dp,
                            color = if (faceReady) KidGreen else Color.White.copy(alpha = 0.65f),
                            shape = CircleShape,
                        )
                )
            }

            Box(
                modifier = Modifier.fillMaxSize().padding(top = 32.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = if (faceReady) "Szuper, tarts így!" else "Told úgy, hogy az egész arcod beleférjen!",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                FilledIconButton(
                    modifier = Modifier.size(88.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = KidRed),
                    onClick = { takePhoto() },
                ) {
                    Icon(
                        Icons.Filled.PhotoCamera,
                        contentDescription = "Fotó készítése",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp),
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Kamera engedély szükséges a fotózáshoz", color = Color.White)
            }
        }

        Box(modifier = Modifier.padding(24.dp)) {
            BackButton(onClick = onBack)
        }
    }
}

/** True when the detected face's box sits fully inside the frame (with margin) and is large
 * enough to be a close-up — catches the common "only the forehead is in shot" framing. */
private fun isFullyInFrame(box: Rect, imageWidth: Int, imageHeight: Int): Boolean {
    val marginX = imageWidth * 0.08f
    val marginY = imageHeight * 0.08f
    val withinBounds = box.left > marginX && box.top > marginY &&
        box.right < imageWidth - marginX && box.bottom < imageHeight - marginY
    val bigEnough = box.width() > imageWidth * 0.28f
    return withinBounds && bigEnough
}
