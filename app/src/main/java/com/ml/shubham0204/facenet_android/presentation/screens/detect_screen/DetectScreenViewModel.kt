package com.ml.shubham0204.facenet_android.presentation.screens.detect_screen

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ml.shubham0204.facenet_android.data.RecognitionMetrics
import com.ml.shubham0204.facenet_android.data.ResultsHolder
import com.ml.shubham0204.facenet_android.data.SettingsStore
import com.ml.shubham0204.facenet_android.domain.ImageVectorUseCase
import com.ml.shubham0204.facenet_android.domain.PersonUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class DetectScreenViewModel(
    val personUseCase: PersonUseCase,
    val imageVectorUseCase: ImageVectorUseCase,
    val settingsStore: SettingsStore,
    private val resultsHolder: ResultsHolder,
    private val context: Context,
) : ViewModel() {
    private val KEY_SETTINGS_CAMERA_FACING = "camera_facing"
    private val CAMERA_FACING_VALUE_BACK = "back"
    private val CAMERA_FACING_VALUE_FRONT = "front"

    val faceDetectionMetricsState = mutableStateOf<RecognitionMetrics?>(null)
    val cameraFacing = mutableIntStateOf(getCameraFacing())

    // Emits Unit to signal the UI to navigate to the results screen
    private val _navigateToResults = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToResults = _navigateToResults.asSharedFlow()

    // Emits an error message when gallery recognition fails
    private val _galleryError = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val galleryError = _galleryError.asSharedFlow()

    val isProcessingGalleryImage = mutableStateOf(false)

    fun processGalleryImage(uri: Uri) {
        viewModelScope.launch(Dispatchers.Default) {
            isProcessingGalleryImage.value = true
            try {
                val bitmap =
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                if (bitmap == null) {
                    _galleryError.emit("Could not read the selected image.")
                    return@launch
                }
                val result = imageVectorUseCase.getTopNCandidatesFromBitmap(bitmap)
                result
                    .onSuccess { candidates ->
                        resultsHolder.candidates = candidates
                        resultsHolder.detectedFaceBitmap = bitmap
                        _navigateToResults.emit(Unit)
                    }.onFailure { error ->
                        _galleryError.emit(error.message ?: "Face detection failed.")
                    }
            } finally {
                isProcessingGalleryImage.value = false
            }
        }
    }

    fun getNumPeople(): Long = personUseCase.getCount()

    private fun getCameraFacing(): Int {
        val cameraFacing = settingsStore.get(KEY_SETTINGS_CAMERA_FACING)
        return if (cameraFacing == CAMERA_FACING_VALUE_FRONT) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
    }

    private fun saveCameraFacingSetting(cameraFacing: Int) {
        settingsStore.save(
            KEY_SETTINGS_CAMERA_FACING,
            if (cameraFacing == CameraSelector.LENS_FACING_FRONT) {
                CAMERA_FACING_VALUE_FRONT
            } else {
                CAMERA_FACING_VALUE_BACK
            }
        )
    }

    fun changeCameraFacing() {
        if (cameraFacing.intValue == CameraSelector.LENS_FACING_FRONT) {
            cameraFacing.intValue = CameraSelector.LENS_FACING_BACK
        } else {
            cameraFacing.intValue = CameraSelector.LENS_FACING_FRONT
        }
        saveCameraFacingSetting(cameraFacing.intValue)
    }
}
