package com.yishell.app.data.local

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicLong

/**
 * 终端日志管理器
 * 
 * 功能：
 * 1. 自动记录终端会话的原始输出
 * 2. 按会话分文件存储
 * 3. 批量写入（每 5 秒或每 4KB）
 * 4. 自动清理（保留最近 7 天，总大小不超过 100MB）
 * 5. 支持查看日志列表和导出
 */
class TerminalLogger(private val context: Context) {

    companion object {
        private const val TAG = "TerminalLogger"
        private const val LOG_DIR = "logs/terminal"
        private const val MAX_LOG_AGE_DAYS = 7L
        private const val MAX_TOTAL_SIZE_MB = 100L
        private const val MAX_TOTAL_SIZE_BYTES = MAX_TOTAL_SIZE_MB * 1024 * 1024
        private const val FLUSH_INTERVAL_MS = 5000L  // 5 秒
        private const val BUFFER_SIZE = 4096         // 4KB
        
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        private val LOG_NAME_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }

    private val logDir: File by lazy {
        File(context.filesDir, LOG_DIR).apply {
            if (!exists()) mkdirs()
        }
    }

    // 当前会话的日志文件
    private var currentLogFile: File? = null
    private var currentOutputStream: FileOutputStream? = null
    private val buffer = StringBuilder()
    private val bufferSize = AtomicLong(0)
    
    // 用于批量写入的协程
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeChannel = Channel<String>(Channel.UNLIMITED)
    private var flushJob: Job? = null

    init {
        startFlushJob()
        cleanOldLogs()
    }

    /**
     * 开始新会话的日志记录
     */
    fun startSession(sessionId: String) {
        try {
            // 关闭之前的会话
            endSession()
            
            // 创建新日志文件
            val timestamp = DATE_FORMAT.format(Date())
            val fileName = "${timestamp}_session_${sessionId.take(8)}.log"
            currentLogFile = File(logDir, fileName)
            currentOutputStream = FileOutputStream(currentLogFile, true)
            
            // 写入会话开始标记（使用 UTF-8 编码）
            val startMarker = "\n=== Session Started: ${Date()} ===\n"
            currentOutputStream?.write(startMarker.toByteArray(Charsets.UTF_8))
            currentOutputStream?.flush()
            
            Log.d(TAG, "Started logging to: ${currentLogFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start session logging", e)
        }
    }

    /**
     * 记录终端输出
     */
    fun log(data: String) {
        if (data.isEmpty()) return
        
        // 发送到通道进行批量写入
        writeChannel.trySend(data)
    }

    /**
     * 结束当前会话的日志记录
     */
    fun endSession() {
        try {
            // 刷新缓冲区
            flushBuffer()
            
            // 写入会话结束标记
            currentOutputStream?.write("\n=== Session Ended: ${Date()} ===\n".toByteArray(Charsets.UTF_8))
            currentOutputStream?.flush()
            currentOutputStream?.close()
            
            currentOutputStream = null
            currentLogFile = null
            buffer.clear()
            bufferSize.set(0)
            
            Log.d(TAG, "Ended session logging")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to end session logging", e)
        }
    }

    /**
     * 获取所有日志文件列表
     */
    fun getLogFiles(): List<LogFileInfo> {
        return logDir.listFiles { file ->
            file.isFile && file.name.endsWith(".log")
        }?.map { file ->
            LogFileInfo(
                file = file,
                name = file.name,
                size = file.length(),
                createdAt = Date(file.lastModified())
            )
        }?.sortedByDescending { it.createdAt } ?: emptyList()
    }

    /**
     * 读取日志文件内容
     */
    fun readLogFile(fileName: String): String? {
        return try {
            val file = File(logDir, fileName)
            if (file.exists() && file.isFile) {
                // 使用 UTF-8 编码读取，替换无法解码的字符
                file.readText(Charsets.UTF_8)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read log file: $fileName", e)
            null
        }
    }

    /**
     * 删除指定日志文件
     */
    fun deleteLogFile(fileName: String): Boolean {
        return try {
            val file = File(logDir, fileName)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete log file: $fileName", e)
            false
        }
    }

    /**
     * 清理所有日志
     */
    fun clearAllLogs() {
        try {
            logDir.listFiles()?.forEach { it.delete() }
            Log.d(TAG, "Cleared all logs")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear logs", e)
        }
    }

    /**
     * 获取日志文件用于分享
     */
    fun getLogFileForShare(fileName: String): File? {
        val file = File(logDir, fileName)
        return if (file.exists() && file.isFile) file else null
    }

    /**
     * 启动批量写入任务
     */
    private fun startFlushJob() {
        flushJob = scope.launch {
            writeChannel.consumeEach { data ->
                buffer.append(data)
                bufferSize.addAndGet(data.length.toLong())
                
                // 缓冲区满时立即刷新
                if (bufferSize.get() >= BUFFER_SIZE) {
                    flushBuffer()
                }
            }
        }
        
        // 定时刷新任务
        scope.launch {
            while (isActive) {
                delay(FLUSH_INTERVAL_MS)
                flushBuffer()
            }
        }
    }

    /**
     * 刷新缓冲区到文件
     */
    private fun flushBuffer() {
        if (buffer.isEmpty()) return
        
        try {
            val data = buffer.toString()
            buffer.clear()
            bufferSize.set(0)
            
            // 使用 UTF-8 编码写入字节
            currentOutputStream?.write(data.toByteArray(Charsets.UTF_8))
            currentOutputStream?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to flush buffer", e)
        }
    }

    /**
     * 清理旧日志文件
     */
    private fun cleanOldLogs() {
        scope.launch {
            try {
                val files = logDir.listFiles { file ->
                    file.isFile && file.name.endsWith(".log")
                } ?: return@launch

                // 按时间清理（保留最近 7 天）
                val cutoffTime = System.currentTimeMillis() - (MAX_LOG_AGE_DAYS * 24 * 60 * 60 * 1000)
                files.filter { it.lastModified() < cutoffTime }.forEach { file ->
                    file.delete()
                    Log.d(TAG, "Deleted old log: ${file.name}")
                }

                // 按大小清理（保留最新的，直到总大小 < 100MB）
                val remainingFiles = logDir.listFiles { file ->
                    file.isFile && file.name.endsWith(".log")
                }?.sortedBy { it.lastModified() } ?: return@launch

                var totalSize = remainingFiles.sumOf { it.length() }
                
                while (totalSize > MAX_TOTAL_SIZE_BYTES && remainingFiles.isNotEmpty()) {
                    val oldestFile = remainingFiles.first()
                    totalSize -= oldestFile.length()
                    oldestFile.delete()
                    Log.d(TAG, "Deleted log to save space: ${oldestFile.name}")
                }

                Log.d(TAG, "Log cleanup completed. Total size: ${totalSize / 1024 / 1024}MB")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clean old logs", e)
            }
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        endSession()
        flushJob?.cancel()
        scope.cancel()
    }

    /**
     * 日志文件信息
     */
    data class LogFileInfo(
        val file: File,
        val name: String,
        val size: Long,
        val createdAt: Date
    ) {
        fun getFormattedSize(): String {
            return when {
                size < 1024 -> "$size B"
                size < 1024 * 1024 -> "${size / 1024} KB"
                else -> "${size / 1024 / 1024} MB"
            }
        }

        fun getFormattedDate(): String {
            return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(createdAt)
        }
    }
}