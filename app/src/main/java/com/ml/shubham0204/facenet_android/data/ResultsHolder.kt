package com.ml.shubham0204.facenet_android.data

import android.graphics.Bitmap
import org.koin.core.annotation.Single

/**
 * Koin singleton that acts as an in-memory channel between DetectScreenViewModel
 * (which writes results) and ResultsViewModel (which reads them).
 * This avoids passing large objects through navigation arguments.
 */
@Single
class ResultsHolder {
    var candidates: List<MatchCandidate> = emptyList()
    var detectedFaceBitmap: Bitmap? = null
}
