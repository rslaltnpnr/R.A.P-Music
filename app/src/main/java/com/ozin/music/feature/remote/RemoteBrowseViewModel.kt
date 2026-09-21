package com.ozin.music.feature.remote

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.data.repository.RemoteServerRepository
import com.ozin.music.core.player.PlayerController
import com.ozin.music.core.remote.RemoteAudioFile
import com.ozin.music.core.remote.RemoteBrowseResult
import com.ozin.music.core.remote.RemoteEntry
import com.ozin.music.core.remote.RemoteError
import com.ozin.music.core.remote.RemoteFolder
import com.ozin.music.core.remote.RemoteMusicSource
import com.ozin.music.core.remote.RemoteServerConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RemoteBrowseUiState(
    val serverName: String = "",
    val currentPath: String = "/",
    val entries: List<RemoteEntry> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val canGoUp: Boolean = false,
)

@HiltViewModel
class RemoteBrowseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RemoteServerRepository,
    private val remoteMusicSource: RemoteMusicSource,
    private val playerController: PlayerController,
) : ViewModel() {

    private val serverId: Long = checkNotNull(savedStateHandle["serverId"])
    private var config: RemoteServerConfig? = null
    private val pathStack = mutableListOf("/")

    private val _state = MutableStateFlow(RemoteBrowseUiState())
    val state: StateFlow<RemoteBrowseUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = repository.getConfig(serverId)
            config = loaded
            if (loaded == null) {
                _state.value = _state.value.copy(isLoading = false, error = "Server credentials not found")
            } else {
                _state.value = _state.value.copy(serverName = loaded.name)
                load(pathStack.last())
            }
        }
    }

    fun openFolder(folder: RemoteFolder) {
        pathStack += folder.path
        load(folder.path)
    }

    fun navigateUp() {
        if (pathStack.size <= 1) return
        pathStack.removeAt(pathStack.lastIndex)
        load(pathStack.last())
    }

    fun retry() = load(pathStack.last())

    fun playAudioFile(file: RemoteAudioFile) {
        val cfg = config ?: return
        val audioFiles = _state.value.entries.filterIsInstance<RemoteAudioFile>()
        val index = audioFiles.indexOf(file).coerceAtLeast(0)
        playerController.playRemoteFiles(audioFiles, cfg, index, remoteMusicSource)
    }

    private fun load(path: String) {
        val cfg = config ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = remoteMusicSource.listEntries(cfg, path)) {
                is RemoteBrowseResult.Success -> _state.value = _state.value.copy(
                    entries = result.entries.sortedWith(compareBy({ it !is RemoteFolder }, { it.name.lowercase() })),
                    currentPath = path,
                    isLoading = false,
                    canGoUp = pathStack.size > 1,
                )
                is RemoteBrowseResult.Error -> _state.value = _state.value.copy(
                    isLoading = false,
                    error = describe(result.reason),
                    canGoUp = pathStack.size > 1,
                )
            }
        }
    }

    private fun describe(reason: RemoteError): String = when (reason) {
        is RemoteError.AuthFailed -> "Authentication failed. Check the username/password."
        is RemoteError.ServerError -> "Server returned an error (code ${reason.code})."
        is RemoteError.ConnectionFailed -> "Could not connect: ${reason.message}"
        is RemoteError.MalformedResponse -> "The server returned an unexpected response."
    }
}
