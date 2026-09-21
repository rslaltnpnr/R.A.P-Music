package com.ozin.music

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.ozin.music.core.player.PlayerController
import com.ozin.music.core.ui.theme.OzinMusicTheme
import com.ozin.music.feature.home.HomeScreen
import com.ozin.music.feature.library.LibraryScreen
import com.ozin.music.feature.permission.PermissionScreen
import com.ozin.music.feature.permission.audioPermissionName
import com.ozin.music.feature.player.MiniPlayerBar
import com.ozin.music.feature.player.NowPlayingScreen
import com.ozin.music.feature.playlist.EqualizerScreen
import com.ozin.music.feature.playlist.PlaylistDetailScreen
import com.ozin.music.feature.playlist.PlaylistsScreen
import com.ozin.music.feature.settings.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val LISTS = "lists"
    const val EQ = "eq"
    const val SETTINGS = "settings"
    const val NOW_PLAYING = "now_playing"
    const val PLAYLIST_DETAIL = "playlist/{playlistId}"
    fun playlistDetail(playlistId: Long) = "playlist/$playlistId"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var playerController: PlayerController

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        playerController.connect()

        setContent {
            OzinMusicTheme {
                var hasPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(this, audioPermissionName()) ==
                            PackageManager.PERMISSION_GRANTED
                    )
                }
                if (hasPermission) {
                    OzinApp()
                } else {
                    PermissionScreen(onGranted = { hasPermission = true })
                }
            }
        }
    }

    override fun onDestroy() {
        playerController.disconnect()
        super.onDestroy()
    }
}

@Composable
private fun OzinApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute != Routes.NOW_PLAYING) {
                Column {
                    MiniPlayerBar(onExpand = { navController.navigate(Routes.NOW_PLAYING) })
                    OzinBottomNav(navController, currentRoute)
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            ) {
                composable(Routes.HOME) { HomeScreen(onSongClick = { navController.navigate(Routes.NOW_PLAYING) }) }
                composable(Routes.LIBRARY) { LibraryScreen(onSongClick = { navController.navigate(Routes.NOW_PLAYING) }) }
                composable(Routes.LISTS) {
                    PlaylistsScreen(onPlaylistClick = { id -> navController.navigate(Routes.playlistDetail(id)) })
                }
                composable(Routes.EQ) { EqualizerScreen() }
                composable(Routes.SETTINGS) { SettingsScreen() }
                composable(Routes.NOW_PLAYING) { NowPlayingScreen(onBack = { navController.popBackStack() }) }
                composable(
                    route = Routes.PLAYLIST_DETAIL,
                    arguments = listOf(navArgument("playlistId") { type = NavType.LongType }),
                ) {
                    PlaylistDetailScreen(
                        onBack = { navController.popBackStack() },
                        onSongClick = { navController.navigate(Routes.NOW_PLAYING) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OzinBottomNav(navController: NavController, currentRoute: String?) {
    NavigationBar {
        val items = listOf(
            Triple(Routes.HOME, Icons.Filled.Home, R.string.nav_home),
            Triple(Routes.LIBRARY, Icons.Filled.LibraryMusic, R.string.nav_music),
            Triple(Routes.LISTS, Icons.Filled.QueueMusic, R.string.nav_lists),
            Triple(Routes.EQ, Icons.Filled.GraphicEq, R.string.nav_eq),
            Triple(Routes.SETTINGS, Icons.Filled.Settings, R.string.nav_settings),
        )
        items.forEach { (route, icon, labelRes) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = {
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(icon, contentDescription = null) },
                label = { Text(stringResource(labelRes)) },
            )
        }
    }
}
