package com.ozin.music.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ozin.music.core.data.model.BluetoothDeviceProfile
import com.ozin.music.core.data.model.ListeningEvent
import com.ozin.music.core.data.model.Playlist
import com.ozin.music.core.data.model.PlaylistSongCrossRef
import com.ozin.music.core.data.model.ProblemFile
import com.ozin.music.core.data.model.SmartPlaylist
import com.ozin.music.core.data.model.Song

@Database(
    entities = [
        Song::class, Playlist::class, PlaylistSongCrossRef::class, ProblemFile::class,
        ListeningEvent::class, SmartPlaylist::class, BluetoothDeviceProfile::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class OzinDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun problemFileDao(): ProblemFileDao
    abstract fun listeningEventDao(): ListeningEventDao
    abstract fun smartPlaylistDao(): SmartPlaylistDao
    abstract fun bluetoothDeviceProfileDao(): BluetoothDeviceProfileDao
}
