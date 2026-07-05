package com.yishell.app.data.repository

import com.yishell.app.data.local.AppDatabase
import com.yishell.app.data.model.QuickCommand
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class QuickCommandRepository @Inject constructor(
    private val database: AppDatabase
) {
    private val dao get() = database.quickCommandDao()

    fun getByConnectionId(connectionId: String): Flow<List<QuickCommand>> =
        dao.getByConnectionId(connectionId)

    suspend fun getByConnectionIdSync(connectionId: String): List<QuickCommand> =
        dao.getByConnectionIdSync(connectionId)

    suspend fun add(connectionId: String, label: String, command: String, order: Int) {
        dao.insert(QuickCommand(connectionId = connectionId, label = label, command = command, sortOrder = order))
    }

    suspend fun update(command: QuickCommand) {
        dao.update(command)
    }

    suspend fun delete(command: QuickCommand) {
        dao.delete(command)
    }

    suspend fun seedDefaults(connectionId: String) {
        val existing = getByConnectionIdSync(connectionId)
        if (existing.isNotEmpty()) return
        val defaults = listOf(
            "ls -la" to "ls -la",
            "htop" to "htop",
            "df -h" to "df -h",
            "free -m" to "free -m",
            "top" to "top",
            "ps aux" to "ps aux",
            "netstat -tlnp" to "netstat -tlnp",
            "os-release" to "cat /etc/os-release",
            "uname -a" to "uname -a",
            "uptime" to "uptime",
            "clear" to "clear",
            "history" to "history",
            "exit" to "exit"
        )
        defaults.forEachIndexed { idx, pair ->
            dao.insert(QuickCommand(connectionId = connectionId, label = pair.first, command = pair.second, sortOrder = idx))
        }
    }
}
