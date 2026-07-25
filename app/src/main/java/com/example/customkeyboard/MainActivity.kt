package com.example.customkeyboard

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.customkeyboard.bluetooth.HidKeyboardManager
import com.example.customkeyboard.data.LayoutRepository
import com.example.customkeyboard.ui.BluetoothScreen
import com.example.customkeyboard.ui.EditorScreen
import com.example.customkeyboard.ui.HomeScreen
import com.example.customkeyboard.ui.RunScreen

class MainActivity : ComponentActivity() {

    private lateinit var hidManager: HidKeyboardManager
    private lateinit var repository: LayoutRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hidManager = HidKeyboardManager(applicationContext)
        repository = LayoutRepository(applicationContext)

        setContent {
            val darkColors = darkColorScheme()
            MaterialTheme(colorScheme = darkColors) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { hidManager.init() }

                LaunchedEffect(Unit) {
                    val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        arrayOf(
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_ADVERTISE
                        )
                    } else emptyArray()
                    if (perms.isNotEmpty()) permissionLauncher.launch(perms) else hidManager.init()
                }

                AppNav(hidManager, repository)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hidManager.teardown()
    }
}

@Composable
fun AppNav(hidManager: HidKeyboardManager, repository: LayoutRepository) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                repository = repository,
                hidManager = hidManager,
                onNewLayout = { navController.navigate("editor/new") },
                onEditLayout = { id -> navController.navigate("editor/$id") },
                onRunLayout = { id -> navController.navigate("run/$id") },
                onOpenBluetooth = { navController.navigate("bluetooth") }
            )
        }
        composable(
            "editor/{layoutId}",
            arguments = listOf(navArgument("layoutId") { type = NavType.StringType })
        ) { backStackEntry ->
            val layoutId = backStackEntry.arguments?.getString("layoutId") ?: "new"
            EditorScreen(
                layoutId = layoutId,
                repository = repository,
                onDone = { navController.popBackStack() }
            )
        }
        composable(
            "run/{layoutId}",
            arguments = listOf(navArgument("layoutId") { type = NavType.StringType })
        ) { backStackEntry ->
            val layoutId = backStackEntry.arguments?.getString("layoutId") ?: return@composable
            RunScreen(
                layoutId = layoutId,
                repository = repository,
                hidManager = hidManager,
                onExit = { navController.popBackStack() }
            )
        }
        composable("bluetooth") {
            BluetoothScreen(
                hidManager = hidManager,
                onDone = { navController.popBackStack() }
            )
        }
    }
}
