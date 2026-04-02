package com.ml.shubham0204.facenet_android.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import com.ml.shubham0204.facenet_android.AppConfig
import com.ml.shubham0204.facenet_android.data.EncounterDB
import com.ml.shubham0204.facenet_android.data.FaceImageRecord
import com.ml.shubham0204.facenet_android.data.ImagesVectorDB
import com.ml.shubham0204.facenet_android.data.MatchCandidate
import com.ml.shubham0204.facenet_android.data.PersonDB
import com.ml.shubham0204.facenet_android.data.RecognitionMetrics
import com.ml.shubham0204.facenet_android.domain.embeddings.FaceNet
import com.ml.shubham0204.facenet_android.domain.face_detection.BaseFaceDetector
import com.ml.shubham0204.facenet_android.domain.face_detection.FaceSpoofDetector
import org.koin.core.annotation.Single
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

@Single
class ImageVectorUseCase(
    private val faceDetector: BaseFaceDetector,
    private val faceSpoofDetector: FaceSpoofDetector,
    val imagesVectorDB: ImagesVectorDB,
    private val personDB: PersonDB,
    private val faceNet: FaceNet,
    private val personUseCase: PersonUseCase,
    private val encounterDB: EncounterDB,
    private val context: Context,
) {
    data class FaceRecognitionResult(
        val personName: String,
        val personID: Long = 0,
        val boundingBox: Rect,
        val spoofResult: FaceSpoofDetector.FaceSpoofResult? = null,
        val notes: String = "",
        val similarity: Float = 0f,
        val lastSeenTime: Long = 0,
        val addTime: Long = 0,
        // Embedding is populated for all detected faces so auto-monitor can reuse it
        // without re-running FaceNet.
        val embedding: FloatArray? = null,
    )

    // Add the person's image to the database
    suspend fun addImage(
        personID: Long,
        personName: String,
        imageUri: Uri,
    ): Result<Boolean> {
        // Perform face-detection and get the cropped face as a Bitmap
        val faceDetectionResult = faceDetector.getCroppedFace(imageUri)
        if (faceDetectionResult.isSuccess) {
            // Get the embedding for the cropped face, and store it
            // in the database, along with `personId` and `personName`
            val embedding = faceNet.getFaceEmbedding(faceDetectionResult.getOrNull()!!)
            imagesVectorDB.addFaceImageRecord(
                FaceImageRecord(
                    personID = personID,
                    personName = personName,
                    faceEmbedding = embedding,
                ),
            )
            return Result.success(true)
        } else {
            return Result.failure(faceDetectionResult.exceptionOrNull()!!)
        }
    }

    // From the given frame, return the name of the person by performing
    // face recognition
    suspend fun getNearestPersonName(
        frameBitmap: Bitmap,
        flatSearch: Boolean,
    ): Pair<RecognitionMetrics?, List<FaceRecognitionResult>> {
        // Perform face-detection and get the cropped face as a Bitmap
        val (faceDetectionResult, t1) =
            measureTimedValue { faceDetector.getAllCroppedFaces(frameBitmap) }
        val faceRecognitionResults = ArrayList<FaceRecognitionResult>()
        var avgT2 = 0L
        var avgT3 = 0L
        var avgT4 = 0L

        for (result in faceDetectionResult) {
            // Get the embedding for the cropped face (query embedding)
            val (croppedBitmap, boundingBox) = result
            val (embedding, t2) = measureTimedValue { faceNet.getFaceEmbedding(croppedBitmap) }
            avgT2 += t2.toLong(DurationUnit.MILLISECONDS)
            // Perform nearest-neighbor search
            val (recognitionResult, t3) =
                measureTimedValue { imagesVectorDB.getNearestEmbeddingPersonName(embedding, flatSearch) }
            avgT3 += t3.toLong(DurationUnit.MILLISECONDS)
            if (recognitionResult == null) {
                faceRecognitionResults.add(
                    FaceRecognitionResult(
                        personName = "Not recognized",
                        boundingBox = boundingBox,
                        embedding = embedding,
                    )
                )
                continue
            }

            val spoofResult = faceSpoofDetector.detectSpoof(frameBitmap, boundingBox)
            avgT4 += spoofResult.timeMillis

            // Calculate cosine similarity between the nearest-neighbor
            // and the query embedding
            val distance = cosineDistance(embedding, recognitionResult.faceEmbedding)
            // If the distance > 0.4, we recognize the person
            // else we conclude that the face does not match enough
            if (distance > 0.3) {
                val person = personDB.getById(recognitionResult.personID)
                faceRecognitionResults.add(
                    FaceRecognitionResult(
                        personName = recognitionResult.personName,
                        personID = recognitionResult.personID,
                        boundingBox = boundingBox,
                        spoofResult = spoofResult,
                        notes = person?.notes ?: "",
                        similarity = distance,
                        lastSeenTime = person?.lastSeenTime ?: 0,
                        addTime = person?.addTime ?: 0,
                        embedding = embedding,
                    ),
                )
            } else {
                faceRecognitionResults.add(
                    FaceRecognitionResult(
                        personName = "Not recognized",
                        boundingBox = boundingBox,
                        spoofResult = spoofResult,
                        embedding = embedding,
                    ),
                )
            }
        }
        val metrics =
            if (faceDetectionResult.isNotEmpty()) {
                RecognitionMetrics(
                    timeFaceDetection = t1.toLong(DurationUnit.MILLISECONDS),
                    timeFaceEmbedding = avgT2 / faceDetectionResult.size,
                    timeVectorSearch = avgT3 / faceDetectionResult.size,
                    timeFaceSpoofDetection = avgT4 / faceDetectionResult.size,
                )
            } else {
                null
            }

        return Pair(metrics, faceRecognitionResults)
    }

    private fun cosineDistance(
        x1: FloatArray,
        x2: FloatArray,
    ): Float = com.ml.shubham0204.facenet_android.domain.cosineDistance(x1, x2)

    // Given a single bitmap (e.g. from gallery), detect the largest face, embed it,
    // and return the top-N matching persons from the database.
    // Returns a failure Result if no face could be detected in the bitmap.
    suspend fun getTopNCandidatesFromBitmap(
        bitmap: Bitmap,
        topN: Int = 5,
    ): Result<List<MatchCandidate>> {
        // Reuse the multi-face detector; take the first (typically largest) detected face
        val croppedFaces = faceDetector.getAllCroppedFaces(bitmap)
        if (croppedFaces.isEmpty()) {
            return Result.failure(AppException(ErrorCode.NO_FACE))
        }
        val (croppedFace, _) = croppedFaces.first()
        val embedding = faceNet.getFaceEmbedding(croppedFace)
        val allPersons = personDB.getAllList()
        val candidates = imagesVectorDB.getTopNCandidates(embedding, allPersons, topN)
        return Result.success(candidates)
    }

    fun removeImages(personID: Long) {
        imagesVectorDB.removeFaceRecordsWithPersonID(personID)
    }

    // ---------------------------------------------------------------------------
    // Auto-Monitor: Batch photo processing
    // ---------------------------------------------------------------------------

    // Processes a batch of image URIs and automatically creates or updates person
    // records based on face similarity. Faces matching existing persons
    // (similarity >= threshold) have their embedding added to that person.
    // Unmatched faces are clustered together and saved as new "Unknown N" persons.
    suspend fun processBatchForAutoMonitor(
        imageUris: List<Uri>,
        threshold: Float = AppConfig.IDENTITY_CERTAINTY_THRESHOLD,
        onProgress: suspend (processed: Int, total: Int, status: String) -> Unit,
    ): BatchProcessingResult {
        val extractedFaces = mutableListOf<ExtractedFace>()
        var skippedNoFace = 0

        // Step A: Extract all faces from all photos
        for ((index, uri) in imageUris.withIndex()) {
            onProgress(index, imageUris.size, "Detecting faces in photo ${index + 1}/${imageUris.size}")
            val croppedFaces = faceDetector.getAllCroppedFacesFromUri(uri)
            if (croppedFaces.isEmpty()) {
                skippedNoFace++
                continue
            }
            for ((croppedBitmap, _) in croppedFaces) {
                val embedding = faceNet.getFaceEmbedding(croppedBitmap)
                extractedFaces.add(ExtractedFace(croppedBitmap, embedding, index))
            }
        }

        // Step B: Match each face against the existing DB
        val allPersons = personDB.getAllList()
        val resolutions = mutableListOf<BatchFaceResolution>()
        for (face in extractedFaces) {
            val candidates = imagesVectorDB.getTopNCandidates(face.embedding, allPersons, topN = 1, threshold = threshold)
            if (candidates.isNotEmpty() && candidates[0].similarity >= threshold) {
                resolutions.add(BatchFaceResolution.ExistingPerson(face, candidates[0].personID, candidates[0].personName, candidates[0].similarity))
            } else {
                resolutions.add(BatchFaceResolution.NewCluster(face, clusterIndex = -1))
            }
        }

        // Step C: In-batch deduplication for unmatched faces
        // Each cluster is represented by its running-sum embedding and count.
        data class ClusterState(val faces: MutableList<ExtractedFace>, val embeddingSum: FloatArray, var count: Int)

        val clusters = mutableListOf<ClusterState>()
        for (resolution in resolutions) {
            if (resolution !is BatchFaceResolution.NewCluster) continue
            val face = resolution.face
            var matchedCluster = -1
            for ((ci, cluster) in clusters.withIndex()) {
                val centroid = FloatArray(face.embedding.size) { cluster.embeddingSum[it] / cluster.count }
                if (cosineDistance(face.embedding, centroid) >= threshold) {
                    matchedCluster = ci
                    break
                }
            }
            if (matchedCluster >= 0) {
                val cluster = clusters[matchedCluster]
                cluster.faces.add(face)
                for (i in face.embedding.indices) cluster.embeddingSum[i] += face.embedding[i]
                cluster.count++
                resolution.clusterIndex = matchedCluster
            } else {
                resolution.clusterIndex = clusters.size
                val sum = face.embedding.copyOf()
                clusters.add(ClusterState(mutableListOf(face), sum, 1))
            }
        }

        // Step D: Commit writes
        onProgress(imageUris.size, imageUris.size, "Saving results…")
        var matchedCount = 0

        // Pre-cache GPS coords and reverse-geocoded names per photo index
        val gpsCache = mutableMapOf<Int, Pair<Double, Double>?>()
        val nameCache = mutableMapOf<Int, String>()
        fun gpsForPhoto(index: Int): Pair<Double, Double>? =
            gpsCache.getOrPut(index) { getGpsFromUri(context, imageUris[index]) }
        fun nameForPhoto(index: Int): String =
            nameCache.getOrPut(index) {
                gpsForPhoto(index)?.let { (lat, lon) -> reverseGeocode(context, lat, lon) } ?: ""
                // reverseGeocode is non-null; the ?: "" only triggers if gpsForPhoto is null
            }

        // D1: Add embeddings to existing persons
        val recordedEncounters = mutableSetOf<Pair<Long, Int>>() // personID to photoIndex
        for (res in resolutions) {
            if (res !is BatchFaceResolution.ExistingPerson) continue
            imagesVectorDB.addFaceImageRecord(
                FaceImageRecord(personID = res.personID, personName = res.personName, faceEmbedding = res.face.embedding)
            )
            personUseCase.incrementImageCount(res.personID, 1)
            matchedCount++
            val key = Pair(res.personID, res.face.sourcePhotoIndex)
            if (key !in recordedEncounters) {
                gpsForPhoto(res.face.sourcePhotoIndex)?.let { (lat, lon) ->
                    encounterDB.addEncounter(res.personID, lat, lon, "photo", nameForPhoto(res.face.sourcePhotoIndex), res.similarity)
                }
                recordedEncounters.add(key)
            }
        }

        // D2: Create new Unknown N persons for each cluster
        val baseUnknownCount = personDB.countUnknownPersons()
        for ((ci, cluster) in clusters.withIndex()) {
            val newName = "Unknown ${baseUnknownCount + ci + 1}"
            val profilePath = personUseCase.saveBitmapAsProfilePhoto(cluster.faces[0].sourceBitmap)
            val newPersonID = personUseCase.addPerson(
                name = newName,
                numImages = cluster.faces.size.toLong(),
                profilePhotoPath = profilePath,
            )
            for (face in cluster.faces) {
                imagesVectorDB.addFaceImageRecord(
                    FaceImageRecord(personID = newPersonID, personName = newName, faceEmbedding = face.embedding)
                )
                val key = Pair(newPersonID, face.sourcePhotoIndex)
                if (key !in recordedEncounters) {
                    gpsForPhoto(face.sourcePhotoIndex)?.let { (lat, lon) ->
                        encounterDB.addEncounter(newPersonID, lat, lon, "photo", nameForPhoto(face.sourcePhotoIndex))
                    }
                    recordedEncounters.add(key)
                }
            }
        }

        onProgress(imageUris.size, imageUris.size, "Done")
        return BatchProcessingResult(
            totalFacesFound = extractedFaces.size,
            matchedToExisting = matchedCount,
            newPersonsCreated = clusters.size,
            skippedNoFace = skippedNoFace,
        )
    }

    // ---------------------------------------------------------------------------
    // Auto-Monitor: Live camera capture
    // ---------------------------------------------------------------------------

    // Called when a live-camera face is unrecognized and auto-monitor is ON.
    // If the embedding matches no existing person, creates a new "Unknown N" record.
    // Debouncing (to avoid multi-frame duplicates) is handled by the caller (ViewModel).
    suspend fun checkAndAutoCapture(
        embedding: FloatArray,
        croppedFace: Bitmap,
        threshold: Float = AppConfig.IDENTITY_CERTAINTY_THRESHOLD,
    ): AutoCaptureResult {
        val allPersons = personDB.getAllList()
        val candidates = imagesVectorDB.getTopNCandidates(embedding, allPersons, topN = 1, threshold = threshold)
        if (candidates.isNotEmpty() && candidates[0].similarity >= threshold) {
            return AutoCaptureResult.AlreadyKnown
        }
        val newName = "Unknown ${personDB.countUnknownPersons() + 1}"
        val profilePath = personUseCase.saveBitmapAsProfilePhoto(croppedFace)
        val newID = personUseCase.addPerson(name = newName, numImages = 1L, profilePhotoPath = profilePath)
        imagesVectorDB.addFaceImageRecord(
            FaceImageRecord(personID = newID, personName = newName, faceEmbedding = embedding)
        )
        return AutoCaptureResult.NewPersonCreated(newID, newName)
    }
}
