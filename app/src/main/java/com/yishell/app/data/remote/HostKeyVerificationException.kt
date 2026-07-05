package com.yishell.app.data.remote

/**
 * 主机密钥验证异常。
 *
 * 安全修复（P0-1）：首次连接到未知主机时，SSH 客户端必须让用户显式确认服务器指纹，
 * 而不是自动信任（TOFU without confirmation）。自动信任会敞开 MITM 攻击窗口——
 * 攻击者可以在首次连接时截获流量。
 *
 * 本异常由 [ServerHostKeyVerifier] 在首次遇到未知主机时抛出，携带服务器指纹信息。
 * 调用方（ViewModel）捕获后展示指纹给用户，用户确认后用 [acceptedFingerprint] 参数
 * 重新调用 [SshManager.connect]，verifier 验证指纹一致后才会持久化并放行。
 *
 * 已知主机的指纹不匹配（中间人攻击或服务器重装）同样抛出本异常，但 [type] 为 [Type.MISMATCH]，
 * UI 层应给出更严厉的警告。
 */
class HostKeyVerificationException(
    val hostname: String,
    val port: Int,
    val algorithm: String,
    val fingerprint: String,
    val storedFingerprint: String? = null,
    val type: Type = Type.UNKNOWN_HOST
) : RuntimeException(
    when (type) {
        Type.UNKNOWN_HOST -> "Unknown host $hostname:$port, fingerprint confirmation required"
        Type.MISMATCH -> "Host key mismatch for $hostname:$port — possible MITM attack"
    }
) {
    enum class Type { UNKNOWN_HOST, MISMATCH }
}
