package com.ml.shubham0204.facenet_android

object AppConfig {
    // Minimum cosine similarity (0–1) required to record a "last seen" timestamp
    const val LAST_SEEN_MIN_CONFIDENCE = 0.7f

    // Minimum cosine similarity required to treat two face embeddings as the same identity
    // during Auto-Monitor (batch import and live camera modes). Adjust to tune sensitivity.
    const val IDENTITY_CERTAINTY_THRESHOLD = 0.70f

    // Learning mode: reject new embeddings with cosine similarity above this value to any
    // *existing database* embedding (hard guard against near-exact duplicates across sessions)
    const val LEARNING_MODE_DB_DUPLICATE_THRESHOLD = 0.97f

    // Learning mode: skip embeddings with cosine similarity above this value to any
    // already-captured embedding in this session (prevents collecting near-duplicate poses)
    const val LEARNING_MODE_DIVERSITY_THRESHOLD = 0.90f

    // Learning mode: minimum similarity to the target person for a face to be eligible
    const val LEARNING_MODE_MIN_CONFIDENCE = 0.50f

    // Learning mode: minimum milliseconds between auto-captures to prevent bursts
    const val LEARNING_MODE_AUTO_CAPTURE_INTERVAL_MS = 2000L
}
