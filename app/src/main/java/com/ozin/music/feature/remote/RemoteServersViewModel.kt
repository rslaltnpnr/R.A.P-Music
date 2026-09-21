package com.ozin.music.feature.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozin.music.core.data.model.RemoteServer
import com.ozin.music.core.data.repository.RemoteServerRepository
import com.ozin.music.core.remote.RemoteConnectionResult
import com.ozin.music.core.remote.RemoteMusicSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ConnectionTestStatus { TESTING, SUCCESS, FAILURE }

@HiltViewModel
class RemoteServersViewModel @Inject constructor(
    private val repository: RemoteServerRepository,
    private val remoteMusicSource: RemoteMusicSource,
) : ViewModel() {

    val servers: StateFlow<List<RemoteServer>> = repository.servers.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _testResults = MutableStateFlow<Map<Long, ConnectionTestStatus>>(emptyMap())
    val testResults: StateFlow<Map<Long, ConnectionTestStatus>> = _testResults.asStateFlow()

    fun addServer(name: String, address: String, username: String, password: String) {
        val trimmedName = name.trim()
        val trimmedAddress = address.trim()
        if (trimmedName.isEmpty() || trimmedAddress.isEmpty()) return
        viewModelScope.launch {
            repository.addServer(trimmedName, trimmedAddress, username.trim(), password)
        }
    }

    fun updateServer(id: Long, name: String, address: String, username: String, password: String?) {
        viewModelScope.launch {
            repository.updateServer(id, name.trim(), address.trim(), username.trim(), password?.takeIf { it.isNotBlank() })
        }
    }

    fun deleteServer(id: Long) {
        viewModelScope.launch {
            repository.deleteServer(id)
            _testResults.value = _testResults.value - id
        }
    }

    fun testConnection(id: Long) {
        viewModelScope.launch {
            _testResults.value = _testResults.value + (id to ConnectionTestStatus.TESTING)
            val config = repository.getConfig(id)
            val status = if (config == null) {
                ConnectionTestStatus.FAILURE
            } else {
                when (remoteMusicSource.testConnection(config)) {
                    is RemoteConnectionResult.Success -> ConnectionTestStatus.SUCCESS
                    is RemoteConnectionResult.Failure -> ConnectionTestStatus.FAILURE
                }
            }
            _testResults.value = _testResults.value + (id to status)
        }
    }
}
