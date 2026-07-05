package com.yishell.app.data.remote

import android.content.Context
import android.util.Log
import com.trilead.ssh2.Connection
import com.trilead.ssh2.ConnectionMonitor
import com.trilead.ssh2.Session
import com.trilead.ssh2.LocalPortForwarder
import com.yishell.app.data.model.AuthType
import com.yishell.app.data.model.ConnectionColor
import com.yishell.app.data.model.ConnectionConfig
import com.yishell.app.data.model.PortForwarding
import com.yishell.app.data.model.PortForwardingType
import com.yishell.app.data.security.CryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SshManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager
) {
    companion object {
        private const val TAG = "SshManager"
        private const val CONNECT_TIMEOUT_MS = 5_000
        private const val MAX_CONNECTIONS = 10
        // P2-2：keepalive 间隔。多数服务器/防火墙在 5-10 分钟空闲后踢连接，30s 足够保活且开销低。
        private const val KEEPALIVE_INTERVAL_MS = 30_000L
    }

    /**
     * SshManager 自管理的协程作用域，用于 keepalive 心跳等长周期任务。
     * 与 ViewModel 生命周期解耦——连接保活需要在 ViewModel 销毁后仍运行（连接由 SSOT 管理）。
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * P2-2：每个连接的 keepalive 心跳 job。发送 NUL 字节维持连接活跃，
     * 防止服务器/防火墙因空闲超时踢掉连接。
     */
    private val keepAliveJobs = mutableMapOf<String, Job>()

    private val lock = Any()

    /**
     * 已激活连接的轻量信息（P1-6 SSOT）。
     * 供 UI 层（HomeViewModel）订阅以派生"已连接列表"，无需查 Repository。
     */
    data class ActiveConnectionInfo(
        val connectionId: String,
        val name: String,
        val username: String,
        val host: String,
        val color: ConnectionColor,
        val connectedAt: Long
    )

    private data class ManagedConnection(
        val connection: Connection,
        val connectionId: String,
        @Volatile var alive: Boolean = true,
        var session: Session? = null,
        var stdin: OutputStream? = null,
        var stdout: InputStream? = null,
        var bufferedStdout: BufferedInputStream? = null,
        val activeForwarders: MutableMap<String, LocalPortForwarder> = mutableMapOf(),
        val remoteForwardPorts: MutableMap<String, Int> = mutableMapOf()
    )

    private val managedConnections = mutableMapOf<String, ManagedConnection>()

    /**
     * SSOT：当前活跃的连接（P1-6）。
     * connect 成功时加入，disconnect/connectionLost 时移除。
     * HomeViewModel 订阅它派生 connectedSessions，消除三处状态漂移
     *（SshManager / TerminalViewModel / HomeViewModel 各自维护连接状态导致漂移）。
     */
    private val _activeConnections = MutableStateFlow<Map<String, ActiveConnectionInfo>>(emptyMap())
    val activeConnections: StateFlow<Map<String, ActiveConnectionInfo>> = _activeConnections.asStateFlow()

    suspend fun connect(
        config: ConnectionConfig,
        acceptedFingerprint: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val connectionId = config.id

        // P1-3 锁粒度收窄：临界区 1 仅做 map 操作（检查上限 + 取出并断开已有连接），
        // 网络连接 / 认证 / openSession 等慢 IO 全部移出锁外，避免 5s 超时期间阻塞其他连接操作。
        synchronized(lock) {
            if (managedConnections.size >= MAX_CONNECTIONS && !managedConnections.containsKey(connectionId)) {
                throw IllegalStateException("已达上限")
            }
            managedConnections.remove(connectionId)?.let { existing ->
                disconnectInternal(connectionId, existing)
                _activeConnections.value = _activeConnections.value - connectionId
            }
        }

        Log.d(TAG, "Connecting to ${config.host}:${config.port}...")

        // 锁外：网络连接 + 主机验证 + 认证 + openSession（全部慢 IO）
        val conn = Connection(config.host, config.port)
        // openedSession 用于异常路径清理：成功移交后置 null
        var openedSession: Session? = null
        try {
            conn.addConnectionMonitor(object : ConnectionMonitor {
                override fun connectionLost(throwable: Throwable?) {
                    Log.w(TAG, "Connection lost for $connectionId: ${throwable?.message}")
                    // P1-6：连接丢失时彻底清理资源并更新 SSOT，让 UI 即时感知。
                    // 不再只置 alive=false 而遗留资源。
                    disconnect(connectionId)
                }
            })

            // 安全修复（P0-1）：传入用户已确认的指纹（首次连接时为 null）。
            // 未知主机或指纹不匹配时，verifier 会抛出 HostKeyVerificationException。
            val serverHostKeyVerifier = ServerHostKeyVerifier(context, acceptedFingerprint)
            try {
                conn.connect(serverHostKeyVerifier, CONNECT_TIMEOUT_MS, 0)
            } catch (e: java.io.IOException) {
                // trilead 可能把 HostKeyVerificationException 包在 IOException 里
                val cause = e.cause
                if (cause is HostKeyVerificationException) {
                    throw cause
                }
                throw e
            }

            val authenticated = when (config.authType) {
                AuthType.PASSWORD -> {
                    // 兼容旧明文：先尝试解密，若失败（如 Base64 长度不足）则回退到原始值。
                    // 这在 P0-3 密码加密修复后的过渡期是必需的——旧连接可能仍存明文。
                    val decryptedPassword = cryptoManager.decrypt(config.password ?: "")
                        ?: config.password
                    if (decryptedPassword.isNullOrEmpty()) {
                        Log.w(TAG, "Password is null or empty for ${config.username}@${config.host}")
                        throw java.io.IOException("密码为空")
                    }
                    try {
                        conn.authenticateWithPassword(config.username, decryptedPassword)
                    } finally {
                        decryptedPassword.toCharArray().fill('\u0000')
                    }
                }
                AuthType.KEY, AuthType.KEY_WITH_PASSPHRASE -> {
                    val keyFile = getKeyFile(config.privateKeyPath)
                    if (keyFile != null) {
                        val decryptedPassphrase = cryptoManager.decrypt(config.passphrase ?: "")
                        try {
                            conn.authenticateWithPublicKey(
                                config.username,
                                keyFile,
                                decryptedPassphrase
                            )
                        } finally {
                            decryptedPassphrase?.toCharArray()?.fill('\u0000')
                        }
                    } else {
                        false
                    }
                }
            }

            if (!authenticated) {
                Log.w(TAG, "Authentication failed for ${config.username}@${config.host}")
                throw java.io.IOException("SSH 认证失败：用户名或密码错误")
            }

            openedSession = conn.openSession()
            openedSession.requestPTY("xterm-256color", 120, 40, 0, 0, null)
            openedSession.startShell()

            // 取同一个 stdout 引用用于直接读和 BufferedInputStream 包装，避免两次 getStdout()
            val stdoutStream = openedSession.getStdout()
            val managed = ManagedConnection(
                connection = conn,
                connectionId = connectionId,
                alive = true,
                session = openedSession,
                stdin = openedSession.getStdin(),
                stdout = stdoutStream,
                bufferedStdout = BufferedInputStream(stdoutStream, 16384)
            )

            // 临界区 2：放入 map 并更新 SSOT（防御并发同 id 连接）
            synchronized(lock) {
                managedConnections.remove(connectionId)?.let { disconnectInternal(connectionId, it) }
                managedConnections[connectionId] = managed
                _activeConnections.value = _activeConnections.value + (connectionId to ActiveConnectionInfo(
                    connectionId = connectionId,
                    name = config.name,
                    username = config.username,
                    host = config.host,
                    color = config.color,
                    connectedAt = System.currentTimeMillis()
                ))
            }

            Log.d(TAG, "Connected to ${config.host}:${config.port} (id=$connectionId)")
            openedSession = null // 已移交 managed，不再在此处关闭
            // P2-2：启动 keepalive 心跳，防止空闲连接被服务器/防火墙断开。
            startKeepAlive(connectionId)
            true
        } catch (e: HostKeyVerificationException) {
            // 安全修复（P0-1）：未知主机或指纹不匹配，必须向上传播给 UI 层。
            // 注意：HostKeyVerificationException 是 RuntimeException，trilead 不一定会包成 IOException，
            // 因此必须在此处显式捕获，不能仅靠外层 catch(Exception) 吞掉。
            Log.w(TAG, "Host key verification required for ${config.host}:${config.port}: ${e.message}")
            try { openedSession?.close() } catch (_: Exception) {}
            try { conn.close() } catch (_: Exception) {}
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "SSH connect failed: ${e.message}", e)
            try { openedSession?.close() } catch (_: Exception) {}
            try { conn.close() } catch (_: Exception) {}
            throw java.io.IOException("SSH 连接失败: ${e.message ?: e.javaClass.simpleName}", e)
        }
    }

    fun sendInput(connectionId: String, data: String) {
        val managed = synchronized(lock) { managedConnections[connectionId] } ?: return
        try {
            managed.stdin?.write(data.toByteArray())
            managed.stdin?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "sendInput failed: ${e.message}", e)
        }
    }

    fun sendRawBytes(connectionId: String, bytes: ByteArray) {
        val managed = synchronized(lock) { managedConnections[connectionId] } ?: return
        try {
            val os = managed.stdin
            if (os != null && managed.alive) {
                os.write(bytes)
                os.flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendRawBytes failed: ${e.message}", e)
        }
    }

    fun readOutput(connectionId: String): String {
        val managed = synchronized(lock) { managedConnections[connectionId] } ?: return ""
        return try {
            readAvailableOutputInternal(managed)
        } catch (e: Exception) {
            Log.w(TAG, "readOutput failed: ${e.message}")
            ""
        }
    }

    fun isConnected(connectionId: String): Boolean {
        val managed = synchronized(lock) { managedConnections[connectionId] }
        return try {
            managed?.alive == true &&
                managed.session != null &&
                managed.stdin != null &&
                managed.stdout != null
        } catch (e: Exception) {
            Log.w(TAG, "isConnected check failed", e)
            false
        }
    }

    fun getConnection(connectionId: String): Connection? {
        return synchronized(lock) { managedConnections[connectionId]?.connection }
    }

    // --- Port Forwarding ---

    fun startLocalForward(connectionId: String, pf: PortForwarding): Boolean {
        val managed = synchronized(lock) { managedConnections[connectionId] } ?: return false
        try {
            if (!managed.alive) return false
            // 安全修复：仅绑定回环地址，避免同一网络下的其他设备访问转发端口
            val forwarder = managed.connection.createLocalPortForwarder(
                java.net.InetSocketAddress("127.0.0.1", pf.localPort),
                pf.remoteHost,
                pf.remotePort
            )
            synchronized(lock) {
                managed.activeForwarders[pf.id] = forwarder
            }
            Log.d(TAG, "Started local forward: :${pf.localPort} -> ${pf.remoteHost}:${pf.remotePort}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "startLocalForward failed: ${e.message}", e)
            return false
        }
    }

    fun stopLocalForward(connectionId: String, pfId: String) {
        val managed = synchronized(lock) { managedConnections[connectionId] } ?: return
        managed.activeForwarders.remove(pfId)?.let { forwarder ->
            try {
                forwarder.close()
            } catch (e: Exception) {
                Log.w(TAG, "stopLocalForward close failed: ${e.message}")
            }
        }
    }

    fun startRemoteForward(connectionId: String, pf: PortForwarding): Boolean {
        val managed = synchronized(lock) { managedConnections[connectionId] } ?: return false
        try {
            if (!managed.alive) return false
            // 安全修复：远程转发同样仅请求回环地址
            managed.connection.requestRemotePortForwarding(
                "127.0.0.1", pf.localPort,
                pf.remoteHost, pf.remotePort
            )
            synchronized(lock) {
                managed.remoteForwardPorts[pf.id] = pf.localPort
            }
            Log.d(TAG, "Started remote forward: remote:${pf.localPort} -> ${pf.remoteHost}:${pf.remotePort}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "startRemoteForward failed: ${e.message}", e)
            return false
        }
    }

    fun stopRemoteForward(connectionId: String, pfId: String) {
        val managed = synchronized(lock) { managedConnections[connectionId] } ?: return
        managed.remoteForwardPorts.remove(pfId)?.let { port ->
            try {
                managed.connection.cancelRemotePortForwarding(port)
            } catch (e: Exception) {
                Log.w(TAG, "stopRemoteForward failed: ${e.message}")
            }
        }
    }

    fun stopAllForwards(connectionId: String) {
        val managed = synchronized(lock) { managedConnections[connectionId] } ?: return
        managed.activeForwarders.forEach { (id, f) ->
            try { f.close() } catch (_: Exception) {}
        }
        managed.activeForwarders.clear()
        managed.remoteForwardPorts.forEach { (id, port) ->
            try {
                managed.connection.cancelRemotePortForwarding(port)
            } catch (_: Exception) {}
        }
        managed.remoteForwardPorts.clear()
    }

    fun getActiveForwardIds(connectionId: String): Set<String> {
        val managed = synchronized(lock) { managedConnections[connectionId] } ?: return emptySet()
        return managed.activeForwarders.keys + managed.remoteForwardPorts.keys
    }

    fun disconnect(connectionId: String) {
        // P1-6：从 map 移除的同时更新 SSOT，让 HomeViewModel 即时感知断开。
        // P2-2：同时停止 keepalive 心跳。
        stopKeepAlive(connectionId)
        val managed = synchronized(lock) {
            managedConnections.remove(connectionId)?.also {
                _activeConnections.value = _activeConnections.value - connectionId
            }
        } ?: return
        disconnectInternal(connectionId, managed)
    }

    /**
     * P2-2：启动 keepalive 心跳。
     *
     * 实现说明（Feynman 破妄——不假装用 SSH 协议级 IGNORE 包）：
     * trilead-ssh2 未公开 sendIgnore API，这里通过 stdin 周期发送单个 NUL 字节(0x00)。
     * - POSIX 终端在 canonical 模式下会丢弃 NUL，不影响交互式 shell。
     * - 全屏应用（vim/tmux）在 raw 模式下可能把 NUL 解释为输入，但通常视为无操作（vim 显示 ^@）。
     * - 30s 间隔下影响可忽略，远比"连接被踢后重连"代价低。
     * 若未来 trilead 暴露协议级 keepalive，应改用之。
     */
    private fun startKeepAlive(connectionId: String) {
        stopKeepAlive(connectionId)
        keepAliveJobs[connectionId] = scope.launch {
            while (isActive) {
                delay(KEEPALIVE_INTERVAL_MS)
                val managed = synchronized(lock) { managedConnections[connectionId] }
                if (managed == null || !managed.alive) break
                try {
                    val os = managed.stdin
                    if (os != null) {
                        os.write(0)
                        os.flush()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Keepalive write failed for $connectionId: ${e.message}")
                    // 写失败通常意味着连接已断，connectionLost 回调会处理清理，这里退出循环。
                    break
                }
            }
        }
    }

    private fun stopKeepAlive(connectionId: String) {
        keepAliveJobs.remove(connectionId)?.cancel()
    }

    private fun disconnectInternal(connectionId: String, managed: ManagedConnection) {
        stopAllForwards(connectionId)

        try { managed.stdin?.close() } catch (_: Exception) {}
        try { managed.bufferedStdout?.close() } catch (_: Exception) {}
        try { managed.stdout?.close() } catch (_: Exception) {}
        try { managed.session?.close() } catch (_: Exception) {}
        try { managed.connection.close() } catch (_: Exception) {}

        managed.stdin = null
        managed.stdout = null
        managed.bufferedStdout = null
        managed.session = null
        managed.alive = false
    }

    private fun readAvailableOutputInternal(managed: ManagedConnection): String {
        val input = managed.bufferedStdout ?: return ""
        val buffer = ByteArray(8192)
        val result = StringBuilder()

        try {
            val available = input.available()
            if (available > 0) {
                val read = input.read(buffer, 0, minOf(available, buffer.size))
                if (read > 0) {
                    result.append(String(buffer, 0, read))
                }
            } else {
                if (input.markSupported()) {
                    input.reset()
                }
                val read = input.read(buffer, 0, 1)
                if (read > 0) {
                    result.append(String(buffer, 0, read))
                    while (input.available() > 0) {
                        val moreRead = input.read(buffer, 0, minOf(input.available(), buffer.size))
                        if (moreRead > 0) {
                            result.append(String(buffer, 0, moreRead))
                        } else break
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "readAvailableOutputInternal error: ${e.message}")
        }

        return result.toString()
    }

    private fun getKeyFile(keyPath: String?): File? {
        if (keyPath == null) return null
        return try {
            val file = File(keyPath)
            if (file.exists()) file else null
        } catch (_: Exception) {
            null
        }
    }
}
