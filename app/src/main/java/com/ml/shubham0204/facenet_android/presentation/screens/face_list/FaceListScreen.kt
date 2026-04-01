package com.ml.shubham0204.facenet_android.presentation.screens.face_list

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ml.shubham0204.facenet_android.data.PersonRecord
import com.ml.shubham0204.facenet_android.presentation.components.AppAlertDialog
import com.ml.shubham0204.facenet_android.presentation.components.createAlertDialog
import com.ml.shubham0204.facenet_android.presentation.theme.HudisTheme
import org.koin.androidx.compose.koinViewModel

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
    val faces by viewModel.personFlow.collectAsState(emptyList())
    LazyColumn {
        items(faces) { person ->
            FaceListItem(
                personRecord = person,
                onItemClick = { onItemClick(person.personID) },
                onRemoveFaceClick = { viewModel.removeFace(person.personID) },
            )
        }
    }
}

@Composable
private fun FaceListItem(
    personRecord: PersonRecord,
    onItemClick: () -> Unit,
    onRemoveFaceClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onItemClick() }.background(Color.White).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Profile photo thumbnail
        if (personRecord.profilePhotoPath != null) {
            AsyncImage(
                model = personRecord.profilePhotoPath,
                contentDescription = "Profile photo of ${personRecord.personName}",
                modifier =
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
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
            Spacer(modifier = Modifier.height(2.dp))
            if (personRecord.notes.isNotBlank()) {
                Text(
                    text = personRecord.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
            Text(
                text = "${personRecord.numImages} image(s) • ${DateUtils.getRelativeTimeSpanString(personRecord.addTime)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.DarkGray,
            )
        }
        Icon(
            modifier =
                Modifier.clickable {
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
            imageVector = Icons.Default.Clear,
            contentDescription = "Remove face",
        )
        Spacer(modifier = Modifier.width(2.dp))
    }
}
