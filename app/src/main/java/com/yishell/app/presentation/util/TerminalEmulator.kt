package com.yishell.app.presentation.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

/**
 * 单层终端模拟器
 * 
 * 第一性原理：终端是一个状态机，输入字节流直接转换为带样式的文本
 * 不再分两层解析（VirtualTerminal + AnsiParserOptimized）
 * 
 * 状态：
 * - cursor: 光标位置 (row, col)
 * - buffer: 屏幕缓冲区，每个 Cell 包含字符和样式
 * - currentStyle: 当前 SGR 样式状态
 * - parserState: ANSI 解析状态
 */
class TerminalEmulator(
    private val width: Int = 80,
    private val maxLines: Int = 500
) {
    /**
     * 单元格：包含字符和完整样式信息
     */
    data class Cell(
        val char: Char = ' ',
        val fgColor: Int? = null,  // ANSI 颜色代码 0-255，null 表示默认
        val bgColor: Int? = null,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val underline: Boolean = false,
        val strikethrough: Boolean = false,
        val inverse: Boolean = false
    )

    /**
     * ANSI 解析状态
     */
    private enum class ParserState {
        NORMAL,      // 正常模式
        ESC,         // 收到 ESC (0x1B)
        CSI,         // 收到 CSI 开头 ESC[
        OSC,         // 收到 OSC 开头 ESC]
        CSI_PARAM    // 解析 CSI 参数
    }

    // 屏幕缓冲区：行列表，每行是单元格列表
    private val buffer = mutableListOf<MutableList<Cell>>()
    
    // 光标位置
    private var cursorRow = 0
    private var cursorCol = 0
    
    // 当前 SGR 状态
    private var currentFgColor: Int? = null
    private var currentBgColor: Int? = null
    private var bold = false
    private var italic = false
    private var underline = false
    private var strikethrough = false
    private var inverse = false
    
    // 解析状态
    private var parserState = ParserState.NORMAL
    private var csiParams = mutableListOf<Int>()
    private var paramBuffer = StringBuilder()
    private var csiPrivatePrefix: Char? = null  // 用于存储 CSI 私有序列前缀（如 ?）

    init {
        buffer.add(mutableListOf())
    }

    /**
     * 处理输入并直接返回 AnnotatedString
     */
    fun process(input: String): AnnotatedString {
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            i = when (parserState) {
                ParserState.NORMAL -> processNormal(ch, i)
                ParserState.ESC -> processEsc(ch, i)
                ParserState.CSI, ParserState.CSI_PARAM -> processCsi(ch, i)
                ParserState.OSC -> processOsc(ch, i)
            }
        }
        trimBuffer()
        return buildAnnotatedString()
    }

    /**
     * 正常模式：处理普通字符和控制字符
     */
    private fun processNormal(ch: Char, pos: Int): Int {
        when {
            ch == '\u001b' -> {
                parserState = ParserState.ESC
                return pos + 1
            }
            ch == '\r' -> {
                cursorCol = 0
                return pos + 1
            }
            ch == '\n' -> {
                cursorRow++
                ensureRowExists(cursorRow)
                return pos + 1
            }
            ch == '\b' -> {
                if (cursorCol > 0) {
                    cursorCol--
                    // 删除光标处字符
                    if (cursorRow < buffer.size) {
                        val line = buffer[cursorRow]
                        if (cursorCol < line.size) {
                            line.removeAt(cursorCol)
                        }
                    }
                }
                return pos + 1
            }
            ch == '\t' -> {
                val nextTab = ((cursorCol / 8) + 1) * 8
                cursorCol = minOf(nextTab, width - 1)
                return pos + 1
            }
            ch == '\u0000' || ch == '\u0007' -> {
                // 忽略 NUL 和 BEL
                return pos + 1
            }
            ch < ' ' -> {
                // 其他控制字符也忽略
                return pos + 1
            }
            else -> {
                // 可打印字符
                putChar(ch)
                return pos + 1
            }
        }
    }

    /**
     * ESC 模式：处理转义序列开头
     */
    private fun processEsc(ch: Char, pos: Int): Int {
        parserState = when (ch) {
            '[' -> ParserState.CSI
            ']' -> ParserState.OSC
            else -> {
                // 不支持的转义序列，忽略 ESC 和当前字符
                ParserState.NORMAL
            }
        }
        return pos + 1
    }

    /**
     * CSI 模式：处理 CSI 序列
     */
    private fun processCsi(ch: Char, pos: Int): Int {
        when {
            ch == '?' || ch == '>' || ch == '!' -> {
                // 私有序列前缀（如 CSI ?2004 h）
                csiPrivatePrefix = ch
                parserState = ParserState.CSI_PARAM
                return pos + 1
            }
            ch.isDigit() -> {
                parserState = ParserState.CSI_PARAM
                paramBuffer.append(ch)
                return pos + 1
            }
            ch == ';' -> {
                parserState = ParserState.CSI_PARAM
                if (paramBuffer.isNotEmpty()) {
                    csiParams.add(paramBuffer.toString().toIntOrNull() ?: 0)
                    paramBuffer.clear()
                }
                return pos + 1
            }
            ch.isLetter() || ch == '@' || ch == '`' -> {
                // CSI 终止字符
                if (paramBuffer.isNotEmpty()) {
                    csiParams.add(paramBuffer.toString().toIntOrNull() ?: 0)
                    paramBuffer.clear()
                }
                executeCsi(ch)
                csiParams.clear()
                csiPrivatePrefix = null
                parserState = ParserState.NORMAL
                return pos + 1
            }
            else -> {
                // 非法字符，终止 CSI
                csiParams.clear()
                paramBuffer.clear()
                csiPrivatePrefix = null
                parserState = ParserState.NORMAL
                return pos + 1
            }
        }
    }

    /**
     * OSC 模式：处理 OSC 序列（窗口标题等，暂时忽略）
     */
    private fun processOsc(ch: Char, pos: Int): Int {
        // OSC 序列以 BEL (0x07) 或 ST (ESC\) 结束
        if (ch == '\u0007' || ch == '\u001b') {
            if (ch == '\u001b') {
                // 可能是 ST 的开头，检查下一个字符
                // 简化处理：直接忽略
            }
            parserState = ParserState.NORMAL
        }
        return pos + 1
    }

    /**
     * 执行 CSI 命令
     */
    private fun executeCsi(cmd: Char) {
        // 处理私有序列（以 ? 开头）
        if (csiPrivatePrefix == '?') {
            executePrivateCsi(cmd)
            return
        }
        
        when (cmd) {
            'A' -> { // 光标上移
                val n = csiParams.getOrElse(0) { 1 }.coerceAtLeast(1)
                cursorRow = (cursorRow - n).coerceAtLeast(0)
            }
            'B' -> { // 光标下移
                val n = csiParams.getOrElse(0) { 1 }.coerceAtLeast(1)
                cursorRow += n
                ensureRowExists(cursorRow)
            }
            'C' -> { // 光标右移
                val n = csiParams.getOrElse(0) { 1 }.coerceAtLeast(1)
                cursorCol = (cursorCol + n).coerceAtMost(width - 1)
            }
            'D' -> { // 光标左移
                val n = csiParams.getOrElse(0) { 1 }.coerceAtLeast(1)
                cursorCol = (cursorCol - n).coerceAtLeast(0)
            }
            'G' -> { // 光标水平绝对定位
                val n = csiParams.getOrElse(0) { 1 }.coerceAtLeast(1)
                cursorCol = (n - 1).coerceAtMost(width - 1).coerceAtLeast(0)
            }
            'H', 'f' -> { // 光标定位
                val row = csiParams.getOrElse(0) { 1 }.coerceAtLeast(1) - 1
                val col = csiParams.getOrElse(1) { 1 }.coerceAtLeast(1) - 1
                cursorRow = row
                cursorCol = col.coerceAtMost(width - 1)
                ensureRowExists(cursorRow)
            }
            'J' -> { // 清屏
                val mode = csiParams.getOrElse(0) { 0 }
                when (mode) {
                    0 -> { // 从光标到屏幕末尾
                        if (cursorRow < buffer.size) {
                            buffer[cursorRow].subList(cursorCol, buffer[cursorRow].size).clear()
                        }
                        for (r in (cursorRow + 1) until buffer.size) {
                            buffer[r].clear()
                        }
                    }
                    2, 3 -> { // 清整个屏幕
                        buffer.clear()
                        buffer.add(mutableListOf())
                        cursorRow = 0
                        cursorCol = 0
                    }
                }
            }
            'K' -> { // 清行
                val mode = csiParams.getOrElse(0) { 0 }
                if (cursorRow < buffer.size) {
                    val line = buffer[cursorRow]
                    when (mode) {
                        0 -> { // 从光标到行尾
                            if (cursorCol < line.size) {
                                line.subList(cursorCol, line.size).clear()
                            }
                        }
                        1 -> { // 从行首到光标
                            if (cursorCol > 0) {
                                val end = minOf(cursorCol, line.size)
                                repeat(end) { line.removeAt(0) }
                                cursorCol = 0
                            }
                        }
                        2 -> { // 整行
                            line.clear()
                            cursorCol = 0
                        }
                    }
                }
            }
            'm' -> { // SGR 样式设置
                if (csiParams.isEmpty()) {
                    csiParams.add(0)
                }
                var i = 0
                while (i < csiParams.size) {
                    val code = csiParams[i]
                    when (code) {
                        0 -> resetStyle()
                        1 -> bold = true
                        2 -> bold = false
                        3 -> italic = true
                        4 -> underline = true
                        7 -> inverse = true
                        9 -> strikethrough = true
                        21, 22 -> bold = false
                        23 -> italic = false
                        24 -> underline = false
                        27 -> inverse = false
                        29 -> strikethrough = false
                        in 30..37 -> currentFgColor = code
                        38 -> { // 256色前景
                            if (i + 2 < csiParams.size && csiParams[i + 1] == 5) {
                                currentFgColor = csiParams[i + 2]
                                i += 2
                            }
                        }
                        39 -> currentFgColor = null
                        in 40..47 -> currentBgColor = code
                        48 -> { // 256色背景
                            if (i + 2 < csiParams.size && csiParams[i + 1] == 5) {
                                currentBgColor = csiParams[i + 2]
                                i += 2
                            }
                        }
                        49 -> currentBgColor = null
                        in 90..97 -> currentFgColor = code
                        in 100..107 -> currentBgColor = code
                    }
                    i++
                }
            }
            'S' -> { // 向上滚动
                val n = csiParams.getOrElse(0) { 1 }.coerceAtLeast(1)
                repeat(n) {
                    if (buffer.isNotEmpty()) buffer.removeAt(0)
                }
                cursorRow = cursorRow.coerceAtMost(buffer.size - 1)
            }
            'T' -> { // 向下滚动
                val n = csiParams.getOrElse(0) { 1 }.coerceAtLeast(1)
                repeat(n) {
                    buffer.add(0, mutableListOf())
                }
                trimBuffer()
            }
            'X' -> { // 删除字符（用空格替换）
                val n = csiParams.getOrElse(0) { 1 }.coerceAtLeast(1)
                if (cursorRow < buffer.size) {
                    val line = buffer[cursorRow]
                    val end = minOf(cursorCol + n, width)
                    for (j in cursorCol until end) {
                        if (j < line.size) {
                            line[j] = createCell(' ')
                        }
                    }
                }
            }
            'P' -> { // 删除字符（左移）
                val n = csiParams.getOrElse(0) { 1 }.coerceAtLeast(1)
                if (cursorRow < buffer.size) {
                    val line = buffer[cursorRow]
                    if (cursorCol < line.size) {
                        val end = minOf(cursorCol + n, line.size)
                        repeat(end - cursorCol) { 
                            if (cursorCol < line.size) line.removeAt(cursorCol) 
                        }
                    }
                }
            }
        }
    }

    /**
     * 执行私有 CSI 序列（以 ? 开头）
     * 处理如 CSI ?2004 h/l (bracketed paste mode)、CSI ?25 h/l (光标显示/隐藏) 等
     */
    private fun executePrivateCsi(cmd: Char) {
        when (cmd) {
            'h' -> { // 设置模式 (SET MODE)
                when {
                    csiParams.contains(2004) -> {
                        // Bracketed paste mode 开启 - 忽略（移动端不需要）
                    }
                    csiParams.contains(25) -> {
                        // 显示光标 - 忽略（移动端不需要显示光标）
                    }
                    csiParams.contains(7) -> {
                        // 自动换行模式 - 忽略
                    }
                    csiParams.contains(1) -> {
                        // 应用键盘模式 - 忽略
                    }
                }
            }
            'l' -> { // 重置模式 (RESET MODE)
                when {
                    csiParams.contains(2004) -> {
                        // Bracketed paste mode 关闭 - 忽略
                    }
                    csiParams.contains(25) -> {
                        // 隐藏光标 - 忽略
                    }
                    csiParams.contains(7) -> {
                        // 自动换行模式关闭 - 忽略
                    }
                    csiParams.contains(1) -> {
                        // 键盘模式重置 - 忽略
                    }
                }
            }
        }
    }

    /**
     * 在当前光标位置放置字符
     */
    private fun putChar(ch: Char) {
        ensureRowExists(cursorRow)
        val line = buffer[cursorRow]
        
        // 填充到光标位置
        while (line.size < cursorCol) {
            line.add(Cell(' '))
        }
        
        val cell = createCell(ch)
        
        if (cursorCol < line.size) {
            line[cursorCol] = cell
        } else {
            line.add(cell)
        }
        
        cursorCol++
        
        // 自动换行
        if (cursorCol >= width) {
            cursorCol = 0
            cursorRow++
            ensureRowExists(cursorRow)
        }
    }

    /**
     * 创建带当前样式的单元格
     */
    private fun createCell(ch: Char): Cell {
        return Cell(
            char = ch,
            fgColor = currentFgColor,
            bgColor = currentBgColor,
            bold = bold,
            italic = italic,
            underline = underline,
            strikethrough = strikethrough,
            inverse = inverse
        )
    }

    /**
     * 重置样式
     */
    private fun resetStyle() {
        currentFgColor = null
        currentBgColor = null
        bold = false
        italic = false
        underline = false
        strikethrough = false
        inverse = false
    }

    /**
     * 确保行存在
     */
    private fun ensureRowExists(row: Int) {
        while (buffer.size <= row) {
            buffer.add(mutableListOf())
        }
    }

    /**
     * 限制缓冲区大小
     */
    private fun trimBuffer() {
        while (buffer.size > maxLines) {
            buffer.removeAt(0)
            cursorRow = (cursorRow - 1).coerceAtLeast(0)
        }
    }

    /**
     * 获取纯文本内容（不含 ANSI 序列，用于日志记录）
     */
    fun getPlainText(): String {
        return buffer.joinToString("\n") { row ->
            row.joinToString("") { cell -> cell.char.toString() }
        }
    }

    /**
     * 构建 AnnotatedString
     */
    private fun buildAnnotatedString(): AnnotatedString {
        val builder = AnnotatedString.Builder()
        
        for ((rowIndex, row) in buffer.withIndex()) {
            var lastStyle: SpanStyle? = null
            var styleStart = 0
            
            for ((colIndex, cell) in row.withIndex()) {
                val style = cell.toSpanStyle()
                
                if (style != lastStyle) {
                    // 样式变化，推入之前的样式
                    if (lastStyle != null) {
                        builder.pop()
                    }
                    builder.pushStyle(style)
                    lastStyle = style
                }
                
                builder.append(cell.char)
            }
            
            // 弹出最后一行的样式
            if (lastStyle != null) {
                builder.pop()
            }
            
            // 添加换行（最后一行除外）
            if (rowIndex < buffer.size - 1) {
                builder.append('\n')
            }
        }
        
        return builder.toAnnotatedString()
    }

    /**
     * Cell 转换为 SpanStyle
     */
    private fun Cell.toSpanStyle(): SpanStyle {
        val fg = if (inverse) {
            bgColor?.let { ansiToColor(it) } ?: Color.White
        } else {
            fgColor?.let { ansiToColor(it) } ?: Color.Unspecified
        }
        
        val bg = if (inverse) {
            fgColor?.let { ansiToColor(it) } ?: Color.Black
        } else {
            bgColor?.let { ansiToColor(it) } ?: Color.Unspecified
        }
        
        return SpanStyle(
            color = fg,
            background = bg,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (italic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
            textDecoration = buildTextDecoration()
        )
    }

    /**
     * ANSI 颜色代码转换为 Compose Color
     */
    private fun ansiToColor(code: Int): Color {
        return when (code) {
            // 标准 8 色
            30 -> Color(0xFF000000) // Black
            31 -> Color(0xFFCD3131) // Red
            32 -> Color(0xFF0DBC79) // Green
            33 -> Color(0xFFE5E510) // Yellow
            34 -> Color(0xFF2472C8) // Blue
            35 -> Color(0xFFBC3FBC) // Magenta
            36 -> Color(0xFF11A8CD) // Cyan
            37 -> Color(0xFFE5E5E5) // White
            // 亮色
            90 -> Color(0xFF666666) // Bright Black
            91 -> Color(0xFFF14C4C) // Bright Red
            92 -> Color(0xFF23D18B) // Bright Green
            93 -> Color(0xFFF5F543) // Bright Yellow
            94 -> Color(0xFF3B8EEA) // Bright Blue
            95 -> Color(0xFFD670D6) // Bright Magenta
            96 -> Color(0xFF29B8DB) // Bright Cyan
            97 -> Color(0xFFFFFFFF) // Bright White
            // 256 色（简化处理，只支持基本色）
            in 0..255 -> {
                if (code < 16) {
                    // 使用标准色
                    ansiToColor(if (code < 8) code + 30 else code + 82)
                } else {
                    // 灰度或其他颜色，简化处理
                    Color.Gray
                }
            }
            else -> Color.Unspecified
        }
    }

    /**
     * 构建文本装饰
     */
    private fun Cell.buildTextDecoration(): TextDecoration? {
        val decorations = mutableListOf<TextDecoration>()
        if (underline) decorations.add(TextDecoration.Underline)
        if (strikethrough) decorations.add(TextDecoration.LineThrough)
        return when (decorations.size) {
            0 -> null
            1 -> decorations[0]
            else -> TextDecoration.combine(decorations)
        }
    }

    /**
     * 重置终端
     */
    fun reset() {
        buffer.clear()
        buffer.add(mutableListOf())
        cursorRow = 0
        cursorCol = 0
        resetStyle()
        parserState = ParserState.NORMAL
        csiParams.clear()
        paramBuffer.clear()
        csiPrivatePrefix = null
    }
}
