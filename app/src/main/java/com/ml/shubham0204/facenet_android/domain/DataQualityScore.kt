package com.ml.shubham0204.facenet_android.domain

import kotlin.math.sqrt

/**
 * Describes how useful a person's stored face data is for recognition.
 *
 * ## Scoring method: Gram-matrix Participation Ratio
 *
 * Given n embeddings e₁…eₙ, build the n×n Gram matrix G where G[i,j] = cosine_similarity(eᵢ, eⱼ).
 *
 *   PR = trace(G)² / ‖G‖²_F  =  n² / Σᵢⱼ G[i,j]²
 *
 * PR is the spectral effective-rank of G — it counts how many *linearly independent* directions
 * the embeddings collectively span:
 *   • PR = 1        → all embeddings identical (rank-1 space, useless repetition)
 *   • PR ≈ 2        → two distinguishable clusters of poses; anything else redundant
 *   • PR = n        → all embeddings mutually orthogonal (theoretical maximum diversity)
 *
 * Unlike simple mean pairwise distance, PR penalises *uneven* variance: 9 near-duplicates + 1
 * outlier yields PR ≈ 1.1, whereas 10 uniformly spread embeddings yield PR ≈ 10.  The
 * difference is invisible to a mean-distance metric but critical for recognition robustness.
 *
 * **coverageScore** = √(1 − 1/PR)  maps PR ∈ [1, ∞) → [0, 1)
 * **quantityScore** = min(n / TARGET_N, 1)
 * **score**         = 0.65 · coverageScore + 0.35 · quantityScore
 *
 * [score]              — composite 0–1
 * [coverageScore]      — √(1 − 1/PR): geometric spread of embeddings in [0, 1)
 * [participationRatio] — raw PR ∈ [1, n]: effective number of independent directions
 * [numEmbeddings]      — raw stored embedding count
 * [label]              — "Poor" / "Fair" / "Good" / "Excellent"
 */
data class DataQualityScore(
    val score: Float,
    val coverageScore: Float,
    val participationRatio: Float,
    val numEmbeddings: Int,
    val label: String,
) {
    companion object {
        val EMPTY = DataQualityScore(
            score = 0f, coverageScore = 0f, participationRatio = 1f,
            numEmbeddings = 0, label = "No data",
        )

        // Number of embeddings at which quantity stops adding meaningful value
        private const val TARGET_N = 20

        fun compute(embeddings: List<FloatArray>): DataQualityScore {
            val n = embeddings.size
            if (n == 0) return EMPTY

            // ‖G‖²_F = Σᵢⱼ cosine_similarity(eᵢ, eⱼ)²
            // Diagonal entries = 1, so the sum is n + 2·Σᵢ<ⱼ sim²
            var gramFrobeniusSq = n.toFloat()    // diagonal contribution (each G[i,i] = 1)
            for (i in 0 until n) {
                for (j in i + 1 until n) {
                    val sim = cosineDistance(embeddings[i], embeddings[j])
                    gramFrobeniusSq += 2f * sim * sim   // G[i,j] and G[j,i]
                }
            }

            // Participation ratio: PR = n² / ‖G‖²_F
            val pr = (n.toFloat() * n.toFloat()) / gramFrobeniusSq

            // Coverage: √(1 − 1/PR) ∈ [0, 1)
            //   PR = 1 → coverage = 0  (all identical)
            //   PR → ∞ → coverage → 1  (fully orthogonal, unreachable for real faces)
            val coverage = sqrt((1f - 1f / pr).coerceAtLeast(0f))

            val quantity = (n.toFloat() / TARGET_N).coerceIn(0f, 1f)
            val composite = (0.65f * coverage + 0.35f * quantity).coerceIn(0f, 1f)

            val label = when {
                composite < 0.30f -> "Poor"
                composite < 0.55f -> "Fair"
                composite < 0.80f -> "Good"
                else              -> "Excellent"
            }

            return DataQualityScore(
                score = composite,
                coverageScore = coverage,
                participationRatio = pr,
                numEmbeddings = n,
                label = label,
            )
        }
    }
}
