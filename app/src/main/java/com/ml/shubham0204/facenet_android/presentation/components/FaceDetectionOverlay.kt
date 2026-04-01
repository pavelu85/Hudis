package com.ml.shubham0204.facenet_android.presentation.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.text.format.DateUtils
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRectF
import androidx.core.view.doOnLayout
import androidx.lifecycle.LifecycleOwner
import com.ml.shubham0204.facenet_android.presentation.screens.detect_screen.DetectScreenViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.ml.shubham0204.facenet_android.AppConfig

@SuppressLint("ViewConstructor")
@ExperimentalGetImage
class FaceDetectionOverlay(
    private val lifecycleOwner: LifecycleOwner,
    private val context: Context,
    private val viewModel: DetectScreenViewModel,
) : FrameLayout(context) {
    // Setting `flatSearch` to `true` enables precise calculation
    // of cosine similarity.
    // This is slower than ObjectBox's vector search, which approximates
    // nearest neighbor search
    private val flatSearch: Boolean = false
    private var overlayWidth: Int = 0
    private var overlayHeight: Int = 0

    private var imageTransform: Matrix = Matrix()
    private var boundingBoxTransform: Matrix = Matrix()
    private var isImageTransformedInitialized = false
    private var isBoundingBoxTransformedInitialized = false

    private lateinit var frameBitmap: Bitmap
    private var isProcessing = false
    private var cameraFacing: Int? = null
    private lateinit var boundingBoxOverlay: BoundingBoxOverlay
    private lateinit var previewView: PreviewView
    private lateinit var frozenFrameView: ImageView

    var predictions: Array<Prediction> = arrayOf()

    private var camera: Camera? = null
    var currentCameraFacing: Int = -1
        private set

    init {
        doOnLayout {
            overlayHeight = it.measuredHeight
            overlayWidth = it.measuredWidth
        }
    }

    fun applyZoom(ratio: Float) {
        if (currentCameraFacing == CameraSelector.LENS_FACING_FRONT) return
        camera?.cameraControl?.setZoomRatio(ratio)
    }

    fun setPaused(paused: Boolean) {
        if (!::frozenFrameView.isInitialized) return
        if (paused) {
            frozenFrameView.setImageBitmap(previewView.bitmap)
            frozenFrameView.visibility = View.VISIBLE
        } else {
            frozenFrameView.visibility = View.GONE
            frozenFrameView.setImageBitmap(null)
        }
    }

    fun initializeCamera(cameraFacing: Int) {
        currentCameraFacing = cameraFacing
        this.cameraFacing = cameraFacing
        this.isImageTransformedInitialized = false
        this.isBoundingBoxTransformedInitialized = false
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val previewView = PreviewView(context)
        val executor = ContextCompat.getMainExecutor(context)
        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()
                val preview =
                    Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                val cameraSelector =
                    CameraSelector.Builder().requireLensFacing(cameraFacing).build()
                val frameAnalyzer =
                    ImageAnalysis
                        .Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build()
                frameAnalyzer.setAnalyzer(Executors.newSingleThreadExecutor(), analyzer)
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    frameAnalyzer,
                )
                camera?.cameraInfo?.zoomState?.observe(lifecycleOwner) { state ->
                    viewModel.minZoomRatio.floatValue = state.minZoomRatio
                    viewModel.maxZoomRatio.floatValue = state.maxZoomRatio
                    viewModel.currentZoomRatio.floatValue = state.zoomRatio
                }
                val pending = viewModel.requestedZoomRatio.floatValue
                if (pending != 1f) camera?.cameraControl?.setZoomRatio(pending)
            },
            executor,
        )
        removeAllViews()
        this.previewView = previewView
        addView(this.previewView)

        this.frozenFrameView = ImageView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = View.GONE
        }
        addView(this.frozenFrameView)

        val boundingBoxOverlayParams =
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        this.boundingBoxOverlay = BoundingBoxOverlay(context)
        this.boundingBoxOverlay.setWillNotDraw(false)
        this.boundingBoxOverlay.setZOrderOnTop(true)
        addView(this.boundingBoxOverlay, boundingBoxOverlayParams)
    }

    private val analyzer =
        ImageAnalysis.Analyzer { image ->
            if (isProcessing || viewModel.isPaused.value) {
                image.close()
                return@Analyzer
            }
            isProcessing = true

            // Transform android.net.Image to Bitmap
            frameBitmap =
                createBitmap(image.image!!.width, image.image!!.height)
            frameBitmap.copyPixelsFromBuffer(image.planes[0].buffer)

            // Configure frameHeight and frameWidth for output2overlay transformation matrix
            // and apply it to `frameBitmap`
            if (!isImageTransformedInitialized) {
                imageTransform = Matrix()
                imageTransform.apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }
                isImageTransformedInitialized = true
            }
            frameBitmap =
                Bitmap.createBitmap(
                    frameBitmap,
                    0,
                    0,
                    frameBitmap.width,
                    frameBitmap.height,
                    imageTransform,
                    false,
                )

            if (!isBoundingBoxTransformedInitialized) {
                boundingBoxTransform = Matrix()
                boundingBoxTransform.apply {
                    setScale(
                        overlayWidth / frameBitmap.width.toFloat(),
                        overlayHeight / frameBitmap.height.toFloat(),
                    )
                    if (cameraFacing == CameraSelector.LENS_FACING_FRONT) {
                        // Mirror the bounding box coordinates
                        // for front-facing camera
                        postScale(
                            -1f,
                            1f,
                            overlayWidth.toFloat() / 2.0f,
                            overlayHeight.toFloat() / 2.0f,
                        )
                    }
                }
                isBoundingBoxTransformedInitialized = true
            }
            CoroutineScope(Dispatchers.Default).launch {
                val predictions = ArrayList<Prediction>()
                val (metrics, results) =
                    viewModel.imageVectorUseCase.getNearestPersonName(
                        frameBitmap,
                        flatSearch,
                    )
                results.forEach { result ->
                    val box = result.boundingBox.toRectF()
                    var personName = result.personName
                    if (viewModel.getNumPeople().toInt() == 0) {
                        personName = ""
                    }
                    if (result.spoofResult != null && result.spoofResult.isSpoof) {
                        personName = "$personName (Spoof: ${result.spoofResult.score})"
                    }
                    if (result.similarity >= AppConfig.LAST_SEEN_MIN_CONFIDENCE && result.personID > 0) {
                        viewModel.recordSeenPerson(result.personID)
                    }
                    boundingBoxTransform.mapRect(box)
                    predictions.add(Prediction(box, personName, result.notes, result.similarity, result.lastSeenTime, result.addTime))
                }
                withContext(Dispatchers.Main) {
                    viewModel.faceDetectionMetricsState.value = metrics
                    this@FaceDetectionOverlay.predictions = predictions.toTypedArray()
                    boundingBoxOverlay.invalidate()
                    isProcessing = false
                }
            }
            image.close()
        }

    data class Prediction(
        var bbox: RectF,
        var label: String,
        var notes: String = "",
        var similarity: Float = 0f,
        var lastSeenTime: Long = 0,
        var addTime: Long = 0,
    )

    inner class BoundingBoxOverlay(
        context: Context,
    ) : SurfaceView(context),
        SurfaceHolder.Callback {
        private val boxPaint =
            Paint().apply {
                color = "#4D90caf9".toColorInt()
                style = Paint.Style.FILL
            }
        private val namePaint =
            Paint().apply {
                strokeWidth = 2.0f
                textSize = 36f
                textAlign = Paint.Align.CENTER
                color = Color.WHITE
            }
        private val subTextPaint =
            Paint().apply {
                strokeWidth = 1.5f
                textSize = 28f
                textAlign = Paint.Align.CENTER
                color = "#CCFFFFFF".toColorInt()
            }

        override fun surfaceCreated(holder: SurfaceHolder) {}

        override fun surfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int,
        ) {}

        override fun surfaceDestroyed(holder: SurfaceHolder) {}

        override fun onDraw(canvas: Canvas) {
            predictions.forEach { pred ->
                canvas.drawRoundRect(pred.bbox, 16f, 16f, boxPaint)

                val lines = mutableListOf(pred.label)
                if (pred.similarity > 0f) lines.add("${(pred.similarity * 100).toInt()}%")
                if (pred.similarity > 0f && pred.lastSeenTime == 0L) {
                    lines.add("First detection!")
                } else if (pred.lastSeenTime > 0) {
                    lines.add("Last seen: ${DateUtils.getRelativeTimeSpanString(
                        pred.lastSeenTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
                    )}")
                }
                if (pred.notes.isNotEmpty()) lines.add(pred.notes)

                val lineHeight = 42f
                val totalHeight = lines.size * lineHeight
                var y = pred.bbox.centerY() - totalHeight / 2 + lineHeight

                lines.forEachIndexed { i, text ->
                    val paint = if (i == 0) namePaint else subTextPaint
                    canvas.drawText(text, pred.bbox.centerX(), y, paint)
                    y += lineHeight
                }
            }
        }
    }
}
