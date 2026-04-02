package com.ml.shubham0204.facenet_android.presentation.screens.face_list

import android.text.format.DateUtils
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
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
    SortOrder.NAME_ASC        to "Name",
    SortOrder.LAST_SEEN_DESC  to "Last Seen",

    SortOrder.DATE_ADDED_DESC to "Newest",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceListScreen(
    onNavigateBack: (() -> Unit),
    onAddFaceClick: (() -> Unit),
    onItemClick: (Long) -> Unit,
) {
    HudisTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
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
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onAddFaceClick) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add a new face")
                }
            },
        ) { innerPadding ->
            val viewModel: FaceListScreenViewModel = koinViewModel()
            Column(modifier = Modifier.padding(innerPadding)) {
                ScreenUI(viewModel, onItemClick)
                AppAlertDialog()
            }
        }
    }
}

@Composable
private fun ScreenUI(viewModel: FaceListScreenViewModel, onItemClick: (Long) -> Unit) {
    val faces by viewModel.displayedPersons.collectAsState()
    val currentSort by viewModel.sortOrder.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
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
            items(SortOrder.entries) { order ->
                FilterChip(
                    selected = currentSort == order,
                    onClick = { viewModel.sortOrder.value = order },
                    label = { Text(sortLabels[order]!!) },
                )
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
            LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                items(faces) { person ->
                    FaceListItem(
                        personRecord = person,
                        onItemClick = { onItemClick(person.personID) },
                        onRemoveFaceClick = { viewModel.removeFace(person.personID) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FaceListItem(
    personRecord: PersonRecord,
    onItemClick: () -> Unit,
    onRemoveFaceClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { onItemClick() },
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
