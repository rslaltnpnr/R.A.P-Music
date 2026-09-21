package com.ozin.music

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import com.ozin.music.core.ui.theme.Spacing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.ozin.music.core.settings.AppSettings
import com.ozin.music.core.settings.SettingsRepository
import com.ozin.music.core.settings.toLocaleListCompat
import com.ozin.music.core.ui.theme.OzinMusicTheme
import com.ozin.music.core.ui.theme.ThemeMode
import com.ozin.music.feature.bluetooth.BluetoothDevicesScreen
import com.ozin.music.feature.carmode.CarModeScreen
import com.ozin.music.feature.debug.DebugScreen
import com.ozin.music.feature.dj.DjModeScreen
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
import com.ozin.music.feature.remote.RemoteBrowseScreen
import com.ozin.music.feature.remote.RemoteServersScreen
import com.ozin.music.feature.search.SmartSearchScreen
import com.ozin.music.feature.settings.SettingsScreen
import com.ozin.music.feature.similar.SimilarSongsScreen
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
    const val BLUETOOTH_DEVICES = "bluetooth_devices"
    const val CAR_MODE = "car_mode"
    const val DJ_MODE = "dj_mode"
    const val REMOTE_SERVERS = "remote_servers"
    const val REMOTE_BROWSE = "remote_browse/{serverId}"
    const val SMART_SEARCH = "smart_search"
    const val SIMILAR_SONGS = "similar_songs/{songId}"
    const val DEBUG_INFO = "debug_info"
    fun playlistDetail(playlistId: Long) = "playlist/$playlistId"
    fun metadataEdit(songId: Long) = "metadata_edit/$songId"
    fun fileManagement(songId: Long) = "file_management/$songId"
    fun smartPlaylistDetail(smartPlaylistId: Long) = "smart_playlist/$smartPlaylistId"
    fun remoteBrowse(serverId: Long) = "remote_browse/$serverId"
    fun similarSongs(songId: Long) = "similar_songs/$songId"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var playerController: PlayerController
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        playerController.connect()

        // Apply the persisted per-app language choice (if any) before the
        // first composition, so the initial UI already renders in the
        // right language rather than flashing the system default first.
        lifecycleScope.launch {
            val languageOption = settingsRepository.settings.first().languageOption
            AppCompatDelegate.setApplicationLocales(languageOption.toLocaleListCompat())
        }

        setContent {
            val appSettings by settingsRepository.settings.collectAsState(initial = AppSettings())
            val darkMode = when (appSettings.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            OzinMusicTheme(
                preset = appSettings.themePreset,
                accent = appSettings.accentColorOption.color,
                darkMode = darkMode,
            ) {
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

    // Full-screen, immersive destinations manage their own exit affordance and
    // must never show the main app's mini player / bottom nav underneath them
    // (it used to overlap DJ Mode's controls at the bottom of the screen).
    val hideChromeFor = setOf(Routes.NOW_PLAYING, Routes.DJ_MODE, Routes.CAR_MODE)

    // Slim brand identity strip shown above the main tabs (not on DJ Mode,
    // Now Playing, or Car Mode, which keep their own distinct chrome/none).
    val showBrandBarFor = setOf(Routes.HOME, Routes.LIBRARY, Routes.LISTS, Routes.EQ, Routes.SETTINGS)

    Scaffold(
        topBar = {
            if (currentRoute in showBrandBarFor) {
                OzinBrandBar()
            }
        },
        bottomBar = {
            if (currentRoute !in hideChromeFor) {
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
                modifier = Modifier.padding(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding(),
                ),
            ) {
                composable(Routes.HOME) { HomeScreen(onSongClick = { navController.navigate(Routes.NOW_PLAYING) }) }
                composable(Routes.LIBRARY) {
                    LibraryScreen(
                        onSongClick = { navController.navigate(Routes.NOW_PLAYING) },
                        onEditSong = { songId -> navController.navigate(Routes.metadataEdit(songId)) },
                        onManageFile = { songId -> navController.navigate(Routes.fileManagement(songId)) },
                        onSimilarSongs = { songId -> navController.navigate(Routes.similarSongs(songId)) },
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
                        onOpenBluetoothDevices = { navController.navigate(Routes.BLUETOOTH_DEVICES) },
                        onOpenRemoteServers = { navController.navigate(Routes.REMOTE_SERVERS) },
                        onOpenSmartSearch = { navController.navigate(Routes.SMART_SEARCH) },
                        onOpenDjMode = { navController.navigate(Routes.DJ_MODE) },
                        onOpenDebugInfo = { navController.navigate(Routes.DEBUG_INFO) },
                    )
                }
                composable(Routes.DEBUG_INFO) { DebugScreen(onBack = { navController.popBackStack() }) }
                composable(Routes.DJ_MODE) { DjModeScreen(onExit = { navController.popBackStack() }) }
                composable(Routes.SMART_SEARCH) {
                    SmartSearchScreen(
                        onBack = { navController.popBackStack() },
                        onSongClick = { navController.navigate(Routes.NOW_PLAYING) },
                    )
                }
                composable(
                    route = Routes.SIMILAR_SONGS,
                    arguments = listOf(navArgument("songId") { type = NavType.LongType }),
                ) {
                    SimilarSongsScreen(
                        onBack = { navController.popBackStack() },
                        onSongClick = { navController.navigate(Routes.NOW_PLAYING) },
                    )
                }
                composable(Routes.BLUETOOTH_DEVICES) { BluetoothDevicesScreen() }
                composable(Routes.REMOTE_SERVERS) {
                    RemoteServersScreen(
                        onBack = { navController.popBackStack() },
                        onOpenServer = { id -> navController.navigate(Routes.remoteBrowse(id)) },
                    )
                }
                composable(
                    route = Routes.REMOTE_BROWSE,
                    arguments = listOf(navArgument("serverId") { type = NavType.LongType }),
                ) {
                    RemoteBrowseScreen(
                        onBack = { navController.popBackStack() },
                        onSongClick = { navController.navigate(Routes.NOW_PLAYING) },
                    )
                }
                composable(Routes.CAR_MODE) { CarModeScreen(onExit = { navController.popBackStack() }) }
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
                        onOpenCarMode = { navController.navigate(Routes.CAR_MODE) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OzinBrandBar() {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.logo_mark),
                    contentDescription = null,
                    modifier = Modifier.size(Spacing.xl),
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(),
    )
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
