package com.ml.shubham0204.facenet_android.presentation.screens.face_list

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ml.shubham0204.facenet_android.data.PersonRecord
import com.ml.shubham0204.facenet_android.presentation.components.AppAlertDialog
import com.ml.shubham0204.facenet_android.presentation.components.createAlertDialog
import com.ml.shubham0204.facenet_android.presentation.theme.HudisTheme
import org.koin.androidx.compose.koinViewModel

private val sortLabels = mapOf(
    SortField.NAME       to "Name",
    SortField.LAST_SEEN  to "Last Seen",
    SortField.DATE_ADDED to "Date Added",
)

private val defaultSortDirection = mapOf(
    SortField.NAME       to SortDirection.ASC,
    SortField.LAST_SEEN  to SortDirection.DESC,
    SortField.DATE_ADDED to SortDirection.DESC,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceListScreen(
    onNavigateBack: (() -> Unit),
    onAddFaceClick: (() -> Unit),
    onItemClick: (Long) -> Unit,
    onOpenAutoMonitor: () -> Unit,
    scrollToPersonId: Long? = null,
    onScrollHandled: () -> Unit = {},
) {
    val viewModel: FaceListScreenViewModel = koinViewModel()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val mergeDialogState by viewModel.mergeDialogState.collectAsState()
    val similarPairsState by viewModel.similarPairsState.collectAsState()

    HudisTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (isSelectionMode) {
                    SelectionTopAppBar(
                        selectedCount = selectedIds.size,
                        onSelectAll = { viewModel.selectAll() },
                        onMerge = { viewModel.showMergeDialogForSelected() },
                        onDelete = {
                            createAlertDialog(
                                dialogTitle = "Delete ${selectedIds.size} person(s)?",
                                dialogText = "This will permanently remove the selected people and all their face data.",
                                dialogPositiveButtonText = "Delete",
                                onPositiveButtonClick = { viewModel.removeSelectedFaces() },
                                dialogNegativeButtonText = "Cancel",
                                onNegativeButtonClick = {},
                            )
                        },
                        onCancel = { viewModel.clearSelection() },
                    )
                } else {
                    TopAppBar(
                        title = {
                            Text(text = "Face List", style = MaterialTheme.typography.headlineSmall)
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                    contentDescription = "Navigate Back",
                                )
                            }
                        },
                        actions = {
                            var menuExpanded by remember { mutableStateOf(false) }
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Batch Import") },
                                    leadingIcon = {
                                        Icon(Icons.Default.ImageSearch, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenAutoMonitor()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Find Duplicates") },
                                    leadingIcon = {
                                        Icon(Icons.Default.CallMerge, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.findSimilarPersons()
                                    },
                                )
                            }
                        },
                    )
                }
            },
            floatingActionButton = {
                if (!isSelectionMode) {
                    FloatingActionButton(onClick = onAddFaceClick) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add a new face")
                    }
                }
            },
        ) { innerPadding ->
            BackHandler(enabled = isSelectionMode) { viewModel.clearSelection() }
            Column(modifier = Modifier.padding(innerPadding)) {
                ScreenUI(viewModel, isSelectionMode, selectedIds, onItemClick, scrollToPersonId, onScrollHandled)
                AppAlertDialog()
            }

            // Merge dialog (manual selection or from similar pairs)
            mergeDialogState?.let { state ->
                MergeDialog(
                    state = state,
                    onConfirm = { keepId, removeIds, name, notes, photoPath ->
                        viewModel.executeMerge(keepId, removeIds, name, notes, photoPath)
                    },
                    onDismiss = { viewModel.dismissMergeDialog() },
                )
            }

            // Similar pairs bottom sheet
            similarPairsState?.let { state ->
                SimilarPairsBottomSheet(
                    state = state,
                    onPairSelected = { a, b, similarity ->
                        viewModel.dismissSimilarPairs()
                        viewModel.showMergeDialogForPair(a, b, similarity)
                    },
                    onDismiss = { viewModel.dismissSimilarPairs() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopAppBar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onMerge: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    TopAppBar(
        title = { Text("$selectedCount selected") },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Default.DoneAll, contentDescription = "Select all")
            }
            if (selectedCount >= 2) {
                IconButton(onClick = onMerge) {
                    Icon(Icons.Default.CallMerge, contentDescription = "Merge selected")
                }
            }
            IconButton(onClick = onDelete, enabled = selectedCount > 0) {
                Icon(Icons.Default.Delete, contentDescription = "Delete selected")
            }
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel selection")
            }
        },
    )
}

@Composable
private fun ScreenUI(
    viewModel: FaceListScreenViewModel,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    onItemClick: (Long) -> Unit,
    scrollToPersonId: Long? = null,
    onScrollHandled: () -> Unit = {},
) {
    val faces by viewModel.displayedPersons.collectAsState()
    val currentSort by viewModel.sortState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(scrollToPersonId, faces) {
        if (scrollToPersonId != null && scrollToPersonId != 0L && faces.isNotEmpty()) {
            val index = faces.indexOfFirst { it.personID == scrollToPersonId }
            if (index >= 0) {
                listState.animateScrollToItem(index)
                onScrollHandled()
            }
        }
    }

    LaunchedEffect(currentSort) {
        listState.scrollToItem(0)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!isSelectionMode) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.searchQuery.value = it
                },
                placeholder = { Text("Search…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            viewModel.searchQuery.value = ""
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                shape = RoundedCornerShape(50),
            )

            // Sort chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            ) {
                items(SortField.entries) { field ->
                    val isSelected = currentSort.field == field
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.sortState.value = if (isSelected) {
                                currentSort.copy(direction = if (currentSort.direction == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC)
                            } else {
                                SortState(field, defaultSortDirection[field]!!)
                            }
                        },
                        label = { Text(sortLabels[field]!!) },
                        trailingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = if (currentSort.direction == SortDirection.ASC) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = if (currentSort.direction == SortDirection.ASC) "Ascending" else "Descending",
                                )
                            }
                        } else null,
                    )
                }
            }
        }

        // List or empty state
        if (faces.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (searchQuery.isNotEmpty()) "No results for \"$searchQuery\"" else "No faces added yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp),
                )
            }
        } else {
            LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 80.dp)) {
                items(faces, key = { it.personID }) { person ->
                    FaceListItem(
                        personRecord = person,
                        isSelectionMode = isSelectionMode,
                        isSelected = selectedIds.contains(person.personID),
                        onItemClick = {
                            if (isSelectionMode) viewModel.toggleSelection(person.personID)
                            else onItemClick(person.personID)
                        },
                        onLongPress = { viewModel.enterSelectionMode(person.personID) },
                        onRemoveFaceClick = { viewModel.removeFace(person.personID) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FaceListItem(
    personRecord: PersonRecord,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    onLongPress: () -> Unit,
    onRemoveFaceClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .combinedClickable(onClick = onItemClick, onLongClick = onLongPress),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                             else CardDefaults.elevatedCardColors().containerColor,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onItemClick() },
                    modifier = Modifier.padding(end = 4.dp),
                )
            }

            // Avatar
            if (personRecord.profilePhotoPath != null) {
                AsyncImage(
                    model = personRecord.profilePhotoPath,
                    contentDescription = "Profile photo of ${personRecord.personName}",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = personRecord.personName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (personRecord.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = personRecord.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Last seen
                val lastSeenText = if (personRecord.lastSeenTime > 0) {
                    "Last seen: ${DateUtils.getRelativeTimeSpanString(
                        personRecord.lastSeenTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
                    )}"
                } else {
                    null
                }
                if (lastSeenText != null) {
                    Text(
                        text = lastSeenText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Images count + added time
                val addedText = if (personRecord.addTime > 0) {
                    "Added ${DateUtils.getRelativeTimeSpanString(
                        personRecord.addTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
                    )}"
                } else null
                Text(
                    text = "${personRecord.numImages} image(s)" + (addedText?.let { " • $it" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // "Never seen" badge
                if (personRecord.lastSeenTime == 0L) {
                    Spacer(modifier = Modifier.height(4.dp))
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Never seen", style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            if (!isSelectionMode) {
                IconButton(
                    onClick = {
                        createAlertDialog(
                            dialogTitle = "Remove person",
                            dialogText =
                                "Are you sure to remove this person from the database. The face for this person will not " +
                                    "be detected in realtime",
                            dialogPositiveButtonText = "Remove",
                            onPositiveButtonClick = onRemoveFaceClick,
                            dialogNegativeButtonText = "Cancel",
                            onNegativeButtonClick = {},
                        )
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Remove face",
                    )
                }
            }
        }
    }
}
