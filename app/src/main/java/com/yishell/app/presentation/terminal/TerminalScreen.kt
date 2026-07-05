package com.yishell.app.presentation.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import org.json.JSONArray
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import com.yishell.app.presentation.util.AnsiParserOptimized
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yishell.app.data.model.PortForwarding
import com.yishell.app.data.model.PortForwardingType
import com.yishell.app.data.local.TerminalColorScheme
import com.yishell.app.presentation.theme.*

import com.yishell.app.presentation.util.FormatUtils
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Switch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.PaddingValues

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    connectionId: String,
    onBack: () -> Unit,
    onSftp: () -> Unit = {},
    onMonitor: () -> Unit = {},
    viewModel: TerminalViewModel = hiltViewModel()
) {
    var inputText by remember { mutableStateOf("") }
    var showQuickCommandsBar by remember { mutableStateOf(false) }
    var showKeyboard by remember { mutableStateOf(false) }
    var showKeyboardFab by remember { mutableStateOf(true) }
    var showExitDialog by remember { mutableStateOf(false) }
    val terminalOutput by viewModel.terminalOutput.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val keyboardLayout by viewModel.keyboardLayout.collectAsState()
    val scrollState = rememberScrollState()
    val portForwardings by viewModel.portForwardings.collectAsState()
    val showPortForwardDialog by viewModel.showPortForwardDialog.collectAsState()

    val terminalScheme by viewModel.terminalColorScheme.collectAsState()
    val themeColors = remember(terminalScheme) { TerminalThemes.forScheme(terminalScheme) }
    val connectionName by viewModel.connectionName.collectAsState()
    val quickCommands by viewModel.quickCommands.collectAsState()
    var showQuickCommandEditor by remember { mutableStateOf(false) }
    // P1-1：sessions/currentSessionIndex 随假多会话 tab UI 一并停用，待真多 session 实现后恢复。
    // P2-3：待确认的大段粘贴内容，非空时弹确认对话框。
    val pendingPaste by viewModel.pendingPaste.collectAsState()

    // ========== 双光标选择模式状态 ==========
    // 选择模式：正常=false，长按终端区域后进入选择模式
    var selectionMode by remember { mutableStateOf(false) }
    // 光标A（起点）和光标B（终点）的字符偏移
    var selAnchorA by remember { mutableStateOf<Int?>(null) }
    var selAnchorB by remember { mutableStateOf<Int?>(null) }
    // 当前正在拖动的光标（A 或 B）
    var draggingHandle by remember { mutableStateOf<Char?>(null) }
    // 文本布局结果，用于 offset↔像素坐标 转换
    var realLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    // 选择模式下冻结自动滚动
    val copyFeedback by viewModel.copyFeedback.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(copyFeedback) {
        copyFeedback?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.consumeCopyFeedback()
        }
    }

    LaunchedEffect(terminalOutput) {
        if (!selectionMode) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            // 紧凑顶栏：内容高度 46dp，16sp 标题，图标按钮保持 46dp 触摸区
            // 关键：windowInsetsPadding 让 Row 内容避开系统状态栏，
            // 同时 Surface 的深色背景仍延伸到状态栏区域（沉浸式）。
            // 之前用自定义 Surface 绕过了 M3 TopAppBar 的 inset 机制，导致内容画到状态栏上。
            Surface(
                color = TerminalBackground,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .height(46.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showExitDialog = true },
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val statusColor = when (connectionState) {
                            is ConnectionState.Connected -> Green500
                            is ConnectionState.Connecting -> Color(0xFFFBBF24)
                            is ConnectionState.Disconnected -> Danger
                            is ConnectionState.Error -> Danger
                            is ConnectionState.HostKeyPending -> Color(0xFFFBBF24)
                        }
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(statusColor)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = when (connectionState) {
                                is ConnectionState.Connected -> "终端 - $connectionName"
                                is ConnectionState.Connecting -> "终端 - 连接中..."
                                is ConnectionState.Disconnected -> "终端"
                                is ConnectionState.Error -> "终端 - 错误"
                                is ConnectionState.HostKeyPending -> "终端 - 待确认主机"
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp)
                        )
                    }
                    if (connectionState is ConnectionState.Connected) {
                        IconButton(
                            onClick = onMonitor,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                contentDescription = "监控",
                                tint = Cyan500,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = onSftp,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "文件管理",
                                tint = Green500,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        var showActionMenu by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showActionMenu = true },
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多操作",
                                tint = TerminalForeground
                            )
                        }
                        DropdownMenu(
                            expanded = showActionMenu,
                            onDismissRequest = { showActionMenu = false },
                            modifier = Modifier.background(DarkSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("清屏", color = TerminalForeground) },
                                onClick = { showActionMenu = false; viewModel.clearOutput() },
                                leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Cyan500, modifier = Modifier.size(18.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("端口转发", color = TerminalForeground) },
                                onClick = { showActionMenu = false; viewModel.showPortForwardDialog() },
                                leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Cyan500, modifier = Modifier.size(18.dp)) }
                            )
                            HorizontalDivider(color = DarkSurfaceVariant)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("显示快捷键盘浮钮", color = TerminalForeground)
                                        Spacer(Modifier.width(8.dp))
                                        Switch(
                                            checked = showKeyboardFab,
                                            onCheckedChange = { showKeyboardFab = it }
                                        )
                                    }
                                },
                                onClick = { showKeyboardFab = !showKeyboardFab }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("显示快捷命令栏", color = TerminalForeground)
                                        Spacer(Modifier.width(8.dp))
                                        Switch(
                                            checked = showQuickCommandsBar,
                                            onCheckedChange = { showQuickCommandsBar = it }
                                        )
                                    }
                                },
                                onClick = { showQuickCommandsBar = !showQuickCommandsBar }
                            )
                            DropdownMenuItem(
                                text = { Text("编辑快捷命令", color = TerminalForeground) },
                                onClick = { showQuickCommandEditor = true }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        QuickCommandEditorSheet(
            visible = showQuickCommandEditor,
            onDismiss = { showQuickCommandEditor = false },
            commands = quickCommands,
            onAdd = { label, cmd -> viewModel.addQuickCommand(label, cmd) },
            onDelete = { cmd -> viewModel.deleteQuickCommand(cmd) },
            onReorder = { list -> viewModel.reorderQuickCommands(list) }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .background(TerminalBackground)
        ) {
            // P1-1：已移除假的"多会话"tab 栏。
            // 原 addSession 只创建 UI 状态而不开新 SSH session，所有 tab 共享同一 shell，
            // 切换 tab 误导用户以为切换了会话。真正的多 session 需在 SshManager 上 openSession
            // 多路复用 + 每个 session 独立输出 buffer，待后续迭代实现。

            // ============================================================
            // 终端输出区域 + 双光标选择模式 + 浮动快捷键盘
            // ============================================================
            //
            // 第一性原理：终端输出是流式文本，选择是临时模式。
            // - 正常模式：Text 显示输出，URL 可点击，自动滚动到底
            // - 选择模式：长按触发，冻结滚动 + 快照文本，显示双光标（A/B），拖动调整区间
            // - 选中区域用半透明蓝色覆盖高亮
            // - 底部浮出操作栏：复制选中 / 全选 / 取消
            //
            // 对抗性审查修复：
            // 1. 不用 TextMeasurer 独立 measure（与 Text 实际布局宽度不一致）
            //    改用 Text + onTextLayout 获取真实 TextLayoutResult
            // 2. 高亮/光标不用独立 Canvas（与滚动偏移不一致）
            //    改用 Modifier.drawWithContent 在文本层内绘制
            // 3. 选择模式期间快照 annotatedOutput，防止新输出改变偏移
            // 4. 选择模式用 Text 替代 ClickableText，避免手势冲突

            val annotatedOutput = remember(terminalOutput) {
                AnsiParserOptimized.parse(terminalOutput, themeColors.foreground)
            }
            val uriHandler = LocalUriHandler.current
            val textStyle = TextStyle(color = themeColors.foreground, fontSize = 14.sp, fontFamily = FontFamily.Monospace)

            // 选择模式快照：进入选择模式时冻结当前文本
            val selectionSnapshot = remember { mutableStateOf<AnnotatedString?>(null) }
            val displayText = if (selectionMode) selectionSnapshot.value ?: annotatedOutput else annotatedOutput

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // 内层：终端输出滚动区
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .background(themeColors.background)
                        .verticalScroll(scrollState)
                ) {
                val currentLayout = realLayoutResult

                if (selectionMode) {
                    // 选择模式：用 Text + drawWithContent + pointerInput
                    Text(
                        text = displayText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(displayText) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentLayout?.let { layout ->
                                            val charOffset = layout.getOffsetForPosition(offset)
                                            if (selAnchorA == null) {
                                                selAnchorA = charOffset
                                                selAnchorB = charOffset
                                                draggingHandle = 'B'
                                            } else {
                                                val a = selAnchorA!!
                                                val b = selAnchorB!!
                                                val distA = kotlin.math.abs(charOffset - a)
                                                val distB = kotlin.math.abs(charOffset - b)
                                                if (distA <= distB) {
                                                    selAnchorA = charOffset
                                                    draggingHandle = 'A'
                                                } else {
                                                    selAnchorB = charOffset
                                                    draggingHandle = 'B'
                                                }
                                            }
                                        }
                                    },
                                    onDragEnd = { draggingHandle = null },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        currentLayout?.let { layout ->
                                            val charOffset = layout.getOffsetForPosition(change.position)
                                            when (draggingHandle) {
                                                'A' -> selAnchorA = charOffset
                                                'B' -> selAnchorB = charOffset
                                            }
                                        }
                                    }
                                )
                            }
                            .drawWithContent {
                                drawContent()
                                // 在文本内容之上绘制选择高亮和光标
                                val layout = currentLayout ?: return@drawWithContent
                                if (selAnchorA != null && selAnchorB != null) {
                                    val start = minOf(selAnchorA!!, selAnchorB!!)
                                    val end = maxOf(selAnchorA!!, selAnchorB!!)
                                    val handleColor = Color(0xFF4D8DFF)
                                    val handleRadius = 8.dp.toPx()

                                    if (end > start) {
                                        // 高亮选中区域
                                        val startLine = layout.getLineForOffset(start)
                                        val endLine = layout.getLineForOffset(end)
                                        for (line in startLine..endLine) {
                                            val lineLeft = if (line == startLine) {
                                                layout.getHorizontalPosition(start, false)
                                            } else {
                                                layout.getLineLeft(line)
                                            }
                                            val lineRight = if (line == endLine) {
                                                layout.getHorizontalPosition(end, true)
                                            } else {
                                                layout.getLineRight(line)
                                            }
                                            val lineTop = layout.getLineTop(line)
                                            val lineBottom = layout.getLineBottom(line)
                                            drawRect(
                                                color = handleColor.copy(alpha = 0.25f),
                                                topLeft = Offset(lineLeft, lineTop),
                                                size = androidx.compose.ui.geometry.Size(lineRight - lineLeft, lineBottom - lineTop)
                                            )
                                        }
                                    }

                                    // 光标 A（起点）- 顶部圆点 + 竖线
                                    val aPos = selAnchorA!!
                                    val aLine = layout.getLineForOffset(aPos)
                                    val aX = layout.getHorizontalPosition(aPos, false)
                                    drawLine(
                                        color = handleColor,
                                        start = Offset(aX, layout.getLineTop(aLine)),
                                        end = Offset(aX, layout.getLineBottom(aLine)),
                                        strokeWidth = 2.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                    drawCircle(handleColor, handleRadius, Offset(aX, layout.getLineTop(aLine)))

                                    // 光标 B（终点）- 底部圆点 + 竖线
                                    val bPos = selAnchorB!!
                                    val bLine = layout.getLineForOffset(bPos)
                                    val bX = layout.getHorizontalPosition(bPos, true)
                                    drawLine(
                                        color = handleColor,
                                        start = Offset(bX, layout.getLineTop(bLine)),
                                        end = Offset(bX, layout.getLineBottom(bLine)),
                                        strokeWidth = 2.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                    drawCircle(handleColor, handleRadius, Offset(bX, layout.getLineBottom(bLine)))
                                }
                            },
                        style = textStyle,
                        onTextLayout = { realLayoutResult = it }
                    )
                } else {
                    // 正常模式：Text + pointerInput 处理 URL 点击 + 长按进入选择模式
                    // 不用 ClickableText —— 它内部 onClick 会消费事件导致 onLongPress 永不触发
                    Text(
                        text = annotatedOutput,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(annotatedOutput) {
                                detectTapGestures(
                                    onLongPress = { offset ->
                                        realLayoutResult?.let { layout ->
                                            val charOffset = layout.getOffsetForPosition(offset)
                                            // 快照当前文本，防止选择期间新输出改变偏移
                                            selectionSnapshot.value = annotatedOutput
                                            selectionMode = true
                                            selAnchorA = charOffset
                                            selAnchorB = charOffset
                                            draggingHandle = null
                                        }
                                    },
                                    onTap = { offset ->
                                        realLayoutResult?.let { layout ->
                                            val charOffset = layout.getOffsetForPosition(offset)
                                            annotatedOutput.getStringAnnotations(tag = "URL", start = charOffset, end = charOffset)
                                                .firstOrNull()?.let { annotation ->
                                                    uriHandler.openUri(annotation.item)
                                                }
                                        }
                                    }
                                )
                            },
                        style = textStyle,
                        onTextLayout = { realLayoutResult = it }
                    )
                }
                } // end 内层Box（滚动区）

                // 浮动快捷键盘按钮（可开关，键盘打开时隐藏——由面板内收起按钮替代）
                if (showKeyboardFab && !showKeyboard && !selectionMode && connectionState is ConnectionState.Connected) {
                    SmallFloatingActionButton(
                        onClick = { showKeyboard = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp),
                        containerColor = DarkSurfaceVariant,
                        contentColor = Green500
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "快捷键盘",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 浮动快捷键盘面板
                if (showKeyboard && !selectionMode) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(8.dp),
                        color = DarkSurface,
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 快捷键盘按键区（横向滚动）
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                            // 从配置中读取键盘布局，若失败则使用默认分组布局
                            val buttons = remember(keyboardLayout) {
                                try {
                                    val jsonArray = JSONArray(keyboardLayout)
                                    (0 until jsonArray.length()).map { jsonArray.getString(it) }
                                } catch (e: Exception) {
                                    listOf(
                                        // 导航组
                                        "←", "↑", "→", "↓", "PgUp", "PgDn",
                                        // 编辑组
                                        "Esc", "Tab", "⏎ Enter", "␣ Space",
                                        // 修饰组
                                        "Ctrl", "Alt",
                                        // 历史组
                                        "历史↑", "历史↓",
                                        // 符号组
                                        ";", "/", "|", "-", "_", "~", "."
                                    )
                                }
                            }

                            // 按类别分组渲染，组间插入细竖线分隔
                            val navSet = setOf("←", "↑", "→", "↓", "PgUp", "PgDn")
                            val editSet = setOf("Esc", "Tab", "⏎ Enter", "␣ Space")
                            val modSet = setOf("Ctrl", "Alt")
                            val histSet = setOf("历史↑", "历史↓")
                            val symSet = setOf(";", "/", "|", "-", "_", "~", ".")

                            val grouped = buttons.groupBy {
                                when (it) {
                                    in navSet -> "nav"
                                    in editSet -> "edit"
                                    in modSet -> "mod"
                                    in histSet -> "hist"
                                    in symSet -> "sym"
                                    else -> "other"
                                }
                            }
                            val groupOrder = listOf("nav", "edit", "mod", "hist", "sym", "other")
                            var isFirstGroup = true

                            groupOrder.forEach { groupKey ->
                                val groupItems = grouped[groupKey] ?: return@forEach
                                if (groupItems.isEmpty()) return@forEach

                                if (!isFirstGroup) {
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 2.dp)
                                            .width(1.dp)
                                            .height(20.dp)
                                            .background(Color.LightGray.copy(alpha = 0.3f))
                                    )
                                }
                                isFirstGroup = false

                                groupItems.forEach { button ->
                                    when (button) {
                                        "Ctrl" -> { val ctrlActive by viewModel.ctrlActive.collectAsState(); KeyboardToggleButton("Ctrl", ctrlActive) { viewModel.toggleCtrl() } }
                                        "Alt" -> { val altActive by viewModel.altActive.collectAsState(); KeyboardToggleButton("Alt", altActive) { viewModel.toggleAlt() } }
                                        "历史↑" -> KeyboardButton("历史↑") { inputText = viewModel.historyUp() ?: inputText }
                                        "历史↓" -> KeyboardButton("历史↓") { inputText = viewModel.historyDown() ?: inputText }
                                        "⏎ Enter" -> KeyboardIconButton(Icons.AutoMirrored.Filled.KeyboardReturn) { viewModel.sendKey(13) }
                                        "␣ Space" -> KeyboardIconButton(Icons.Default.SpaceBar) { viewModel.sendKey(32) }
                                        "←" -> KeyboardButton("←") { viewModel.sendEscapeSequence("[D") }
                                        "↑" -> KeyboardButton("↑") { viewModel.sendEscapeSequence("[A") }
                                        "→" -> KeyboardButton("→") { viewModel.sendEscapeSequence("[C") }
                                        "↓" -> KeyboardButton("↓") { viewModel.sendEscapeSequence("[B") }
                                        "Esc" -> KeyboardButton("Esc") { viewModel.sendKey(27) }
                                        "Tab" -> KeyboardButton("Tab") { viewModel.sendKey(9) }
                                        ";" -> KeyboardButton(";") { viewModel.sendKey(59) }
                                        "/" -> KeyboardButton("/") { viewModel.sendKey(47) }
                                        "|" -> KeyboardButton("|") { viewModel.sendKey(124) }
                                        "-" -> KeyboardButton("-") { viewModel.sendKey(45) }
                                        "_" -> KeyboardButton("_") { viewModel.sendKey(95) }
                                        "~" -> KeyboardButton("~") { viewModel.sendKey(126) }
                                        "." -> KeyboardButton(".") { viewModel.sendKey(46) }
                                        "PgUp" -> KeyboardButton("PgUp") { viewModel.sendEscapeSequence("[5~") }
                                        "PgDn" -> KeyboardButton("PgDn") { viewModel.sendEscapeSequence("[6~") }
                                    }
                                }
                            }
                        } // end 按键区 Row（横向滚动）

                            // 收起键盘按钮
                            IconButton(
                                onClick = { showKeyboard = false },
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "收起键盘",
                                    tint = TerminalForeground,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } // end 外层 Row
                    } // end Surface
                }
            }
            if (selectionMode) {
                Surface(
                    color = DarkSurface,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
 TextButton(onClick = { selectionMode = false; selAnchorA = null; selAnchorB = null }) {
                            Text("取消", color = TerminalForeground)
                        }
                        Row {
                            TextButton(onClick = {
                                // 全选
                                selAnchorA = 0
                                selAnchorB = displayText.length
                            }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "全选", tint = Cyan500, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("全选", color = Cyan500)
                            }
                            Spacer(Modifier.width(8.dp))
                            TextButton(
                                onClick = {
                                    val s = minOf(selAnchorA ?: 0, selAnchorB ?: 0)
                                    val e = maxOf(selAnchorA ?: 0, selAnchorB ?: 0)
                                    if (e > s) {
                                        val selectedText = displayText.subSequence(s, e).text
                                        viewModel.copyPlainText(selectedText)
                                    }
                                    selectionMode = false
                                    selAnchorA = null
                                    selAnchorB = null
                                },
                                enabled = (selAnchorA != null && selAnchorB != null &&
                                    minOf(selAnchorA!!, selAnchorB!!) < maxOf(selAnchorA!!, selAnchorB!!))
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "复制选中", tint = Green500, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("复制选中", color = Green500)
                            }
                        }
                    }
                }
            }

            // 快捷命令栏：受三点菜单开关控制，不常驻
            if (showQuickCommandsBar && connectionState is ConnectionState.Connected) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface)
                        .padding(vertical = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(8.dp))
                    if (quickCommands.isEmpty()) {
                        Text("暂无快捷命令", color = TerminalForeground.copy(alpha = 0.5f), fontSize = 12.sp)
                    } else {
                        quickCommands.forEach { qc ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = DarkSurfaceVariant,
                                modifier = Modifier.clickable {
                                    viewModel.sendCommand(qc.command)
                                    inputText = ""
                                }
                            ) {
                                Text(
                                    text = qc.label,
                                    color = Green500,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .background(themeColors.foreground.copy(alpha = 0.1f)),
                    textStyle = TextStyle(
                        color = themeColors.foreground,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    cursorBrush = SolidColor(themeColors.cursor),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.padding(8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (inputText.isEmpty()) {
                                Text(
                                    text = "输入命令...",
                                    color = themeColors.foreground.copy(alpha = 0.3f),
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotEmpty()) {
                            viewModel.sendCommand(inputText)
                            inputText = ""
                        }
                    },
                    enabled = connectionState is ConnectionState.Connected
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "发送",
                        tint = if (connectionState is ConnectionState.Connected) Green500 else TerminalForeground.copy(alpha = 0.3f)
                    )
                }
            }

            // 底部异常警告条：仅断连时显示，正常不占空间
            if (connectionState is ConnectionState.Disconnected) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Danger.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Danger)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "连接已断开",
                            color = Danger,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else if (connectionState is ConnectionState.Error) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Danger.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Danger)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "连接错误",
                            color = Danger,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }

    // Port Forwarding Dialog
    if (showPortForwardDialog) {
        PortForwardingDialog(
            portForwardings = portForwardings,
            onAdd = { viewModel.addPortForward(it) },
            onRemove = { viewModel.removePortForward(it) },
            onDismiss = { viewModel.dismissPortForwardDialog() }
        )
    }

    // Exit Confirmation Dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("确认退出", color = TerminalForeground) },
            containerColor = DarkSurface,
            text = { Text("确定要断开连接并退出终端吗？", color = TerminalForeground) },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    viewModel.disconnect()
                    onBack()
                }) {
                    Text("确定", color = Green500)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("取消", color = TerminalForeground)
                }
            }
        )
    }

    // 安全修复（P0-1）：主机密钥确认对话框
    // 首次连接未知主机或指纹变更时，必须让用户显式确认服务器指纹。
    val pendingHostKey = connectionState as? ConnectionState.HostKeyPending
    if (pendingHostKey != null) {
        AlertDialog(
            onDismissRequest = { viewModel.rejectHostKey() },
            title = {
                Text(
                    if (pendingHostKey.isMismatch) "⚠ 主机指纹已变更" else "确认主机指纹",
                    color = if (pendingHostKey.isMismatch) Danger else TerminalForeground
                )
            },
            containerColor = DarkSurface,
            text = {
                Column {
                    if (pendingHostKey.isMismatch) {
                        Text(
                            "警告：该主机的指纹与之前记录的不一致！\n" +
                                "这可能是服务器重装系统所致，但也可能有人正在实施中间人攻击。\n" +
                                "如非预期，请勿确认。",
                            color = Danger,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "主机: ${pendingHostKey.hostname}:${pendingHostKey.port}",
                            color = TerminalForeground, fontSize = 13.sp
                        )
                        Text(
                            "算法: ${pendingHostKey.algorithm}",
                            color = TerminalForeground, fontSize = 13.sp
                        )
                        Text(
                            "原指纹: ${pendingHostKey.storedFingerprint}",
                            color = TerminalForeground, fontSize = 12.sp
                        )
                        Text(
                            "新指纹: ${pendingHostKey.fingerprint}",
                            color = Green500, fontSize = 12.sp
                        )
                    } else {
                        Text(
                            "首次连接该主机，请确认服务器指纹是否正确。\n" +
                                "你可对比服务器管理员提供的指纹，不一致请拒绝。",
                            color = TerminalForeground,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "主机: ${pendingHostKey.hostname}:${pendingHostKey.port}",
                            color = TerminalForeground, fontSize = 13.sp
                        )
                        Text(
                            "算法: ${pendingHostKey.algorithm}",
                            color = TerminalForeground, fontSize = 13.sp
                        )
                        Text(
                            "指纹: ${pendingHostKey.fingerprint}",
                            color = Green500, fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmHostKey() }) {
                    Text(
                        if (pendingHostKey.isMismatch) "我确认变更" else "确认并连接",
                        color = Green500
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.rejectHostKey() }) {
                    Text("拒绝", color = Danger)
                }
            }
        )
    }

    // P2-3：大段粘贴确认对话框。超过阈值的剪贴板内容不直接发送，先让用户预览确认。
    pendingPaste?.let { pasteText ->
        val preview = if (pasteText.length > 300) pasteText.take(300) + "\n…（共 ${pasteText.length} 字符）" else pasteText
        AlertDialog(
            onDismissRequest = { viewModel.cancelPaste() },
            title = { Text("确认粘贴？") },
            text = {
                Column {
                    Text(
                        "即将粘贴 ${pasteText.length} 字符到终端，大段文本可能触发意外行为。",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        preview,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmPaste() }) {
                    Text("粘贴", color = PrimaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelPaste() }) {
                    Text("取消", color = Danger)
                }
            }
        )
    }
}

@Composable
fun KeyboardButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .width(48.dp)
            .lightGlassButton()
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSurfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Green500,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun KeyboardIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .width(48.dp)
            .lightGlassButton()
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSurfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Green500,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun KeyboardToggleButton(label: String, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .width(48.dp)
            .lightGlassButton()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) Cyan500 else DarkSurfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isActive) Color.Black else Green500,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun PortForwardingDialog(
    portForwardings: List<PortForwarding>,
    onAdd: (PortForwarding) -> Unit,
    onRemove: (PortForwarding) -> Unit,
    onDismiss: () -> Unit
) {
    var type by remember { mutableStateOf(PortForwardingType.LOCAL) }
    var localPort by remember { mutableStateOf("") }
    var remoteHost by remember { mutableStateOf("127.0.0.1") }
    var remotePort by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("端口转发", color = TerminalForeground) },
        containerColor = DarkSurface,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Active forwards list
                if (portForwardings.isNotEmpty()) {
                    Text(
                        "活跃的转发规则",
                        color = Green500,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    portForwardings.forEach { pf ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${if (pf.type == PortForwardingType.LOCAL) "本地" else "远程"}",
                                    color = Cyan500,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "${pf.localPort} → ${pf.remoteHost}:${pf.remotePort}",
                                    color = TerminalForeground,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            IconButton(
                                onClick = { onRemove(pf) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "删除",
                                    tint = AnsiRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = DarkSurfaceVariant)
                }

                // Add new forwarding
                Text(
                    "添加转发规则",
                    color = Green500,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == PortForwardingType.LOCAL,
                        onClick = { type = PortForwardingType.LOCAL },
                        label = { Text("本地转发") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Green500.copy(alpha = 0.2f),
                            selectedLabelColor = Green500
                        )
                    )
                    FilterChip(
                        selected = type == PortForwardingType.REMOTE,
                        onClick = { type = PortForwardingType.REMOTE },
                        label = { Text("远程转发") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Cyan500.copy(alpha = 0.2f),
                            selectedLabelColor = Cyan500
                        )
                    )
                }

                OutlinedTextField(
                    value = localPort,
                    onValueChange = { localPort = it.filter { c -> c.isDigit() } },
                    label = { Text(if (type == PortForwardingType.LOCAL) "本地端口" else "远程端口") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(color = TerminalForeground, fontFamily = FontFamily.Monospace)
                )

                OutlinedTextField(
                    value = remoteHost,
                    onValueChange = { remoteHost = it },
                    label = { Text("目标主机") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(color = TerminalForeground, fontFamily = FontFamily.Monospace)
                )

                OutlinedTextField(
                    value = remotePort,
                    onValueChange = { remotePort = it.filter { c -> c.isDigit() } },
                    label = { Text("目标端口") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(color = TerminalForeground, fontFamily = FontFamily.Monospace)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val lp = localPort.toIntOrNull()
                    val rp = remotePort.toIntOrNull()
                    if (lp != null && rp != null && lp in 1..65535 && rp in 1..65535) {
                        onAdd(
                            PortForwarding(
                                type = type,
                                localPort = lp,
                                remoteHost = remoteHost.ifBlank { "127.0.0.1" },
                                remotePort = rp
                            )
                        )
                        localPort = ""
                        remotePort = ""
                    }
                }
            ) {
                Text("添加", color = Green500)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = TerminalForeground)
            }
        }
    )
}