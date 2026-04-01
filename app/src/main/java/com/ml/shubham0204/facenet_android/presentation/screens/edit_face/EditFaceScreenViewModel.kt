package com.ml.shubham0204.facenet_android.presentation.screens.edit_face

import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
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
class EditFaceScreenViewModel(
    private val personUseCase: PersonUseCase,
    private val imageVectorUseCase: ImageVectorUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val personId: Long = savedStateHandle.get<Long>("personId")!!

    val personNameState: MutableState<String> = mutableStateOf("")
    val notesState: MutableState<String> = mutableStateOf("")
    val profilePhotoPath: MutableState<String?> = mutableStateOf(null)
    val numImages: MutableState<Long> = mutableLongStateOf(0L)

    // URI chosen in this session (not yet saved to disk)
    val newProfilePhotoUri: MutableState<Uri?> = mutableStateOf(null)

    val selectedImageURIs: MutableState<List<Uri>> = mutableStateOf(emptyList())
    val isProcessingImages: MutableState<Boolean> = mutableStateOf(false)
    val numImagesProcessed: MutableState<Int> = mutableIntStateOf(0)

    init {
        personUseCase.getById(personId)?.let { record ->
            personNameState.value = record.personName
            notesState.value = record.notes
            profilePhotoPath.value = record.profilePhotoPath
            numImages.value = record.numImages
        }
    }

    fun saveChanges() {
        CoroutineScope(Dispatchers.Default).launch {
            personUseCase.updatePerson(
                personID = personId,
                name = personNameState.value,
                notes = notesState.value,
                newProfilePhotoUri = newProfilePhotoUri.value,
            )
        }
    }

    fun addMoreImages() {
        isProcessingImages.value = true
        numImagesProcessed.value = 0
        CoroutineScope(Dispatchers.Default).launch {
            selectedImageURIs.value.forEach { uri ->
                imageVectorUseCase
                    .addImage(personId, personNameState.value, uri)
                    .onFailure {
                        val errorMessage = (it as AppException).errorCode.message
                        setProgressDialogText(errorMessage)
                    }.onSuccess {
                        numImagesProcessed.value += 1
                        setProgressDialogText("Processed ${numImagesProcessed.value} image(s)")
                    }
            }
            if (numImagesProcessed.value > 0) {
                personUseCase.incrementImageCount(personId, numImagesProcessed.value)
                numImages.value += numImagesProcessed.value
                if (profilePhotoPath.value == null && newProfilePhotoUri.value == null) {
                    personUseCase.updatePerson(
                        personID = personId,
                        name = personNameState.value,
                        notes = notesState.value,
                        newProfilePhotoUri = selectedImageURIs.value.first(),
                    )
                    profilePhotoPath.value = personUseCase.getById(personId)?.profilePhotoPath
                }
            }
            selectedImageURIs.value = emptyList()
            isProcessingImages.value = false
        }
    }
}
