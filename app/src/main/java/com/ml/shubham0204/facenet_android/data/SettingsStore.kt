package com.ml.shubham0204.facenet_android.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.koin.core.annotation.Single

@Single
class SettingsStore(
    context: Context
) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        context.packageName + "_preferences",
        Context.MODE_PRIVATE
    )

    // Generic methods for backward compatibility
    fun save(key: String, value: String) {
        sharedPreferences.edit { putString(key, value) }
    }

    fun get(key: String): String? = sharedPreferences.getString(key, null)

    // Float settings
    private fun saveFloat(key: String, value: Float) {
        sharedPreferences.edit { putFloat(key, value) }
    }

    private fun getFloat(key: String, default: Float): Float {
        return sharedPreferences.getFloat(key, default)
    }

    // Long settings
    private fun saveLong(key: String, value: Long) {
        sharedPreferences.edit { putLong(key, value) }
    }

    private fun getLong(key: String, default: Long): Long {
        return sharedPreferences.getLong(key, default)
    }

    // Recognition Settings
    fun getConfidenceThreshold(): Float = getFloat(KEY_CONFIDENCE_THRESHOLD, 0.70f)
    fun setConfidenceThreshold(value: Float) = saveFloat(KEY_CONFIDENCE_THRESHOLD, value)

    fun getLastSeenMinConfidence(): Float = getFloat(KEY_LAST_SEEN_MIN_CONFIDENCE, 0.70f)
    fun setLastSeenMinConfidence(value: Float) = saveFloat(KEY_LAST_SEEN_MIN_CONFIDENCE, value)

    // Learning Mode Settings
    fun getLearningModeDiversityThreshold(): Float = getFloat(KEY_LEARNING_MODE_DIVERSITY_THRESHOLD, 0.92f)
    fun setLearningModeDiversityThreshold(value: Float) = saveFloat(KEY_LEARNING_MODE_DIVERSITY_THRESHOLD, value)

    fun getAutoCapureIntervalMs(): Long = getLong(KEY_AUTO_CAPTURE_INTERVAL_MS, 2000L)
    fun setAutoCapureIntervalMs(value: Long) = saveLong(KEY_AUTO_CAPTURE_INTERVAL_MS, value)

    // Advanced Learning Settings
    fun getLearningModeMinConfidence(): Float = getFloat(KEY_LEARNING_MODE_MIN_CONFIDENCE, 0.50f)
    fun setLearningModeMinConfidence(value: Float) = saveFloat(KEY_LEARNING_MODE_MIN_CONFIDENCE, value)

    // Reset all settings to defaults
    fun resetToDefaults() {
        sharedPreferences.edit { clear() }
    }

    companion object {
        // Recognition Settings Keys
        private const val KEY_CONFIDENCE_THRESHOLD = "confidence_threshold"
        private const val KEY_LAST_SEEN_MIN_CONFIDENCE = "last_seen_min_confidence"

        // Learning Mode Settings Keys
        private const val KEY_LEARNING_MODE_DIVERSITY_THRESHOLD = "learning_mode_diversity_threshold"
        private const val KEY_AUTO_CAPTURE_INTERVAL_MS = "auto_capture_interval_ms"

        // Advanced Settings Keys
        private const val KEY_LEARNING_MODE_MIN_CONFIDENCE = "learning_mode_min_confidence"
    }
}