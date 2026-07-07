package com.yishell.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yishell.app.data.model.ConnectionConfig
import com.yishell.app.data.model.ConnectionColor
import com.yishell.app.presentation.components.*
import com.yishell.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onConnect: (String) -> Unit,
    onAddConnection: () -> Unit,
    onEditConnection: (String) -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit = {},
    onSearch: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val connections by viewModel.connections.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<ConnectionConfig?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val connectedSessions by viewModel.connectedSessions.collectAsState()

    val recentList = remember(connections) {
        viewModel.getRecentConnections().take(4)
    }
    val favoriteList = remember(connections) {
        viewModel.getFavoriteConnections().take(3)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppBackground),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp) // Section 间距缩紧
        ) {
            item {
                TopNavBar(
                    onSearch = onSearch,
                    onSettings = onSettings
                )
            }

            item {
                ConnectedSection(
                    sessions = connectedSessions,
                    onDisconnect = { viewModel.disconnect(it) },
                    onDisconnectAll = { viewModel.disconnectAll() },
                    onOpenTerminal = onConnect,
                    onEditConnection = onEditConnection
                )
            }

            item {
                RecentSection(
                    recentList = recentList,
                    onConnect = onConnect
                )
            }

            item {
                FavoriteSection(
                    favoriteList = favoriteList,
                    onConnect = onConnect,
                    onEditConnection = onEditConnection,
                    onDelete = { showDeleteDialog = it },
                    onDuplicate = { viewModel.duplicateConnection(it) }
                )
            }

            item {
                NewConnectionCard(onClick = onAddConnection)
            }
        }

        showDeleteDialog?.let { config ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("删除连接") },
                text = { Text("确定删除 \"${config.name}\"？此操作不可恢复。") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteConnection(config)
                        showDeleteDialog = null
                    }) {
                        Text("删除", color = Danger)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
private fun TopNavBar(
    onSearch: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 2.dp), // 规范 5.2 页面左右 24dp
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            GlassLogoIcon(
                contentDescription = null,
                modifier = Modifier.size(40.dp) // 待截图确认
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "逸飞",
                    fontSize = 28.sp, // 规范 4.4 Display 28sp Bold
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 34.sp // 规范 4.5 Display 行高 34sp
                )
                Text(
                    text = "现代 SSH 客户端",
                    fontSize = 13.sp, // 规范 4.4 Caption 13sp
                    color = TextTertiary,
                    lineHeight = 18.sp // 规范 4.5 Caption 行高 18sp
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onSearch, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "搜索",
                    modifier = Modifier.size(24.dp),
                    tint = TextPrimary
                )
            }
            IconButton(onClick = onSettings, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "设置",
                    modifier = Modifier.size(24.dp),
                    tint = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun RoundIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(0.5.dp, GlassBorder, CircleShape) // 规范 6.3 Glass Border
            .shadow(8.dp, CircleShape, ambientColor = ShadowColor, spotColor = ShadowColor) // 规范 8.8 Soft Shadow
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun ConnectedSection(
    sessions: List<HomeViewModel.ConnectedSession>,
    onDisconnect: (HomeViewModel.ConnectedSession) -> Unit,
    onDisconnectAll: () -> Unit,
    onOpenTerminal: (String) -> Unit,
    onEditConnection: (String) -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp) // 规范 5.2
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp) // 规范 5.5 Card 内边距 16dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(StatusGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "已连接 (${sessions.size})",
                    fontSize = 16.sp, // 规范 4.4 Body 16sp
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                if (sessions.isNotEmpty()) {
                    Text(
                        text = "全部断开",
                        fontSize = 13.sp,
                        color = PrimaryBlue,
                        modifier = Modifier.clickable { onDisconnectAll() }
                    )
                }
            }

            if (sessions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                sessions.forEachIndexed { index, session ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        DashedDivider()
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    ConnectedCard(
                        session = session,
                        onDisconnect = { onDisconnect(session) },
                        onOpenTerminal = { onOpenTerminal(session.info.connectionId) },
                        onEditConnection = onEditConnection
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无已连接的服务器",
                        fontSize = 13.sp,
                        color = TextTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectedCard(
    session: HomeViewModel.ConnectedSession,
    onDisconnect: () -> Unit,
    onOpenTerminal: () -> Unit,
    onEditConnection: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LightSurface, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassServerIcon(
            modifier = Modifier.size(48.dp),
            color = session.info.color,
            customIconUri = session.info.customIconUri,
            size = 48
        )
        Spacer(modifier = Modifier.width(12.dp)) // 规范 5.6 图标与文字 12dp
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.info.name,
                fontSize = 18.sp, // 规范 4.4 Title 18sp
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${session.info.username}@${session.info.host}",
                fontSize = 13.sp, // 规范 4.4 Caption 13sp
                color = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(StatusGreen)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "已连接",
                    fontSize = 12.sp,
                    color = StatusGreen
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = getDurationString(session.connectedAt),
                    fontSize = 12.sp,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp)) // 规范 2.8 Button 18dp
                .background(TerminalButtonBg)
                .border(1.dp, PrimaryBlue.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
                .clickable { onOpenTerminal() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = ">_",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    lineHeight = 16.sp
                )
                Text(
                    text = "终端",
                    fontSize = 10.sp,
                    color = PrimaryBlue,
                    lineHeight = 12.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier.wrapContentSize(Alignment.TopEnd)
        ) {
            GlassMoreVertIcon(
                modifier = Modifier
                    .size(20.dp)
                    .clickable { expanded = true },
                tint = TextTertiary
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(LightSurface),
                offset = DpOffset((-4).dp, 4.dp)
            ) {
                DropdownMenuItem(
                    text = { Text("编辑") },
                    onClick = {
                        expanded = false
                        onEditConnection(session.info.connectionId)
                    },
                    leadingIcon = { GlassEditIcon(size = 18, tint = TextPrimary) }
                )
                DropdownMenuItem(
                    text = { Text("断开连接", color = Danger) },
                    onClick = {
                        expanded = false
                        onDisconnect()
                    },
                    leadingIcon = { GlassLinkOffIcon(size = 18, tint = Danger) }
                )
            }
        }
    }
}

@Composable
private fun RecentSection(
    recentList: List<ConnectionConfig>,
    onConnect: (String) -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp) // 规范 5.2
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassClockIcon(
                    modifier = Modifier.size(16.dp),
                    tint = TextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "最近连接",
                    fontSize = 16.sp, // 规范 4.4 Body 16sp
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { }
                ) {
                    Text(
                        text = "更多",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    GlassChevronRightIcon(
                        modifier = Modifier.size(14.dp),
                        tint = TextSecondary
                    )
                }
            }

            if (recentList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                recentList.forEachIndexed { index, config ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        DashedDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    RecentItem(
                        config = config,
                        onClick = { onConnect(config.id) }
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无最近连接",
                        fontSize = 13.sp,
                        color = TextTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentItem(
    config: ConnectionConfig,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassServerIcon(
            modifier = Modifier.size(48.dp),
            color = config.color,
            useTerminal = true,
            size = 48
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = config.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "${config.username}@${config.host}",
                fontSize = 12.sp,
                color = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = formatRelativeTime(config.lastConnected),
            fontSize = 12.sp,
            color = TextTertiary
        )
        Spacer(modifier = Modifier.width(2.dp))
        GlassChevronRightIcon(
            modifier = Modifier.size(14.dp),
            tint = Color(0xFFCCCCCC)
        )
    }
}

@Composable
private fun FavoriteSection(
    favoriteList: List<ConnectionConfig>,
    onConnect: (String) -> Unit,
    onEditConnection: (String) -> Unit,
    onDelete: (ConnectionConfig) -> Unit,
    onDuplicate: (ConnectionConfig) -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp) // 规范 5.2
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassStarIcon(
                    modifier = Modifier.size(16.dp),
                    tint = FavoriteYellow
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "收藏连接",
                    fontSize = 16.sp, // 规范 4.4 Body 16sp
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { }
                ) {
                    Text(
                        text = "管理",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    GlassChevronRightIcon(
                        modifier = Modifier.size(14.dp),
                        tint = TextSecondary
                    )
                }
            }

            if (favoriteList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                favoriteList.forEachIndexed { index, config ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        DashedDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    FavoriteItem(
                        config = config,
                        onConnect = { onConnect(config.id) },
                        onEditConnection = { onEditConnection(config.id) },
                        onDelete = { onDelete(config) },
                        onDuplicate = { onDuplicate(config) }
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无收藏连接",
                        fontSize = 13.sp,
                        color = TextTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteItem(
    config: ConnectionConfig,
    onConnect: () -> Unit,
    onEditConnection: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LightSurface, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassServerIcon(
            modifier = Modifier.size(48.dp),
            color = config.color,
            useTerminal = true,
            size = 48
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = config.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "${config.username}@${config.host}",
                fontSize = 12.sp,
                color = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp)) // 规范 2.8 Button 18dp
                .border(1.dp, PrimaryBlue.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
                .background(Color.White)
                .clickable { onConnect() }
                .padding(horizontal = 14.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "连接",
                fontSize = 13.sp,
                color = PrimaryBlue,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier.wrapContentSize(Alignment.TopEnd)
        ) {
            GlassMoreVertIcon(
                modifier = Modifier
                    .size(20.dp)
                    .clickable { expanded = true },
                tint = TextTertiary
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(LightSurface),
                offset = DpOffset((-4).dp, 4.dp)
            ) {
                DropdownMenuItem(
                    text = { Text("编辑") },
                    onClick = {
                        expanded = false
                        onEditConnection()
                    },
                    leadingIcon = { GlassEditIcon(size = 18, tint = TextPrimary) }
                )
                DropdownMenuItem(
                    text = { Text("复制") },
                    onClick = {
                        expanded = false
                        onDuplicate()
                    },
                    leadingIcon = { GlassCopyIcon(size = 18, tint = TextPrimary) }
                )
                DropdownMenuItem(
                    text = { Text("删除", color = Danger) },
                    onClick = {
                        expanded = false
                        onDelete()
                    },
                    leadingIcon = { GlassDeleteIcon(size = 18, tint = Danger) }
                )
            }
        }
    }
}

@Composable
private fun NewConnectionCard(onClick: () -> Unit) {
    GlassActionCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp) // 规范 5.2
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp), // Glass Card 内边距 20dp
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassServerIcon(
                modifier = Modifier.size(72.dp),
                useTerminal = true,
                size = 72
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "新建 SSH 连接",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "添加一台新的服务器",
                    fontSize = 13.sp,
                    color = TextTertiary
                )
            }
        }
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 8.dp, // 规范 8.8 Soft Shadow 0,8,20,8%
                shape = RoundedCornerShape(24.dp), // 规范 8.2 Card 24dp
                ambientColor = ShadowColor,
                spotColor = ShadowColor
            )
            .clip(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White) // 规范 13.6 普通 Card 纯白
    ) {
        content()
    }
}

/**
 * Glass Action Card — 规范 8.5/10.7
 * 透明白玻璃背景，用于新建 SSH 等操作入口
 */
@Composable
private fun GlassActionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 8.dp, // 规范 8.8 Soft Shadow
                shape = RoundedCornerShape(24.dp), // 规范 8.2 Card 24dp
                ambientColor = ShadowColor,
                spotColor = ShadowColor
            )
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp)), // 规范 6.3 Glass Border
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)) // 规范 13.6 Glass Card 透明白
    ) {
        content()
    }
}

private fun formatRelativeTime(timestamp: Long?): String {
    if (timestamp == null) return ""
    val now = System.currentTimeMillis()
    if (timestamp > now) return ""

    val nowCal = java.util.Calendar.getInstance().apply { timeInMillis = now }
    val tsCal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }

    val sameDay = nowCal.get(java.util.Calendar.YEAR) == tsCal.get(java.util.Calendar.YEAR) &&
            nowCal.get(java.util.Calendar.DAY_OF_YEAR) == tsCal.get(java.util.Calendar.DAY_OF_YEAR)

    if (sameDay) {
        return String.format(
            "%02d:%02d",
            tsCal.get(java.util.Calendar.HOUR_OF_DAY),
            tsCal.get(java.util.Calendar.MINUTE)
        )
    }

    nowCal.add(java.util.Calendar.DAY_OF_YEAR, -1)
    val yesterday = nowCal.get(java.util.Calendar.YEAR) == tsCal.get(java.util.Calendar.YEAR) &&
            nowCal.get(java.util.Calendar.DAY_OF_YEAR) == tsCal.get(java.util.Calendar.DAY_OF_YEAR)

    if (yesterday) {
        return String.format(
            "昨天 %02d:%02d",
            tsCal.get(java.util.Calendar.HOUR_OF_DAY),
            tsCal.get(java.util.Calendar.MINUTE)
        )
    }

    nowCal.add(java.util.Calendar.DAY_OF_YEAR, -1)
    val dayBeforeYesterday = nowCal.get(java.util.Calendar.YEAR) == tsCal.get(java.util.Calendar.YEAR) &&
            nowCal.get(java.util.Calendar.DAY_OF_YEAR) == tsCal.get(java.util.Calendar.DAY_OF_YEAR)

    if (dayBeforeYesterday) {
        return String.format(
            "前天 %02d:%02d",
            tsCal.get(java.util.Calendar.HOUR_OF_DAY),
            tsCal.get(java.util.Calendar.MINUTE)
        )
    }

    return String.format(
        "%02d-%02d %02d:%02d",
        tsCal.get(java.util.Calendar.MONTH) + 1,
        tsCal.get(java.util.Calendar.DAY_OF_MONTH),
        tsCal.get(java.util.Calendar.HOUR_OF_DAY),
        tsCal.get(java.util.Calendar.MINUTE)
    )
}

private fun getDurationString(connectedAt: Long): String {
    val elapsed = System.currentTimeMillis() - connectedAt
    val hours = elapsed / 3600000
    val minutes = (elapsed % 3600000) / 60000
    val seconds = (elapsed % 60000) / 1000
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

@Composable
private fun DashedDivider(
    color: Color = Color(0xFFE5E5EA).copy(alpha = 0.6f),
    thickness: Float = 1f
) {
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(thickness.dp)
    ) {
        val dashWidth = 6.dp.toPx()
        val dashGap = 4.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(minOf(x + dashWidth, size.width), 0f),
                strokeWidth = thickness
            )
            x += dashWidth + dashGap
        }
    }
}
