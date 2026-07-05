package com.yishell.app.presentation.sftp

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yishell.app.data.remote.SftpManager
import com.yishell.app.data.remote.SshManager
import com.yishell.app.data.model.SftpItem
import com.yishell.app.domain.repository.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SftpViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val sshManager: SshManager,
    private val sftpManager: SftpManager,
    private val connectionRepository: ConnectionRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val connectionId: String = savedStateHandle["connectionId"] ?: ""

    companion object {
        private const val TAG = "SftpViewModel"
    }

    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _files = MutableStateFlow<List<SftpItem>>(emptyList())
    val files: StateFlow<List<SftpItem>> = _files.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _selectedFiles = MutableStateFlow(emptySet<String>())
    val selectedFiles: StateFlow<Set<String>> = _selectedFiles.asStateFlow()
    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()

    /** P2-4：当前传输进度，非 null 时 UI 显示进度条。 */
    private val _transferProgress = MutableStateFlow<TransferProgress?>(null)
    val transferProgress: StateFlow<TransferProgress?> = _transferProgress.asStateFlow()

    private var connected = false

    init {
        if (connectionId.isNotEmpty()) {
            connectSftp()
        }
    }

    private fun connectSftp() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val config = withContext(Dispatchers.IO) {
                    connectionRepository.getConnectionById(connectionId)
                }
                if (config == null) {
                    _error.value = "Connection not found"
                    _isLoading.value = false
                    return@launch
                }

                // Retry logic for intermittent SFTP failures
                var lastException: Exception? = null
                val maxRetries = 3
                for (attempt in 1..maxRetries) {
                    try {
                        withContext(Dispatchers.IO) {
                            // Ensure SSH connection is alive; reconnect if needed
                            if (!sshManager.isConnected(connectionId)) {
                                Log.d(TAG, "SSH not connected, attempting reconnect (attempt $attempt)")
                                val reconnected = sshManager.connect(config)
                                if (!reconnected) {
                                    throw IllegalStateException("SSH reconnection failed")
                                }
                            }

                            val connection = sshManager.getConnection(connectionId)
                                ?: throw IllegalStateException("SSH connection not available")

                            // Check if existing SFTP client is still valid, reconnect if stale
                            if (!sftpManager.isConnected()) {
                                Log.d(TAG, "SFTP client stale, reconnecting (attempt $attempt)")
                                sftpManager.reconnect(connection)
                            } else {
                                // Use existing client
                                sftpManager.setSftpClient(
                                    com.trilead.ssh2.SFTPv3Client(connection)
                                )
                            }
                        }

                        connected = true
                        val home = withContext(Dispatchers.IO) {
                            sftpManager.getHomeDir()
                        }
                        _currentPath.value = home
                        loadFiles(home)
                        lastException = null
                        break // Success, exit retry loop
                    } catch (e: com.yishell.app.data.remote.HostKeyVerificationException) {
                        // 安全修复（P0-1）：SFTP 无独立的指纹确认 UI。
                        // 提示用户先通过终端页面连接并确认主机指纹。
                        Log.w(TAG, "SFTP connect blocked by host key verification: ${e.message}")
                        _error.value = if (e.type == com.yishell.app.data.remote.HostKeyVerificationException.Type.MISMATCH) {
                            "主机指纹已变更，可能存在安全风险。请先通过终端连接并确认指纹后再使用 SFTP。"
                        } else {
                            "首次连接该主机，请先通过终端页面连接并确认主机指纹，再使用 SFTP。"
                        }
                        _isLoading.value = false
                        return@launch
                    } catch (e: Exception) {
                        Log.w(TAG, "SFTP connect attempt $attempt failed: ${e.message}")
                        lastException = e
                        if (attempt < maxRetries) {
                            delay(1000L * attempt) // Exponential backoff
                        }
                    }
                }

                if (lastException != null) {
                    _error.value = "SFTP connection failed: ${lastException!!.message}"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "SFTP connection failed: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun loadFiles(path: String = _currentPath.value) {
        if (!connected) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val items = withContext(Dispatchers.IO) {
                    sftpManager.listFiles(path)
                }
                _files.value = items
                _currentPath.value = path
            } catch (e: Exception) {
                // If SFTP operation fails, try reconnecting once
                Log.w(TAG, "loadFiles failed, attempting reconnect: ${e.message}")
                try {
                    withContext(Dispatchers.IO) {
                        val config = connectionRepository.getConnectionById(connectionId)
                        if (config != null && sshManager.isConnected(connectionId)) {
                            val connection = sshManager.getConnection(connectionId)
                            if (connection != null) {
                                sftpManager.reconnect(connection)
                                val items = sftpManager.listFiles(path)
                                _files.value = items
                                _currentPath.value = path
                                return@withContext
                            }
                        }
                    }
                    _error.value = "Failed to list files: ${e.message}"
                } catch (reconnectError: Exception) {
                    _error.value = "Failed to list files: ${e.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun navigateToFolder(folderPath: String) {
        loadFiles(folderPath)
    }

    fun navigateUp() {
        val current = _currentPath.value
        val parent = if (current.endsWith("/")) {
            val idx = current.lastIndexOf('/', current.length - 2)
            if (idx >= 0) current.substring(0, idx + 1) else "/"
        } else {
            val idx = current.lastIndexOf('/')
            if (idx >= 0) {
                if (idx == 0) "/" else current.substring(0, idx)
            } else {
                "/"
            }
        }
        loadFiles(parent)
    }

    fun navigateToPath(index: Int) {
        val segments = _currentPath.value.split("/").filter { it.isNotEmpty() }
        val target = "/" + segments.take(index + 1).joinToString("/")
        loadFiles(target)
    }

    fun refresh() {
        loadFiles()
    }

    fun downloadFile(item: SftpItem) {
        viewModelScope.launch {
            _transferProgress.value = TransferProgress(0, item.size, item.name, isDownload = true)
            try {
                withContext(Dispatchers.IO) {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    )
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs()
                    }
                    val localFile = File(downloadsDir, item.name)
                    // P2-4：节流更新进度，避免大文件高频回调拖垮 UI（每 64KB 或完成时更新）。
                    var lastReported = 0L
                    sftpManager.downloadFile(item.path, localFile) { transferred, total ->
                        if (transferred - lastReported >= 65_536 || transferred >= total) {
                            _transferProgress.value = TransferProgress(transferred, total, item.name, isDownload = true)
                            lastReported = transferred
                        }
                    }
                }
                _snackbarMessage.value = "Downloaded: ${item.name}"
            } catch (e: Exception) {
                _error.value = "Download failed: ${e.message}"
            } finally {
                _transferProgress.value = null
            }
        }
    }

    fun deleteFile(item: SftpItem) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    sftpManager.deleteFile(item.path)
                }
                _snackbarMessage.value = "Deleted: ${item.name}"
                loadFiles()
            } catch (e: Exception) {
                _error.value = "Delete failed: ${e.message}"
            }
        }
    }

    fun uploadFile(localFile: File) {
        viewModelScope.launch {
            val total = localFile.length()
            _transferProgress.value = TransferProgress(0, total, localFile.name, isDownload = false)
            try {
                val remotePath = if (_currentPath.value.endsWith("/")) {
                    "${_currentPath.value}${localFile.name}"
                } else {
                    "${_currentPath.value}/${localFile.name}"
                }
                withContext(Dispatchers.IO) {
                    // P2-4：节流更新进度，避免大文件高频回调拖垮 UI。
                    var lastReported = 0L
                    sftpManager.uploadFile(remotePath, localFile) { transferred, t ->
                        if (transferred - lastReported >= 65_536 || transferred >= t) {
                            _transferProgress.value = TransferProgress(transferred, t, localFile.name, isDownload = false)
                            lastReported = transferred
                        }
                    }
                }
                _snackbarMessage.value = "Uploaded: ${localFile.name}"
                loadFiles()
            } catch (e: Exception) {
                _error.value = "Upload failed: ${e.message}"
            } finally {
                _transferProgress.value = null
            }
        }
    }

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val newPath = if (_currentPath.value.endsWith("/")) {
                    "${_currentPath.value}$name"
                } else {
                    "${_currentPath.value}/$name"
                }
                withContext(Dispatchers.IO) {
                    sftpManager.mkdir(newPath)
                }
                _snackbarMessage.value = "已创建文件夹: $name"
                loadFiles()
            } catch (e: Exception) {
                _error.value = "创建文件夹失败: ${e.message}"
            }
        }
    }

    fun renameItem(item: SftpItem, newName: String) {
        if (newName.isBlank() || newName == item.name) return
        viewModelScope.launch {
            try {
                val parentDir = item.path.substringBeforeLast('/')
                val newPath = "$parentDir/$newName"
                withContext(Dispatchers.IO) {
                    sftpManager.rename(item.path, newPath)
                }
                _snackbarMessage.value = "已重命名: $newName"
                loadFiles()
            } catch (e: Exception) {
                _error.value = "重命名失败: ${e.message}"
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun clearError() {
        _error.value = null
    }

    fun toggleSelection(fileName: String) {
        val current = _selectedFiles.value.toMutableSet()
        if (current.contains(fileName)) current.remove(fileName) else current.add(fileName)
        _selectedFiles.value = current
        _isMultiSelectMode.value = current.isNotEmpty()
    }
    fun selectAll() {
        val allNames = _files.value.map { it.name }.toSet()
        _selectedFiles.value = allNames
        _isMultiSelectMode.value = true
    }
    fun clearSelection() {
        _selectedFiles.value = emptySet()
        _isMultiSelectMode.value = false
    }
    fun deleteSelected() {
        val selected = _selectedFiles.value
        if (selected.isEmpty()) return
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    sftpManager.batchDelete(selected.toList(), _currentPath.value)
                }
                // P2-6：原 batchDelete 静默吞异常，现据结果给用户诚实反馈。
                _snackbarMessage.value = if (result.failed.isEmpty()) {
                    "已删除 ${result.deletedCount} 个项目"
                } else {
                    "已删除 ${result.deletedCount} 个，失败 ${result.failed.size} 个：${result.failed.joinToString(", ")}"
                }
                clearSelection()
                loadFiles()
            } catch (e: Exception) {
                _error.value = "批量删除失败: ${e.message}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sftpManager.close()
    }
}

/** P2-4：SFTP 传输进度快照，供 UI 显示进度条。 */
data class TransferProgress(
    val transferred: Long,
    val total: Long,
    val fileName: String,
    val isDownload: Boolean
) {
    /** 完成百分比 0-100；total 未知时返回 0（UI 可显示为不确定进度）。 */
    val percent: Int get() = if (total > 0) ((transferred.toFloat() / total) * 100).toInt().coerceIn(0, 100) else 0
}
