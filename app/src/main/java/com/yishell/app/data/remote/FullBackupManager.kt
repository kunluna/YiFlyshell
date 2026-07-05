package com.yishell.app.data.remote

import android.content.Context
import android.os.Environment
import com.yishell.app.data.model.AuthType
import com.yishell.app.data.model.ConnectionColor
import com.yishell.app.data.model.ConnectionConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Full backup/restore manager for YiShell.
 * Backs up all connection configurations (including encrypted passwords/keys) plus settings metadata
 * to external storage as a single JSON bundle.
 */
@Singleton
class FullBackupManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val BACKUP_DIR = "YiShell/Backups"
        private const val BACKUP_FILE_PREFIX = "yishell_full_backup_"
        private const val MAX_FILE_SIZE = 10L * 1024 * 1024
        private const val MAX_NAME_LENGTH = 128
        private const val MAX_HOST_LENGTH = 255
        private const val MAX_USERNAME_LENGTH = 128
    }

    data class BackupResult(
        val success: Boolean,
        val message: String,
        val file: File? = null
    )

    data class RestoreResult(
        val success: Boolean,
        val message: String,
        val connections: List<ConnectionConfig> = emptyList()
    )

    /**
     * Create a full backup of all connections to external storage.
     */
    fun createFullBackup(connections: List<ConnectionConfig>): BackupResult {
        return try {
            val root = JSONObject()
            root.put("app", "YiShell")
            root.put("backupType", "full")
            root.put("version", "1.2.2")
            root.put("timestamp", System.currentTimeMillis())
            root.put("connectionCount", connections.size)

            val arr = JSONArray()
            connections.forEach { config ->
                val obj = JSONObject().apply {
                    put("id", config.id)
                    put("name", config.name)
                    put("host", config.host)
                    put("port", config.port)
                    put("username", config.username)
                    put("authType", config.authType.name)
                    put("password", "")
                    put("privateKeyPath", config.privateKeyPath ?: "")
                    put("passphrase", config.passphrase ?: "")
                    put("color", config.color.name)
                    put("group", config.group)
                    put("lastConnected", config.lastConnected ?: 0L)
                    put("createdAt", config.createdAt)
                }
                arr.put(obj)
            }
            root.put("connections", arr)

            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                BACKUP_DIR
            )
            if (!dir.exists()) dir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "$BACKUP_FILE_PREFIX$timestamp.json")
            file.writeText(root.toString(2))

            BackupResult(
                success = true,
                message = "备份已创建: ${file.absolutePath}",
                file = file
            )
        } catch (e: Exception) {
            BackupResult(
                success = false,
                message = "备份失败: ${e.message}"
            )
        }
    }

    /**
     * List all available backup files.
     */
    fun listBackups(): List<File> {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            BACKUP_DIR
        )
        if (!dir.exists()) return emptyList()
        return dir.listFiles { file -> file.name.startsWith(BACKUP_FILE_PREFIX) && file.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * Restore connections from a backup file.
     */
    fun restoreFromFile(file: File): RestoreResult {
        return try {
            if (!file.exists()) {
                return RestoreResult(false, "备份文件不存在")
            }
            if (file.length() > MAX_FILE_SIZE) {
                return RestoreResult(false, "备份文件过大")
            }

            val json = file.readText()
            val root = JSONObject(json)
            val arr = root.getJSONArray("connections")
            val result = mutableListOf<ConnectionConfig>()

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.optString("name", "Restored").take(MAX_NAME_LENGTH)
                    .replace(Regex("[\\p{Cntrl}]"), "").trim().ifBlank { "Restored" }
                val host = obj.optString("host", "").take(MAX_HOST_LENGTH)
                val username = obj.optString("username", "").take(MAX_USERNAME_LENGTH)
                val port = obj.optInt("port", 22).coerceIn(1, 65535)

                if (host.isBlank() || username.isBlank()) continue

                result.add(
                    ConnectionConfig(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        name = name,
                        host = host,
                        port = port,
                        username = username,
                        authType = try {
                            AuthType.valueOf(obj.optString("authType", "PASSWORD"))
                        } catch (_: Exception) {
                            AuthType.PASSWORD
                        },
                        password = obj.optString("password", "").ifBlank { null },
                        privateKeyPath = obj.optString("privateKeyPath", "").ifBlank { null },
                        passphrase = obj.optString("passphrase", "").ifBlank { null },
                        color = try {
                            ConnectionColor.valueOf(obj.optString("color", "DEFAULT"))
                        } catch (_: Exception) {
                            ConnectionColor.DEFAULT
                        },
                        group = obj.optString("group", "Default").take(MAX_NAME_LENGTH),
                        lastConnected = obj.optLong("lastConnected", 0L).takeIf { it > 0 },
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            RestoreResult(
                success = true,
                message = "已恢复 ${result.size} 个连接",
                connections = result
            )
        } catch (e: Exception) {
            RestoreResult(
                success = false,
                message = "恢复失败: ${e.message}"
            )
        }
    }
}
