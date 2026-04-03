package com.ml.shubham0204.facenet_android.domain

sealed class LearningFrameResult {
    /** A face was detected and matched the target person above the confidence threshold. */
    data class MatchFound(val similarity: Float, val embedding: FloatArray) : LearningFrameResult()

    /** A face was detected but did not match the target person. */
    object WrongPerson : LearningFrameResult()

    /** No face was detected in this frame. */
    object NoFace : LearningFrameResult()

    /** A match was found but the embedding is too similar to already-captured ones (diversity skipped). */
    data class TooSimilar(val similarity: Float) : LearningFrameResult()

    /** An embedding was successfully captured and saved. */
    data class Captured(val newTotalCount: Int) : LearningFrameResult()
}
