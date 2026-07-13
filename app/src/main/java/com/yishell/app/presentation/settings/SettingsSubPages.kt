package com.yishell.app.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yishell.app.data.local.IconBreathMode
import com.yishell.app.presentation.home.HomeViewModel
import com.yishell.app.presentation.theme.TerminalThemes
import com.yishell.app.presentation.theme.whiteGlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("外观设置") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "外观",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("深色主题", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = settings.isDarkTheme,
                    onCheckedChange = { viewModel.updateDarkTheme(it) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("玻璃效果", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = settings.glassEffect,
                    onCheckedChange = { viewModel.updateGlassEffect(it) }
                )
            }

            HorizontalDivider()

            Text(
                text = "已连接图标动效",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "仅影响首页已连接卡片的图标",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            IconBreathMode.entries.forEach { mode ->
                val modeLabel = when (mode) {
                    IconBreathMode.OFF -> "关闭"
                    IconBreathMode.SLOW -> "缓慢呼吸"
                    IconBreathMode.FAST -> "快速脉冲"
                }
                val modeDesc = when (mode) {
                    IconBreathMode.OFF -> "静态图标，无动效"
                    IconBreathMode.SLOW -> "3秒周期，柔和呼吸"
                    IconBreathMode.FAST -> "1秒周期，明显脉冲"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (settings.iconBreathMode == mode) {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                            } else {
                                Modifier
                            }
                        )
                        .clickable { viewModel.updateIconBreathMode(mode) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(modeLabel, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            modeDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    RadioButton(
                        selected = settings.iconBreathMode == mode,
                        onClick = { viewModel.updateIconBreathMode(mode) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    var showKeyboardLayoutEditor by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("终端设置") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "字体大小",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "${settings.fontSize}sp",
                style = MaterialTheme.typography.bodyLarge
            )
            Slider(
                value = settings.fontSize.toFloat(),
                onValueChange = { viewModel.updateFontSize(it.toInt()) },
                valueRange = 10f..24f,
                steps = 6
            )

            HorizontalDivider()

            Text(
                text = "终端键盘布局",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showKeyboardLayoutEditor = true }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("终端键盘布局", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "自定义键盘按键顺序",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "\u203A",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (showKeyboardLayoutEditor) {
                KeyboardLayoutEditor(
                    currentLayout = settings.keyboardLayout,
                    onSave = { layout ->
                        viewModel.updateKeyboardLayout(layout)
                        showKeyboardLayoutEditor = false
                    },
                    onDismiss = { showKeyboardLayoutEditor = false }
                )
            }

            HorizontalDivider()

            Text(
                text = "终端配色",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            TerminalThemePicker(
                selectedScheme = settings.terminalColorScheme,
                onSelect = { viewModel.updateTerminalColorScheme(it) }
            )

            Button(
                onClick = { viewModel.resetTheme() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("恢复默认主题")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("连接设置") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "连接",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = settings.sshTimeout.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let { timeout ->
                        if (timeout in 5..300) viewModel.updateSshTimeout(timeout)
                    }
                },
                label = { Text("SSH超时(秒)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("自动重连", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = settings.autoReconnect,
                    onCheckedChange = { viewModel.updateAutoReconnect(it) }
                )
            }

            OutlinedTextField(
                value = settings.keepAliveInterval.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let { interval ->
                        if (interval in 10..300) viewModel.updateKeepAliveInterval(interval)
                    }
                },
                label = { Text("保活间隔(秒)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            HorizontalDivider()

            Text(
                text = "默认端口",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = settings.defaultPort.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let { port ->
                        if (port in 1..65535) viewModel.updateDefaultPort(port)
                    }
                },
                label = { Text("新建连接默认端口") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            HorizontalDivider()

            Text(
                text = "收藏显示数量请在收藏管理页面调整",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val homeViewModel: com.yishell.app.presentation.home.HomeViewModel = hiltViewModel()
    val backupMessage by homeViewModel.backupMessage.collectAsState()
    val availableBackups by homeViewModel.availableBackups.collectAsState()
    var showRestoreDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        homeViewModel.loadBackups()
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("选择备份文件恢复") },
            text = {
                Column {
                    if (availableBackups.isEmpty()) {
                        Text("暂无可用备份文件")
                    } else {
                        availableBackups.forEach { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        homeViewModel.restoreFromBackup(file)
                                        showRestoreDialog = false
                                    }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "${file.length() / 1024}KB",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    backupMessage?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(3000)
            homeViewModel.clearBackupMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据管理") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "备份与恢复",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "备份所有连接配置到本地文件，可用于换手机或重装应用时恢复。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = { homeViewModel.createFullBackup() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("创建备份")
            }

            OutlinedButton(
                onClick = {
                    homeViewModel.loadBackups()
                    showRestoreDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("从备份恢复")
            }

            backupMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (msg.contains("成功")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
