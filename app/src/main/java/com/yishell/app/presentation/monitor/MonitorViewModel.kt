package com.yishell.app.presentation.monitor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trilead.ssh2.Connection
import com.yishell.app.data.model.MonitorItem
import com.yishell.app.data.model.ProcessInfo
import com.yishell.app.data.remote.SshManager
import com.yishell.app.domain.repository.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class MonitorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sshManager: SshManager,
    private val connectionRepository: ConnectionRepository
) : ViewModel() {

    private val TAG = "MonitorViewModel"

    private val connectionId: String = savedStateHandle["connectionId"] ?: ""

    private val _monitorData = MutableStateFlow(MonitorItem())
    val monitorData: StateFlow<MonitorItem> = _monitorData.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _lastUpdated = MutableStateFlow("")
    val lastUpdated: StateFlow<String> = _lastUpdated.asStateFlow()

    private val _connectionInfo = MutableStateFlow("")
    val connectionInfo: StateFlow<String> = _connectionInfo.asStateFlow()

    private var pollJob: Job? = null

    // CPU/网络 速率需要前后两次采样做差值，缓存上一次的原始值
    private var lastCpuStat: LongArray? = null       // [user, nice, system, idle, iowait, irq, softirq, steal] 累计 jiffies
    private var lastCpuStatTimeMs: Long = 0L
    private var lastNetRxBytes: Long = -1L
    private var lastNetTxBytes: Long = -1L
    private var lastNetSampleTimeMs: Long = 0L
    private var cpuCoreCount: Int = 1                // /proc/stat 的行数 - 1，或 nproc

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        _error.value = "Coroutine error: ${throwable.message}"
    }

    init {
        if (connectionId.isNotEmpty()) {
            loadConnectionInfo()
            startPolling()
        }
    }

    private fun loadConnectionInfo() {
        viewModelScope.launch {
            val config = withContext(Dispatchers.IO) {
                connectionRepository.getConnectionById(connectionId)
            }
            if (config != null) {
                _connectionInfo.value = "${config.username}@${config.host}"
            }
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            fetchMonitorData()
            _isLoading.value = false
            while (isActive) {
                try {
                    fetchMonitorData()
                } catch (e: Exception) {
                    _error.value = "监控数据获取异常: ${e.message}"
                    Log.w(TAG, "fetchMonitorData polling error: ${e.message}", e)
                }
                delay(3000)
            }
        }
    }

    /**
     * 监控数据采集 —— 每个命令独立 SSH session 并发执行。
     *
     * 关键修复点：
     * 1. CPU：读 /proc/stat 两次（间隔 200ms）算差值 —— 真正瞬时值，不依赖 top 实现
     * 2. 进程：用 top -bn1 的进程段（瞬时 %CPU），不用 ps aux（累计平均，多核会 >100% 误导用户）
     * 3. 网络：读 /proc/net/dev 两次算差值 —— 真实上下行速率
     */
    private suspend fun fetchMonitorData() {
        val connection = withContext(Dispatchers.IO) {
            sshManager.getConnection(connectionId)
        } ?: run {
            _error.value = "SSH 连接不可用"
            return
        }

        val results = withTimeoutOrNull(10_000L) {
            withContext(Dispatchers.IO) {
                listOf(
                    async { "cpuStat" to executeCpuStatSample(connection) },     // 两次采样
                    async { "mem" to executeCommand(connection, "cat /proc/meminfo 2>/dev/null | head -5") },
                    async { "disk" to executeCommand(connection, "df -B1 / 2>/dev/null | tail -1 || df / 2>/dev/null | tail -1") },
                    async { "uptime" to executeCommand(connection, "uptime -p 2>/dev/null || uptime 2>/dev/null || cat /proc/uptime") },
                    async { "hostname" to executeCommand(connection, "hostname 2>/dev/null || cat /etc/hostname") },
                    async { "top" to executeCommand(connection, "top -bn1 2>/dev/null | head -20 || top -n1 2>/dev/null | head -20 || top -b 2>/dev/null | head -20") },
                    async { "net" to executeNetSample(connection) }                // 两次采样
                ).awaitAll().toMap()
            }
        } ?: run {
            _error.value = "监控命令超时 (10s)，服务器响应过慢"
            return
        }

        val cpuStatOutput = results["cpuStat"] ?: ""
        val memOutput = results["mem"] ?: ""
        val diskOutput = results["disk"] ?: ""
        val uptimeOutput = results["uptime"] ?: ""
        val hostnameOutput = results["hostname"] ?: ""
        val topOutput = results["top"] ?: ""
        val netOutput = results["net"] ?: ""

        val cpuUsage = parseCpuFromStat(cpuStatOutput)
        val loadAvg = parseLoadAvg(topOutput)
        val memoryTotal = parseMemoryTotal(memOutput)
        val memoryUsed = parseMemoryUsed(memOutput, memoryTotal)
        val memoryUsage = if (memoryTotal > 0) (memoryUsed.toFloat() / memoryTotal * 100) else 0f
        val diskTotal = parseDiskTotal(diskOutput)
        val diskUsed = parseDiskUsed(diskOutput)
        val diskUsage = if (diskTotal > 0) (diskUsed.toFloat() / diskTotal * 100) else 0f
        val uptime = parseUptime(uptimeOutput)
        val hostname = hostnameOutput.lines().firstOrNull()?.trim() ?: ""

        val processes = parseProcessListFromTop(topOutput)
        val (netUp, netDown) = parseNetSpeed(netOutput)

        _monitorData.value = MonitorItem(
            cpuUsage = cpuUsage,
            cpuCoreCount = cpuCoreCount,
            memoryUsed = memoryUsed,
            memoryTotal = memoryTotal,
            memoryUsage = memoryUsage,
            diskUsed = diskUsed,
            diskTotal = diskTotal,
            diskUsage = diskUsage,
            uptime = uptime,
            loadAvg1 = loadAvg[0],
            loadAvg5 = loadAvg[1],
            loadAvg15 = loadAvg[2],
            hostname = hostname,
            username = _connectionInfo.value,
            processes = processes,
            netUploadSpeed = netUp,
            netDownloadSpeed = netDown
        )

        val now = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        _lastUpdated.value = "Last updated: $now"
        _error.value = null
    }

    /**
     * CPU 采样：连读两次 /proc/stat，中间 sleep 200ms，返回两次原文以便算差值。
     * 用 marker 包围，避免和别的输出混在一起。
     */
    private fun executeCpuStatSample(connection: Connection): String {
        val session = connection.openSession()
        return try {
            // awk 取 cpu 总行的 8 个累计 jiffies，第一次输出 + sleep 200ms + 第二次输出
            // 用唯一 marker 分隔
            val cmd = "awk '/^cpu /{print \$2,\$3,\$4,\$5,\$6,\$7,\$8,\$9; exit}' /proc/stat; echo '---SLEEP---'; sleep 0.2; awk '/^cpu /{print \$2,\$3,\$4,\$5,\$6,\$7,\$8,\$9; exit}' /proc/stat; echo '---CORES---'; grep -c '^cpu[0-9]' /proc/stat"
            session.execCommand(cmd)
            val stdout = session.getStdout()
            val buffer = ByteArray(8192)
            val result = StringBuilder()
            val endTime = System.currentTimeMillis() + 3000
            while (System.currentTimeMillis() < endTime) {
                val n = stdout.read(buffer)
                if (n > 0) {
                    result.append(String(buffer, 0, n))
                } else if (n == -1) {
                    break
                } else {
                    try { Thread.sleep(20) } catch (_: InterruptedException) { break }
                }
            }
            result.toString().trim()
        } finally {
            try { session.close() } catch (_: Exception) {}
        }
    }

    /**
     * 网络采样：读 /proc/net/dev 两次，中间 sleep 200ms，返回两次的聚合字节数。
     */
    private fun executeNetSample(connection: Connection): String {
        val session = connection.openSession()
        return try {
            // 聚合所有非 lo 接口的 RX bytes（第2字段）和 TX bytes（第10字段）
            // 输出格式："rx1 tx1 ---SLEEP--- rx2 tx2"
            val cmd = "awk 'NR>2 && \$1!~/lo/{r+=\$2; t+=\$10} END{print r, t}' /proc/net/dev; echo '---SLEEP---'; sleep 0.2; awk 'NR>2 && \$1!~/lo/{r+=\$2; t+=\$10} END{print r, t}' /proc/net/dev"
            session.execCommand(cmd)
            val stdout = session.getStdout()
            val buffer = ByteArray(8192)
            val result = StringBuilder()
            val endTime = System.currentTimeMillis() + 3000
            while (System.currentTimeMillis() < endTime) {
                val n = stdout.read(buffer)
                if (n > 0) {
                    result.append(String(buffer, 0, n))
                } else if (n == -1) {
                    break
                } else {
                    try { Thread.sleep(20) } catch (_: InterruptedException) { break }
                }
            }
            result.toString().trim()
        } finally {
            try { session.close() } catch (_: Exception) {}
        }
    }

    /**
     * 从 /proc/stat 两次采样算 CPU 瞬时占用率。
     * /proc/stat cpu 行：user nice system idle iowait irq softirq steal（单位 jiffies，通常 10ms）
     * CPU 总时间 = 所有字段之和；空闲 = idle + iowait；占用率 = 1 - idle/total
     */
    private fun parseCpuFromStat(output: String): Float {
        val parts = output.split("---SLEEP---")
        if (parts.size < 2) return 0f

        val coresPart = if (parts.size >= 3) parts[2] else ""
        coresPart.trim().toIntOrNull()?.let { cpuCoreCount = it.coerceAtLeast(1) }

        val sample1 = parts[0].trim().split(Regex("\\s+")).mapNotNull { it.toLongOrNull() }
        val sample2 = parts[1].trim().split(Regex("\\s+")).mapNotNull { it.toLongOrNull() }
        if (sample1.size < 4 || sample2.size < 4) return 0f

        val total1 = sample1.take(8).sum()
        val idle1 = sample1.getOrElse(3) { 0L } + sample1.getOrElse(4) { 0L }
        val total2 = sample2.take(8).sum()
        val idle2 = sample2.getOrElse(3) { 0L } + sample2.getOrElse(4) { 0L }

        val totalDelta = total2 - total1
        val idleDelta = idle2 - idle1
        if (totalDelta <= 0) return 0f
        return ((totalDelta - idleDelta).toFloat() / totalDelta * 100f).coerceIn(0f, 100f)
    }

    private fun executeCommand(connection: Connection, command: String): String {
        val session = connection.openSession()
        return try {
            session.execCommand(command)
            val stdout = session.getStdout()
            val buffer = ByteArray(8192)
            val result = StringBuilder()
            val endTime = System.currentTimeMillis() + 5000
            while (System.currentTimeMillis() < endTime) {
                val n = stdout.read(buffer)
                if (n > 0) {
                    result.append(String(buffer, 0, n))
                } else if (n == -1) {
                    break
                } else {
                    try { Thread.sleep(20) } catch (_: InterruptedException) { break }
                }
            }
            result.toString().trim()
        } finally {
            try { session.close() } catch (_: Exception) {}
        }
    }

    private fun parseLoadAvg(topOutput: String): FloatArray {
        val result = floatArrayOf(0f, 0f, 0f)
        val lines = topOutput.lines()
        for (line in lines) {
            if (line.contains("load average:")) {
                val afterLoad = line.substringAfter("load average:")
                    .trim()
                val parts = afterLoad.split(",").map { it.trim() }
                if (parts.size >= 3) {
                    result[0] = parts[0].toFloatOrNull() ?: 0f
                    result[1] = parts[1].toFloatOrNull() ?: 0f
                    result[2] = parts[2].toFloatOrNull() ?: 0f
                }
                break
            }
        }
        return result
    }

    private fun parseMemoryUsed(memOutput: String, memoryTotal: Long): Long {
        if (memoryTotal <= 0) return 0L
        val memAvailMatch = Regex("MemAvailable:\\s+(\\d+)").find(memOutput)
        if (memAvailMatch != null) {
            val availableKb = memAvailMatch.groupValues[1].toLongOrNull() ?: 0L
            return ((memoryTotal - availableKb * 1024L).coerceAtLeast(0L))
        }
        val buffersMatch = Regex("Buffers:\\s+(\\d+)").find(memOutput)
        val cachedMatch = Regex("Cached:\\s+(\\d+)").find(memOutput)
        val buffersKb = buffersMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val cachedKb = cachedMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val memFreeMatch = Regex("MemFree:\\s+(\\d+)").find(memOutput)
        val memFreeKb = memFreeMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        return ((memoryTotal - (memFreeKb + buffersKb + cachedKb) * 1024L).coerceAtLeast(0L))
    }

    private fun parseMemoryTotal(memOutput: String): Long {
        val match = Regex("MemTotal:\\s+(\\d+)").find(memOutput)
        if (match != null) {
            val kb = match.groupValues[1].toLongOrNull() ?: 0L
            return kb * 1024L
        }
        return 0L
    }

    private fun parseDiskUsed(diskOutput: String): Long {
        val trimmed = diskOutput.trim()
        val parts = trimmed.split(Regex("\\s+"))
        if (parts.size >= 3) {
            val blocks = parts[1].toLongOrNull() ?: 0L
            val used = parts[2].toLongOrNull() ?: 0L
            return if (blocks > 1_000_000_000L) used else used * 1024
        }
        return 0L
    }

    private fun parseDiskTotal(diskOutput: String): Long {
        val trimmed = diskOutput.trim()
        val parts = trimmed.split(Regex("\\s+"))
        if (parts.size >= 2) {
            val blocks = parts[1].toLongOrNull() ?: 0L
            return if (blocks > 1_000_000_000L) blocks else blocks * 1024
        }
        return 0L
    }

    private fun parseUptime(uptimeOutput: String): String {
        val trimmed = uptimeOutput.trim()
        val firstToken = trimmed.split(Regex("\\s+")).firstOrNull() ?: return trimmed
        firstToken.toFloatOrNull()?.let { seconds ->
            if (seconds > 0) {
                val totalSec = seconds.toLong()
                val days = totalSec / 86400
                val hours = (totalSec % 86400) / 3600
                val mins = (totalSec % 3600) / 60
                return buildString {
                    if (days > 0) append("${days}天 ")
                    if (hours > 0 || days > 0) append("${hours}小时 ")
                    append("${mins}分钟")
                }.trim()
            }
        }
        return trimmed.removePrefix("up ").trim()
    }

    /**
     * 从 top -bn1 输出解析进程列表（瞬时 %CPU）。
     * top 输出格式（procps）：
     *   PID USER PR NI VIRT RES SHR S %CPU %MEM TIME+ COMMAND
     * top 输出格式（busybox）：
     *   PID USER PPID VSZ RSS %CPU %MEM STAT COMMAND  —— 列序不同！
     *
     * 通过表头定位列索引，兼容两种格式。
     */
    private fun parseProcessListFromTop(topOutput: String): List<ProcessInfo> {
        val lines = topOutput.lines()
        if (lines.isEmpty()) return emptyList()

        // 找表头行（含 PID 和 %CPU）
        val headerIdx = lines.indexOfFirst { it.contains("PID") && (it.contains("%CPU") || it.contains("CPU")) }
        if (headerIdx < 0 || headerIdx + 1 >= lines.size) return emptyList()

        val header = lines[headerIdx].split(Regex("\\s+"))
        val pidCol = header.indexOf("PID")
        val userCol = header.indexOf("USER")
        // %CPU 在 procps 是 "%CPU"，busybox 可能是 "CPU%"
        val cpuCol = header.indexOfFirst { it.equals("%CPU", true) || it.equals("CPU%", true) || it.equals("CPU", true) }
        val memCol = header.indexOfFirst { it.equals("%MEM", true) || it.equals("MEM%", true) || it.equals("MEM", true) }
        val cmdCol = header.indexOfFirst { it.equals("COMMAND", true) || it.equals("CMD", true) }

        if (pidCol < 0 || cpuCol < 0) return emptyList()

        val result = mutableListOf<ProcessInfo>()
        // 跳过表头，最多取 15 个进程
        for (i in (headerIdx + 1) until minOf(lines.size, headerIdx + 16)) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            val parts = line.split(Regex("\\s+"))
            if (parts.size <= maxOf(pidCol, cpuCol)) continue

            val pid = parts.getOrNull(pidCol) ?: continue
            val user = if (userCol >= 0) parts.getOrNull(userCol) ?: "?" else "?"
            val cpuStr = parts.getOrNull(cpuCol) ?: "0"
            val cpuPercent = cpuStr.toFloatOrNull() ?: 0f
            val memStr = if (memCol >= 0) parts.getOrNull(memCol) ?: "0" else "0"
            val memPercent = memStr.toFloatOrNull() ?: 0f
            val command = if (cmdCol >= 0 && parts.size > cmdCol) {
                parts.drop(cmdCol).joinToString(" ")
            } else {
                parts.lastOrNull() ?: ""
            }

            result.add(ProcessInfo(user = user, pid = pid, cpuPercent = cpuPercent, memPercent = memPercent, command = command))
        }
        return result
    }

    /**
     * 从两次 /proc/net/dev 采样算上下行速率（字节/秒）。
     */
    private fun parseNetSpeed(output: String): Pair<Long, Long> {
        val parts = output.split("---SLEEP---")
        if (parts.size < 2) return Pair(0L, 0L)

        val s1 = parts[0].trim().split(Regex("\\s+")).mapNotNull { it.toLongOrNull() }
        val s2 = parts[1].trim().split(Regex("\\s+")).mapNotNull { it.toLongOrNull() }
        if (s1.size < 2 || s2.size < 2) return Pair(0L, 0L)

        val rx1 = s1[0]; val tx1 = s1[1]
        val rx2 = s2[0]; val tx2 = s2[1]

        // 采样间隔约 0.2s，转成字节/秒
        val rxDelta = (rx2 - rx1).coerceAtLeast(0L)
        val txDelta = (tx2 - tx1).coerceAtLeast(0L)
        // /5 因为 0.2s 间隔 → 乘 5 = 每秒
        return Pair(txDelta * 5L, rxDelta * 5L)
    }

    fun refresh() {
        viewModelScope.launch(exceptionHandler) {
            _isLoading.value = true
            _error.value = null
            try {
                fetchMonitorData()
            } catch (e: Exception) {
                _error.value = "刷新失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun killProcess(pid: String, force: Boolean = false) {
        viewModelScope.launch(exceptionHandler) {
            withContext(Dispatchers.IO) {
                try {
                    val connection = sshManager.getConnection(connectionId)
                    if (connection != null) {
                        val session = connection.openSession()
                        try {
                            val signal = if (force) "kill -9" else "kill"
                            session.execCommand("$signal $pid")
                            Thread.sleep(300)
                        } finally {
                            try { session.close() } catch (_: Exception) {}
                        }
                    }
                } catch (e: Exception) {
                    _error.value = "Kill failed: ${e.message}"
                }
            }
            fetchMonitorData()
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}
