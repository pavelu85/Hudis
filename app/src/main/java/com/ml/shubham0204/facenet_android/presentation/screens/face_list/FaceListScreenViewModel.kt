package com.ml.shubham0204.facenet_android.presentation.screens.face_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ml.shubham0204.facenet_android.data.PersonRecord
import com.ml.shubham0204.facenet_android.domain.ImageVectorUseCase
import com.ml.shubham0204.facenet_android.domain.PersonUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

enum class SortOrder { NAME_ASC, LAST_SEEN_DESC, DATE_ADDED_DESC }

@KoinViewModel
class FaceListScreenViewModel(
    val imageVectorUseCase: ImageVectorUseCase,
    val personUseCase: PersonUseCase,
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val sortOrder = MutableStateFlow(SortOrder.NAME_ASC)

    val displayedPersons: StateFlow<List<PersonRecord>> = combine(
        personUseCase.getAll(), searchQuery, sortOrder
    ) { list, query, sort ->
        val filtered = if (query.isBlank()) list
            else list.filter {
                it.personName.contains(query, ignoreCase = true) ||
                it.notes.contains(query, ignoreCase = true)
            }
        when (sort) {
            SortOrder.NAME_ASC        -> filtered.sortedBy { it.personName.lowercase() }
            SortOrder.LAST_SEEN_DESC  -> filtered.sortedByDescending { it.lastSeenTime }
            SortOrder.DATE_ADDED_DESC -> filtered.sortedByDescending { it.addTime }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isSelectionMode = MutableStateFlow(false)
    val selectedIds = MutableStateFlow<Set<Long>>(emptySet())

    fun enterSelectionMode(id: Long) {
        selectedIds.value = setOf(id)
        isSelectionMode.value = true
    }

    fun toggleSelection(id: Long) {
        val updated = selectedIds.value.toMutableSet()
        if (!updated.add(id)) updated.remove(id)
        selectedIds.value = updated
        if (updated.isEmpty()) clearSelection()
    }

    fun selectAll() {
        selectedIds.value = displayedPersons.value.map { it.personID }.toSet()
    }

    fun clearSelection() {
        selectedIds.value = emptySet()
        isSelectionMode.value = false
    }

    fun removeSelectedFaces() {
        viewModelScope.launch(Dispatchers.IO) {
            selectedIds.value.forEach { id ->
                personUseCase.removePerson(id)
                imageVectorUseCase.removeImages(id)
            }
            clearSelection()
        }
    }

    fun removeFace(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            personUseCase.removePerson(id)
            imageVectorUseCase.removeImages(id)
        }
    }
}
