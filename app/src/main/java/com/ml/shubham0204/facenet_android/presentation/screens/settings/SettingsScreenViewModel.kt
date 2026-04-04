package com.ml.shubham0204.facenet_android.presentation.screens.settings

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ml.shubham0204.facenet_android.data.SettingsStore
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class SettingsScreenViewModel(
    private val settingsStore: SettingsStore,
) : ViewModel() {

    // Recognition Settings
    val confidenceThreshold = mutableFloatStateOf(settingsStore.getConfidenceThreshold())
    val lastSeenMinConfidence = mutableFloatStateOf(settingsStore.getLastSeenMinConfidence())

    // Learning Mode Settings
    val learningModeDiversityThreshold = mutableFloatStateOf(settingsStore.getLearningModeDiversityThreshold())
    val autoCapureIntervalMs = mutableLongStateOf(settingsStore.getAutoCapureIntervalMs())

    // Advanced Settings
    val learningModeMinConfidence = mutableFloatStateOf(settingsStore.getLearningModeMinConfidence())

    fun setConfidenceThreshold(value: Float) {
        confidenceThreshold.floatValue = value
        viewModelScope.launch {
            settingsStore.setConfidenceThreshold(value)
        }
    }

    fun setLastSeenMinConfidence(value: Float) {
        lastSeenMinConfidence.floatValue = value
        viewModelScope.launch {
            settingsStore.setLastSeenMinConfidence(value)
        }
    }

    fun setLearningModeDiversityThreshold(value: Float) {
        learningModeDiversityThreshold.floatValue = value
        viewModelScope.launch {
            settingsStore.setLearningModeDiversityThreshold(value)
        }
    }

    fun setAutoCapureIntervalMs(value: Long) {
        autoCapureIntervalMs.longValue = value
        viewModelScope.launch {
            settingsStore.setAutoCapureIntervalMs(value)
        }
    }

    fun setLearningModeMinConfidence(value: Float) {
        learningModeMinConfidence.floatValue = value
        viewModelScope.launch {
            settingsStore.setLearningModeMinConfidence(value)
        }
    }

    fun resetToDefaults() {
        settingsStore.resetToDefaults()
        confidenceThreshold.floatValue = 0.70f
        lastSeenMinConfidence.floatValue = 0.70f
        learningModeDiversityThreshold.floatValue = 0.92f
        autoCapureIntervalMs.longValue = 2000L
        learningModeMinConfidence.floatValue = 0.50f
    }
}
