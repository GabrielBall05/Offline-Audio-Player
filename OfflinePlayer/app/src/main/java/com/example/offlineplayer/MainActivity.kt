package com.example.offlineplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.offlineplayer.player.MediaControllerManager
import com.example.offlineplayer.ui.Screen
import com.example.offlineplayer.ui.components.media.MiniPlayerBar
import com.example.offlineplayer.ui.screens.HomeScreen
import com.example.offlineplayer.ui.screens.PlaylistScreen
import com.example.offlineplayer.ui.screens.SettingsScreen
import com.example.offlineplayer.ui.theme.OfflinePlayerTheme
import com.example.offlineplayer.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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

@Composable
fun MainScreen(mainViewModel: MainViewModel = hiltViewModel()) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            //Stack Player and Nav Bar vertically
            Column(modifier = Modifier.fillMaxWidth()) {
                MiniPlayerBar(
                    viewModel = mainViewModel,
                    onExpand = { /*TODO: Expand to full player screen*/ }
                )
                BottomNavigationBar(navController)
            }
        }
    ) { innerPadding ->
        //The NavHost is the "Window" that swaps screens
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(
                route = Screen.Home.route,
                content = { HomeScreen(navController) }
            )
            composable(
                route = Screen.Playlists.route,
                content = { PlaylistScreen(navController) }
            )
            composable(
                route = Screen.Settings.route,
                content = { SettingsScreen(navController) }
            )
            // Detail screens go here too...
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
                    //WILL BE CUSTOMIZING THESE LATER
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
















