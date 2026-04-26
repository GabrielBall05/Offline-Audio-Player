package com.example.offlineplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.offlineplayer.ui.Screen
import com.example.offlineplayer.ui.components.common.MiniPlayerBar
import com.example.offlineplayer.ui.screens.ExpandedPlayerScreen
import com.example.offlineplayer.ui.screens.HomeScreen
import com.example.offlineplayer.ui.screens.PlaylistScreen
import com.example.offlineplayer.ui.screens.SettingsScreen
import com.example.offlineplayer.ui.theme.OfflinePlayerTheme
import com.example.offlineplayer.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            OfflinePlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(mainViewModel: MainViewModel = hiltViewModel()) {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val hideBottomBars = currentRoute == Screen.Player.route //Hide bottom bars if Player is expanded

    //State for ExpandedPlayerScreen Sheet
    var showPlayerSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                    MiniPlayerBar(
                        viewModel = mainViewModel,
                        onExpand = { showPlayerSheet = true }
                    )
                    BottomNavigationBar(navController)
            }
        }
    ) { innerPadding ->
        //The NavHost is the "Window" that swaps screens
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(if (hideBottomBars) PaddingValues(0.dp) else innerPadding)
        ) {
            composable(
                route = Screen.Home.route,
                content = { HomeScreen() }
            )
            composable(
                route = Screen.Playlists.route,
                content = { PlaylistScreen() }
            )
            composable(
                route = Screen.Settings.route,
                content = { SettingsScreen() }
            )
        }

        if (showPlayerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPlayerSheet = false },
                sheetState = sheetState,
                dragHandle = null,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxSize(),
                windowInsets = WindowInsets(0)
            ) {
                ExpandedPlayerScreen(
                    viewModel = mainViewModel,
                    onCollapse = { showPlayerSheet = false }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(Screen.Home, Screen.Playlists, Screen.Settings)

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    //TODO: Customize
                    val icon = when(screen) {
                        Screen.Home -> Icons.Default.Home
                        Screen.Playlists -> Icons.AutoMirrored.Filled.List
                        Screen.Settings -> Icons.Default.Settings
                        else -> Icons.Default.Favorite
                    }
                    Icon(icon, contentDescription = screen.title)
                },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        //Prevents building up massive back stack as users tab over
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
















