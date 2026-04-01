package com.ml.shubham0204.facenet_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
                composable("add-face") { AddFaceScreen { navHostController.navigateUp() } }
                composable("auto-monitor") {
                    AutoMonitorScreen(onNavigateBack = { navHostController.navigateUp() })
                }
                composable("detect") {
                    DetectScreen(
                        onOpenFaceListClick = { navHostController.navigate("face-list") },
                        onNavigateToResults = { navHostController.navigate("results") },
                        onOpenAutoMonitor = { navHostController.navigate("auto-monitor") },
                    )
                }
                composable("face-list") {
                    FaceListScreen(
                        onNavigateBack = { navHostController.navigateUp() },
                        onAddFaceClick = { navHostController.navigate("add-face") },
                        onItemClick = { personId -> navHostController.navigate("edit-face/$personId") },
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
