package com.ozin.music

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.ozin.music.feature.duplicates.DuplicatesScreen
import com.ozin.music.feature.files.FileManagementSheet
import com.ozin.music.feature.folders.FoldersScreen
import com.ozin.music.feature.home.HomeScreen
import com.ozin.music.feature.library.LibraryScreen
import com.ozin.music.feature.lyrics.LyricsScreen
import com.ozin.music.feature.metadata.MetadataEditScreen
import com.ozin.music.feature.permission.PermissionScreen
import com.ozin.music.feature.permission.audioPermissionName
import com.ozin.music.feature.player.MiniPlayerBar
import com.ozin.music.feature.player.NowPlayingScreen
import com.ozin.music.feature.playlist.EqualizerScreen
import com.ozin.music.feature.playlist.PlaylistDetailScreen
import com.ozin.music.feature.playlist.PlaylistsScreen
import com.ozin.music.feature.playlist.SmartPlaylistDetailScreen
import com.ozin.music.feature.playlist.SmartPlaylistsScreen
import com.ozin.music.feature.problems.ProblemFilesScreen
import com.ozin.music.feature.settings.SettingsScreen
import com.ozin.music.feature.stats.StatsScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val LISTS = "lists"
    const val EQ = "eq"
    const val SETTINGS = "settings"
    const val NOW_PLAYING = "now_playing"
    const val LYRICS = "lyrics"
    const val FOLDERS = "folders"
    const val PROBLEM_FILES = "problem_files"
    const val DUPLICATES = "duplicates"
    const val PLAYLIST_DETAIL = "playlist/{playlistId}"
    const val METADATA_EDIT = "metadata_edit/{songId}"
    const val FILE_MANAGEMENT = "file_management/{songId}"
    const val STATS = "stats"
    const val SMART_PLAYLISTS = "smart_playlists"
    const val SMART_PLAYLIST_DETAIL = "smart_playlist/{smartPlaylistId}"
    fun playlistDetail(playlistId: Long) = "playlist/$playlistId"
    fun metadataEdit(songId: Long) = "metadata_edit/$songId"
    fun fileManagement(songId: Long) = "file_management/$songId"
    fun smartPlaylistDetail(smartPlaylistId: Long) = "smart_playlist/$smartPlaylistId"
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

@OptIn(ExperimentalMaterial3Api::class)
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
                composable(Routes.LIBRARY) {
                    LibraryScreen(
                        onSongClick = { navController.navigate(Routes.NOW_PLAYING) },
                        onEditSong = { songId -> navController.navigate(Routes.metadataEdit(songId)) },
                        onManageFile = { songId -> navController.navigate(Routes.fileManagement(songId)) },
                    )
                }
                composable(Routes.LISTS) {
                    PlaylistsScreen(
                        onPlaylistClick = { id -> navController.navigate(Routes.playlistDetail(id)) },
                        onOpenSmartPlaylists = { navController.navigate(Routes.SMART_PLAYLISTS) },
                    )
                }
                composable(Routes.EQ) { EqualizerScreen() }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        onOpenFolders = { navController.navigate(Routes.FOLDERS) },
                        onOpenProblemFiles = { navController.navigate(Routes.PROBLEM_FILES) },
                        onOpenDuplicates = { navController.navigate(Routes.DUPLICATES) },
                        onOpenStats = { navController.navigate(Routes.STATS) },
                    )
                }
                composable(Routes.STATS) { StatsScreen(onBack = { navController.popBackStack() }) }
                composable(Routes.SMART_PLAYLISTS) {
                    SmartPlaylistsScreen(
                        onSmartPlaylistClick = { id -> navController.navigate(Routes.smartPlaylistDetail(id)) },
                    )
                }
                composable(
                    route = Routes.SMART_PLAYLIST_DETAIL,
                    arguments = listOf(navArgument("smartPlaylistId") { type = NavType.LongType }),
                ) {
                    SmartPlaylistDetailScreen(
                        onBack = { navController.popBackStack() },
                        onSongClick = { navController.navigate(Routes.NOW_PLAYING) },
                    )
                }
                composable(Routes.NOW_PLAYING) {
                    NowPlayingScreen(
                        onBack = { navController.popBackStack() },
                        onOpenLyrics = { navController.navigate(Routes.LYRICS) },
                    )
                }
                composable(Routes.LYRICS) { LyricsScreen(onBack = { navController.popBackStack() }) }
                composable(Routes.FOLDERS) { FoldersScreen() }
                composable(Routes.PROBLEM_FILES) { ProblemFilesScreen() }
                composable(Routes.DUPLICATES) { DuplicatesScreen() }
                composable(
                    route = Routes.METADATA_EDIT,
                    arguments = listOf(navArgument("songId") { type = NavType.LongType }),
                ) {
                    MetadataEditScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    route = Routes.FILE_MANAGEMENT,
                    arguments = listOf(navArgument("songId") { type = NavType.LongType }),
                ) {
                    FileManagementSheet(onDismiss = { navController.popBackStack() })
                }
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
