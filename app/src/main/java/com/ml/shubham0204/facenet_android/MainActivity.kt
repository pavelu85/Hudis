package com.ml.shubham0204.facenet_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ml.shubham0204.facenet_android.presentation.screens.add_face.AddFaceScreen
import com.ml.shubham0204.facenet_android.presentation.screens.auto_monitor.AutoMonitorScreen
import com.ml.shubham0204.facenet_android.presentation.screens.detect_screen.DetectScreen
import com.ml.shubham0204.facenet_android.presentation.screens.edit_face.EditFaceScreen
import com.ml.shubham0204.facenet_android.presentation.screens.face_list.FaceListScreen
import com.ml.shubham0204.facenet_android.presentation.screens.results.ResultsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navHostController = rememberNavController()
            NavHost(
                navController = navHostController,
                startDestination = "detect",
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() },
            ) {
                composable("add-face") {
                    AddFaceScreen { newPersonId ->
                        navHostController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("scrollToPersonId", newPersonId)
                        navHostController.navigateUp()
                    }
                }
                composable("auto-monitor") {
                    AutoMonitorScreen(onNavigateBack = { navHostController.navigateUp() })
                }
                composable("detect") {
                    var showExitDialog by remember { mutableStateOf(false) }
                    BackHandler { showExitDialog = true }
                    if (showExitDialog) {
                        AlertDialog(
                            onDismissRequest = { showExitDialog = false },
                            title = { Text("Quit app?") },
                            text = { Text("Are you sure you want to exit?") },
                            confirmButton = {
                                TextButton(onClick = { finish() }) { Text("Quit") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showExitDialog = false }) { Text("Cancel") }
                            },
                        )
                    }
                    DetectScreen(
                        onOpenFaceListClick = { navHostController.navigate("face-list") },
                        onNavigateToResults = { navHostController.navigate("results") },
                    )
                }
                composable("face-list") { backStackEntry ->
                    val scrollToPersonId by backStackEntry.savedStateHandle
                        .getStateFlow<Long?>("scrollToPersonId", null)
                        .collectAsState()
                    FaceListScreen(
                        onNavigateBack = { navHostController.navigateUp() },
                        onAddFaceClick = { navHostController.navigate("add-face") },
                        onItemClick = { personId -> navHostController.navigate("edit-face/$personId") },
                        onOpenAutoMonitor = { navHostController.navigate("auto-monitor") },
                        scrollToPersonId = scrollToPersonId,
                        onScrollHandled = {
                            backStackEntry.savedStateHandle.remove<Long>("scrollToPersonId")
                        },
                    )
                }
                composable(
                    route = "edit-face/{personId}",
                    arguments = listOf(navArgument("personId") { type = NavType.LongType }),
                ) {
                    EditFaceScreen { navHostController.navigateUp() }
                }
                composable("results") {
                    ResultsScreen(
                        onNavigateBack = { navHostController.navigateUp() },
                        onAddAsNewPerson = {
                            navHostController.navigate("add-face")
                        },
                    )
                }
            }
        }
    }
}
