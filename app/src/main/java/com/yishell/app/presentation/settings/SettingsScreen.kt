package com.yishell.app.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yishell.app.data.local.TerminalColorScheme
import com.yishell.app.presentation.theme.TerminalThemes
import com.yishell.app.presentation.theme.whiteGlassCard
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToTerminal: () -> Unit,
    onNavigateToConnection: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsCategoryCard(
                icon = Icons.Default.Palette,
                title = "外观",
                description = "主题、玻璃效果、字体大小",
                onClick = onNavigateToAppearance
            )
            SettingsCategoryCard(
                icon = Icons.Default.Terminal,
                title = "终端",
                description = "键盘布局、终端配色",
                onClick = onNavigateToTerminal
            )
            SettingsCategoryCard(
                icon = Icons.Default.Link,
                title = "连接",
                description = "SSH超时、自动重连、保活间隔",
                onClick = onNavigateToConnection
            )
            SettingsCategoryCard(
                icon = Icons.Default.Info,
                title = "关于",
                description = "YiShell v1.2.2",
                onClick = onNavigateToAbout
            )
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .whiteGlassCard()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TerminalThemePicker(
    selectedScheme: TerminalColorScheme,
    onSelect: (TerminalColorScheme) -> Unit
) {
    val schemes = TerminalColorScheme.entries
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        schemes.forEach { scheme ->
            val theme = TerminalThemes.forScheme(scheme)
            val isSelected = scheme == selectedScheme

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (isSelected) {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSelect(scheme) }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(theme.background)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$",
                        color = theme.foreground,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = theme.name,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                RadioButton(
                    selected = isSelected,
                    onClick = { onSelect(scheme) }
                )
            }
        }
    }
}

@Composable
fun KeyboardLayoutEditor(
    currentLayout: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialItems = remember(currentLayout) {
        try {
            val jsonArray = JSONArray(currentLayout)
            (0 until jsonArray.length()).map { jsonArray.getString(it) }
        } catch (e: Exception) {
            listOf("⏎ Enter", "␣ Space", "←", "↑", "→", "↓", "Esc", "Tab", "Ctrl", "Alt", ";", "/", "|", "-", "_", "~", ".", "历史↑", "历史↓", "PgUp", "PgDn")
        }
    }

    var items by remember { mutableStateOf(initialItems) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑键盘布局") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "拖动上下箭头调整按键顺序",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(items) { index, item ->
                        KeyboardLayoutItem(
                            label = item,
                            onMoveUp = {
                                if (index > 0) {
                                    items = items.toMutableList().apply {
                                        val temp = this[index]
                                        this[index] = this[index - 1]
                                        this[index - 1] = temp
                                    }
                                }
                            },
                            onMoveDown = {
                                if (index < items.size - 1) {
                                    items = items.toMutableList().apply {
                                        val temp = this[index]
                                        this[index] = this[index + 1]
                                        this[index + 1] = temp
                                    }
                                }
                            },
                            isFirst = index == 0,
                            isLast = index == items.size - 1
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val jsonArray = JSONArray()
                    items.forEach { jsonArray.put(it) }
                    onSave(jsonArray.toString())
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    items = listOf("⏎ Enter", "␣ Space", "←", "↑", "→", "↓", "Esc", "Tab", "Ctrl", "Alt", ";", "/", "|", "-", "_", "~", ".", "历史↑", "历史↓", "PgUp", "PgDn")
                }) {
                    Text("重置")
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}

@Composable
fun KeyboardLayoutItem(
    label: String,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    isFirst: Boolean,
    isLast: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onMoveUp,
            enabled = !isFirst,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = "上移",
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(
            onClick = onMoveDown,
            enabled = !isLast,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = "下移",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
