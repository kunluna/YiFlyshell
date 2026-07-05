package com.yishell.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a port forwarding rule.
 * - LOCAL: binds a local port and forwards traffic to a remote host:port via SSH.
 * - REMOTE: binds a remote port and forwards traffic back to a local host:port.
 */
@Entity(tableName = "port_forwardings")
data class PortForwarding(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: PortForwardingType,
    val localPort: Int,
    val remoteHost: String = "127.0.0.1",
    val remotePort: Int,
    val description: String = ""
)

enum class PortForwardingType {
    LOCAL,
    REMOTE
}
