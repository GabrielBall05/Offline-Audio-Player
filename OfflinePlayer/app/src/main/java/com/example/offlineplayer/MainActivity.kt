package com.example.offlineplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.offlineplayer.player.MediaControllerManager
import com.example.offlineplayer.ui.Screen
import com.example.offlineplayer.ui.screens.HomeScreen
import com.example.offlineplayer.ui.screens.PlaylistScreen
import com.example.offlineplayer.ui.screens.SettingsScreen
import com.example.offlineplayer.ui.theme.OfflinePlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var controllerManager: MediaControllerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            OfflinePlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(controllerManager)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            controllerManager.releaseController()
        }
    }
}

@Composable
fun MainScreen(controllerManager: MediaControllerManager) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            //Stack Player and Nav Bar vertically
            Column(modifier = Modifier.fillMaxWidth()) {
                MiniPlayerBar(controllerManager)
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
            composable(Screen.Home.route) {
                HomeScreen(navController)
            }
            composable(Screen.Playlists.route) {
                PlaylistScreen(navController)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(navController)
            }
            // Detail screens go here too...
        }
    }
}

@Composable
fun MiniPlayerBar(controllerManager: MediaControllerManager) {
    //This Box allows me to layer the Progress Bar at the very bottom
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable {
                //TO DO - Expand to Full Player
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            //Artwork Placeholder
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color.Gray
            ) { /*PUT IMAGE HERE*/ }

            //Title & Creator
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text("Media Title", maxLines = 1, style = MaterialTheme.typography.bodyLarge)
                Text("Creator Name", maxLines = 1, style = MaterialTheme.typography.bodySmall)
            }

            //Controls
            IconButton(onClick = { /*PREVIOUS TRACK*/ }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Track")
            }
            IconButton(onClick = { /*PLAY/PAUSE*/ }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play/Pause")
            }
            IconButton(onClick = { /*NEXT TRACK*/ }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next Track")
            }
        }

        //Progress Line
        LinearProgressIndicator(
            progress = { 0.4f }, //PLACEHOLDER (40%) for actual duration progressed
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(3.dp), //Very thin line
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Transparent
        )
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
















