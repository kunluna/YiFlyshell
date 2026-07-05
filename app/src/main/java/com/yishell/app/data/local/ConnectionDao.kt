package com.yishell.app.data.local

import androidx.room.*
import com.yishell.app.data.model.ConnectionConfig
import com.yishell.app.data.model.Session
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionDao {
    @Query("SELECT * FROM connections ORDER BY lastConnected DESC, name ASC")
    fun getAllConnections(): Flow<List<ConnectionConfig>>

    @Query("SELECT * FROM connections WHERE id = :id")
    suspend fun getConnectionById(id: String): ConnectionConfig?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConnection(connection: ConnectionConfig)

    @Update
    suspend fun updateConnection(connection: ConnectionConfig)

    @Delete
    suspend fun deleteConnection(connection: ConnectionConfig)

    @Query("UPDATE connections SET lastConnected = :timestamp WHERE id = :id")
    suspend fun updateLastConnected(id: String, timestamp: Long)

    @Query("SELECT * FROM connections WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): ConnectionConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: Session)

    @Query("UPDATE sessions SET isActive = :isActive WHERE id = :sessionId")
    suspend fun updateSessionActive(sessionId: String, isActive: Boolean)

}
