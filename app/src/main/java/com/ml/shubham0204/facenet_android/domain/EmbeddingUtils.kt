package com.ml.shubham0204.facenet_android.domain

import kotlin.math.sqrt

fun cosineDistance(x1: FloatArray, x2: FloatArray): Float {
    var mag1 = 0.0f
    var mag2 = 0.0f
    var product = 0.0f
    for (i in x1.indices) {
        mag1 += x1[i] * x1[i]
        mag2 += x2[i] * x2[i]
        product += x1[i] * x2[i]
    }
    return product / (sqrt(mag1) * sqrt(mag2))
}

// Component-wise mean of a list of embeddings. Used for cluster centroid computation.
fun meanEmbedding(embeddings: List<FloatArray>): FloatArray {
    require(embeddings.isNotEmpty())
    val dim = embeddings[0].size
    val sum = FloatArray(dim)
    for (e in embeddings) for (i in e.indices) sum[i] += e[i]
    return FloatArray(dim) { sum[it] / embeddings.size }
}
