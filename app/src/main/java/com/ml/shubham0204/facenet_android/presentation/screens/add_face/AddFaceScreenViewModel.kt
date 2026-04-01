package com.ml.shubham0204.facenet_android.presentation.screens.add_face

import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ml.shubham0204.facenet_android.domain.AppException
import com.ml.shubham0204.facenet_android.domain.ImageVectorUseCase
import com.ml.shubham0204.facenet_android.domain.PersonUseCase
import com.ml.shubham0204.facenet_android.presentation.components.setProgressDialogText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class AddFaceScreenViewModel(
    private val personUseCase: PersonUseCase,
    private val imageVectorUseCase: ImageVectorUseCase,
) : ViewModel() {
    val personNameState: MutableState<String> = mutableStateOf("")
    val notesState: MutableState<String> = mutableStateOf("")
    val selectedImageURIs: MutableState<List<Uri>> = mutableStateOf(emptyList())
    // URI of the single profile photo chosen by the user (for display only, not face embedding)
    val profilePhotoUri: MutableState<Uri?> = mutableStateOf(null)

    val isProcessingImages: MutableState<Boolean> = mutableStateOf(false)
    val numImagesProcessed: MutableState<Int> = mutableIntStateOf(0)

    fun addImages() {
        isProcessingImages.value = true
        CoroutineScope(Dispatchers.Default).launch {
            // Copy profile photo to private storage before inserting the record.
            // If no profile photo was explicitly chosen, fall back to the first face image.
            val effectiveProfileUri = profilePhotoUri.value ?: selectedImageURIs.value.firstOrNull()
            val profilePath = effectiveProfileUri?.let { personUseCase.saveProfilePhoto(it) }
            val id =
                personUseCase.addPerson(
                    name = personNameState.value,
                    numImages = selectedImageURIs.value.size.toLong(),
                    notes = notesState.value,
                    profilePhotoPath = profilePath,
                )
            selectedImageURIs.value.forEach {
                imageVectorUseCase
                    .addImage(id, personNameState.value, it)
                    .onFailure {
                        val errorMessage = (it as AppException).errorCode.message
                        setProgressDialogText(errorMessage)
                    }.onSuccess {
                        numImagesProcessed.value += 1
                        setProgressDialogText("Processed ${numImagesProcessed.value} image(s)")
                    }
            }
            isProcessingImages.value = false
        }
    }
}
