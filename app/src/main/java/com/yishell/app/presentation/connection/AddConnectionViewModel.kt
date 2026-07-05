package com.yishell.app.presentation.connection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yishell.app.data.model.AuthType
import com.yishell.app.data.model.ConnectionConfig
import com.yishell.app.data.security.CryptoManager
import com.yishell.app.domain.repository.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddConnectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val connectionRepository: ConnectionRepository,
    private val cryptoManager: CryptoManager
) : ViewModel() {

    private val editId: String? = savedStateHandle["connectionId"]

    private val _isEditing = MutableStateFlow(editId != null)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _host = MutableStateFlow("")
    val host: StateFlow<String> = _host.asStateFlow()

    private val _port = MutableStateFlow("22")
    val port: StateFlow<String> = _port.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _authType = MutableStateFlow(AuthType.PASSWORD)
    val authType: StateFlow<AuthType> = _authType.asStateFlow()

    private val _privateKeyPath = MutableStateFlow("")
    val privateKeyPath: StateFlow<String> = _privateKeyPath.asStateFlow()

    private val _passphrase = MutableStateFlow("")
    val passphrase: StateFlow<String> = _passphrase.asStateFlow()

    private val _group = MutableStateFlow("")
    val group: StateFlow<String> = _group.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _color = MutableStateFlow(com.yishell.app.data.model.ConnectionColor.DEFAULT)
    val color: StateFlow<com.yishell.app.data.model.ConnectionColor> = _color.asStateFlow()

    private val _errors = MutableStateFlow<Map<String, String>>(emptyMap())
    val errors: StateFlow<Map<String, String>> = _errors.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    // P1-2：原 _testResult: Boolean? 会给用户"连接成功"的虚假信心——
    // 实际只做了 TCP 端口连通性测试，未验证 SSH 握手与认证。
    // 改为密封类结果，UI 据此显示诚实的测试范围说明。
    private val _testResult = MutableStateFlow<TestConnectionResult?>(null)
    val testResult: StateFlow<TestConnectionResult?> = _testResult.asStateFlow()

    private var _originalLoadedPassword: String? = null

    init {
        if (editId != null) {
            viewModelScope.launch {
                connectionRepository.getConnectionById(editId)?.let { config ->
                    _name.value = config.name
                    _host.value = config.host
                    _port.value = config.port.toString()
                    _username.value = config.username
                    // P0-3：持久化存储的密码是加密的，加载编辑时需解密显示给用户。
                    // 若解密失败（旧明文兼容），则直接回退到原始值。
                    val decryptedPassword = cryptoManager.decrypt(config.password ?: "")
                        ?: config.password
                    _password.value = decryptedPassword ?: ""
                    _originalLoadedPassword = config.password  // 保存原始加密值，用于保存时判断"是否修改"
                    _authType.value = config.authType
                    _privateKeyPath.value = config.privateKeyPath ?: ""
                    _passphrase.value = config.passphrase ?: ""
                    _group.value = config.group
                    _isFavorite.value = config.isFavorite
                    _color.value = config.color
                }
            }
        }
    }

    fun updateName(value: String) { _name.value = value }
    fun updateHost(value: String) { _host.value = value }
    fun updatePort(value: String) { _port.value = value }
    fun updateUsername(value: String) { _username.value = value }
    fun updatePassword(value: String) { _password.value = value; _originalLoadedPassword = null }
    fun updateAuthType(value: AuthType) { _authType.value = value }
    fun updatePrivateKeyPath(value: String) { _privateKeyPath.value = value }
    fun updatePassphrase(value: String) { _passphrase.value = value }
    fun updateGroup(value: String) { _group.value = value }
    fun updateIsFavorite(value: Boolean) { _isFavorite.value = value }
    fun updateColor(value: com.yishell.app.data.model.ConnectionColor) { _color.value = value }

    fun save() {
        val validationErrors = validate()
        if (validationErrors.isNotEmpty()) {
            _errors.value = validationErrors
            return
        }

        val portInt = _port.value.toIntOrNull() ?: 22
        val encryptedPassword = if (_originalLoadedPassword != null && _password.value == _originalLoadedPassword) {
            _password.value
        } else {
            _password.value.ifBlank { null }?.let { cryptoManager.encrypt(it) }
        }
        val encryptedPassphrase = _passphrase.value.ifBlank { null }?.let { cryptoManager.encrypt(it) }

        val config = ConnectionConfig(
            id = editId ?: UUID.randomUUID().toString(),
            name = _name.value.trim(),
            host = _host.value.trim(),
            port = portInt,
            username = _username.value.trim(),
            authType = _authType.value,
            password = encryptedPassword,
            privateKeyPath = _privateKeyPath.value.ifBlank { null },
            passphrase = encryptedPassphrase,
            group = _group.value.trim(),
            color = _color.value,
            isFavorite = _isFavorite.value
        )

        viewModelScope.launch {
            if (_isEditing.value) {
                connectionRepository.updateConnection(config)
            } else {
                connectionRepository.insertConnection(config)
            }
            _saveSuccess.value = true
        }
    }

    fun testConnection() {
        val validationErrors = validate()
        if (validationErrors.isNotEmpty()) {
            _errors.value = validationErrors
            return
        }

        _isTesting.value = true
        _testResult.value = null

        viewModelScope.launch {
            try {
                val host = _host.value.trim()
                val port = _port.value.toIntOrNull() ?: 22
                val socket = java.net.Socket()
                try {
                    socket.connect(java.net.InetSocketAddress(host, port), 5000)
                } finally {
                    try { socket.close() } catch (_: Exception) {}
                }
                // P1-2：诚实标注测试范围——仅验证 TCP 端口可达，未做 SSH 握手与认证。
                // 完整 SSH 握手会触发 fail2ban 且耗时 1-3s，作为"测试"代价过高；
                // 用户实际连接时仍可能因凭据错误失败。这里明确告知测试边界。
                _testResult.value = TestConnectionResult.PortReachable
            } catch (e: Exception) {
                _testResult.value = TestConnectionResult.PortUnreachable(e.message ?: "连接失败")
            } finally {
                _isTesting.value = false
            }
        }
    }

    private fun validate(): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        if (_name.value.isBlank()) {
            errors["name"] = "请输入连接名称"
        }
        if (_host.value.isBlank()) {
            errors["host"] = "请输入主机地址"
        }
        val portInt = _port.value.toIntOrNull()
        if (portInt == null || portInt < 1 || portInt > 65535) {
            errors["port"] = "端口必须是1-65535"
        }
        if (_username.value.isBlank()) {
            errors["username"] = "请输入用户名"
        }
        if ((_authType.value == AuthType.KEY || _authType.value == AuthType.KEY_WITH_PASSPHRASE) &&
            _privateKeyPath.value.isBlank()
        ) {
            errors["privateKeyPath"] = "请输入私钥路径"
        }

        return errors
    }

    private fun clearError(field: String) {
        _errors.value = _errors.value - field
    }
}

/**
 * 测试连接的结果（P1-2）。
 *
 * 诚实地反映测试范围：当前实现仅做 TCP 端口连通性测试，
 * 未执行 SSH 握手与认证（避免触发服务器 fail2ban，且完整握手耗时较长）。
 * UI 必须根据具体子类型显示对应文案，不能笼统地说"连接成功"。
 */
sealed class TestConnectionResult {
    /** TCP 端口可达，但未验证 SSH 认证——用户实际连接仍可能因凭据失败。 */
    data object PortReachable : TestConnectionResult()

    /** 端口不可达（连接超时 / 拒绝 / DNS 失败等）。[reason] 为简要原因。 */
    data class PortUnreachable(val reason: String) : TestConnectionResult()
}
