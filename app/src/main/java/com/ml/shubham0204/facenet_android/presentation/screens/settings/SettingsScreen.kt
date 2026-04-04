package com.ml.shubham0204.facenet_android.presentation.screens.settings

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
) {
    val viewModel: SettingsScreenViewModel = koinViewModel()
    var showResetConfirmation by remember { mutableStateOf(false) }
    var showConfidenceInfo by remember { mutableStateOf(false) }
    var showDiversityInfo by remember { mutableStateOf(false) }
    var showAutoCaptureIntervalInfo by remember { mutableStateOf(false) }
    var showLastSeenInfo by remember { mutableStateOf(false) }
    var showLearningModeMinConfidenceInfo by remember { mutableStateOf(false) }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Reset to Defaults?") },
            text = { Text("All settings will be reset to their default values. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetToDefaults()
                        showResetConfirmation = false
                    }
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) { Text("Cancel") }
            },
        )
    }

    if (showConfidenceInfo) {
        AlertDialog(
            onDismissRequest = { showConfidenceInfo = false },
            title = { Text("Confidence Threshold") },
            text = { Text("Minimum confidence score required to match a face to a known person. Lower values match more liberally (more false positives), higher values match more strictly (more false negatives).") },
            confirmButton = {
                TextButton(onClick = { showConfidenceInfo = false }) { Text("OK") }
            }
        )
    }

    if (showDiversityInfo) {
        AlertDialog(
            onDismissRequest = { showDiversityInfo = false },
            title = { Text("Diversity Threshold") },
            text = { Text("In learning mode, skip capturing poses that are too similar to already-captured poses. Lower values capture more varied poses, higher values skip more duplicates.") },
            confirmButton = {
                TextButton(onClick = { showDiversityInfo = false }) { Text("OK") }
            }
        )
    }

    if (showAutoCaptureIntervalInfo) {
        AlertDialog(
            onDismissRequest = { showAutoCaptureIntervalInfo = false },
            title = { Text("Auto-Capture Interval") },
            text = { Text("Minimum time between automatic face captures in learning mode. Lower values capture frames more frequently, higher values reduce captures.") },
            confirmButton = {
                TextButton(onClick = { showAutoCaptureIntervalInfo = false }) { Text("OK") }
            }
        )
    }

    if (showLastSeenInfo) {
        AlertDialog(
            onDismissRequest = { showLastSeenInfo = false },
            title = { Text("Last Seen Threshold") },
            text = { Text("Minimum confidence to record an encounter timestamp for a person. Useful if you want to log all encounters but only match high-confidence ones.") },
            confirmButton = {
                TextButton(onClick = { showLastSeenInfo = false }) { Text("OK") }
            }
        )
    }

    if (showLearningModeMinConfidenceInfo) {
        AlertDialog(
            onDismissRequest = { showLearningModeMinConfidenceInfo = false },
            title = { Text("Learning Mode Min Confidence") },
            text = { Text("Minimum similarity score required for a face to be captured in learning mode. Helps filter out non-matching faces during learning.") },
            confirmButton = {
                TextButton(onClick = { showLearningModeMinConfidenceInfo = false }) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(bottom = 16.dp)
        ) {
            // Recognition Settings
            SettingSection(title = "Recognition Settings") {
                SliderSetting(
                    title = "Confidence Threshold",
                    value = viewModel.confidenceThreshold.floatValue,
                    onValueChange = { viewModel.setConfidenceThreshold(it) },
                    valueRange = 0.5f..1.0f,
                    steps = 9,
                    description = "Minimum confidence to match faces",
                    defaultValue = 0.70f,
                    onInfoClick = { showConfidenceInfo = true }
                )
            }

            // Learning Mode Settings
            SettingSection(title = "Learning Mode Settings") {
                SliderSetting(
                    title = "Diversity Threshold",
                    value = viewModel.learningModeDiversityThreshold.floatValue,
                    onValueChange = { viewModel.setLearningModeDiversityThreshold(it) },
                    valueRange = 0.7f..1.0f,
                    steps = 5,
                    description = "Skip similar poses during learning",
                    defaultValue = 0.90f,
                    onInfoClick = { showDiversityInfo = true }
                )

                LongSliderSetting(
                    title = "Auto-Capture Interval",
                    value = viewModel.autoCapureIntervalMs.longValue,
                    onValueChange = { viewModel.setAutoCapureIntervalMs(it) },
                    valueRange = 500f..5000f,
                    steps = 17,
                    description = "Time between auto-captures (milliseconds)",
                    defaultValue = 2000L,
                    onInfoClick = { showAutoCaptureIntervalInfo = true }
                )
            }

            // Advanced Settings
            SettingSection(title = "Advanced Settings") {
                SliderSetting(
                    title = "Last Seen Min Confidence",
                    value = viewModel.lastSeenMinConfidence.floatValue,
                    onValueChange = { viewModel.setLastSeenMinConfidence(it) },
                    valueRange = 0.5f..1.0f,
                    steps = 9,
                    description = "Record all encounters below threshold",
                    defaultValue = 0.70f,
                    onInfoClick = { showLastSeenInfo = true }
                )

                SliderSetting(
                    title = "Learning Mode Min Confidence",
                    value = viewModel.learningModeMinConfidence.floatValue,
                    onValueChange = { viewModel.setLearningModeMinConfidence(it) },
                    valueRange = 0.3f..0.8f,
                    steps = 9,
                    description = "Similarity threshold in learning mode",
                    defaultValue = 0.50f,
                    onInfoClick = { showLearningModeMinConfidenceInfo = true }
                )
            }

            // Info Section
            val context = LocalContext.current
            val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
            SettingSection(title = "Info") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    InfoRow("Model", "FaceNet 512-dim")
                    InfoRow("Detector", "MLKit")
                    InfoRow("App Version", pkg.versionName ?: "unknown")
                    InfoRow("Build Number", PackageInfoCompat.getLongVersionCode(pkg).toString())
                    InfoRow("Android", "API ${Build.VERSION.SDK_INT}")
                }
            }

            // Reset Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { showResetConfirmation = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text("Reset to Defaults")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
