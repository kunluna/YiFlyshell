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

            Text(
                text = "字体大小: ${settings.fontSize}sp",
                style = MaterialTheme.typography.bodyLarge
            )
            Slider(
                value = settings.fontSize.toFloat(),
                onValueChange = { viewModel.updateFontSize(it.toInt()) },
                valueRange = 10f..24f,
                steps = 6
            )
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
        }
    }
}
