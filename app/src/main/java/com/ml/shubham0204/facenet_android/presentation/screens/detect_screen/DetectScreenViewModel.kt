package com.ml.shubham0204.facenet_android.presentation.screens.detect_screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ml.shubham0204.facenet_android.AppConfig
import com.ml.shubham0204.facenet_android.data.EncounterDB
import com.ml.shubham0204.facenet_android.data.RecognitionMetrics
import com.ml.shubham0204.facenet_android.data.ResultsHolder
import com.ml.shubham0204.facenet_android.data.SettingsStore
import com.ml.shubham0204.facenet_android.domain.AutoCaptureResult
import com.ml.shubham0204.facenet_android.domain.ImageVectorUseCase
import com.ml.shubham0204.facenet_android.domain.PersonUseCase
import com.ml.shubham0204.facenet_android.domain.getCurrentLocation
import com.ml.shubham0204.facenet_android.domain.reverseGeocode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class DetectScreenViewModel(
    val personUseCase: PersonUseCase,
    val imageVectorUseCase: ImageVectorUseCase,
    val settingsStore: SettingsStore,
    private val resultsHolder: ResultsHolder,
    private val encounterDB: EncounterDB,
    private val context: Context,
) : ViewModel() {
    private val KEY_SETTINGS_CAMERA_FACING = "camera_facing"
    private val CAMERA_FACING_VALUE_BACK = "back"
    private val CAMERA_FACING_VALUE_FRONT = "front"

    val faceDetectionMetricsState = mutableStateOf<RecognitionMetrics?>(null)
    val cameraFacing = mutableIntStateOf(getCameraFacing())

    val currentZoomRatio = mutableFloatStateOf(1f)
    val maxZoomRatio = mutableFloatStateOf(1f)
    val minZoomRatio = mutableFloatStateOf(1f)
    val requestedZoomRatio = mutableFloatStateOf(1f)

    fun setZoom(ratio: Float) {
        requestedZoomRatio.floatValue =
            ratio.coerceIn(minZoomRatio.floatValue, maxZoomRatio.floatValue)
    }

    // Emits Unit to signal the UI to navigate to the results screen
    private val _navigateToResults = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToResults = _navigateToResults.asSharedFlow()

    // Emits an error message when gallery recognition fails
    private val _galleryError = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val galleryError = _galleryError.asSharedFlow()

    val isProcessingGalleryImage = mutableStateOf(false)

    val isPaused = mutableStateOf(false)

    val focusTapEvent = mutableStateOf<Offset?>(null)
    fun requestFocus(x: Float, y: Float) { focusTapEvent.value = Offset(x, y) }
    fun clearFocusTap() { focusTapEvent.value = null }

    // Auto-Monitor: automatically capture and save unknown faces seen on live camera
    val isAutoMonitorEnabled = mutableStateOf(false)
    private val DEBOUNCE_WINDOW_MS = 10_000L
    // Tracks recently auto-created persons to avoid duplicates across consecutive frames
    private val recentAutoCaptures = LinkedHashMap<Long, Long>()  // personID -> timestamp

    fun toggleAutoMonitor() {
        isAutoMonitorEnabled.value = !isAutoMonitorEnabled.value
        if (!isAutoMonitorEnabled.value) recentAutoCaptures.clear()
    }

    // Returns true if a new person was created, false if debounced or already known.
    // Must be called from a coroutine (Dispatchers.Default is fine).
    suspend fun maybeAutoCapture(embedding: FloatArray, croppedFace: Bitmap): Boolean {
        val now = System.currentTimeMillis()
        // Prune stale debounce entries
        recentAutoCaptures.entries.removeIf { it.value < now - DEBOUNCE_WINDOW_MS }
        // Check this face against recently auto-created persons to avoid multi-frame duplicates
        if (recentAutoCaptures.isNotEmpty()) {
            val recentPersons = recentAutoCaptures.keys.mapNotNull { personUseCase.getById(it) }
            val candidates = imageVectorUseCase.imagesVectorDB.getTopNCandidates(
                embedding, recentPersons, topN = 1, threshold = AppConfig.IDENTITY_CERTAINTY_THRESHOLD
            )
            if (candidates.isNotEmpty() && candidates[0].similarity >= AppConfig.IDENTITY_CERTAINTY_THRESHOLD) {
                return false
            }
        }
        return when (val result = imageVectorUseCase.checkAndAutoCapture(embedding, croppedFace)) {
            is AutoCaptureResult.NewPersonCreated -> {
                recentAutoCaptures[result.personID] = now
                true
            }
            else -> false
        }
    }

    // Maps personID -> best similarity seen in the current flush window
    private val seenPersons = mutableMapOf<Long, Float>()

    fun recordSeenPerson(personID: Long, similarity: Float) {
        val current = seenPersons[personID]
        if (current == null || similarity > current) {
            seenPersons[personID] = similarity
        }
    }

    suspend fun flushSeenPersons() {
        val persons = seenPersons.toMap()
        seenPersons.clear()
        if (persons.isEmpty()) return
        val now = System.currentTimeMillis()
        withContext(Dispatchers.IO) {
            val location = getCurrentLocation(context)
            val locationName = location?.let { reverseGeocode(context, it.first, it.second) }
            persons.forEach { (personID, similarity) ->
                personUseCase.updateLastSeen(personID, now)
                if (location != null && locationName != null) {
                    encounterDB.addEncounter(personID, location.first, location.second, "camera", locationName, similarity)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // viewModelScope is already cancelled at this point, so use a fresh scope with
        // NonCancellable to ensure the flush completes even as the ViewModel is destroyed.
        val persons = seenPersons.toMap()
        seenPersons.clear()
        if (persons.isEmpty()) return
        val now = System.currentTimeMillis()
        CoroutineScope(Dispatchers.IO).launch {
            withContext(NonCancellable) {
                val location = getCurrentLocation(context)
                val locationName = location?.let { reverseGeocode(context, it.first, it.second) } ?: ""
                persons.forEach { (personID, similarity) ->
                    personUseCase.updateLastSeen(personID, now)
                    if (location != null) {
                        encounterDB.addEncounter(personID, location.first, location.second, "camera", locationName, similarity)
                    }
                }
            }
        }
    }

    fun togglePause() {
        isPaused.value = !isPaused.value
    }

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
                        candidates.firstOrNull()?.let { top ->
                            if (top.similarity >= AppConfig.LAST_SEEN_MIN_CONFIDENCE) {
                                personUseCase.updateLastSeen(top.personID, System.currentTimeMillis())
                            }
                        }
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
        requestedZoomRatio.floatValue = 1f
        currentZoomRatio.floatValue = 1f
    }
}
