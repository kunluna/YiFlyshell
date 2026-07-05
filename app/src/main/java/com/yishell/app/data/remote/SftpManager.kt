package com.yishell.app.data.remote

import android.util.Log
import com.trilead.ssh2.SFTPv3Client
import com.yishell.app.data.model.SftpItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

/** P2-6：批量删除结果，不再静默吞异常。 */
data class BatchDeleteResult(
    val deletedCount: Int,
    val failed: List<String>
)

@Singleton
class SftpManager @Inject constructor(
    private val sshManager: SshManager
) {
    private val lock = Any()
    private var sftpClient: SFTPv3Client? = null

    fun setSftpClient(client: SFTPv3Client) {
        synchronized(lock) {
            sftpClient = client
        }
    }

    fun getHomeDir(): String {
        val client = synchronized(lock) { sftpClient } ?: return "/"
        return client.canonicalPath(".")
    }

    suspend fun listFiles(path: String): List<SftpItem> = withContext(Dispatchers.IO) {
        val client = synchronized(lock) { sftpClient }
            ?: throw IllegalStateException("SFTP not connected")
        val entries = client.ls(path)
        entries
            .filter { it.filename != "." && it.filename != ".." }
            .map { entry ->
                SftpItem(
                    name = entry.filename,
                    path = if (path.endsWith("/")) "$path${entry.filename}" else "$path/${entry.filename}",
                    isDir = entry.attributes.isDirectory,
                    size = entry.attributes.size ?: 0L,
                    modTime = (entry.attributes.mtime ?: 0L) * 1000
                )
            }
            .sortedWith(compareByDescending<SftpItem> { it.isDir }.thenBy { it.name })
    }

    /**
     * P2-4：下载文件，带进度回调。
     * [onProgress] 在每次写入后以 (已传输字节, 总字节) 触发；total 为 0 表示未知大小。
     */
    suspend fun downloadFile(
        remotePath: String,
        localFile: File,
        onProgress: ((transferred: Long, total: Long) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val client = synchronized(lock) { sftpClient }
            ?: throw IllegalStateException("SFTP not connected")
        val handle = client.openFileRO(remotePath)
        try {
            val total = runCatching { client.stat(remotePath).size ?: 0L }.getOrDefault(0L)
            var transferred = 0L
            FileOutputStream(localFile).use { output ->
                val buffer = ByteArray(8192)
                var offset = 0L
                while (true) {
                    val bytesRead = client.read(handle, offset, buffer, 0, buffer.size)
                    if (bytesRead <= 0) break
                    output.write(buffer, 0, bytesRead)
                    offset += bytesRead
                    transferred += bytesRead
                    onProgress?.invoke(transferred, total)
                }
            }
        } finally {
            client.closeFile(handle)
        }
    }

    /**
     * P2-4：上传文件，带进度回调。
     * [onProgress] 在每次读取后以 (已传输字节, 总字节) 触发。
     */
    suspend fun uploadFile(
        remotePath: String,
        localFile: File,
        onProgress: ((transferred: Long, total: Long) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val client = synchronized(lock) { sftpClient }
            ?: throw IllegalStateException("SFTP not connected")
        val handle = client.createFile(remotePath)
        try {
            val total = localFile.length()
            var transferred = 0L
            FileInputStream(localFile).use { input ->
                val buffer = ByteArray(8192)
                var offset = 0L
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    client.write(handle, offset, buffer, 0, bytesRead)
                    offset += bytesRead
                    transferred += bytesRead
                    onProgress?.invoke(transferred, total)
                }
            }
        } finally {
            client.closeFile(handle)
        }
    }

    suspend fun deleteFile(remotePath: String) = withTimeoutOrNull(10_000L) {
        withContext(Dispatchers.IO) {
            val client = synchronized(lock) { sftpClient }
                ?: throw IllegalStateException("SFTP not connected")
            client.rm(remotePath)
        }
    }

    /**
     * P2-6：批量删除。原实现 catch(_){} 静默吞异常，用户不知道哪些删失败。
     * 现返回 [BatchDeleteResult]，调用方可据 failed 列表给用户反馈。
     */
    suspend fun batchDelete(fileNames: List<String>, remotePath: String): BatchDeleteResult =
        withTimeoutOrNull(30_000L) {
            withContext(Dispatchers.IO) {
                val client = synchronized(lock) { sftpClient }
                    ?: throw IllegalStateException("SFTP not connected")
                var deleted = 0
                val failed = mutableListOf<String>()
                for (name in fileNames) {
                    val fullPath = if (remotePath.endsWith("/")) "${remotePath}${name}" else "${remotePath}/${name}"
                    try {
                        client.rm(fullPath)
                        deleted++
                    } catch (e: Exception) {
                        Log.w("SftpManager", "batchDelete failed for $fullPath: ${e.message}")
                        failed.add(name)
                    }
                }
                BatchDeleteResult(deletedCount = deleted, failed = failed)
            }
        } ?: BatchDeleteResult(0, fileNames) // 超时：全部视为失败，让 UI 提示

    suspend fun mkdir(remotePath: String) = withTimeoutOrNull(10_000L) {
        withContext(Dispatchers.IO) {
            val client = synchronized(lock) { sftpClient }
                ?: throw IllegalStateException("SFTP not connected")
            client.mkdir(remotePath, 448)
        }
    }

    suspend fun rename(oldPath: String, newPath: String) = withTimeoutOrNull(10_000L) {
        withContext(Dispatchers.IO) {
            val client = synchronized(lock) { sftpClient }
                ?: throw IllegalStateException("SFTP not connected")
            client.mv(oldPath, newPath)
        }
    }

    suspend fun stat(remotePath: String): SftpItem = withContext(Dispatchers.IO) {
        val client = synchronized(lock) { sftpClient }
            ?: throw IllegalStateException("SFTP not connected")
        val attrs = client.stat(remotePath)
        SftpItem(
            name = remotePath.substringAfterLast('/'),
            path = remotePath,
            isDir = attrs.isDirectory,
            size = attrs.size ?: 0L,
            modTime = (attrs.mtime ?: 0L) * 1000
        )
    }

    fun close() {
        val client = synchronized(lock) {
            val c = sftpClient
            sftpClient = null
            c
        }
        try {
            client?.close()
        } catch (_: Exception) {}
    }

    /**
     * Check if the SFTP client connection is still alive.
     * Returns false if the client is null or the underlying socket is closed.
     */
    fun isConnected(): Boolean {
        val client = synchronized(lock) { sftpClient } ?: return false
        return try {
            // Try a lightweight operation to check if connection is alive
            client.canonicalPath(".")
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Close the existing client and create a new SFTPv3Client from the given connection.
     */
    fun reconnect(connection: com.trilead.ssh2.Connection): SFTPv3Client {
        close()
        val newClient = com.trilead.ssh2.SFTPv3Client(connection)
        synchronized(lock) {
            sftpClient = newClient
        }
        return newClient
    }
}
