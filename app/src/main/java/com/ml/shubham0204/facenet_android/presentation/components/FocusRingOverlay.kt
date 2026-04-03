package com.ml.shubham0204.facenet_android.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ml.shubham0204.facenet_android.presentation.screens.detect_screen.DetectScreenViewModel
import kotlinx.coroutines.delay

@Composable
fun FocusRingOverlay(viewModel: DetectScreenViewModel, modifier: Modifier = Modifier) {
    val tapOffset by remember { viewModel.focusTapEvent }
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    var ringOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(tapOffset) {
        val offset = tapOffset ?: return@LaunchedEffect
        ringOffset = offset
        alpha.snapTo(1f)
        scale.snapTo(1.3f)
        scale.animateTo(1f, tween(150))
        delay(600)
        alpha.animateTo(0f, tween(500))
        viewModel.clearFocusTap()
    }

    if (alpha.value > 0f) {
        Canvas(modifier = modifier) {
            drawCircle(
                color = Color.White.copy(alpha = alpha.value),
                radius = 40.dp.toPx() * scale.value,
                center = ringOffset,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}
