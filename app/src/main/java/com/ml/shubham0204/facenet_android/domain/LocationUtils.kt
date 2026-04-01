package com.ml.shubham0204.facenet_android.domain

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Extracts GPS coordinates from a photo's EXIF metadata.
 * Returns null if the photo has no GPS data or the URI cannot be read.
 */
fun getGpsFromUri(context: Context, uri: Uri): Pair<Double, Double>? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)
            val latLon = FloatArray(2)
            if (exif.getLatLong(latLon)) Pair(latLon[0].toDouble(), latLon[1].toDouble()) else null
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Gets the device's current location using FusedLocationProviderClient.
 * Returns null if location permission is not granted or location is unavailable.
 */
suspend fun getCurrentLocation(context: Context): Pair<Double, Double>? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED
    ) {
        return null
    }
    return try {
        suspendCancellableCoroutine { cont ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { location ->
                    cont.resume(location?.let { Pair(it.latitude, it.longitude) })
                }
                .addOnFailureListener {
                    cont.resume(null)
                }
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Reverse-geocodes coordinates to a human-readable label like "Wenceslas Square, Prague"
 * or "Hlavní 12, Brno". Falls back to formatted GPS coordinates if geocoding is
 * unavailable or returns no result, so callers always receive a non-blank string.
 * Must be called from a background thread (Geocoder is blocking).
 */
fun reverseGeocode(context: Context, lat: Double, lon: Double): String {
    val coordsFallback = "%.5f, %.5f".format(lat, lon)
    return try {
        val geocoder = Geocoder(context)
        @Suppress("DEPRECATION")
        val addresses = geocoder.getFromLocation(lat, lon, 1)
        val address = addresses?.firstOrNull() ?: return coordsFallback
        // Build a concise label: prefer landmark/feature name, then street, then neighbourhood/city
        val parts = listOfNotNull(
            address.featureName?.takeIf { it != address.thoroughfare && !it.matches(Regex("\\d+")) },
            address.thoroughfare,
            address.subLocality,
            address.locality ?: address.subAdminArea,
        ).distinctBy { it }.take(2)
        parts.joinToString(", ").ifBlank { coordsFallback }
    } catch (_: Exception) {
        coordsFallback
    }
}
