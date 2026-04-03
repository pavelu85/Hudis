package com.ml.shubham0204.facenet_android.presentation.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.core.view.doOnLayout
import androidx.lifecycle.LifecycleOwner
import com.ml.shubham0204.facenet_android.domain.LearningFrameResult
import com.ml.shubham0204.facenet_android.presentation.screens.learn_face.LearnFaceViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

@SuppressLint("ViewConstructor")
@ExperimentalGetImage
class LearningModeOverlay(
    private val lifecycleOwner: LifecycleOwner,
    private val context: Context,
    private val viewModel: LearnFaceViewModel,
) : FrameLayout(context) {

    private var overlayWidth: Int = 0
    private var overlayHeight: Int = 0

    private var imageTransform: Matrix = Matrix()
    private var boundingBoxTransform: Matrix = Matrix()
    private var isImageTransformInitialized = false
    private var isBoundingBoxTransformInitialized = false

    private lateinit var frameBitmap: Bitmap
    private var isProcessing = false
    private var cameraFacing: Int = CameraSelector.LENS_FACING_BACK

    private lateinit var boundingBoxOverlay: BoundingBoxOverlay
    private lateinit var previewView: PreviewView

    var currentCameraFacing: Int = -1
        private set

    init {
        doOnLayout {
            overlayHeight = it.measuredHeight
            overlayWidth = it.measuredWidth
        }
    }

    fun initializeCamera(cameraFacing: Int) {
        currentCameraFacing = cameraFacing
        this.cameraFacing = cameraFacing
        isImageTransformInitialized = false
        isBoundingBoxTransformInitialized = false

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val previewView = PreviewView(context)
        val executor = ContextCompat.getMainExecutor(context)

        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(cameraFacing)
                    .build()
                val frameAnalyzer = ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                frameAnalyzer.setAnalyzer(Executors.newSingleThreadExecutor(), analyzer)
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, frameAnalyzer)
            },
            executor,
        )

        removeAllViews()
        this.previewView = previewView
        addView(this.previewView)

        val overlayParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        this.boundingBoxOverlay = BoundingBoxOverlay(context)
        this.boundingBoxOverlay.setWillNotDraw(false)
        this.boundingBoxOverlay.setZOrderOnTop(true)
        addView(this.boundingBoxOverlay, overlayParams)
    }

    private val analyzer = ImageAnalysis.Analyzer { image ->
        if (isProcessing) {
            image.close()
            return@Analyzer
        }
        isProcessing = true

        frameBitmap = createBitmap(image.image!!.width, image.image!!.height)
        frameBitmap.copyPixelsFromBuffer(image.planes[0].buffer)

        if (!isImageTransformInitialized) {
            imageTransform = Matrix()
            imageTransform.postRotate(image.imageInfo.rotationDegrees.toFloat())
            isImageTransformInitialized = true
        }
        frameBitmap = Bitmap.createBitmap(
            frameBitmap, 0, 0, frameBitmap.width, frameBitmap.height, imageTransform, false
        )

        if (!isBoundingBoxTransformInitialized) {
            boundingBoxTransform = Matrix()
            boundingBoxTransform.setScale(
                overlayWidth / frameBitmap.width.toFloat(),
                overlayHeight / frameBitmap.height.toFloat(),
            )
            if (cameraFacing == CameraSelector.LENS_FACING_FRONT) {
                boundingBoxTransform.postScale(
                    -1f, 1f,
                    overlayWidth.toFloat() / 2.0f,
                    overlayHeight.toFloat() / 2.0f,
                )
            }
            isBoundingBoxTransformInitialized = true
        }

        CoroutineScope(Dispatchers.Default).launch {
            val result = viewModel.processFrame(frameBitmap)
            withContext(Dispatchers.Main) {
                boundingBoxOverlay.lastResult = result
                boundingBoxOverlay.invalidate()
                isProcessing = false
            }
        }
        image.close()
    }

    inner class BoundingBoxOverlay(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

        var lastResult: LearningFrameResult = LearningFrameResult.NoFace

        private val greenPaint = Paint().apply {
            color = "#CC4CAF50".toColorInt()
            style = Paint.Style.FILL
        }
        private val redPaint = Paint().apply {
            color = "#CCF44336".toColorInt()
            style = Paint.Style.FILL
        }
        private val amberPaint = Paint().apply {
            color = "#CCFF9800".toColorInt()
            style = Paint.Style.FILL
        }
        private val textPaint = Paint().apply {
            strokeWidth = 2.0f
            textSize = 36f
            textAlign = Paint.Align.CENTER
            color = Color.WHITE
        }

        override fun surfaceCreated(holder: SurfaceHolder) {}
        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
        override fun surfaceDestroyed(holder: SurfaceHolder) {}

        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f
            val cy = height / 2f
            val radius = minOf(width, height) * 0.35f
            val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

            when (val r = lastResult) {
                is LearningFrameResult.MatchFound -> {
                    canvas.drawOval(rect, greenPaint)
                    canvas.drawText("${(r.similarity * 100).toInt()}%", cx, cy + textPaint.textSize / 3, textPaint)
                }
                is LearningFrameResult.Captured -> {
                    canvas.drawOval(rect, greenPaint)
                    canvas.drawText("Captured!", cx, cy + textPaint.textSize / 3, textPaint)
                }
                is LearningFrameResult.TooSimilar -> {
                    canvas.drawOval(rect, amberPaint)
                    canvas.drawText("${(r.similarity * 100).toInt()}%", cx, cy + textPaint.textSize / 3, textPaint)
                }
                is LearningFrameResult.WrongPerson -> {
                    canvas.drawOval(rect, redPaint)
                }
                is LearningFrameResult.NoFace -> { /* nothing */ }
            }
        }
    }
}
