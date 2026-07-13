package com.yishell.app.data.remote

import android.content.Context
import android.os.Environment
import android.util.Log
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

@Singleton
class ConnectionExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ConnectionExporter"
        private const val MAX_FILE_SIZE = 10L * 1024 * 1024
        private const val MAX_NAME_LENGTH = 128
        private const val MAX_HOST_LENGTH = 255
        private const val MAX_USERNAME_LENGTH = 128
    }

    fun exportToJson(connections: List<ConnectionConfig>): String {
        val root = JSONObject()
        root.put("app", "YiShell")
        root.put("version", "1.2.2")
        root.put("exportTime", System.currentTimeMillis())
        root.put("count", connections.size)

        val arr = JSONArray()
        connections.forEach { config ->
            val obj = JSONObject().apply {
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
            }
            arr.put(obj)
        }
        root.put("connections", arr)
        return root.toString(2)
    }

    fun importFromJson(json: String): List<ConnectionConfig> {
        val root = JSONObject(json)
        val arr = root.getJSONArray("connections")
        val result = mutableListOf<ConnectionConfig>()

        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val name = sanitizeName(obj.optString("name", "Imported"))
            val host = obj.optString("host", "").take(MAX_HOST_LENGTH)
            val port = obj.optInt("port", 22).coerceIn(1, 65535)
            val username = obj.optString("username", "").take(MAX_USERNAME_LENGTH)

            if (host.isBlank() || username.isBlank()) {
                Log.w(TAG, "Skipping invalid connection entry at index $i")
                continue
            }

            result.add(
                ConnectionConfig(
                    name = name,
                    host = host,
                    port = port,
                    username = username,
                    authType = try { AuthType.valueOf(obj.optString("authType", "PASSWORD")) } catch (_: Exception) { AuthType.PASSWORD },
                    password = obj.optString("password", "").ifBlank { null },
                    privateKeyPath = obj.optString("privateKeyPath", "").ifBlank { null },
                    passphrase = obj.optString("passphrase", "").ifBlank { null },
                    color = try { ConnectionColor.valueOf(obj.optString("color", "DEFAULT")) } catch (_: Exception) { ConnectionColor.DEFAULT },
                    group = obj.optString("group", "Default").take(MAX_NAME_LENGTH)
                )
            )
        }
        return result
    }

    fun saveToFile(json: String): File? {
        return try {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "YiShell")
            if (!dir.exists()) dir.mkdirs()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "yishell_backup_$timestamp.json")
            file.writeText(json)
            file
        } catch (e: Exception) {
            null
        }
    }

    private fun sanitizeName(name: String): String {
        return name.take(MAX_NAME_LENGTH)
            .replace(Regex("[\\p{Cntrl}]"), "")
            .trim()
            .ifBlank { "Imported" }
    }
}
