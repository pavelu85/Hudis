package com.ml.shubham0204.facenet_android.presentation.screens.learn_face

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ml.shubham0204.facenet_android.AppConfig
import com.ml.shubham0204.facenet_android.domain.ImageVectorUseCase
import com.ml.shubham0204.facenet_android.domain.LearningFrameResult
import com.ml.shubham0204.facenet_android.domain.PersonUseCase
import com.ml.shubham0204.facenet_android.domain.cosineDistance
import com.ml.shubham0204.facenet_android.domain.embeddings.FaceNet
import com.ml.shubham0204.facenet_android.domain.face_detection.BaseFaceDetector
import com.ml.shubham0204.facenet_android.domain.face_detection.FaceSpoofDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import java.util.concurrent.atomic.AtomicBoolean

@KoinViewModel
class LearnFaceViewModel(
    private val personUseCase: PersonUseCase,
    private val imageVectorUseCase: ImageVectorUseCase,
    private val faceDetector: BaseFaceDetector,
    private val faceNet: FaceNet,
    private val faceSpoofDetector: FaceSpoofDetector,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val personID: Long = savedStateHandle.get<Long>("personId")!!

    val personName = mutableStateOf("")
    val capturedThisSession = mutableIntStateOf(0)
    val lastFrameResult = mutableStateOf<LearningFrameResult>(LearningFrameResult.NoFace)
    val isAutoCapture = mutableStateOf(true)
    val cameraFacing = mutableIntStateOf(CameraSelector.LENS_FACING_BACK)

    val currentZoomRatio = mutableFloatStateOf(1f)
    val maxZoomRatio = mutableFloatStateOf(1f)
    val minZoomRatio = mutableFloatStateOf(1f)
    val requestedZoomRatio = mutableFloatStateOf(1f)

    fun setZoom(ratio: Float) {
        requestedZoomRatio.floatValue =
            ratio.coerceIn(minZoomRatio.floatValue, maxZoomRatio.floatValue)
    }

    private val sessionEmbeddings = mutableListOf<FloatArray>()
    private val existingEmbeddingsLoaded = AtomicBoolean(false)
    private var lastAutoCaptureTime = 0L
    private var lastGoodEmbedding: FloatArray? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            personUseCase.getById(personID)?.let { person ->
                withContext(Dispatchers.Main) { personName.value = person.personName }
            }
        }
    }

    private suspend fun ensureExistingEmbeddingsLoaded() {
        if (existingEmbeddingsLoaded.get()) return
        val embeddings = withContext(Dispatchers.IO) {
            imageVectorUseCase.getPersonEmbeddings(personID)
        }
        sessionEmbeddings.addAll(embeddings)
        existingEmbeddingsLoaded.set(true)
    }

    private fun maxSimilarityToSession(embedding: FloatArray): Float {
        if (sessionEmbeddings.isEmpty()) return 0f
        return sessionEmbeddings.maxOf { cosineDistance(embedding, it) }
    }

    private suspend fun commitCapture(embedding: FloatArray) {
        val name = personName.value
        withContext(Dispatchers.IO) {
            imageVectorUseCase.addLiveEmbedding(personID, name, embedding)
        }
        sessionEmbeddings.add(embedding)
        capturedThisSession.intValue++
        lastAutoCaptureTime = System.currentTimeMillis()
    }

    suspend fun processFrame(frameBitmap: Bitmap): LearningFrameResult {
        ensureExistingEmbeddingsLoaded()

        val faces = faceDetector.getAllCroppedFaces(frameBitmap)
        if (faces.isEmpty()) {
            return LearningFrameResult.NoFace.also { lastFrameResult.value = it }
        }

        val (croppedBitmap, boundingBox) = faces.first()
        val embedding = faceNet.getFaceEmbedding(croppedBitmap)

        // Spoof check
        val spoofResult = faceSpoofDetector.detectSpoof(frameBitmap, boundingBox)
        if (spoofResult.isSpoof) {
            return LearningFrameResult.WrongPerson.also { lastFrameResult.value = it }
        }

        // Check similarity to the target person
        val personRecord = withContext(Dispatchers.IO) { personUseCase.getById(personID) }
        if (personRecord == null) {
            return LearningFrameResult.WrongPerson.also { lastFrameResult.value = it }
        }

        val allPersonEmbeddings = sessionEmbeddings
        val similarity = if (allPersonEmbeddings.isEmpty()) {
            AppConfig.LEARNING_MODE_MIN_CONFIDENCE  // no prior data — assume match
        } else {
            allPersonEmbeddings.maxOf { cosineDistance(embedding, it) }
        }

        if (similarity < AppConfig.LEARNING_MODE_MIN_CONFIDENCE) {
            return LearningFrameResult.WrongPerson.also { lastFrameResult.value = it }
        }

        // Diversity check
        val maxSessionSim = maxSimilarityToSession(embedding)
        if (maxSessionSim > AppConfig.LEARNING_MODE_DIVERSITY_THRESHOLD) {
            lastGoodEmbedding = embedding
            return LearningFrameResult.TooSimilar(similarity).also { lastFrameResult.value = it }
        }

        lastGoodEmbedding = embedding

        // Auto-capture
        if (isAutoCapture.value) {
            val now = System.currentTimeMillis()
            if (now - lastAutoCaptureTime >= AppConfig.LEARNING_MODE_AUTO_CAPTURE_INTERVAL_MS) {
                commitCapture(embedding)
                return LearningFrameResult.Captured(capturedThisSession.intValue)
                    .also { lastFrameResult.value = it }
            }
        }

        return LearningFrameResult.MatchFound(similarity, embedding)
            .also { lastFrameResult.value = it }
    }

    fun manualCapture() {
        val emb = lastGoodEmbedding ?: return
        viewModelScope.launch {
            commitCapture(emb)
            lastGoodEmbedding = null
        }
    }

    fun flipCamera() {
        cameraFacing.intValue = if (cameraFacing.intValue == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        requestedZoomRatio.floatValue = 1f
        currentZoomRatio.floatValue = 1f
    }
}
