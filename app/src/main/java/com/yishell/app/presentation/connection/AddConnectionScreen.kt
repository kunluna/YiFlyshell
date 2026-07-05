package com.yishell.app.presentation.connection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yishell.app.data.model.AuthType
import com.yishell.app.data.model.ConnectionColor
import com.yishell.app.presentation.theme.Danger
import com.yishell.app.presentation.theme.LightBackground
import com.yishell.app.presentation.theme.PrimaryBlue
import com.yishell.app.presentation.theme.Success
import com.yishell.app.presentation.theme.whiteGlassCard

private const val NAME_MAX_LENGTH = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddConnectionScreen(
    connectionId: String? = null,
    onBack: () -> Unit,
    viewModel: AddConnectionViewModel = hiltViewModel()
) {
    val name by viewModel.name.collectAsState()
    val host by viewModel.host.collectAsState()
    val port by viewModel.port.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val authType by viewModel.authType.collectAsState()
    val privateKeyPath by viewModel.privateKeyPath.collectAsState()
    val passphrase by viewModel.passphrase.collectAsState()
    val group by viewModel.group.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val selectedColor by viewModel.color.collectAsState()
    val errors by viewModel.errors.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()
    val testResult by viewModel.testResult.collectAsState()

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) onBack()
    }

    val portInt = port.toIntOrNull()
    val portInvalid = port.isNotBlank() && (portInt == null || portInt !in 1..65535)

    Scaffold(
        containerColor = LightBackground,
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "编辑连接" else "新建连接") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .whiteGlassCard()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= NAME_MAX_LENGTH) viewModel.updateName(it) },
                    label = { Text("连接名称") },
                    leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) },
                    trailingIcon = {
                        Text(
                            text = "${name.length}/$NAME_MAX_LENGTH",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (name.length >= NAME_MAX_LENGTH) Danger else Color.Gray
                        )
                    },
                    isError = errors.containsKey("name"),
                    supportingText = errors["name"]?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = host,
                    onValueChange = viewModel::updateHost,
                    label = { Text("主机地址") },
                    leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
                    isError = errors.containsKey("host"),
                    supportingText = errors["host"]?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = port,
                    onValueChange = viewModel::updatePort,
                    label = { Text("端口") },
                    leadingIcon = { Icon(Icons.Default.Lan, contentDescription = null) },
                    isError = portInvalid || errors.containsKey("port"),
                    supportingText = when {
                        portInvalid -> { { Text("端口必须是1-65535", color = Danger) } }
                        errors.containsKey("port") -> { { Text(errors["port"]!!) } }
                        else -> null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .whiteGlassCard()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = viewModel::updateUsername,
                    label = { Text("用户名") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    isError = errors.containsKey("username"),
                    supportingText = errors["username"]?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = group,
                    onValueChange = viewModel::updateGroup,
                    label = { Text("分组") },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 收藏开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = if (isFavorite) Color(0xFFFFCC00) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "添加到收藏",
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = isFavorite,
                        onCheckedChange = viewModel::updateIsFavorite
                    )
                }

                // 颜色选择器
                Text(
                    text = "标签颜色",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val colors = listOf(
                        ConnectionColor.DEFAULT to "默认",
                        ConnectionColor.BLUE to "蓝",
                        ConnectionColor.GREEN to "绿",
                        ConnectionColor.YELLOW to "黄",
                        ConnectionColor.PURPLE to "紫",
                        ConnectionColor.CYAN to "青",
                        ConnectionColor.RED to "红"
                    )
                    colors.forEach { (color, label) ->
                        FilterChip(
                            selected = selectedColor == color,
                            onClick = { viewModel.updateColor(color) },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text(
                    text = "认证方式",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = authType == AuthType.PASSWORD,
                        onClick = { viewModel.updateAuthType(AuthType.PASSWORD) },
                        label = { Text("密码") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = authType == AuthType.KEY,
                        onClick = { viewModel.updateAuthType(AuthType.KEY) },
                        label = { Text("密钥") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = authType == AuthType.KEY_WITH_PASSPHRASE,
                        onClick = { viewModel.updateAuthType(AuthType.KEY_WITH_PASSPHRASE) },
                        label = { Text("密钥+口令") },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (authType == AuthType.PASSWORD) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = viewModel::updatePassword,
                        label = { Text("密码") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                if (authType == AuthType.KEY || authType == AuthType.KEY_WITH_PASSPHRASE) {
                    OutlinedTextField(
                        value = privateKeyPath,
                        onValueChange = viewModel::updatePrivateKeyPath,
                        label = { Text("私钥路径") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                if (authType == AuthType.KEY_WITH_PASSPHRASE) {
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = viewModel::updatePassphrase,
                        label = { Text("口令") },
                        leadingIcon = { Icon(Icons.Default.Password, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            AnimatedVisibility(visible = testResult != null) {
                testResult?.let { result ->
                    val isSuccess = result is TestConnectionResult.PortReachable
                    val containerColor = if (isSuccess) Success.copy(alpha = 0.1f) else Danger.copy(alpha = 0.1f)
                    val tint = if (isSuccess) Success else Danger
                    val iconVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error
                    val titleText: String
                    val bodyText: String
                    when (result) {
                        is TestConnectionResult.PortReachable -> {
                            titleText = "端口可达"
                            bodyText = "已验证 TCP 端口可达，但未验证 SSH 认证。实际连接仍可能因凭据错误失败。"
                        }
                        is TestConnectionResult.PortUnreachable -> {
                            titleText = "端口不可达"
                            bodyText = result.reason
                        }
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = containerColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = titleText,
                                    color = tint,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = bodyText,
                                    color = tint.copy(alpha = 0.85f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            val portValid = portInt?.let { it in 1..65535 } ?: false
            val privateKeyValid = authType == AuthType.PASSWORD || privateKeyPath.isNotBlank()
            val formValid = name.isNotBlank() && host.isNotBlank() && username.isNotBlank() && portValid && privateKeyValid

            OutlinedButton(
                onClick = { viewModel.testConnection() },
                modifier = Modifier.fillMaxWidth(),
                enabled = formValid && !isTesting,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("测试中...")
                } else {
                    Icon(Icons.Default.WifiFind, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("测试连接")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("取消")
                }
                Button(
                    onClick = { viewModel.save() },
                    modifier = Modifier.weight(1f),
                    enabled = formValid,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isEditing) "更新连接" else "保存连接")
                }
            }
        }
    }
}
