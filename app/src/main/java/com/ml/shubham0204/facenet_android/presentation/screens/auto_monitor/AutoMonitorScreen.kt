package com.ml.shubham0204.facenet_android.presentation.screens.auto_monitor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ml.shubham0204.facenet_android.presentation.theme.HudisTheme
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoMonitorScreen(onNavigateBack: () -> Unit) {
    val viewModel: AutoMonitorViewModel = koinViewModel()
    val isProcessing by viewModel.batchIsProcessing
    val progress by viewModel.batchProgress
    val total by viewModel.batchTotal
    val statusMessage by viewModel.batchStatusMessage
    val result by viewModel.batchResult

    var selectedUris by remember { mutableStateOf(listOf<android.net.Uri>()) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) selectedUris = uris
    }

    HudisTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Auto-Monitor") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                )
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // --- Batch Import Card ---
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Batch Photo Import",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "Select photos and the app will automatically identify known people and create new records for unrecognized faces.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        // Photo selection
                        AnimatedVisibility(visible = !isProcessing && result == null) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        photoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        if (selectedUris.isEmpty()) "Select Photos"
                                        else "${selectedUris.size} photo(s) selected"
                                    )
                                }
                                AnimatedVisibility(visible = selectedUris.isNotEmpty()) {
                                    Button(
                                        onClick = { viewModel.startBatchProcessing(selectedUris) },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("Start Processing")
                                    }
                                }
                            }
                        }

                        // Progress
                        AnimatedVisibility(visible = isProcessing) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                LinearProgressIndicator(
                                    progress = { if (total > 0) progress.toFloat() / total else 0f },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Text(
                                    text = statusMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        // Result summary
                        AnimatedVisibility(visible = result != null) {
                            result?.let { r ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Done!", style = MaterialTheme.typography.titleSmall)
                                    ResultRow("Faces found", r.totalFacesFound)
                                    ResultRow("Matched to existing people", r.matchedToExisting)
                                    ResultRow("New people created", r.newPersonsCreated)
                                    ResultRow("Photos skipped (no face)", r.skippedNoFace)
                                    Spacer(Modifier.height(4.dp))
                                    OutlinedButton(
                                        onClick = {
                                            selectedUris = emptyList()
                                            viewModel.batchResult.value = null
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("Process Another Batch")
                                    }
                                }
                            }
                        }
                    }
                }

                // --- Live Camera Info Card ---
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Live Camera Auto-Monitor",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "Toggle the Auto-Monitor icon in the Detect screen to automatically save unrecognized faces seen by the camera.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value.toString(), style = MaterialTheme.typography.bodyMedium)
    }
}
