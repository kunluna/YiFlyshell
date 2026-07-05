package com.yishell.app.presentation.terminal

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yishell.app.data.model.PortForwarding
import com.yishell.app.data.model.PortForwardingType
import com.yishell.app.data.model.QuickCommand
import com.yishell.app.data.remote.HostKeyVerificationException
import com.yishell.app.data.remote.SshManager
import com.yishell.app.data.repository.QuickCommandRepository
import com.yishell.app.domain.repository.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.yishell.app.data.local.SettingsDataStore
import com.yishell.app.data.local.TerminalColorScheme
import com.yishell.app.presentation.util.AnsiParserOptimized
import com.yishell.app.presentation.util.VirtualTerminal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

data class SessionInfo(
    val id: String,
    val name: String,
    val connectionId: String
)

@HiltViewModel
class TerminalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sshManager: SshManager,
    private val connectionRepository: ConnectionRepository,
    private val quickCommandRepository: QuickCommandRepository,
    private val settingsDataStore: SettingsDataStore,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: Context
) : ViewModel() {

    companion object {
        private const val TAG = "TerminalVM"
        // P1-4：命令历史上限，防止长会话内存无限增长。超出时丢弃最旧条目。
        private const val MAX_COMMAND_HISTORY = 500
        // P2-3：粘贴确认阈值。超过此字符数的大段文本需用户确认，防止误粘贴。
        private const val PASTE_CONFIRM_THRESHOLD = 200
    }

    private val connectionId: String = savedStateHandle["connectionId"] ?: ""

    private val _terminalOutput = MutableStateFlow("")
    val terminalOutput: StateFlow<String> = _terminalOutput.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _quickCommands = MutableStateFlow<List<QuickCommand>>(emptyList())
    val quickCommands: StateFlow<List<QuickCommand>> = _quickCommands.asStateFlow()

    private var outputReaderJob: Job? = null
    private var outputBuffer = StringBuilder()
    private val outputLock = Any()
    private var currentSessionId: String? = null
    private val virtualTerminal = VirtualTerminal()

    private val _ctrlActive = MutableStateFlow(false)
    val ctrlActive: StateFlow<Boolean> = _ctrlActive.asStateFlow()

    private val _altActive = MutableStateFlow(false)
    val altActive: StateFlow<Boolean> = _altActive.asStateFlow()

    private val _autoReconnect = MutableStateFlow(true)
    val autoReconnect: StateFlow<Boolean> = _autoReconnect.asStateFlow()
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 3

    private val _portForwardings = MutableStateFlow<List<PortForwarding>>(emptyList())
    val portForwardings: StateFlow<List<PortForwarding>> = _portForwardings.asStateFlow()

    private val _showPortForwardDialog = MutableStateFlow(false)
    val showPortForwardDialog: StateFlow<Boolean> = _showPortForwardDialog.asStateFlow()

    private val _keyboardLayout = MutableStateFlow("")
    val keyboardLayout: StateFlow<String> = _keyboardLayout.asStateFlow()

    private val _terminalColorScheme = MutableStateFlow(TerminalColorScheme.DEFAULT)
    val terminalColorScheme: StateFlow<TerminalColorScheme> = _terminalColorScheme.asStateFlow()

    private val _connectionDuration = MutableStateFlow(0L)
    val connectionDuration: StateFlow<Long> = _connectionDuration.asStateFlow()

    private val _bytesSent = MutableStateFlow(0L)
    val bytesSent: StateFlow<Long> = _bytesSent.asStateFlow()

    private val _bytesReceived = MutableStateFlow(0L)
    val bytesReceived: StateFlow<Long> = _bytesReceived.asStateFlow()

    private val _connectionName = MutableStateFlow("")
    val connectionName: StateFlow<String> = _connectionName.asStateFlow()

    private val _sessions = MutableStateFlow<List<SessionInfo>>(emptyList())
    val sessions: StateFlow<List<SessionInfo>> = _sessions.asStateFlow()

    private val _currentSessionIndex = MutableStateFlow(0)
    val currentSessionIndex: StateFlow<Int> = _currentSessionIndex.asStateFlow()

    /**
     * P2-3：待确认的粘贴内容。当剪贴板文本超过 [PASTE_CONFIRM_THRESHOLD] 字符时，
     * 不直接发送，而是暂存到这里并暴露给 UI 弹确认对话框，防止误粘贴大段文本。
     */
    private val _pendingPaste = MutableStateFlow<String?>(null)
    val pendingPaste: StateFlow<String?> = _pendingPaste.asStateFlow()

    private var connectionStartTime: Long = 0L
    private var statsTimerJob: Job? = null

    private val commandHistory = mutableListOf<String>()
    private var historyIndex = -1

    /**
     * 安全修复（P0-1）：当连接遇到未知主机指纹时，保存待确认的指纹信息。
     * 用户在 UI 确认后，[confirmHostKey] 会用这个指纹重新调用 [SshManager.connect]。
     */
    private var pendingHostKey: HostKeyVerificationException? = null

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Coroutine exception: ${throwable.message}", throwable)
    }

    init {
        // 加载键盘布局设置
        viewModelScope.launch {
            val settings = settingsDataStore.settings.first()
            _keyboardLayout.value = settings.keyboardLayout
            _terminalColorScheme.value = settings.terminalColorScheme
        }
        // 加载快捷命令：先 seed 默认值，再收集 Flow
        if (connectionId.isNotEmpty()) {
            viewModelScope.launch {
                quickCommandRepository.seedDefaults(connectionId)
                quickCommandRepository.getByConnectionId(connectionId).collect {
                    _quickCommands.value = it
                }
            }
            connect()
        } else {
            _connectionState.value = ConnectionState.Error("无效的连接 ID")
        }
    }

    fun connect() {
        viewModelScope.launch(exceptionHandler) {
            _connectionState.value = ConnectionState.Connecting

            val config = withContext(Dispatchers.IO) {
                try {
                    connectionRepository.getConnectionById(connectionId)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load connection: ${e.message}", e)
                    null
                }
            }

            if (config == null) {
                _connectionState.value = ConnectionState.Error("连接配置不存在")
                appendOutput("错误：连接配置不存在\n")
                return@launch
            }

            _connectionName.value = config.name

            appendOutput("正在连接 ${config.host}:${config.port}...\n")

            val success = withContext(Dispatchers.IO) {
                try {
                    sshManager.connect(config)
                } catch (e: HostKeyVerificationException) {
                    // 安全修复（P0-1）：未知主机或指纹不匹配，需要用户确认。
                    // 保存异常信息，切换到 HostKeyPending 状态，等待 UI 介入。
                    pendingHostKey = e
                    when (e.type) {
                        HostKeyVerificationException.Type.UNKNOWN_HOST -> {
                            appendOutput(
                                "首次连接该主机，需要确认服务器指纹：\n" +
                                    "  算法: ${e.algorithm}\n" +
                                    "  指纹: ${e.fingerprint}\n"
                            )
                        }
                        HostKeyVerificationException.Type.MISMATCH -> {
                            appendOutput(
                                "⚠ 警告：主机指纹已变更！可能是服务器重装或中间人攻击。\n" +
                                    "  算法: ${e.algorithm}\n" +
                                    "  原指纹: ${e.storedFingerprint}\n" +
                                    "  新指纹: ${e.fingerprint}\n"
                            )
                        }
                    }
                    _connectionState.value = ConnectionState.HostKeyPending(
                        hostname = e.hostname,
                        port = e.port,
                        algorithm = e.algorithm,
                        fingerprint = e.fingerprint,
                        storedFingerprint = e.storedFingerprint,
                        isMismatch = e.type == HostKeyVerificationException.Type.MISMATCH
                    )
                    return@withContext false
                } catch (e: Exception) {
                    Log.e(TAG, "connect() threw: ${e.message}", e)
                    appendOutput("连接错误: ${e.message}\n")
                    false
                }
            }

            if (success) {
                _connectionState.value = ConnectionState.Connected
                connectionStartTime = System.currentTimeMillis()
                startStatsTimer()
                appendOutput("已连接到 ${config.name}\n\n")
                currentSessionId = try {
                    connectionRepository.startSession(connectionId)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start session: ${e.message}", e)
                    null
                }
                startOutputReader()
            } else {
                // 安全修复（P0-1）：如果已进入 HostKeyPending 状态（等待用户确认指纹），
                // 不要覆盖成 Error —— 用户还没做决定，不是"连接失败"。
                val currentState = _connectionState.value
                if (currentState !is ConnectionState.HostKeyPending) {
                    _connectionState.value = ConnectionState.Error("连接失败")
                }
            }
        }
    }

    /**
     * 安全修复（P0-1）：用户确认主机指纹后，用已确认的指纹重新连接。
     * 仅在 [ConnectionState.HostKeyPending] 状态下有效。
     */
    fun confirmHostKey() {
        val pending = pendingHostKey ?: return
        if (_connectionState.value !is ConnectionState.HostKeyPending) return

        appendOutput("已确认指纹，正在重新连接...\n")
        _connectionState.value = ConnectionState.Connecting

        viewModelScope.launch(exceptionHandler) {
            val config = withContext(Dispatchers.IO) {
                try {
                    connectionRepository.getConnectionById(connectionId)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load connection for confirm: ${e.message}", e)
                    null
                }
            }

            if (config == null) {
                _connectionState.value = ConnectionState.Error("连接配置不存在")
                pendingHostKey = null
                return@launch
            }

            val success = withContext(Dispatchers.IO) {
                try {
                    // 用用户已确认的指纹重新连接，verifier 会验证一致后持久化
                    sshManager.connect(config, acceptedFingerprint = pending.fingerprint)
                } catch (e: HostKeyVerificationException) {
                    // 极端情况：确认后指纹又变了，重新进入待确认
                    pendingHostKey = e
                    _connectionState.value = ConnectionState.HostKeyPending(
                        hostname = e.hostname,
                        port = e.port,
                        algorithm = e.algorithm,
                        fingerprint = e.fingerprint,
                        storedFingerprint = e.storedFingerprint,
                        isMismatch = e.type == HostKeyVerificationException.Type.MISMATCH
                    )
                    false
                } catch (e: Exception) {
                    Log.e(TAG, "confirmHostKey reconnect failed: ${e.message}", e)
                    false
                }
            }

            if (success) {
                pendingHostKey = null
                _connectionState.value = ConnectionState.Connected
                connectionStartTime = System.currentTimeMillis()
                startStatsTimer()
                appendOutput("已连接到 ${config.name}\n\n")
                currentSessionId = try {
                    connectionRepository.startSession(connectionId)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start session: ${e.message}", e)
                    null
                }
                startOutputReader()
            } else {
                if (_connectionState.value !is ConnectionState.HostKeyPending) {
                    _connectionState.value = ConnectionState.Error("连接失败")
                    appendOutput("连接失败，请检查凭据\n")
                    pendingHostKey = null
                }
            }
        }
    }

    /**
     * 安全修复（P0-1）：用户拒绝主机指纹，放弃连接。
     */
    fun rejectHostKey() {
        appendOutput("已拒绝主机指纹，连接取消。\n")
        pendingHostKey = null
        _connectionState.value = ConnectionState.Disconnected
    }

    fun historyUp(): String? {
        if (commandHistory.isEmpty()) return null
        if (historyIndex < commandHistory.size - 1) {
            historyIndex++
        }
        return commandHistory[commandHistory.size - 1 - historyIndex]
    }

    fun historyDown(): String? {
        if (historyIndex > 0) {
            historyIndex--
            return commandHistory[commandHistory.size - 1 - historyIndex]
        }
        historyIndex = -1
        return ""
    }

    fun sendCommand(command: String) {
        if (command.isBlank()) return
        if (commandHistory.isEmpty() || commandHistory.last() != command) {
            commandHistory.add(command)
            // P1-4：超出上限时移除最旧条目，保持历史有界。
            while (commandHistory.size > MAX_COMMAND_HISTORY) {
                commandHistory.removeAt(0)
            }
        }
        historyIndex = -1

        val cmdBytes = "$command\n".toByteArray().size.toLong()
        _bytesSent.value = _bytesSent.value + cmdBytes

        viewModelScope.launch(exceptionHandler) {
            withContext(Dispatchers.IO) {
                try {
                    sshManager.sendInput(connectionId, "$command\n")
                } catch (e: Exception) {
                    Log.e(TAG, "sendCommand failed: ${e.message}", e)
                }
            }
        }
    }

    fun toggleCtrl() {
        _ctrlActive.value = !_ctrlActive.value
        if (_ctrlActive.value) _altActive.value = false
    }

    fun toggleAlt() {
        _altActive.value = !_altActive.value
        if (_altActive.value) _ctrlActive.value = false
    }

    fun sendKey(code: Int) {
        viewModelScope.launch(exceptionHandler) {
            val wasCtrl = _ctrlActive.value
            val wasAlt = _altActive.value
            if (wasCtrl) {
                _ctrlActive.value = false
            } else if (wasAlt) {
                _altActive.value = false
            }
            withContext(Dispatchers.IO) {
                try {
                    if (wasCtrl) {
                        val ctrlCode = if (code in 0x61..0x7A) code - 0x60 else code
                        sshManager.sendRawBytes(connectionId, byteArrayOf(ctrlCode.toByte()))
                    } else if (wasAlt) {
                        sshManager.sendRawBytes(connectionId, byteArrayOf(0x1B, code.toByte()))
                    } else {
                        sshManager.sendRawBytes(connectionId, byteArrayOf(code.toByte()))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "sendKey failed: ${e.message}", e)
                }
            }
        }
    }

    fun sendEscapeSequence(sequence: String) {
        viewModelScope.launch(exceptionHandler) {
            _altActive.value = false
            withContext(Dispatchers.IO) {
                try {
                    sshManager.sendRawBytes(connectionId, byteArrayOf(0x1B) + sequence.toByteArray())
                } catch (e: Exception) {
                    Log.e(TAG, "sendEscapeSequence failed: ${e.message}", e)
                }
            }
        }
    }

    fun copyOutputToClipboard() {
        val text = _terminalOutput.value
        if (text.isNotBlank()) {
            val stripped = AnsiParserOptimized.stripAllAnsi(text)
            val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("terminal_output", stripped)
            clipboard.setPrimaryClip(clip)
            _copyFeedback.value = "已复制 ${stripped.length} 字符"
        }
    }

    /**
     * 双光标选择模式：复制终端输出中 [start, end) 区间的纯文本。
     * 区间基于 _terminalOutput 的**原始字符串**偏移（含 ANSI 序列），
     * 先剥离 ANSI 后提取可见文本的对应区间。
     *
     * 第一性原理：用户在屏幕上看到的是已渲染的可见文本，选择区间也应映射到可见文本。
     * 但 UI 层拿到的 offset 是 AnnotatedString 的偏移（已跳过 ANSI），
     * 所以直接对 stripped 文本截取即可。
     */
    fun copySelectedText(start: Int, end: Int) {
        val text = _terminalOutput.value
        if (text.isBlank()) return
        val stripped = AnsiParserOptimized.stripAllAnsi(text)
        val s = start.coerceIn(0, stripped.length)
        val e = end.coerceIn(0, stripped.length)
        if (s >= e) return
        val selected = stripped.substring(s, e)
        val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("terminal_selection", selected)
        clipboard.setPrimaryClip(clip)
        _copyFeedback.value = "已复制 ${selected.length} 字符"
    }

    /**
     * 双光标选择模式（对抗性审查修复版）：直接接收 UI 层从 AnnotatedString
     * 提取的纯文本子串，避免 offset 在 parse()/stripAllAnsi() 之间的映射不一致。
     *
     * 第一性原理：UI 层的 AnnotatedString 已经跳过了 ANSI 和 hidden 段，
     * 其 subSequence(s, e).text 就是用户看到的文本。直接复制即可。
     */
    fun copyPlainText(text: String) {
        if (text.isBlank()) return
        val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("terminal_selection", text)
        clipboard.setPrimaryClip(clip)
        _copyFeedback.value = "已复制 ${text.length} 字符"
    }

    /** 复制反馈消息，UI 层观察后显示 Snackbar/Toast。null 表示无待显示反馈。 */
    private val _copyFeedback = MutableStateFlow<String?>(null)
    val copyFeedback: StateFlow<String?> = _copyFeedback.asStateFlow()

    /** UI 层显示反馈后调用，清除消息。 */
    fun consumeCopyFeedback() {
        _copyFeedback.value = null
    }

    fun pasteFromClipboard() {
        val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: return
            // P2-3：大段文本粘贴需确认，避免误操作把大块内容灌进 shell。
            if (text.length > PASTE_CONFIRM_THRESHOLD) {
                _pendingPaste.value = text
            } else {
                sendCommand(text)
            }
        }
    }

    /** P2-3：用户确认粘贴待确认的大段文本。 */
    fun confirmPaste() {
        val text = _pendingPaste.value ?: return
        _pendingPaste.value = null
        sendCommand(text)
    }

    /** P2-3：用户取消粘贴。 */
    fun cancelPaste() {
        _pendingPaste.value = null
    }

    // ===== 快捷命令 CRUD =====
    fun addQuickCommand(label: String, command: String) {
        viewModelScope.launch {
            quickCommandRepository.add(connectionId, label, command, _quickCommands.value.size)
        }
    }

    fun updateQuickCommand(command: QuickCommand) {
        viewModelScope.launch {
            quickCommandRepository.update(command)
        }
    }

    fun deleteQuickCommand(command: QuickCommand) {
        viewModelScope.launch {
            quickCommandRepository.delete(command)
        }
    }

    fun reorderQuickCommands(commands: List<QuickCommand>) {
        viewModelScope.launch {
            commands.forEachIndexed { idx, cmd ->
                quickCommandRepository.update(cmd.copy(sortOrder = idx))
            }
        }
    }

    fun clearOutput() {
        synchronized(outputLock) {
            outputBuffer.clear()
            virtualTerminal.reset()
        }
        _terminalOutput.value = ""
    }

    fun reconnect() {
        reconnectAttempts = 0
        synchronized(outputLock) {
            outputBuffer.clear()
        }
        _terminalOutput.value = ""
        connect()
    }

    fun disconnect() {
        stopStatsTimer()
        outputReaderJob?.cancel()
        currentSessionId?.let { sessionId ->
            viewModelScope.launch(exceptionHandler) {
                try {
                    connectionRepository.endSession(sessionId)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to end session: ${e.message}", e)
                }
            }
            currentSessionId = null
        }
        viewModelScope.launch(exceptionHandler) {
            withContext(Dispatchers.IO) {
                try {
                    sshManager.disconnect(connectionId)
                } catch (e: Exception) {
                    Log.e(TAG, "disconnect failed: ${e.message}", e)
                }
            }
        }
        _connectionState.value = ConnectionState.Disconnected
        appendOutput("\n[已断开连接]\n")
    }

    private fun startOutputReader() {
        outputReaderJob?.cancel()
        outputReaderJob = viewModelScope.launch(exceptionHandler) {
            var consecutiveReads = 0
            while (isActive) {
                val connected = withContext(Dispatchers.IO) {
                    try {
                        sshManager.isConnected(connectionId)
                    } catch (e: Exception) {
                        false
                    }
                }
                if (!connected) break

                val output = withContext(Dispatchers.IO) {
                    try {
                        sshManager.readOutput(connectionId)
                    } catch (e: Exception) {
                        Log.w(TAG, "readOutput in loop failed: ${e.message}")
                        ""
                    }
                }
                if (output.isNotEmpty()) {
                    appendOutput(output)
                    _bytesReceived.value = _bytesReceived.value + output.toByteArray().size
                    consecutiveReads++
                    if (consecutiveReads < 10) {
                        continue
                    }
                }
                consecutiveReads = 0
                delay(50)
            }

            if (_connectionState.value is ConnectionState.Connected) {
                _connectionState.value = ConnectionState.Disconnected
                appendOutput("\n[连接已断开]\n")
                if (_autoReconnect.value && reconnectAttempts < maxReconnectAttempts) {
                    attemptReconnect()
                }
            }
        }
    }

    private suspend fun attemptReconnect() {
        reconnectAttempts++
        appendOutput("\n[正在重连... ($reconnectAttempts/$maxReconnectAttempts)]\n")
        _connectionState.value = ConnectionState.Connecting
        delay(2000L * reconnectAttempts)

        val success = withContext(Dispatchers.IO) {
            try {
                val config = connectionRepository.getConnectionById(connectionId) ?: return@withContext false
                sshManager.connect(config)
            } catch (e: HostKeyVerificationException) {
                // 重连时遇到主机密钥问题 —— 服务器可能在重连期间重装了系统或换了密钥。
                // 不自动重试，让用户知情并决定。
                appendOutput(
                    "\n[重连失败：主机指纹已变更，可能是服务器重装或中间人攻击]\n"
                )
                pendingHostKey = e
                _connectionState.value = ConnectionState.HostKeyPending(
                    hostname = e.hostname,
                    port = e.port,
                    algorithm = e.algorithm,
                    fingerprint = e.fingerprint,
                    storedFingerprint = e.storedFingerprint,
                    isMismatch = e.type == HostKeyVerificationException.Type.MISMATCH
                )
                false
            } catch (e: Exception) {
                Log.e(TAG, "Reconnect failed: ${e.message}", e)
                false
            }
        }

        if (success) {
            _connectionState.value = ConnectionState.Connected
            reconnectAttempts = 0
            appendOutput("[重连成功]\n")
            startOutputReader()
        } else if (reconnectAttempts < maxReconnectAttempts) {
            attemptReconnect()
        } else {
            appendOutput("[重连失败，已达最大重试次数]\n")
            reconnectAttempts = 0
        }
    }

    fun toggleAutoReconnect() {
        _autoReconnect.value = !_autoReconnect.value
    }

    fun showPortForwardDialog() {
        _showPortForwardDialog.value = true
    }

    fun dismissPortForwardDialog() {
        _showPortForwardDialog.value = false
    }

    fun addPortForward(pf: PortForwarding) {
        if (_connectionState.value !is ConnectionState.Connected) return
        viewModelScope.launch(exceptionHandler) {
            val success = withContext(Dispatchers.IO) {
                when (pf.type) {
                    PortForwardingType.LOCAL -> sshManager.startLocalForward(connectionId, pf)
                    PortForwardingType.REMOTE -> sshManager.startRemoteForward(connectionId, pf)
                }
            }
            if (success) {
                _portForwardings.value = _portForwardings.value + pf
                val typeStr = if (pf.type == PortForwardingType.LOCAL) "本地" else "远程"
                appendOutput("[端口转发] ${typeStr}转发已建立: ${pf.localPort} -> ${pf.remoteHost}:${pf.remotePort}\n")
            } else {
                appendOutput("[端口转发] 建立失败: ${pf.localPort} -> ${pf.remoteHost}:${pf.remotePort}\n")
            }
        }
        _showPortForwardDialog.value = false
    }

    fun removePortForward(pf: PortForwarding) {
        viewModelScope.launch(exceptionHandler) {
            withContext(Dispatchers.IO) {
                when (pf.type) {
                    PortForwardingType.LOCAL -> sshManager.stopLocalForward(connectionId, pf.id)
                    PortForwardingType.REMOTE -> sshManager.stopRemoteForward(connectionId, pf.id)
                }
            }
            _portForwardings.value = _portForwardings.value.filter { it.id != pf.id }
            val typeStr = if (pf.type == PortForwardingType.LOCAL) "本地" else "远程"
            appendOutput("[端口转发] ${typeStr}转发已关闭: ${pf.localPort} -> ${pf.remoteHost}:${pf.remotePort}\n")
        }
    }

    private fun startStatsTimer() {
        statsTimerJob?.cancel()
        statsTimerJob = viewModelScope.launch(exceptionHandler) {
            while (isActive && _connectionState.value is ConnectionState.Connected) {
                val elapsed = (System.currentTimeMillis() - connectionStartTime) / 1000
                _connectionDuration.value = elapsed
                delay(1000)
            }
        }
    }

    private fun stopStatsTimer() {
        statsTimerJob?.cancel()
        statsTimerJob = null
    }

    private fun appendOutput(text: String) {
        synchronized(outputLock) {
            // P2-1（\e hack 根因澄清，Feynman 破妄）：
            // 此 replace 不是在修 SSH 库的 bug——trilead 的 readOutput 用 String(buffer,0,read)
            // 解码字节流，ESC(0x1B) 会正确解码为 \u001b，不会产生字面 "\e"。
            // 字面 "\e" 的真实来源是【服务器端】：某些 shell 的 `echo "\e[31m"`（未加 -e）、
            // 部分 PS1 配置、或脚本里 printf 未展开的转义会向 stdout 输出字面 '\','e' 两字符。
            // 这里把它们转回真正的 ESC，让 VirtualTerminal 的 ANSI 解析能正确着色。
            // 保留此防御性替换，但澄清真相以消除"自欺"。
            val processed = text.replace("\\e", "\u001b")
            val result = virtualTerminal.process(processed)
            outputBuffer = StringBuilder(result)
            if (outputBuffer.length > 100_000) {
                val full = outputBuffer.toString()
                val tail = full.takeLast(50_000)
                var start = 0
                while (start < tail.length && tail[start] == '\u001b') {
                    if (start + 1 < tail.length && tail[start + 1] == '[') {
                        val mIdx = tail.indexOf('m', start + 2)
                        if (mIdx >= 0) {
                            start = mIdx + 1
                        } else {
                            start = tail.length
                        }
                    } else if (start + 1 < tail.length && tail[start + 1] == ']') {
                        var j = start + 2
                        while (j < tail.length) {
                            if (tail[j] == '\u0007') { j++; break }
                            if (tail[j] == '\u001b' && j + 1 < tail.length && tail[j + 1] == '\\') { j += 2; break }
                            j++
                        }
                        start = j
                    } else {
                        start = minOf(start + 2, tail.length)
                    }
                }
                outputBuffer = StringBuilder("\u001b[0m" + tail.substring(start))
            }
            _terminalOutput.value = outputBuffer.toString()
        }
    }

    fun switchSession(index: Int) {
        if (index in _sessions.value.indices) {
            _currentSessionIndex.value = index
        }
    }

    fun closeSession(index: Int) {
        val current = _sessions.value
        if (current.size <= 1 || index !in current.indices) return
        _sessions.value = current.toMutableList().apply { removeAt(index) }
        if (_currentSessionIndex.value >= _sessions.value.size) {
            _currentSessionIndex.value = (_sessions.value.size - 1).coerceAtLeast(0)
        } else if (index < _currentSessionIndex.value) {
            _currentSessionIndex.value--
        } else if (index == _currentSessionIndex.value) {
            _currentSessionIndex.value = (_currentSessionIndex.value - 1).coerceAtLeast(0)
        }
    }

    fun addSession(name: String, connectionId: String) {
        val session = SessionInfo(
            id = UUID.randomUUID().toString(),
            name = name,
            connectionId = connectionId
        )
        _sessions.value = _sessions.value + session
        _currentSessionIndex.value = _sessions.value.size - 1
    }

    override fun onCleared() {
        stopStatsTimer()
        statsTimerJob?.cancel()
        outputReaderJob?.cancel()
        currentSessionId?.let { sessionId ->
            viewModelScope.launch(exceptionHandler) {
                try {
                    connectionRepository.endSession(sessionId)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to end session on clear: ${e.message}", e)
                }
            }
            currentSessionId = null
        }
        viewModelScope.launch(exceptionHandler) {
            withContext(Dispatchers.IO) {
                try {
                    sshManager.disconnect(connectionId)
                } catch (e: Exception) {
                    Log.e(TAG, "onCleared disconnect failed: ${e.message}", e)
                }
            }
        }
        super.onCleared()
    }
}

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()

    /**
     * 安全修复（P0-1）：首次连接未知主机或指纹不匹配时，进入此状态等待用户确认。
     * [fingerprint] 是服务器实际指纹，[storedFingerprint] 非 null 表示已知主机指纹变更（更危险）。
     * [algorithm] 是密钥算法（如 ssh-ed25519），[hostname]/[port] 用于 UI 展示。
     */
    data class HostKeyPending(
        val hostname: String,
        val port: Int,
        val algorithm: String,
        val fingerprint: String,
        val storedFingerprint: String? = null,
        val isMismatch: Boolean = false
    ) : ConnectionState()
}
