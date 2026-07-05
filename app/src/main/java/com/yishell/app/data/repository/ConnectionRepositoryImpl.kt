package com.yishell.app.data.repository

import com.yishell.app.data.local.AppDatabase
import com.yishell.app.data.local.ConnectionDao
import com.yishell.app.data.model.ConnectionConfig
import com.yishell.app.data.model.Session
import com.yishell.app.data.security.CryptoManager
import com.yishell.app.domain.repository.ConnectionRepository
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionRepositoryImpl @Inject constructor(
    private val connectionDao: ConnectionDao,
    private val database: AppDatabase,
    private val cryptoManager: CryptoManager
) : ConnectionRepository {

    /**
     * 临时连接条目（P1-5）。
     *
     * 快速连接创建的 ConnectionConfig 不写库，只存内存。原实现用 ConcurrentHashMap 永不清理，
     * 长时间运行会累积过期条目。现包装 [savedAt] 时间戳，在 [saveTempConnection] 时懒清理
     * 超过 [MAX_TEMP_AGE_MS] 的条目，避免内存泄漏。
     */
    private data class TempEntry(
        val config: ConnectionConfig,
        val savedAt: Long = System.currentTimeMillis()
    )

    private val tempConnections = ConcurrentHashMap<String, TempEntry>()

    companion object {
        // P1-5：临时连接存活上限 24 小时。超过即视为过期可清理。
        private const val MAX_TEMP_AGE_MS = 24L * 60 * 60 * 1000
    }

    override fun getAllConnections(): Flow<List<ConnectionConfig>> =
        connectionDao.getAllConnections()

    override suspend fun getConnectionById(id: String): ConnectionConfig? =
        tempConnections[id]?.config ?: connectionDao.getConnectionById(id)

    override suspend fun getByName(name: String): ConnectionConfig? =
        connectionDao.getByName(name)

    override suspend fun insertConnection(connection: ConnectionConfig) =
        connectionDao.insertConnection(connection)

    override suspend fun updateConnection(connection: ConnectionConfig) =
        connectionDao.updateConnection(connection)

    override suspend fun deleteConnection(connection: ConnectionConfig) =
        connectionDao.deleteConnection(connection)

    override suspend fun updateLastConnected(id: String, timestamp: Long) =
        connectionDao.updateLastConnected(id, timestamp)

    override suspend fun duplicateConnection(id: String, newName: String): String? {
        val original = tempConnections[id]?.config ?: connectionDao.getConnectionById(id) ?: return null
        val newId = java.util.UUID.randomUUID().toString()
        val duplicate = original.copy(
            id = newId,
            name = newName,
            createdAt = System.currentTimeMillis()
        )
        connectionDao.insertConnection(duplicate)
        return newId
    }

    override fun saveTempConnection(config: ConnectionConfig) {
        // P1-5：懒清理——每次保存新临时连接时顺便扫一遍过期条目，避免单独的定时任务。
        // 注意：调用方（如 HomeViewModel.quickConnect）已负责加密密码，这里不再重复加密，
        // 防止双重加密导致后续解密失败（表现为密码变成乱码、SSH 认证失败）。
        cleanupExpiredTempConnections()
        tempConnections[config.id] = TempEntry(config = config)
    }

    override fun getTempConnection(id: String): ConnectionConfig? =
        tempConnections[id]?.config

    /**
     * 删除超过 [MAX_TEMP_AGE_MS] 的临时连接。可在保存新条目或 App 启动时调用。
     */
    fun cleanupExpiredTempConnections() {
        val cutoff = System.currentTimeMillis() - MAX_TEMP_AGE_MS
        val iterator = tempConnections.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.savedAt < cutoff) {
                iterator.remove()
            }
        }
    }

    override suspend fun startSession(connectionId: String): String {
        val sessionId = java.util.UUID.randomUUID().toString()
        val session = Session(
            id = sessionId,
            configId = connectionId,
            isActive = true,
            startedAt = System.currentTimeMillis(),
            lastActivity = System.currentTimeMillis()
        )
        connectionDao.insertSession(session)
        connectionDao.updateLastConnected(connectionId, System.currentTimeMillis())
        return sessionId
    }

    override suspend fun endSession(sessionId: String) {
        connectionDao.updateSessionActive(sessionId, false)
    }

}
