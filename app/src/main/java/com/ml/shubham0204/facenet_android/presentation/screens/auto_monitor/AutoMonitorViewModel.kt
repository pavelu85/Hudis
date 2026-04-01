package com.ml.shubham0204.facenet_android.presentation.screens.auto_monitor

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ml.shubham0204.facenet_android.domain.BatchProcessingResult
import com.ml.shubham0204.facenet_android.domain.ImageVectorUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class AutoMonitorViewModel(
    private val imageVectorUseCase: ImageVectorUseCase,
) : ViewModel() {

    val batchIsProcessing = mutableStateOf(false)
    val batchProgress = mutableIntStateOf(0)
    val batchTotal = mutableIntStateOf(0)
    val batchStatusMessage = mutableStateOf("")
    val batchResult = mutableStateOf<BatchProcessingResult?>(null)

    fun startBatchProcessing(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.Default) {
            batchIsProcessing.value = true
            batchProgress.intValue = 0
            batchTotal.intValue = uris.size
            batchResult.value = null
            batchResult.value = imageVectorUseCase.processBatchForAutoMonitor(
                imageUris = uris,
                onProgress = { processed, total, msg ->
                    batchProgress.intValue = processed
                    batchTotal.intValue = total
                    batchStatusMessage.value = msg
                },
            )
            batchIsProcessing.value = false
        }
    }
}
