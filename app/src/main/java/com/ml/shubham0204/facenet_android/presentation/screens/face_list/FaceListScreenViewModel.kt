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

enum class SortField { NAME, LAST_SEEN, DATE_ADDED }
enum class SortDirection { ASC, DESC }
data class SortState(val field: SortField, val direction: SortDirection)

data class MergeDialogState(
    val persons: List<PersonRecord>,
    val similarity: Float = 0f,
)

data class SimilarPairsState(
    val pairs: List<Triple<PersonRecord, PersonRecord, Float>>,
    val isLoading: Boolean,
)

@KoinViewModel
class FaceListScreenViewModel(
    val imageVectorUseCase: ImageVectorUseCase,
    val personUseCase: PersonUseCase,
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val sortState = MutableStateFlow(SortState(SortField.NAME, SortDirection.ASC))

    val displayedPersons: StateFlow<List<PersonRecord>> = combine(
        personUseCase.getAll(), searchQuery, sortState
    ) { list, query, sort ->
        val filtered = if (query.isBlank()) list
            else list.filter {
                it.personName.contains(query, ignoreCase = true) ||
                it.notes.contains(query, ignoreCase = true)
            }
        when (sort.field) {
            SortField.NAME       -> if (sort.direction == SortDirection.ASC) filtered.sortedBy { it.personName.lowercase() }
                                    else filtered.sortedByDescending { it.personName.lowercase() }
            SortField.LAST_SEEN  -> if (sort.direction == SortDirection.DESC) filtered.sortedByDescending { it.lastSeenTime }
                                    else filtered.sortedBy { it.lastSeenTime }
            SortField.DATE_ADDED -> if (sort.direction == SortDirection.DESC) filtered.sortedByDescending { it.addTime }
                                    else filtered.sortedBy { it.addTime }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isSelectionMode = MutableStateFlow(false)
    val selectedIds = MutableStateFlow<Set<Long>>(emptySet())

    val mergeDialogState = MutableStateFlow<MergeDialogState?>(null)
    val similarPairsState = MutableStateFlow<SimilarPairsState?>(null)

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

    // Opens merge dialog for all currently selected persons (≥2).
    fun showMergeDialogForSelected() {
        val ids = selectedIds.value.toList()
        if (ids.size < 2) return
        val allPersons = displayedPersons.value
        val persons = ids.mapNotNull { id -> allPersons.firstOrNull { it.personID == id } }
        if (persons.size < 2) return
        mergeDialogState.value = MergeDialogState(persons)
    }

    fun showMergeDialogForPair(a: PersonRecord, b: PersonRecord, similarity: Float) {
        mergeDialogState.value = MergeDialogState(listOf(a, b), similarity)
    }

    fun dismissMergeDialog() {
        mergeDialogState.value = null
    }

    fun executeMerge(keepId: Long, removeIds: List<Long>, name: String, notes: String, photoPath: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            personUseCase.mergePersons(keepId, removeIds, name, notes, photoPath)
            mergeDialogState.value = null
            clearSelection()
            if (similarPairsState.value != null) {
                findSimilarPersons()
            }
        }
    }

    fun findSimilarPersons() {
        similarPairsState.value = SimilarPairsState(emptyList(), isLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            val pairs = personUseCase.findSimilarPersonPairs()
            similarPairsState.value = SimilarPairsState(pairs, isLoading = false)
        }
    }

    fun dismissSimilarPairs() {
        similarPairsState.value = null
    }
}
