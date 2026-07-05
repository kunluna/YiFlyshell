package com.yishell.app.domain.repository

import com.yishell.app.data.model.ConnectionConfig
import kotlinx.coroutines.flow.Flow

interface ConnectionRepository {
    fun getAllConnections(): Flow<List<ConnectionConfig>>
    suspend fun getConnectionById(id: String): ConnectionConfig?
    suspend fun getByName(name: String): ConnectionConfig?
    suspend fun insertConnection(connection: ConnectionConfig)
    suspend fun updateConnection(connection: ConnectionConfig)
    suspend fun deleteConnection(connection: ConnectionConfig)
    suspend fun updateLastConnected(id: String, timestamp: Long)
    suspend fun duplicateConnection(id: String, newName: String): String?
    fun saveTempConnection(config: ConnectionConfig)
    fun getTempConnection(id: String): ConnectionConfig?
    suspend fun startSession(connectionId: String): String
    suspend fun endSession(sessionId: String)
}
