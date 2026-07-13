package com.yishell.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "connections")
data class ConnectionConfig(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val authType: AuthType,
    val password: String? = null,
    val privateKeyPath: String? = null,
    val passphrase: String? = null,
    val color: ConnectionColor = ConnectionColor.DEFAULT,
    val iconResName: String? = null, // 预设图标资源名（如 ic_server_large_red），null 则按 color 默认推导
    val customIconUri: String? = null,
    val group: String = "Default",
    val lastConnected: Long? = null,
    val isConnected: Boolean = false,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val favoriteOrder: Int = 0
)

enum class AuthType {
    PASSWORD,
    KEY,
    KEY_WITH_PASSPHRASE
}

enum class ConnectionColor {
    DEFAULT,
    GREEN,
    BLUE,
    RED,
    YELLOW,
    PURPLE,
    CYAN
}
