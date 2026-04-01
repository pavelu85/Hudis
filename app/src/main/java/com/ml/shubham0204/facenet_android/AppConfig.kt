package com.ml.shubham0204.facenet_android

object AppConfig {
    // Minimum cosine similarity (0–1) required to record a "last seen" timestamp
    const val LAST_SEEN_MIN_CONFIDENCE = 0.7f

    // Minimum cosine similarity required to treat two face embeddings as the same identity
    // during Auto-Monitor (batch import and live camera modes). Adjust to tune sensitivity.
    const val IDENTITY_CERTAINTY_THRESHOLD = 0.70f
}
