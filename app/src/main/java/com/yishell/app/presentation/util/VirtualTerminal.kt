package com.yishell.app.presentation.util

class VirtualTerminal(private var width: Int = 80, private val maxLines: Int = 500) {

    data class Cell(
        val char: Char,
        val fgSgr: String = "",
        val bgSgr: String = "",
        val bold: Boolean = false,
        val italic: Boolean = false,
        val underline: Boolean = false,
        val strikethrough: Boolean = false,
        val inverse: Boolean = false,
        val hidden: Boolean = false
    )

    private val buffer = mutableListOf<MutableList<Cell>>()
    private var cursorRow = 0
    private var cursorCol = 0

    private var currentFgSgr: String = ""
    private var currentBgSgr: String = ""
    private var bold = false
    private var italic = false
    private var underline = false
    private var strikethrough = false
    private var inverse = false
    private var hidden = false

    init {
        buffer.add(mutableListOf())
    }

    fun process(input: String): String {
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            when {
                ch == '\u001b' -> {
                    i = processEscapeSequence(input, i + 1)
                }
                ch == '\r' -> {
                    cursorCol = 0
                    i++
                }
                ch == '\n' -> {
                    cursorRow++
                    ensureRowExists(cursorRow)
                    i++
                }
                ch == '\b' -> {
                    if (cursorCol > 0) {
                        cursorCol--
                        // 删除光标处的字符
                        if (cursorRow < buffer.size) {
                            val line = buffer[cursorRow]
                            if (cursorCol < line.size) {
                                line.removeAt(cursorCol)
                            }
                        }
                    }
                    i++
                }
                ch == '\t' -> {
                    cursorCol = ((cursorCol / 8) + 1) * 8
                    if (cursorCol >= width) cursorCol = width - 1
                    i++
                }
                ch == '\u0007' -> {
                    i++
                }
                ch == '\u0000' -> {
                    // 忽略 NUL 字符
                    i++
                }
                else -> {
                    ensureRowExists(cursorRow)
                    val line = buffer[cursorRow]
                    while (line.size < cursorCol) line.add(Cell(' '))
                    val cell = Cell(
                        char = ch,
                        fgSgr = currentFgSgr,
                        bgSgr = currentBgSgr,
                        bold = bold,
                        italic = italic,
                        underline = underline,
                        strikethrough = strikethrough,
                        inverse = inverse,
                        hidden = hidden
                    )
                    if (cursorCol < line.size) {
                        line[cursorCol] = cell
                    } else {
                        line.add(cell)
                    }
                    cursorCol++
                    if (cursorCol >= width) {
                        cursorCol = 0
                        cursorRow++
                        ensureRowExists(cursorRow)
                    }
                    i++
                }
            }
        }
        trimBuffer()
        return getOutput()
    }

    fun reset() {
        buffer.clear()
        buffer.add(mutableListOf())
        cursorRow = 0
        cursorCol = 0
        currentFgSgr = ""
        currentBgSgr = ""
        bold = false
        italic = false
        underline = false
        strikethrough = false
        inverse = false
        hidden = false
    }

    fun getCurrentFgColor(): Int? = null
    fun getCurrentBgColor(): Int? = null
    fun isBold(): Boolean = bold

    private fun processEscapeSequence(input: String, startPos: Int): Int {
        if (startPos >= input.length) return startPos

        var pos = startPos
        val first = input[pos]

        when (first) {
            ']' -> {
                pos++
                while (pos < input.length) {
                    if (input[pos] == '\u0007') { pos++; break }
                    if (input[pos] == '\u001b' && pos + 1 < input.length && input[pos + 1] == '\\') { pos += 2; break }
                    pos++
                }
                return pos
            }
            '[' -> {
                pos++
                if (pos >= input.length) return pos

                if (input[pos] == '?') {
                    pos++
                    while (pos < input.length && (input[pos].isDigit() || input[pos] == ';')) pos++
                    if (pos < input.length && (input[pos] == 'h' || input[pos] == 'l')) pos++
                    return pos
                }

                val params = mutableListOf<Int>()
                var paramStr = StringBuilder()

                while (pos < input.length) {
                    val c = input[pos]
                    when {
                        c.isDigit() -> {
                            paramStr.append(c)
                            pos++
                        }
                        c == ';' -> {
                            params.add(paramStr.toString().toIntOrNull() ?: 0)
                            paramStr = StringBuilder()
                            pos++
                        }
                        else -> {
                            if (paramStr.isNotEmpty()) {
                                params.add(paramStr.toString().toIntOrNull() ?: 0)
                            }
                            handleCsiSequence(c, params)
                            pos++
                            return pos
                        }
                    }
                }
                if (paramStr.isNotEmpty()) {
                    params.add(paramStr.toString().toIntOrNull() ?: 0)
                }
                return pos
            }
            'M' -> {
                pos++
                if (cursorRow > 0) {
                    buffer.removeAt(cursorRow)
                    ensureRowExists(cursorRow)
                }
                return pos
            }
            'D' -> {
                pos++
                scrollUp(1)
                return pos
            }
            else -> {
                pos++
                return pos
            }
        }
    }

    private fun handleCsiSequence(finalChar: Char, params: List<Int>) {
        when (finalChar) {
            'A' -> {
                val n = params.getOrElse(0) { 1 }.coerceAtLeast(1)
                cursorRow = (cursorRow - n).coerceAtLeast(0)
            }
            'B' -> {
                val n = params.getOrElse(0) { 1 }.coerceAtLeast(1)
                cursorRow += n
                ensureRowExists(cursorRow)
            }
            'C' -> {
                val n = params.getOrElse(0) { 1 }.coerceAtLeast(1)
                cursorCol = (cursorCol + n).coerceAtMost(width - 1)
            }
            'D' -> {
                val n = params.getOrElse(0) { 1 }.coerceAtLeast(1)
                cursorCol = (cursorCol - n).coerceAtLeast(0)
            }
            'G' -> {
                // CSI G: 光标移到指定列（1-based）
                val n = params.getOrElse(0) { 1 }.coerceAtLeast(1)
                cursorCol = (n - 1).coerceAtMost(width - 1).coerceAtLeast(0)
            }
            'H', 'f' -> {
                val row = (params.getOrElse(0) { 1 }).coerceAtLeast(1) - 1
                val col = (params.getOrElse(1) { 1 }).coerceAtLeast(1) - 1
                cursorRow = row
                cursorCol = col.coerceAtMost(width - 1)
                ensureRowExists(cursorRow)
            }
            'J' -> {
                val mode = params.getOrElse(0) { 0 }
                when (mode) {
                    0 -> {
                        ensureRowExists(cursorRow)
                        val line = buffer[cursorRow]
                        if (cursorCol < line.size) {
                            line.subList(cursorCol, line.size).clear()
                        }
                        for (r in (cursorRow + 1) until buffer.size) {
                            buffer[r].clear()
                        }
                    }
                    1 -> {
                        ensureRowExists(cursorRow)
                        val line = buffer[cursorRow]
                        if (cursorCol > 0 && cursorCol <= line.size) {
                            line.subList(0, cursorCol).clear()
                        }
                        for (r in 0 until cursorRow) {
                            buffer[r].clear()
                        }
                    }
                    2, 3 -> {
                        buffer.clear()
                        buffer.add(mutableListOf())
                        cursorRow = 0
                        cursorCol = 0
                    }
                }
            }
            'K' -> {
                val mode = params.getOrElse(0) { 0 }
                ensureRowExists(cursorRow)
                val line = buffer[cursorRow]
                when (mode) {
                    0 -> {
                        if (cursorCol < line.size) {
                            line.subList(cursorCol, line.size).clear()
                        }
                    }
                    1 -> {
                        if (cursorCol > 0) {
                            val end = cursorCol.coerceAtMost(line.size)
                            line.subList(0, end).clear()
                            cursorCol = 0
                        }
                    }
                    2 -> {
                        line.clear()
                        cursorCol = 0
                    }
                }
            }
            'S' -> {
                val n = params.getOrElse(0) { 1 }.coerceAtLeast(1)
                scrollUp(n)
            }
            'T' -> {
                val n = params.getOrElse(0) { 1 }.coerceAtLeast(1)
                scrollDown(n)
            }
            'X' -> {
                val n = params.getOrElse(0) { 1 }.coerceAtLeast(1)
                ensureRowExists(cursorRow)
                val line = buffer[cursorRow]
                while (line.size < cursorCol) line.add(Cell(' '))
                val end = (cursorCol + n).coerceAtMost(width)
                for (j in cursorCol until end.coerceAtMost(line.size)) {
                    line[j] = Cell(' ')
                }
                while (line.size < end) {
                    line.add(Cell(' '))
                }
            }
            'P' -> {
                val n = params.getOrElse(0) { 1 }.coerceAtLeast(1)
                ensureRowExists(cursorRow)
                val line = buffer[cursorRow]
                if (cursorCol < line.size) {
                    val end = (cursorCol + n).coerceAtMost(line.size)
                    line.subList(cursorCol, end).clear()
                }
            }
            'm' -> {
                var i = 0
                while (i < params.size) {
                    val code = params[i]
                    when (code) {
                        0 -> {
                            currentFgSgr = ""
                            currentBgSgr = ""
                            bold = false
                            italic = false
                            underline = false
                            strikethrough = false
                            inverse = false
                            hidden = false
                        }
                        1 -> {
                            bold = true
                            currentFgSgr = remapFgForBold(currentFgSgr)
                        }
                        2 -> bold = false
                        3 -> italic = true
                        4 -> underline = true
                        5, 6 -> {}
                        7 -> inverse = true
                        8 -> hidden = true
                        9 -> strikethrough = true
                        21 -> bold = false
                        22 -> bold = false
                        23 -> italic = false
                        24 -> underline = false
                        25 -> {}
                        27 -> inverse = false
                        28 -> hidden = false
                        29 -> strikethrough = false
                        in 30..37 -> {
                            currentFgSgr = if (bold) (code + 60).toString() else code.toString()
                        }
                        38 -> {
                            if (i + 1 < params.size && params[i + 1] == 5) {
                                if (i + 2 < params.size) {
                                    currentFgSgr = "38;5;${params[i + 2]}"
                                    i += 2
                                }
                            } else if (i + 1 < params.size && params[i + 1] == 2) {
                                if (i + 4 < params.size) {
                                    currentFgSgr = "38;2;${params[i + 2]};${params[i + 3]};${params[i + 4]}"
                                    i += 4
                                }
                            }
                        }
                        39 -> currentFgSgr = ""
                        in 40..47 -> {
                            currentBgSgr = code.toString()
                        }
                        48 -> {
                            if (i + 1 < params.size && params[i + 1] == 5) {
                                if (i + 2 < params.size) {
                                    currentBgSgr = "48;5;${params[i + 2]}"
                                    i += 2
                                }
                            } else if (i + 1 < params.size && params[i + 1] == 2) {
                                if (i + 4 < params.size) {
                                    currentBgSgr = "48;2;${params[i + 2]};${params[i + 3]};${params[i + 4]}"
                                    i += 4
                                }
                            }
                        }
                        49 -> currentBgSgr = ""
                        in 90..97 -> currentFgSgr = code.toString()
                        in 100..107 -> currentBgSgr = code.toString()
                    }
                    i++
                }
            }
        }
    }

    private fun remapFgForBold(fgSgr: String): String {
        val code = fgSgr.toIntOrNull() ?: return fgSgr
        return if (code in 30..37) (code + 60).toString() else fgSgr
    }

    private fun scrollUp(n: Int) {
        repeat(n) {
            if (buffer.isNotEmpty()) {
                buffer.removeAt(0)
            }
        }
        ensureRowExists(cursorRow)
    }

    private fun scrollDown(n: Int) {
        repeat(n) {
            buffer.add(0, mutableListOf())
        }
        while (buffer.size > maxLines) {
            buffer.removeAt(buffer.size - 1)
        }
    }

    private fun ensureRowExists(row: Int) {
        while (buffer.size <= row) {
            buffer.add(mutableListOf())
        }
    }

    private fun trimBuffer() {
        while (buffer.size > maxLines) {
            buffer.removeAt(0)
            cursorRow = (cursorRow - 1).coerceAtLeast(0)
        }
    }

    private fun buildSgrParams(cell: Cell): String {
        val params = mutableListOf<String>()
        if (cell.bold) params.add("1")
        if (cell.italic) params.add("3")
        if (cell.underline) params.add("4")
        if (cell.strikethrough) params.add("9")
        if (cell.inverse) params.add("7")
        if (cell.hidden) params.add("8")
        if (cell.fgSgr.isNotEmpty()) params.add(cell.fgSgr)
        if (cell.bgSgr.isNotEmpty()) params.add(cell.bgSgr)
        return params.joinToString(";")
    }

    private fun getOutput(): String {
        val sb = StringBuilder()
        var lastSgr = ""
        for ((rowIndex, row) in buffer.withIndex()) {
            for (cell in row) {
                val sgr = buildSgrParams(cell)
                if (sgr != lastSgr) {
                    if (sgr.isEmpty()) {
                        sb.append("\u001b[0m")
                    } else {
                        sb.append("\u001b[")
                        sb.append(sgr)
                        sb.append("m")
                    }
                    lastSgr = sgr
                }
                sb.append(cell.char)
            }
            // 仅在非最后一行添加换行，避免重复换行
            if (rowIndex < buffer.size - 1) {
                if (lastSgr.isNotEmpty()) {
                    sb.append("\u001b[0m")
                    lastSgr = ""
                }
                sb.append("\n")
            }
        }
        return sb.toString()
    }
}
