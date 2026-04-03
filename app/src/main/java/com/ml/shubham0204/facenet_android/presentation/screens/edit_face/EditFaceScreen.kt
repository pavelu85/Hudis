package com.ml.shubham0204.facenet_android.presentation.screens.edit_face

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ml.shubham0204.facenet_android.data.EncounterRecord
import com.ml.shubham0204.facenet_android.presentation.components.AppAlertDialog
import com.ml.shubham0204.facenet_android.presentation.components.AppProgressDialog
import com.ml.shubham0204.facenet_android.presentation.components.DelayedVisibility
import com.ml.shubham0204.facenet_android.presentation.components.hideProgressDialog
import com.ml.shubham0204.facenet_android.presentation.components.showProgressDialog
import com.ml.shubham0204.facenet_android.presentation.theme.HudisTheme
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFaceScreen(onNavigateBack: () -> Unit, onLearnFaceClick: () -> Unit = {}) {
    HudisTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "Edit Face", style = MaterialTheme.typography.headlineSmall)
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
        ) { innerPadding ->
            val viewModel: EditFaceScreenViewModel = koinViewModel()
            Column(modifier = Modifier.padding(innerPadding)) {
                ScreenUI(viewModel, onNavigateBack, onLearnFaceClick)
                AddMoreImagesProgressDialog(viewModel)
                AppAlertDialog()
            }
        }
    }
}

@Composable
private fun ScreenUI(viewModel: EditFaceScreenViewModel, onNavigateBack: () -> Unit, onLearnFaceClick: () -> Unit) {
    val pickFaceImagesLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia(),
        ) {
            viewModel.selectedImageURIs.value = it
        }
    val pickProfilePhotoLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
        ) { uri ->
            uri?.let { viewModel.newProfilePhotoUri.value = it }
        }

    var personName by remember { viewModel.personNameState }
    var notes by remember { viewModel.notesState }
    val profilePhotoPath by remember { viewModel.profilePhotoPath }
    val newProfilePhotoUri by remember { viewModel.newProfilePhotoUri }
    val numImages by remember { viewModel.numImages }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Profile photo selector
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable {
                        pickProfilePhotoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                val displayModel: Any? = newProfilePhotoUri ?: profilePhotoPath?.let { File(it) }
                if (displayModel != null) {
                    AsyncImage(
                        model = displayModel,
                        contentDescription = "Profile photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Tap to pick profile photo",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Text(
            text = "Tap to change profile photo",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = personName,
            onValueChange = { personName = it },
            label = { Text(text = "Name") },
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = notes,
            onValueChange = { notes = it },
            label = { Text(text = "Notes (optional)") },
            minLines = 2,
            maxLines = 4,
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$numImages image(s) in database",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(
                onClick = {
                    pickFaceImagesLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            ) {
                Icon(imageVector = Icons.Default.Photo, contentDescription = "Choose photos")
                Text(text = " Add more photos")
            }
            Button(
                enabled = personName.isNotEmpty(),
                onClick = {
                    viewModel.saveChanges()
                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                },
            ) {
                Text(text = "Save changes")
            }
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onLearnFaceClick,
        ) {
            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Improve Recognition")
            Text(text = " Improve Recognition")
        }
        DelayedVisibility(viewModel.selectedImageURIs.value.isNotEmpty()) {
            Column {
                Text(
                    text = "${viewModel.selectedImageURIs.value.size} image(s) selected",
                    style = MaterialTheme.typography.labelSmall,
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.addMoreImages() },
                ) {
                    Text(text = "Add to database")
                }
            }
        }
        ImagesGrid(viewModel)
        EncounterHistory(viewModel)
    }
}

@Composable
private fun EncounterHistory(viewModel: EditFaceScreenViewModel) {
    val encounters by remember { viewModel.encounters }
    if (encounters.isEmpty()) return
    val context = LocalContext.current

    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Last Encounters",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(4.dp))
    encounters.forEach { encounter ->
        EncounterRow(encounter) {
            val mapUri = Uri.parse("geo:${encounter.latitude},${encounter.longitude}?q=${encounter.latitude},${encounter.longitude}")
            val intent = Intent(Intent.ACTION_VIEW, mapUri)
            context.startActivity(intent)
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun EncounterRow(encounter: EncounterRecord, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = "Location",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(Date(encounter.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = encounter.locationName?.ifBlank {
                    "%.5f, %.5f".format(encounter.latitude, encounter.longitude)
                } ?: "%.5f, %.5f".format(encounter.latitude, encounter.longitude),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (encounter.matchPercentage > 0f) {
            Text(
                text = "${(encounter.matchPercentage * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Icon(
            imageVector = if (encounter.source == "camera") Icons.Default.PhotoCamera else Icons.Default.CameraAlt,
            contentDescription = encounter.source,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun ImagesGrid(viewModel: EditFaceScreenViewModel) {
    val uris by remember { viewModel.selectedImageURIs }
    LazyVerticalGrid(columns = GridCells.Fixed(2)) {
        items(uris) { AsyncImage(model = it, contentDescription = null) }
    }
}

@Composable
private fun AddMoreImagesProgressDialog(viewModel: EditFaceScreenViewModel) {
    val isProcessing by remember { viewModel.isProcessingImages }
    AppProgressDialog()
    if (isProcessing) {
        showProgressDialog()
    } else {
        hideProgressDialog()
    }
}
