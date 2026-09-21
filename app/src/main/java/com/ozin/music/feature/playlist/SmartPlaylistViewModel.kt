package com.ozin.music.feature.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.data.model.SmartPlaylist
import com.ozin.music.core.data.repository.SmartPlaylistRepository
import com.ozin.music.core.domain.SmartPlaylistRule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SmartPlaylistViewModel @Inject constructor(
    private val smartPlaylistRepository: SmartPlaylistRepository,
) : ViewModel() {

    val playlists: StateFlow<List<SmartPlaylist>> = smartPlaylistRepository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String, rules: List<SmartPlaylistRule>) {
        if (name.isBlank() || rules.isEmpty()) return
        viewModelScope.launch { smartPlaylistRepository.create(name.trim(), rules) }
    }

    fun delete(playlist: SmartPlaylist) {
        viewModelScope.launch { smartPlaylistRepository.delete(playlist) }
    }
}
