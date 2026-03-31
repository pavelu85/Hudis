package com.ml.shubham0204.facenet_android.presentation.screens.results

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ml.shubham0204.facenet_android.data.MatchCandidate
import com.ml.shubham0204.facenet_android.data.ResultsHolder
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class ResultsViewModel(
    private val resultsHolder: ResultsHolder,
) : ViewModel() {
    val candidates: List<MatchCandidate> get() = resultsHolder.candidates
    val detectedFaceBitmap: Bitmap? get() = resultsHolder.detectedFaceBitmap
}
