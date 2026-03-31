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

    fun save(key: String, value: String) {
        sharedPreferences.edit { putString(key, value) }
    }

    fun get(key: String): String? = sharedPreferences.getString(key, null)
}