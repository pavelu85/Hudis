package com.ml.shubham0204.facenet_android.presentation.screens.learn_face

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ml.shubham0204.facenet_android.domain.LearningFrameResult
import com.ml.shubham0204.facenet_android.presentation.components.LearningModeOverlay
import com.ml.shubham0204.facenet_android.presentation.theme.HudisTheme
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnFaceScreen(onNavigateBack: () -> Unit) {
    val viewModel: LearnFaceViewModel = koinViewModel()
    val personName by viewModel.personName
    val capturedCount by viewModel.capturedThisSession
    val lastResult by viewModel.lastFrameResult
    val isAutoCapture by viewModel.isAutoCapture
    val cameraFacing by viewModel.cameraFacing
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    HudisTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (personName.isNotEmpty()) "Improve: $personName" else "Improve Recognition",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "Navigate Back",
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                // Camera + face overlay
                AndroidView(
                    factory = { ctx ->
                        LearningModeOverlay(lifecycleOwner, ctx, viewModel).also {
                            it.initializeCamera(cameraFacing)
                        }
                    },
                    update = { overlay ->
                        if (overlay.currentCameraFacing != cameraFacing) {
                            overlay.initializeCamera(cameraFacing)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                // Capture count badge (top-left)
                if (capturedCount > 0) {
                    Text(
                        text = "+$capturedCount captured",
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                // Status text
                val statusText = when (lastResult) {
                    is LearningFrameResult.NoFace -> if (personName.isNotEmpty()) "Point camera at $personName" else "Point camera at face"
                    is LearningFrameResult.WrongPerson -> "Different person in frame"
                    is LearningFrameResult.TooSimilar -> "Move slightly for a new angle"
                    is LearningFrameResult.MatchFound -> "Face recognized — hold still"
                    is LearningFrameResult.Captured -> "Capturing…"
                }
                Text(
                    text = statusText,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 96.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )

                // Bottom controls
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Auto-capture toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Auto", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        Switch(
                            checked = isAutoCapture,
                            onCheckedChange = { viewModel.isAutoCapture.value = it },
                        )
                    }

                    // Manual capture button
                    Button(onClick = { viewModel.manualCapture() }) {
                        Text("Capture Now")
                    }

                    // Camera flip
                    IconButton(onClick = { viewModel.flipCamera() }) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Flip Camera",
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}
