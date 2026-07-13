package com.yishell.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yishell.app.presentation.components.*
import com.yishell.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreRecentScreen(
    onBack: () -> Unit,
    onConnect: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val connections by viewModel.connections.collectAsState()
    val recentList = remember(connections) {
        viewModel.getRecentConnections()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("最近连接") },
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (recentList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无最近连接",
                        fontSize = 14.sp,
                        color = TextTertiary
                    )
                }
            } else {
                recentList.forEachIndexed { index, config ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(LightSurface, RoundedCornerShape(14.dp))
                            .clickable { onConnect(config.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassServerIcon(
                            modifier = Modifier.size(48.dp),
                            color = config.color,
                            useTerminal = true,
                            iconResName = config.iconResName,
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
                            Spacer(modifier = Modifier.height(2.dp))
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
                        Spacer(modifier = Modifier.width(4.dp))
                        GlassChevronRightIcon(
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFFCCCCCC)
                        )
                    }
                }
            }
        }
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

    return String.format(
        "%02d-%02d %02d:%02d",
        tsCal.get(java.util.Calendar.MONTH) + 1,
        tsCal.get(java.util.Calendar.DAY_OF_MONTH),
        tsCal.get(java.util.Calendar.HOUR_OF_DAY),
        tsCal.get(java.util.Calendar.MINUTE)
    )
}
