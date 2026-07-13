package com.yishell.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yishell.app.data.model.ConnectionConfig
import com.yishell.app.presentation.components.*
import com.yishell.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteManageScreen(
    onBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    settingsViewModel: com.yishell.app.presentation.settings.SettingsViewModel = hiltViewModel()
) {
    val connections by viewModel.connections.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val favorites = remember(connections) {
        viewModel.getAllFavoriteConnections()
    }

    var displayCount by remember { mutableStateOf(settings.favoriteDisplayCount) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("收藏管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        containerColor = AppBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // 展示数量设置
            Text(
                text = "首页展示数量",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(2, 3, 5, 0).forEach { count ->
                    val label = if (count == 0) "全部" else count.toString()
                    val isSelected = displayCount == count
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.dp, PrimaryBlue, RoundedCornerShape(12.dp))
                                } else {
                                    Modifier.border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                }
                            )
                            .background(
                                if (isSelected) PrimaryBlue.copy(alpha = 0.1f) else Color.White,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                displayCount = count
                                settingsViewModel.updateFavoriteDisplayCount(count)
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            color = if (isSelected) PrimaryBlue else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 排序列表
            Text(
                text = "排序（长按拖拽或使用箭头）",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (favorites.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无收藏连接",
                        fontSize = 14.sp,
                        color = TextTertiary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(favorites, key = { _, item -> item.id }) { index, config ->
                        FavoriteManageItem(
                            config = config,
                            index = index,
                            total = favorites.size,
                            onMoveUp = { viewModel.swapFavoriteOrder(index, index - 1) },
                            onMoveDown = { viewModel.swapFavoriteOrder(index, index + 1) },
                            onMoveTo = { from, to -> viewModel.moveFavorite(from, to) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteManageItem(
    config: ConnectionConfig,
    index: Int,
    total: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveTo: (Int, Int) -> Unit
) {
    var dragOffset by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragStartIndex by remember { mutableStateOf(-1) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LightSurface, RoundedCornerShape(14.dp))
            .then(
                if (isDragging) {
                    Modifier.border(2.dp, PrimaryBlue, RoundedCornerShape(14.dp))
                } else {
                    Modifier
                }
            )
            .pointerInput(index) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        isDragging = true
                        dragStartIndex = index
                    },
                    onDragEnd = {
                        isDragging = false
                        dragOffset = 0f
                        dragStartIndex = -1
                    },
                    onDragCancel = {
                        isDragging = false
                        dragOffset = 0f
                        dragStartIndex = -1
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount.y
                        // 当拖拽超过一个 item 高度时触发移动
                        val itemHeight = 72.dp.toPx()
                        val targetOffset = (dragOffset / itemHeight).toInt()
                        if (targetOffset != 0 && dragStartIndex >= 0) {
                            val targetIndex = (dragStartIndex + targetOffset).coerceIn(0, total - 1)
                            if (targetIndex != dragStartIndex) {
                                onMoveTo(dragStartIndex, targetIndex)
                                dragStartIndex = targetIndex
                                dragOffset = 0f
                            }
                        }
                    }
                )
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 拖拽手柄
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "拖拽排序",
            modifier = Modifier.size(20.dp),
            tint = TextTertiary
        )
        Spacer(modifier = Modifier.width(8.dp))

        GlassServerIcon(
            modifier = Modifier.size(40.dp),
            color = config.color,
            useTerminal = true,
            iconResName = config.iconResName,
            size = 40
        )
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = config.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${config.username}@${config.host}",
                fontSize = 12.sp,
                color = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 上移按钮
        IconButton(
            onClick = onMoveUp,
            enabled = index > 0,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = "上移",
                modifier = Modifier.size(18.dp),
                tint = if (index > 0) TextSecondary else TextTertiary.copy(alpha = 0.3f)
            )
        }

        // 下移按钮
        IconButton(
            onClick = onMoveDown,
            enabled = index < total - 1,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = "下移",
                modifier = Modifier.size(18.dp),
                tint = if (index < total - 1) TextSecondary else TextTertiary.copy(alpha = 0.3f)
            )
        }
    }
}
