package com.yishell.app.presentation.util

object FormatUtils {
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            .coerceIn(0, units.size - 1)
        return String.format(
            "%.1f %s",
            bytes / Math.pow(1024.0, digitGroups.toDouble()),
            units[digitGroups]
        )
    }

    /**
     * 格式化网络速率（字节/秒 → KB/s 或 MB/s）。
     * 网络 IO 习惯用 KB/s、MB/s 而非 KiB/s，所以用 1000 进制。
     */
    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return "0 KB/s"
        val kbps = bytesPerSec / 1000.0
        if (kbps < 1000.0) {
            return String.format("%.1f KB/s", kbps)
        }
        return String.format("%.1f MB/s", kbps / 1000.0)
    }
}
