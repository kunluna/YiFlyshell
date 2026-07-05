package com.yishell.app.presentation.monitor

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yishell.app.data.model.ProcessInfo
import com.yishell.app.presentation.theme.*
import com.yishell.app.presentation.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorScreen(
    connectionId: String,
    onBack: () -> Unit,
    viewModel: MonitorViewModel = hiltViewModel()
) {
    val monitorData by viewModel.monitorData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val lastUpdated by viewModel.lastUpdated.collectAsState()
    val connectionInfo by viewModel.connectionInfo.collectAsState()
    var processesExpanded by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("服务器监控") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新",
                            tint = Green500
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TerminalBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        when {
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = error ?: "",
                            color = AnsiRed,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.clearError(); viewModel.refresh() }
                        ) {
                            Text("重试")
                        }
                    }
                }
            }
            isLoading && monitorData.cpuUsage == 0f -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Green500)
                }
            }
            else -> {
                if (monitorData.cpuUsage == 0f && !isLoading && error == null && monitorData.processes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "暂无监控数据",
                                color = TerminalForeground.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.refresh() }) {
                                Text("刷新")
                            }
                        }
                    }
                } else {
                PullToRefreshBox(
                    isRefreshing = isLoading,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        if (connectionInfo.isNotEmpty()) {
                            Text(
                                text = connectionInfo,
                                color = Green500,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSurface, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        MonitorCard(
                            title = "CPU使用率${if (monitorData.cpuCoreCount > 1) " (${monitorData.cpuCoreCount}核)" else ""}",
                            value = String.format("%.1f%%", monitorData.cpuUsage),
                            progress = monitorData.cpuUsage / 100f,
                            progressColor = when {
                                monitorData.cpuUsage > 80 -> AnsiRed
                                monitorData.cpuUsage > 50 -> AnsiYellow
                                else -> Green500
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        MonitorCard(
                            title = "内存",
                            value = "${FormatUtils.formatBytes(monitorData.memoryUsed)} / ${FormatUtils.formatBytes(monitorData.memoryTotal)}",
                            subValue = String.format("%.1f%%", monitorData.memoryUsage),
                            progress = monitorData.memoryUsage / 100f,
                            progressColor = when {
                                monitorData.memoryUsage > 80 -> AnsiRed
                                monitorData.memoryUsage > 50 -> AnsiYellow
                                else -> Cyan500
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 磁盘：容量在 3 秒观察窗口内无变化，进度条无信息增量，降级为纯文字 InfoCard。
                        // 颜色阈值保留：>80% 变红提醒清理。
                        val diskColor = when {
                            monitorData.diskUsage > 80 -> AnsiRed
                            monitorData.diskUsage > 50 -> AnsiYellow
                            else -> TerminalForeground
                        }
                        InfoCard(
                            title = "磁盘",
                            value = "${FormatUtils.formatBytes(monitorData.diskUsed)} / ${FormatUtils.formatBytes(monitorData.diskTotal)} (${String.format("%.1f%%", monitorData.diskUsage)})",
                            valueColor = diskColor
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 网络上下行速率：实时变化，和 CPU/内存同类资源指标。
                        NetSpeedCard(
                            uploadSpeed = monitorData.netUploadSpeed,
                            downloadSpeed = monitorData.netDownloadSpeed
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        InfoCard(
                            title = "运行时间",
                            value = monitorData.uptime.ifEmpty { "N/A" }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LoadAverageCard(
                            load1 = monitorData.loadAvg1,
                            load5 = monitorData.loadAvg5,
                            load15 = monitorData.loadAvg15
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (lastUpdated.isNotEmpty()) {
                            Text(
                                text = lastUpdated,
                                color = TerminalForeground.copy(alpha = 0.5f),
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        ProcessListCard(
                            processes = monitorData.processes,
                            expanded = processesExpanded,
                            onToggle = { processesExpanded = !processesExpanded },
                            onKill = { pid, force -> viewModel.killProcess(pid, force) }
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
fun MonitorCard(
    title: String,
    value: String,
    subValue: String? = null,
    progress: Float,
    progressColor: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
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
                    text = title,
                    color = TerminalForeground.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
                Text(
                    text = value,
                    color = TerminalForeground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (subValue != null) {
                Text(
                    text = subValue,
                    color = progressColor,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(DarkSurfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(progressColor)
                )
            }
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    value: String,
    valueColor: Color = TerminalForeground
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                color = TerminalForeground.copy(alpha = 0.7f),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 网络上下行速率卡片。无进度条（网络无"总量"概念，只有速率）。
 */
@Composable
fun NetSpeedCard(
    uploadSpeed: Long,
    downloadSpeed: Long
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "网络",
                color = TerminalForeground.copy(alpha = 0.7f),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("↓", color = Cyan500, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = FormatUtils.formatSpeed(downloadSpeed),
                            color = Cyan500,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text("下载", color = TerminalForeground.copy(alpha = 0.5f), fontSize = 11.sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("↑", color = Green500, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = FormatUtils.formatSpeed(uploadSpeed),
                            color = Green500,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text("上传", color = TerminalForeground.copy(alpha = 0.5f), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun LoadAverageCard(
    load1: Float,
    load5: Float,
    load15: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "负载均值",
                color = TerminalForeground.copy(alpha = 0.7f),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LoadItem(label = "1分钟", value = load1)
                LoadItem(label = "5分钟", value = load5)
                LoadItem(label = "15分钟", value = load15)
            }
        }
    }
}

@Composable
fun LoadItem(label: String, value: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = String.format("%.2f", value),
            color = TerminalForeground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = TerminalForeground.copy(alpha = 0.5f),
            fontSize = 11.sp
        )
    }
}

@Composable
fun ProcessListCard(
    processes: List<ProcessInfo>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onKill: (String, Boolean) -> Unit
) {
    var showKillDialog by remember { mutableStateOf<ProcessInfo?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { onToggle() },
                            onTap = { onToggle() }
                        )
                    }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "进程 (${processes.size})",
                    color = TerminalForeground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = Green500
                )
            }

            if (expanded && processes.isNotEmpty()) {
                HorizontalDivider(color = DarkSurfaceVariant)
                
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("PID", color = TerminalForeground.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.width(50.dp))
                    Text("用户", color = TerminalForeground.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.width(60.dp))
                    Text("CPU%", color = TerminalForeground.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.width(45.dp))
                    Text("内存%", color = TerminalForeground.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.width(45.dp))
                    Text("命令", color = TerminalForeground.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.weight(1f))
                }

                processes.forEach { proc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(proc.pid, color = Cyan500, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(50.dp))
                        Text(proc.user, color = TerminalForeground.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.width(60.dp), maxLines = 1)
                        Text(
                            String.format("%.1f", proc.cpuPercent),
                            color = when { proc.cpuPercent > 50 -> AnsiRed; proc.cpuPercent > 20 -> AnsiYellow; else -> Green500 },
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(45.dp)
                        )
                        Text(
                            String.format("%.1f", proc.memPercent),
                            color = when { proc.memPercent > 50 -> AnsiRed; proc.memPercent > 20 -> AnsiYellow; else -> Cyan500 },
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(45.dp)
                        )
                        Text(
                            proc.command,
                            color = TerminalForeground.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        // Kill button
                        IconButton(
                            onClick = { showKillDialog = proc },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "结束",
                                tint = AnsiRed,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    showKillDialog?.let { proc ->
        AlertDialog(
            onDismissRequest = { showKillDialog = null },
            title = { Text("结束进程") },
            text = {
                Column {
                    Text("PID: ${proc.pid}")
                    Text("命令: ${proc.command}", maxLines = 2)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("选择终止信号:", color = TerminalForeground.copy(alpha = 0.7f))
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = { showKillDialog = null }) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        onKill(proc.pid, false)
                        showKillDialog = null
                    }) {
                        Text("结束 (SIGTERM)")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        onKill(proc.pid, true)
                        showKillDialog = null
                    }) {
                        Text("强制结束 (SIGKILL)", color = AnsiRed)
                    }
                }
            }
        )
    }
}
