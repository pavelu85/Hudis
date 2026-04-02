package com.ml.shubham0204.facenet_android.domain

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.ml.shubham0204.facenet_android.data.EncounterDB
import com.ml.shubham0204.facenet_android.data.ImagesVectorDB
import com.ml.shubham0204.facenet_android.data.PersonDB
import com.ml.shubham0204.facenet_android.data.PersonRecord
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single
import java.io.File

@Single
class PersonUseCase(
    private val personDB: PersonDB,
    private val encounterDB: EncounterDB,
    private val imagesVectorDB: ImagesVectorDB,
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
        personDB.getById(id)?.profilePhotoPath?.let { File(it).delete() }
        encounterDB.removeAllForPerson(id)
        personDB.removePerson(id)
    }

    fun getAll(): Flow<List<PersonRecord>> = personDB.getAll()  // immutable snapshot per emission

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

    fun updateLastSeen(personID: Long, timestamp: Long) {
        personDB.updateLastSeen(personID, timestamp)
    }

    fun incrementImageCount(personID: Long, count: Int) {
        val existing = personDB.getById(personID) ?: return
        personDB.addPerson(existing.copy(numImages = existing.numImages + count))
    }

    // Saves a Bitmap directly to app private storage (used when the source is a face crop, not a URI).
    fun saveBitmapAsProfilePhoto(bitmap: Bitmap): String? {
        return try {
            val dir = File(context.filesDir, "profile_photos").also { it.mkdirs() }
            val destFile = File(dir, "profile_${System.currentTimeMillis()}.jpg")
            destFile.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    // Merges removeId into keepId: reassigns embeddings + encounters, updates the kept record,
    // cleans up the unchosen profile photo, and deletes the removed person record.
    fun mergePersons(
        keepId: Long,
        removeIds: List<Long>,
        mergedName: String,
        mergedNotes: String,
        chosenPhotoPath: String?,
    ) {
        val keepRecord = personDB.getById(keepId) ?: return
        val allPhotoPaths = mutableListOf(keepRecord.profilePhotoPath)
        var totalImages = keepRecord.numImages

        for (removeId in removeIds) {
            val removeRecord = personDB.getById(removeId) ?: continue
            allPhotoPaths.add(removeRecord.profilePhotoPath)
            totalImages += removeRecord.numImages
            // 1. Re-assign face embeddings and encounter records
            imagesVectorDB.reassignEmbeddings(removeId, keepId, mergedName)
            encounterDB.reassignEncounters(removeId, keepId)
            // 2. Delete the removed person record only (embeddings + encounters already reassigned)
            personDB.removePerson(removeId)
        }

        // 3. Delete all unchosen profile photo files
        for (path in allPhotoPaths.filterNotNull()) {
            if (path != chosenPhotoPath) File(path).delete()
        }

        // 4. Update the surviving person record
        personDB.addPerson(
            keepRecord.copy(
                personName = mergedName,
                notes = mergedNotes,
                profilePhotoPath = chosenPhotoPath,
                numImages = totalImages,
            )
        )
    }

    // Returns person pairs whose embedding centroids exceed the similarity threshold.
    fun findSimilarPersonPairs(threshold: Float = 0.55f) =
        imagesVectorDB.findSimilarPersonPairs(personDB.getAllList(), threshold)

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
