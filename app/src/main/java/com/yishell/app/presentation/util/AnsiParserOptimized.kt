package com.yishell.app.presentation.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

object AnsiParserOptimized {

    private const val ESC = '\u001B'
    private const val BEL = '\u0007'

    // 8-color and bright-color lookup: [0-7] = standard, [8-15] = bright
    private val ANSI_COLORS = intArrayOf(
        0xFF1A1A1A.toInt(), 0xFFCC0000.toInt(), 0xFF4E9A06.toInt(), 0xFFC4A000.toInt(),
        0xFF3465A4.toInt(), 0xFF75507B.toInt(), 0xFF06989A.toInt(), 0xFFD3D7CF.toInt(),
        0xFF555753.toInt(), 0xFFEF2929.toInt(), 0xFF8AE234.toInt(), 0xFFFCE94F.toInt(),
        0xFF729FCF.toInt(), 0xFFAD7FA8.toInt(), 0xFF34E2E2.toInt(), 0xFFEEEEEC.toInt()
    )

    // Full 256-color palette: [0-15] standard/bright, [16-231] 6x6x6 cube, [232-255] grayscale
    private val COLOR_256 = IntArray(256).also { c ->
        for (i in 0..7) c[i] = ANSI_COLORS[i]
        for (i in 0..7) c[i + 8] = ANSI_COLORS[i + 8]
        val levels = intArrayOf(0, 95, 135, 175, 215, 255)
        for (i in 16..231) {
            val x = i - 16
            c[i] = (0xFF shl 24) or (levels[x / 36] shl 16) or (levels[(x / 6) % 6] shl 8) or levels[x % 6]
        }
        for (i in 232..255) {
            val g = 8 + (i - 232) * 10
            c[i] = (0xFF shl 24) or (g shl 16) or (g shl 8) or g
        }
    }

    fun parse(
        text: String,
        defaultColor: Color = Color.Unspecified,
        onUrl: ((String) -> Unit)? = null
    ): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")

        // State machine states
        val S_NORM = 0; val S_ESC = 1; val S_CSI = 2; val S_PRIV = 3; val S_OSC = 4

        // Current SGR style state
        var fg: Color? = null; var bg: Color? = null
        var bold = false; var italic = false; var underline = false
        var strike = false; var inverse = false; var hidden = false
        var fgCode: Int? = null

        // CSI parameter accumulator
        val params = mutableListOf<Int>()
        var curParam = 0; var hasParam = false

        var state = S_NORM; var segStart = 0

        // Snapshot of text segment with its associated style
        data class Seg(
            val start: Int, val end: Int,
            val fg: Color?, val bg: Color?,
            val bold: Boolean, val italic: Boolean,
            val underline: Boolean, val strike: Boolean,
            val inverse: Boolean, val hidden: Boolean
        )
        val segs = mutableListOf<Seg>()

        fun emit(end: Int) {
            if (end > segStart) {
                segs.add(Seg(segStart, end, fg, bg, bold, italic, underline, strike, inverse, hidden))
            }
            segStart = end
        }

        // Apply SGR codes to current style state
        fun processSGR(p: List<Int>) {
            var idx = 0
            while (idx < p.size) {
                when (p[idx]) {
                    0 -> { fg = null; bg = null; bold = false; italic = false; underline = false; strike = false; inverse = false; hidden = false; fgCode = null }
                    1 -> {
                        bold = true
                        if (fgCode in 30..37) {
                            fg = Color(ANSI_COLORS[fgCode!! - 30 + 8])
                        }
                    }
                    3 -> italic = true
                    4 -> underline = true
                    7 -> inverse = true
                    8 -> hidden = true
                    9 -> strike = true
                    21, 22 -> bold = false
                    23 -> italic = false
                    24 -> underline = false
                    27 -> inverse = false
                    28 -> hidden = false
                    29 -> strike = false
                    in 30..37 -> {
                        fgCode = p[idx]
                        fg = if (bold) Color(ANSI_COLORS[p[idx] - 30 + 8]) else Color(ANSI_COLORS[p[idx] - 30])
                    }
                    38 -> {
                        fgCode = null
                        // Extended foreground: 38;5;N (256-color) or 38;2;R;G;B (true color)
                        if (idx + 1 < p.size && p[idx + 1] == 5 && idx + 2 < p.size) {
                            fg = Color(COLOR_256[p[idx + 2].coerceIn(0, 255)]); idx += 2
                        } else if (idx + 1 < p.size && p[idx + 1] == 2 && idx + 4 < p.size) {
                            fg = Color((0xFF shl 24) or (p[idx + 2] shl 16) or (p[idx + 3] shl 8) or p[idx + 4]); idx += 4
                        }
                    }
                    39 -> { fg = null; fgCode = null }
                    in 40..47 -> bg = Color(ANSI_COLORS[p[idx] - 40])
                    48 -> {
                        // Extended background: 48;5;N (256-color) or 48;2;R;G;B (true color)
                        if (idx + 1 < p.size && p[idx + 1] == 5 && idx + 2 < p.size) {
                            bg = Color(COLOR_256[p[idx + 2].coerceIn(0, 255)]); idx += 2
                        } else if (idx + 1 < p.size && p[idx + 1] == 2 && idx + 4 < p.size) {
                            bg = Color((0xFF shl 24) or (p[idx + 2] shl 16) or (p[idx + 3] shl 8) or p[idx + 4]); idx += 4
                        }
                    }
                    49 -> bg = null
                    in 90..97 -> { fg = Color(ANSI_COLORS[p[idx] - 90 + 8]); fgCode = null }
                    in 100..107 -> bg = Color(ANSI_COLORS[p[idx] - 100 + 8])
                }
                idx++
            }
        }

        // Main state machine: scan text in O(n) time
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when (state) {
                S_NORM -> {
                    if (ch == ESC) { emit(i); state = S_ESC }
                    i++
                }
                S_ESC -> {
                    when (ch) {
                        '[' -> { state = S_CSI; params.clear(); curParam = 0; hasParam = false }
                        ']' -> state = S_OSC
                        else -> { segStart = i + 1; state = S_NORM }
                    }
                    i++
                }
                S_CSI -> {
                    when {
                        ch == '?' && !hasParam -> { state = S_PRIV; i++ }
                        ch.isDigit() -> { curParam = curParam * 10 + (ch - '0'); hasParam = true; i++ }
                        ch == ';' -> { params.add(curParam); curParam = 0; i++ }
                        else -> {
                            if (hasParam) params.add(curParam)
                            if (ch == 'm') processSGR(params)
                            state = S_NORM; segStart = i + 1; i++
                        }
                    }
                }
                S_PRIV -> {
                    i++
                    if (ch.isLetter()) { state = S_NORM; segStart = i }
                }
                S_OSC -> {
                    when {
                        ch == BEL -> { state = S_NORM; segStart = i + 1; i++ }
                        ch == ESC && i + 1 < text.length && text[i + 1] == '\\' -> {
                            state = S_NORM; segStart = i + 2; i += 2
                        }
                        else -> i++
                    }
                }
            }
        }

        // Discard incomplete escape sequence at end of string
        if (state != S_NORM) segStart = text.length
        emit(text.length)

        // Build AnnotatedString from collected segments
        return buildAnnotatedString {
            for (seg in segs) {
                if (seg.hidden) continue
                val content = text.substring(seg.start, seg.end)
                var effFg = seg.fg ?: seg.bg ?: if (defaultColor != Color.Unspecified) defaultColor else Color.Unspecified
                if (seg.inverse) effFg = seg.bg ?: Color(0xFF000000)
                val hasStyle = effFg != Color.Unspecified || seg.bold || seg.italic || seg.underline || seg.strike
                appendSegment(content, effFg, hasStyle, seg.bold, seg.italic, seg.underline, seg.strike, onUrl)
            }
        }
    }

    private fun AnnotatedString.Builder.appendSegment(
        text: String, color: Color, hasStyle: Boolean,
        bold: Boolean, italic: Boolean, underline: Boolean, strike: Boolean,
        onUrl: ((String) -> Unit)?
    ) {
        if (text.isEmpty()) return
        var pos = 0
        while (pos < text.length) {
            val urlStart = findUrlStart(text, pos)
            if (urlStart < 0) {
                appendStyled(text.substring(pos), color, hasStyle, bold, italic, underline, strike)
                return
            }
            if (urlStart > pos) {
                appendStyled(text.substring(pos, urlStart), color, hasStyle, bold, italic, underline, strike)
            }
            var urlEnd = urlStart
            while (urlEnd < text.length && !text[urlEnd].isWhitespace() && text[urlEnd] != '>' && text[urlEnd] != '<') urlEnd++
            val url = text.substring(urlStart, urlEnd)
            pushStringAnnotation(tag = "URL", annotation = url)
            pushStyle(SpanStyle(color = Color(0xFF06989A), textDecoration = TextDecoration.Underline))
            append(url)
            pop(); pop()
            onUrl?.invoke(url)
            pos = urlEnd
        }
    }

    private fun AnnotatedString.Builder.appendStyled(
        text: String, color: Color, hasStyle: Boolean,
        bold: Boolean, italic: Boolean, underline: Boolean, strike: Boolean
    ) {
        if (!hasStyle) { append(text); return }
        pushStyle(SpanStyle(
            color = color,
            fontWeight = if (bold) FontWeight.Bold else null,
            fontStyle = if (italic) FontStyle.Italic else null,
            textDecoration = textDecoration(underline, strike)
        ))
        append(text)
        pop()
    }

    private fun findUrlStart(text: String, start: Int): Int {
        for (i in start until text.length - 6) {
            if (text.startsWith("http://", i) || text.startsWith("https://", i)) return i
        }
        return -1
    }

    fun stripAllAnsi(text: String): String {
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            when {
                text[i] == ESC && i + 1 < text.length && text[i + 1] == '[' -> {
                    i += 2
                    while (i < text.length && !text[i].isLetter()) i++
                    if (i < text.length) i++
                }
                text[i] == ESC && i + 1 < text.length && text[i + 1] == ']' -> {
                    i += 2
                    while (i < text.length) {
                        if (text[i] == BEL) { i++; break }
                        if (text[i] == ESC && i + 1 < text.length && text[i + 1] == '\\') { i += 2; break }
                        i++
                    }
                }
                text[i] == ESC -> i = minOf(i + 2, text.length)
                text[i] == '\u009B' -> {
                    i++
                    while (i < text.length && !text[i].isLetter()) i++
                    if (i < text.length) i++
                }
                else -> { sb.append(text[i]); i++ }
            }
        }
        return sb.toString()
    }

    private fun textDecoration(underline: Boolean, strikethrough: Boolean): TextDecoration? {
        if (!underline && !strikethrough) return null
        var td = TextDecoration.None
        if (underline) td += TextDecoration.Underline
        if (strikethrough) td += TextDecoration.LineThrough
        return td
    }
}
