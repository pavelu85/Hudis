package com.ml.shubham0204.facenet_android.domain

import android.graphics.Bitmap

data class ExtractedFace(
    val sourceBitmap: Bitmap,
    val embedding: FloatArray,
    val sourcePhotoIndex: Int,
)

sealed class BatchFaceResolution {
    data class ExistingPerson(
        val face: ExtractedFace,
        val personID: Long,
        val personName: String,
    ) : BatchFaceResolution()

    data class NewCluster(
        val face: ExtractedFace,
        var clusterIndex: Int,
    ) : BatchFaceResolution()
}

data class BatchProcessingResult(
    val totalFacesFound: Int,
    val matchedToExisting: Int,
    val newPersonsCreated: Int,
    val skippedNoFace: Int,
)

sealed class AutoCaptureResult {
    object AlreadyKnown : AutoCaptureResult()
    data class NewPersonCreated(val personID: Long, val personName: String) : AutoCaptureResult()
}
