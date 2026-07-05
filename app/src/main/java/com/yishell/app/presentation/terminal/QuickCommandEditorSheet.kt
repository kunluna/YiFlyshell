package com.yishell.app.presentation.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.yishell.app.data.model.QuickCommand
import com.yishell.app.presentation.theme.*

/**
 * 快捷命令编辑 BottomSheet。
 * 支持：增、删、改、查看列表。
 * 排序通过长按拖动实现（简化版：上下箭头按钮）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCommandEditorSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    commands: List<QuickCommand>,
    onAdd: (String, String) -> Unit,
    onDelete: (QuickCommand) -> Unit,
    onReorder: (List<QuickCommand>) -> Unit
) {
    if (!visible) return

    var showAddDialog by remember { mutableStateOf(false) }
    var newLabel by remember { mutableStateOf("") }
    var newCommand by remember { mutableStateOf("") }

    var localCommands by remember(commands) { mutableStateOf(commands) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "编辑快捷命令",
                        color = TerminalForeground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = TerminalForeground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(localCommands, key = { it.id }) { cmd ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cmd.label,
                                    color = TerminalForeground,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = cmd.command,
                                    color = TerminalForeground.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            // 上移
                            if (localCommands.size > 1) {
                                TextButton(
                                    onClick = {
                                        val idx = localCommands.indexOf(cmd)
                                        if (idx > 0) {
                                            val list = localCommands.toMutableList()
                                            list.removeAt(idx)
                                            list.add(idx - 1, cmd)
                                            localCommands = list
                                            onReorder(list)
                                        }
                                    }
                                ) {
                                    Text("↑", color = Cyan500, fontSize = 16.sp)
                                }
                                // 下移
                                TextButton(
                                    onClick = {
                                        val idx = localCommands.indexOf(cmd)
                                        if (idx < localCommands.size - 1) {
                                            val list = localCommands.toMutableList()
                                            list.removeAt(idx)
                                            list.add(idx + 1, cmd)
                                            localCommands = list
                                            onReorder(list)
                                        }
                                    }
                                ) {
                                    Text("↓", color = Cyan500, fontSize = 16.sp)
                                }
                            }
                            IconButton(onClick = { onDelete(cmd) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "删除",
                                    tint = AnsiRed
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Green500)
                ) {
                    Text("+ 添加命令", color = DarkSurface)
                }
            }
        }
    }

    // 添加对话框
    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "添加快捷命令",
                        color = TerminalForeground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newLabel,
                        onValueChange = { newLabel = it },
                        label = { Text("显示名称", color = TerminalForeground.copy(alpha = 0.6f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = TerminalForeground)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCommand,
                        onValueChange = { newCommand = it },
                        label = { Text("命令内容", color = TerminalForeground.copy(alpha = 0.6f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = TerminalForeground,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("取消", color = TerminalForeground.copy(alpha = 0.7f))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newLabel.isNotBlank() && newCommand.isNotBlank()) {
                                    onAdd(newLabel.trim(), newCommand.trim())
                                    newLabel = ""
                                    newCommand = ""
                                    showAddDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Green500)
                        ) {
                            Text("添加", color = DarkSurface)
                        }
                    }
                }
            }
        }
    }
}
