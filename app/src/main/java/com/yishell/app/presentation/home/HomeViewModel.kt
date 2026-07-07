package com.yishell.app.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yishell.app.data.model.AuthType
import com.yishell.app.data.model.ConnectionConfig
import com.yishell.app.data.remote.ConnectionExporter
import com.yishell.app.data.remote.FullBackupManager
import com.yishell.app.data.remote.SshManager
import com.yishell.app.data.security.CryptoManager
import com.yishell.app.domain.repository.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val connectionRepository: ConnectionRepository,
    private val connectionExporter: ConnectionExporter,
    private val fullBackupManager: FullBackupManager,
    private val sshManager: SshManager,
    private val cryptoManager: CryptoManager
) : ViewModel() {

    /**
     * 已连接会话（P1-6 SSOT 重构）。
     * 包装来自 [SshManager.activeConnections] 的 [SshManager.ActiveConnectionInfo]，
     * 不再独立维护本地状态，消除 Home / Terminal / SshManager 三方漂移。
     */
    data class ConnectedSession(
        val info: com.yishell.app.data.remote.SshManager.ActiveConnectionInfo
    ) {
        val connectedAt: Long get() = info.connectedAt
    }

    companion object {
        private const val TAG = "HomeVM"
    }

    val connections: StateFlow<List<ConnectionConfig>> =
        connectionRepository.getAllConnections()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private val _connectionHistory = MutableStateFlow<List<String>>(emptyList())
    val connectionHistory: StateFlow<List<String>> = _connectionHistory.asStateFlow()

    // P1-6：connectedSessions 从 SshManager SSOT 派生，不再独立维护本地列表。
    // _clockTick 每秒翻转，触发派生 flow 重新 emit 以刷新"已连接时长"显示。
    private val _clockTick = MutableStateFlow(0L)

    val connectedSessions: StateFlow<List<ConnectedSession>> =
        combine(sshManager.activeConnections, _clockTick) { active, _ ->
            active.values
                .map { ConnectedSession(it) }
                .sortedByDescending { it.connectedAt }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var durationTimerJob: Job? = null

    init {
        // P1-6：启动时钟，让 connectedSessions 每秒重新 emit 刷新时长。
        startDurationTimer()
        // 监听数据库变更，编辑连接后同步刷新已连接卡片的 color / customIconUri
        viewModelScope.launch {
            connections.collect { list ->
                val active = sshManager.activeConnections.value
                for (conn in list) {
                    val session = active[conn.id] ?: continue
                    if (session.color != conn.color || session.customIconUri != conn.customIconUri) {
                        sshManager.refreshConnectionMetadata(conn.id, conn.color, conn.customIconUri)
                    }
                }
            }
        }
    }

    fun disconnect(session: ConnectedSession) {
        // P1-6：直接调用 SSOT 的 disconnect，连接移除后 activeConnections 自动更新，
        // 派生的 connectedSessions 跟着更新，无需手动维护本地列表。
        val connectionId = session.info.connectionId
        try {
            sshManager.disconnect(connectionId)
            Log.d(TAG, "SSH connection closed for $connectionId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect SSH connection $connectionId: ${e.message}", e)
        }
    }

    fun disconnectAll() {
        // P1-6：从 SSOT 取当前所有活跃连接 id，逐个断开。SSOT 更新后派生 flow 自动同步。
        sshManager.activeConnections.value.keys.toList().forEach { connectionId ->
            try {
                sshManager.disconnect(connectionId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to disconnect $connectionId: ${e.message}", e)
            }
        }
    }

    fun getDuration(connectedAt: Long): String {
        val elapsed = System.currentTimeMillis() - connectedAt
        val hours = elapsed / 3600000
        val minutes = (elapsed % 3600000) / 60000
        val seconds = (elapsed % 60000) / 1000
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun startDurationTimer() {
        durationTimerJob?.cancel()
        durationTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                // P1-6：每秒翻转 tick，触发 connectedSessions 派生 flow 重新 emit，刷新时长显示。
                _clockTick.value = _clockTick.value + 1
            }
        }
    }

    fun getRecentConnections(): List<ConnectionConfig> {
        // 按 lastConnected (fallback createdAt) 降序排列，取前10条
        // DAO 已按 lastConnected DESC 排序，但 null 值需要 fallback 到 createdAt
        return connections.value
            .sortedByDescending { it.lastConnected ?: it.createdAt }
            .take(10)
    }

    fun getFavoriteConnections(): List<ConnectionConfig> {
        return connections.value.filter { it.isFavorite }
            .sortedByDescending { it.lastConnected ?: 0L }
            .take(10)
    }

    fun addToHistory(connectionId: String) {
        val currentHistory = _connectionHistory.value.toMutableList()
        currentHistory.remove(connectionId)
        currentHistory.add(0, connectionId)
        if (currentHistory.size > 10) {
            currentHistory.removeAt(currentHistory.lastIndex)
        }
        _connectionHistory.value = currentHistory
    }

    private val _isQuickConnectExpanded = MutableStateFlow(false)
    val isQuickConnectExpanded: StateFlow<Boolean> = _isQuickConnectExpanded.asStateFlow()

    private val _quickHost = MutableStateFlow("")
    val quickHost: StateFlow<String> = _quickHost.asStateFlow()

    private val _quickPort = MutableStateFlow("22")
    val quickPort: StateFlow<String> = _quickPort.asStateFlow()

    private val _quickUsername = MutableStateFlow("")
    val quickUsername: StateFlow<String> = _quickUsername.asStateFlow()

    private val _quickPassword = MutableStateFlow("")
    val quickPassword: StateFlow<String> = _quickPassword.asStateFlow()

    fun toggleQuickConnect() {
        _isQuickConnectExpanded.value = !_isQuickConnectExpanded.value
    }

    fun updateQuickHost(value: String) {
        _quickHost.value = value
    }

    fun updateQuickPort(value: String) {
        // 只允许数字
        if (value.all { it.isDigit() }) {
            _quickPort.value = value
        }
    }

    fun updateQuickUsername(value: String) {
        _quickUsername.value = value
    }

    fun updateQuickPassword(value: String) {
        _quickPassword.value = value
    }

    fun quickConnect(): String? {
        val host = _quickHost.value.trim()
        val port = _quickPort.value.trim().toIntOrNull() ?: 22
        val username = _quickUsername.value.trim()
        val password = _quickPassword.value

        // 输入校验
        if (host.isBlank()) {
            Log.w(TAG, "Quick connect: host is blank")
            return null
        }
        if (username.isBlank()) {
            Log.w(TAG, "Quick connect: username is blank")
            return null
        }
        if (port !in 1..65535) {
            Log.w(TAG, "Quick connect: invalid port $port")
            return null
        }

        // 安全修复（P0-3）：快速连接的密码必须加密后再放入 ConnectionConfig，
        // 否则明文密码会留在内存和 tempConnections map 中，与持久化连接的加密标准不一致。
        val encryptedPassword = if (password.isBlank()) null else cryptoManager.encrypt(password)

        val config = ConnectionConfig(
            id = UUID.randomUUID().toString(),
            name = "$username@$host",
            host = host,
            port = port,
            username = username,
            authType = AuthType.PASSWORD,
            password = encryptedPassword
        )

        try {
            connectionRepository.saveTempConnection(config)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save temp connection: ${e.message}", e)
            return null
        }

        return config.id
    }

    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage.asStateFlow()

    private val _availableBackups = MutableStateFlow<List<java.io.File>>(emptyList())
    val availableBackups: StateFlow<List<java.io.File>> = _availableBackups.asStateFlow()

    fun deleteConnection(connection: ConnectionConfig) {
        viewModelScope.launch {
            try {
                connectionRepository.deleteConnection(connection)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete connection: ${e.message}", e)
            }
        }
    }

    fun duplicateConnection(connection: ConnectionConfig) {
        viewModelScope.launch {
            try {
                val suffix = " (副本)"
                var newName = connection.name + suffix
                var counter = 2
                while (connectionRepository.getByName(newName) != null) {
                    newName = connection.name + " (副本$counter)"
                    counter++
                }
                connectionRepository.duplicateConnection(connection.id, newName)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to duplicate connection: ${e.message}", e)
            }
        }
    }

    fun exportConnections() {
        viewModelScope.launch {
            try {
                val allConnections = connections.value
                val json = connectionExporter.exportToJson(allConnections)
                val file = connectionExporter.saveToFile(json)
                _exportMessage.value = if (file != null) {
                    "已导出到: ${file.absolutePath}"
                } else {
                    "导出失败"
                }
            } catch (e: Exception) {
                _exportMessage.value = "导出失败: ${e.message}"
            }
        }
    }

    fun importConnections(json: String) {
        viewModelScope.launch {
            try {
                val imported = connectionExporter.importFromJson(json)
                imported.forEach { config ->
                    connectionRepository.insertConnection(config)
                }
                _exportMessage.value = "已导入 ${imported.size} 个连接"
            } catch (e: Exception) {
                _exportMessage.value = "导入失败: ${e.message}"
            }
        }
    }

    fun clearExportMessage() {
        _exportMessage.value = null
    }

    fun createFullBackup() {
        viewModelScope.launch {
            try {
                val allConnections = connections.value
                val result = fullBackupManager.createFullBackup(allConnections)
                _backupMessage.value = result.message
            } catch (e: Exception) {
                _backupMessage.value = "备份失败: ${e.message}"
            }
        }
    }

    fun loadBackups() {
        viewModelScope.launch {
            _availableBackups.value = fullBackupManager.listBackups()
        }
    }

    fun restoreFromBackup(file: java.io.File) {
        viewModelScope.launch {
            try {
                val result = fullBackupManager.restoreFromFile(file)
                if (result.success) {
                    result.connections.forEach { config ->
                        connectionRepository.insertConnection(config)
                    }
                    _backupMessage.value = result.message
                } else {
                    _backupMessage.value = result.message
                }
            } catch (e: Exception) {
                _backupMessage.value = "恢复失败: ${e.message}"
            }
        }
    }

    fun clearBackupMessage() {
        _backupMessage.value = null
    }
}
