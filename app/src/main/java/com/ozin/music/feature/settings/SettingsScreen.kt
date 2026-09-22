package com.ozin.music.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ozin.music.R
import com.ozin.music.core.ui.components.SettingsCategoryRow
import com.ozin.music.core.ui.theme.Spacing

/**
 * Top-level Settings screen: a list of category rows, each opening its own
 * dedicated page (see `SettingsSubScreens.kt`) instead of one long scrolling
 * wall of every setting at once. `onOpenX` callbacks that already lead to a
 * fully separate existing feature screen (Smart Search, Bluetooth device
 * profiles, DJ Mode, remote/WebDAV servers) are wired directly here with no
 * intermediate page, since those destinations are already their own screens.
 */
@Composable
fun SettingsScreen(
    onOpenPlayback: () -> Unit = {},
    onOpenLibrary: () -> Unit = {},
    onOpenPersonalization: () -> Unit = {},
    onOpenLockScreen: () -> Unit = {},
    onOpenNowPlaying: () -> Unit = {},
    onOpenBackupRestore: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onOpenBluetoothDevices: () -> Unit = {},
    onOpenRemoteServers: () -> Unit = {},
    onOpenSmartSearch: () -> Unit = {},
    onOpenDjMode: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        SettingsCategoryRow(
            icon = Icons.Filled.Equalizer,
            title = stringResource(R.string.settings_section_playback),
            subtitle = stringResource(R.string.settings_section_playback_subtitle),
            onClick = onOpenPlayback,
        )
        SettingsCategoryRow(
            icon = Icons.Filled.Folder,
            title = stringResource(R.string.settings_section_library),
            subtitle = stringResource(R.string.settings_section_library_subtitle),
            onClick = onOpenLibrary,
        )
        SettingsCategoryRow(
            icon = Icons.Filled.Palette,
            title = stringResource(R.string.settings_section_personalization),
            subtitle = stringResource(R.string.settings_section_personalization_subtitle),
            onClick = onOpenPersonalization,
        )
        SettingsCategoryRow(
            icon = Icons.Filled.Lock,
            title = stringResource(R.string.settings_lock_screen),
            subtitle = stringResource(R.string.settings_section_lock_screen_subtitle),
            onClick = onOpenLockScreen,
        )
        SettingsCategoryRow(
            icon = Icons.Filled.MusicNote,
            title = stringResource(R.string.settings_section_now_playing),
            subtitle = stringResource(R.string.settings_section_now_playing_subtitle),
            onClick = onOpenNowPlaying,
        )
        SettingsCategoryRow(
            icon = Icons.Filled.Search,
            title = stringResource(R.string.settings_search_by_keyword),
            subtitle = stringResource(R.string.settings_section_smart_search_subtitle),
            onClick = onOpenSmartSearch,
        )
        SettingsCategoryRow(
            icon = Icons.Filled.Bluetooth,
            title = stringResource(R.string.settings_bluetooth_device_profiles),
            subtitle = stringResource(R.string.settings_section_bluetooth_subtitle),
            onClick = onOpenBluetoothDevices,
        )
        SettingsCategoryRow(
            icon = Icons.Filled.Speed,
            title = stringResource(R.string.nav_dj_mode),
            subtitle = stringResource(R.string.settings_section_dj_mode_subtitle),
            onClick = onOpenDjMode,
        )
        SettingsCategoryRow(
            icon = Icons.Filled.Cloud,
            title = stringResource(R.string.settings_network),
            subtitle = stringResource(R.string.settings_section_network_subtitle),
            onClick = onOpenRemoteServers,
        )
        SettingsCategoryRow(
            icon = Icons.Filled.Backup,
            title = stringResource(R.string.settings_backup_restore),
            subtitle = stringResource(R.string.settings_section_backup_subtitle),
            onClick = onOpenBackupRestore,
        )
        SettingsCategoryRow(
            icon = Icons.Filled.Info,
            title = stringResource(R.string.settings_about),
            subtitle = stringResource(R.string.settings_section_about_subtitle),
            onClick = onOpenAbout,
        )
    }
}
