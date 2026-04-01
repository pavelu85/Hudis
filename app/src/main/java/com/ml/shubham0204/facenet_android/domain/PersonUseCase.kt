package com.ml.shubham0204.facenet_android.domain

import android.content.Context
import android.net.Uri
import com.ml.shubham0204.facenet_android.data.PersonDB
import com.ml.shubham0204.facenet_android.data.PersonRecord
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single
import java.io.File

@Single
class PersonUseCase(
    private val personDB: PersonDB,
    private val context: Context,
) {
    fun addPerson(
        name: String,
        numImages: Long,
        notes: String = "",
        profilePhotoPath: String? = null,
    ): Long =
        personDB.addPerson(
            PersonRecord(
                personName = name,
                notes = notes,
                profilePhotoPath = profilePhotoPath,
                numImages = numImages,
                addTime = System.currentTimeMillis(),
            ),
        )

    fun removePerson(id: Long) {
        personDB.removePerson(id)
    }

    fun getAll(): Flow<List<PersonRecord>> = personDB.getAll()

    fun getCount(): Long = personDB.getCount()

    fun getById(personID: Long): PersonRecord? = personDB.getById(personID)

    fun updatePerson(personID: Long, name: String, notes: String, newProfilePhotoUri: Uri? = null) {
        val existing = personDB.getById(personID) ?: return
        val newProfilePath = if (newProfilePhotoUri != null) {
            existing.profilePhotoPath?.let { File(it).delete() }
            saveProfilePhoto(newProfilePhotoUri)
        } else {
            existing.profilePhotoPath
        }
        personDB.addPerson(existing.copy(personName = name, notes = notes, profilePhotoPath = newProfilePath))
    }

    fun incrementImageCount(personID: Long, count: Int) {
        val existing = personDB.getById(personID) ?: return
        personDB.addPerson(existing.copy(numImages = existing.numImages + count))
    }

    // Copies the photo at the given content URI to app private storage and returns the absolute path.
    // Storing the absolute path avoids content-URI expiry across app restarts.
    fun saveProfilePhoto(uri: Uri): String? {
        return try {
            val dir = File(context.filesDir, "profile_photos").also { it.mkdirs() }
            val destFile = File(dir, "profile_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
