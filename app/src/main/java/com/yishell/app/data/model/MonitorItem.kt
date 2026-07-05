package com.yishell.app.data.model

data class ProcessInfo(
    val pid: String,
    val user: String,
    val cpuPercent: Float,
    val memPercent: Float,
    val command: String
)

data class MonitorItem(
    val cpuUsage: Float = 0f,
    val cpuCoreCount: Int = 1,
    val memoryUsed: Long = 0,
    val memoryTotal: Long = 0,
    val memoryUsage: Float = 0f,
    val diskUsed: Long = 0,
    val diskTotal: Long = 0,
    val diskUsage: Float = 0f,
    val uptime: String = "",
    val loadAvg1: Float = 0f,
    val loadAvg5: Float = 0f,
    val loadAvg15: Float = 0f,
    val hostname: String = "",
    val username: String = "",
    val processes: List<ProcessInfo> = emptyList(),
    val netUploadSpeed: Long = 0L,   // bytes/sec
    val netDownloadSpeed: Long = 0L  // bytes/sec
)
