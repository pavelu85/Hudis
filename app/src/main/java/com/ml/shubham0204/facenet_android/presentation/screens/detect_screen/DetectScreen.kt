package com.ml.shubham0204.facenet_android.presentation.screens.detect_screen

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.ml.shubham0204.facenet_android.R
import com.ml.shubham0204.facenet_android.presentation.components.AppAlertDialog
import com.ml.shubham0204.facenet_android.presentation.components.DelayedVisibility
import com.ml.shubham0204.facenet_android.presentation.components.FaceDetectionOverlay
import com.ml.shubham0204.facenet_android.presentation.components.FocusRingOverlay
import com.ml.shubham0204.facenet_android.presentation.components.createAlertDialog
import com.ml.shubham0204.facenet_android.presentation.theme.HudisTheme
import org.koin.androidx.compose.koinViewModel

private val cameraPermissionStatus = mutableStateOf(false)
private val cameraFacing = mutableIntStateOf(CameraSelector.LENS_FACING_BACK)
private lateinit var cameraPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>
private lateinit var locationPermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectScreen(
    onOpenFaceListClick: () -> Unit,
    onNavigateToResults: () -> Unit,
) {
    val viewModel: DetectScreenViewModel = koinViewModel()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Request location permission silently at startup (needed for encounter location tracking)
    locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* result ignored — location is optional */ }
    LaunchedEffect(Unit) {
        val hasLocation = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasLocation) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }
    }

    // Gallery image picker
    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { viewModel.processGalleryImage(it) }
        }

    // Observe navigation events from the ViewModel
    LaunchedEffect(Unit) {
        viewModel.navigateToResults.collect {
            viewModel.flushSeenPersons()
            onNavigateToResults()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.galleryError.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    HudisTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(),
                    title = {
                        val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
                        Text(
                            text = "${stringResource(id = R.string.app_name)} v${pkg.versionName} (${androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(pkg)})",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    },
                    actions = {
                        val isProcessing by remember { viewModel.isProcessingGalleryImage }
                        val isAutoMonitor by remember { viewModel.isAutoMonitorEnabled }
                        // Live camera Auto-Monitor toggle
                        IconButton(onClick = { viewModel.toggleAutoMonitor() }) {
                            Icon(
                                imageVector = if (isAutoMonitor) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isAutoMonitor) "Auto-Monitor ON" else "Auto-Monitor OFF",
                                tint = if (isAutoMonitor) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Unspecified,
                            )
                        }
                        IconButton(
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            enabled = !isProcessing,
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Identify from Gallery",
                                )
                            }
                        }
                        IconButton(onClick = {
                            coroutineScope.launch {
                                viewModel.flushSeenPersons()
                                onOpenFaceListClick()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "Open Face List",
                            )
                        }
                        IconButton(onClick = { viewModel.changeCameraFacing() }) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera",
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                val isAutoMonitor by remember { viewModel.isAutoMonitorEnabled }
                if (isAutoMonitor) {
                    Text(
                        text = "Auto-Monitor ON — unknown faces will be saved automatically",
                        color = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                    )
                }
                ScreenUI(viewModel)
            }
        }
    }
}

@Composable
private fun ScreenUI(viewModel: DetectScreenViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        Camera(viewModel)
        DelayedVisibility(viewModel.getNumPeople() > 0) {
            val metrics by remember { viewModel.faceDetectionMetricsState }
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 4.dp)
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "Recognition on ${viewModel.getNumPeople()} face(s)",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
                metrics?.let {
                    Text(
                        text =
                            "face detection: ${it.timeFaceDetection} ms" +
                                "\nface embedding: ${it.timeFaceEmbedding} ms" +
                                "\nvector search: ${it.timeVectorSearch} ms" +
                                "\nspoof detection: ${it.timeFaceSpoofDetection} ms",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        DelayedVisibility(viewModel.getNumPeople() == 0L) {
            Text(
                text = "No images in database",
                color = Color.White,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(Color.Blue, RoundedCornerShape(16.dp))
                        .padding(8.dp),
                textAlign = TextAlign.Center,
            )
        }
        AppAlertDialog()
        val isPaused by remember { viewModel.isPaused }
        if (isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Text(
                    text = "Paused — long-press to resume",
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        val isFront = viewModel.cameraFacing.intValue == CameraSelector.LENS_FACING_FRONT
        if (!isFront) {
            ZoomControls(viewModel = viewModel)
        }
        FocusRingOverlay(viewModel = viewModel, modifier = Modifier.fillMaxSize())
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun Camera(viewModel: DetectScreenViewModel) {
    val context = LocalContext.current
    cameraPermissionStatus.value =
        ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    val cameraFacing by remember { viewModel.cameraFacing }
    val requestedZoom by remember { viewModel.requestedZoomRatio }
    val isPaused by remember { viewModel.isPaused }
    val lifecycleOwner = LocalLifecycleOwner.current

    cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                cameraPermissionStatus.value = true
            } else {
                camaraPermissionDialog()
            }
        }

    var overlayRef by remember { mutableStateOf<FaceDetectionOverlay?>(null) }

    DelayedVisibility(cameraPermissionStatus.value) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput("tap") {
                    detectTapGestures(
                        onTap = { offset ->
                            if (viewModel.isPaused.value) {
                                viewModel.togglePause()
                            } else {
                                viewModel.requestFocus(offset.x, offset.y)
                                overlayRef?.startFocusAt(offset.x, offset.y)
                            }
                        },
                        onLongPress = { viewModel.togglePause() },
                    )
                }
                .pointerInput("zoom") {
                    detectTransformGestures { _, _, zoom, _ ->
                        val newRatio = (viewModel.currentZoomRatio.floatValue * zoom)
                            .coerceIn(
                                viewModel.minZoomRatio.floatValue,
                                viewModel.maxZoomRatio.floatValue,
                            )
                        viewModel.currentZoomRatio.floatValue = newRatio
                        viewModel.requestedZoomRatio.floatValue = newRatio
                    }
                },
            factory = { FaceDetectionOverlay(lifecycleOwner, context, viewModel).also { overlayRef = it } },
            update = { overlay ->
                overlayRef = overlay
                if (overlay.currentCameraFacing != cameraFacing) {
                    overlay.initializeCamera(cameraFacing)
                }
                overlay.applyZoom(requestedZoom)
                overlay.setPaused(isPaused)
            },
        )
    }
    DelayedVisibility(!cameraPermissionStatus.value) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Allow Camera Permissions\nThe app cannot work without the camera permission.",
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(text = "Allow")
            }
        }
    }
}

private fun camaraPermissionDialog() {
    createAlertDialog(
        "Camera Permission",
        "The app couldn't function without the camera permission.",
        "ALLOW",
        "CLOSE",
        onPositiveButtonClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
        onNegativeButtonClick = {
            // TODO: Handle deny camera permission action
            //       close the app
        },
    )
}

@Composable
private fun ZoomControls(viewModel: DetectScreenViewModel) {
    val maxZoom = viewModel.maxZoomRatio.floatValue
    val currentZoom = viewModel.currentZoomRatio.floatValue

    val presets = buildList {
        add(1f)
        if (maxZoom >= 2f) add(2f)
        if (maxZoom >= 5f) add(5f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 96.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "%.1fx".format(currentZoom),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                presets.forEach { ratio ->
                    val isSelected = kotlin.math.abs(currentZoom - ratio) < 0.15f
                    OutlinedButton(
                        onClick = { viewModel.setZoom(ratio) },
                        shape = CircleShape,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                            contentColor = Color.White,
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(48.dp),
                    ) {
                        Text("${ratio.toInt()}x", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
