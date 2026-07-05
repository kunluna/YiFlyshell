package com.yishell.app.presentation.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

object AnsiParser {

    // 行业标准CSI正则（基于 ansi-regex npm库）
    // 一个正则匹配所有CSI序列：ESC/C1 + 中间字节 + 参数 + 终止符
    private val CSI_SEQUENCE = Regex(
        "[\u001B\u009B]" +  // ESC 或 CSI (0x9B)
        "[\\[\\]()#;?]*" +  // 中间字节
        "(?:\\d{1,4}(?:[;:]\\d{0,4})*)?" +  // 参数（可选）
        "[\\dA-PR-TZcf-nq-uy=><~]"  // 终止符（行业标准字符集）
    )

    // SGR终止符字符 'm'
    private const val SGR_TERMINATOR = 'm'

    // OSC序列: ESC ] ... BEL/ESC\
    private val OSC_SEQUENCE = Regex(
        "\u001b\\][^\u0007\u001b]*(?:\u0007|\u001b\\\\)"
    )

    // URL检测
    private val URL_PATTERN = Regex("""https?://[^\s<>"'()]+""")

    private val ANSI_COLORS = mapOf(
        30 to Color(0xFF1A1A1A),
        31 to Color(0xFFCC0000),
        32 to Color(0xFF4E9A06),
        33 to Color(0xFFC4A000),
        34 to Color(0xFF3465A4),
        35 to Color(0xFF75507B),
        36 to Color(0xFF06989A),
        37 to Color(0xFFD3D7CF),
        90 to Color(0xFF555753),
        91 to Color(0xFFEF2929),
        92 to Color(0xFF8AE234),
        93 to Color(0xFFFCE94F),
        94 to Color(0xFF729FCF),
        95 to Color(0xFFAD7FA8),
        96 to Color(0xFF34E2E2),
        97 to Color(0xFFEEEEEC),
    )

    fun parse(
        text: String,
        defaultColor: Color = Color.Unspecified,
        onUrl: ((String) -> Unit)? = null
    ): AnnotatedString {
        // 第一步：找到所有ANSI序列的位置
        data class SeqInfo(val start: Int, val end: Int)

        val csiMatches = CSI_SEQUENCE.findAll(text).map { SeqInfo(it.range.first, it.range.last + 1) }.toMutableList()
        val oscMatches = OSC_SEQUENCE.findAll(text).map { SeqInfo(it.range.first, it.range.last + 1) }.toMutableList()

        // 合并并按位置排序
        val allMatches = (csiMatches + oscMatches).sortedBy { it.start }

        // 第二步：提取SGR颜色代码并构建输出
        val segments = mutableListOf<Pair<String, List<Int>>>()
        var currentCodes = mutableListOf<Int>()
        var lastIndex = 0

        for (match in allMatches) {
            // 添加序列之前的文本
            if (match.start > lastIndex) {
                segments.add(text.substring(lastIndex, match.start) to currentCodes.toList())
            }

            // 检查是否是SGR序列（以'm'结尾且不以'?'开头的CSI序列）
            // 注意：\x1b[?...m 是私有模式序列，不是SGR，应跳过
            val seq = text.substring(match.start, match.end)
            val isCSI = seq.startsWith("\u001b[") || seq.startsWith("\u009b")
            val isSGR = isCSI && seq.endsWith(SGR_TERMINATOR.toString()) && 
                        !seq.startsWith("\u001b[?") && !seq.startsWith("\u009b?")
            
            if (isSGR) {
                // 提取参数并更新颜色（跳过 ESC[ 或 CSI 前缀）
                val params = seq.substring(2, seq.length - 1)
                val codes = params.split(";").mapNotNull { it.toIntOrNull() }

                codes.forEach { code ->
                    when {
                        code == 0 -> currentCodes.clear()
                        code in 1..9 -> currentCodes.add(code)
                        code in ANSI_COLORS -> {
                            currentCodes.removeAll { it in 30..37 || it in 90..97 }
                            currentCodes.add(code)
                        }
                        code in 40..47 || code in 100..107 -> {
                            currentCodes.removeAll { it in 40..47 || it in 100..107 }
                            currentCodes.add(code)
                        }
                        code == 22 -> currentCodes.removeAll { it in 1..9 }
                        code == 23 -> currentCodes.removeAll { it in 1..9 }
                    }
                }
            }

            lastIndex = match.end
        }

        // 添加剩余文本
        if (lastIndex < text.length) {
            segments.add(text.substring(lastIndex) to currentCodes.toList())
        }

        return buildAnnotatedString {
            for ((content, codes) in segments) {
                val color = resolveColor(codes, defaultColor)
                val isBold = codes.any { it == 1 }
                val isItalic = codes.any { it == 3 }
                val isUnderline = codes.any { it == 4 }
                val isStrikethrough = codes.any { it == 9 }

                val urlMatches = URL_PATTERN.findAll(content).toList()
                if (urlMatches.isEmpty()) {
                    appendSegment(content, color, isBold, isItalic, isUnderline, isStrikethrough)
                } else {
                    var pos = 0
                    for (urlMatch in urlMatches) {
                        if (urlMatch.range.first > pos) {
                            appendSegment(
                                content.substring(pos, urlMatch.range.first),
                                color, isBold, isItalic, isUnderline, isStrikethrough
                            )
                        }
                        val url = urlMatch.value
                        pushStringAnnotation(tag = "URL", annotation = url)
                        pushStyle(
                            SpanStyle(
                                color = Color(0xFF06989A),
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                            )
                        )
                        append(url)
                        pop()
                        pop()
                        if (onUrl != null) {
                            onUrl(url)
                        }
                        pos = urlMatch.range.last + 1
                    }
                    if (pos < content.length) {
                        appendSegment(
                            content.substring(pos),
                            color, isBold, isItalic, isUnderline, isStrikethrough
                        )
                    }
                }
            }
        }
    }

    private fun androidx.compose.ui.text.AnnotatedString.Builder.appendSegment(
        text: String,
        color: Color,
        isBold: Boolean,
        isItalic: Boolean,
        isUnderline: Boolean,
        isStrikethrough: Boolean
    ) {
        if (color == Color.Unspecified && !isBold && !isItalic && !isUnderline && !isStrikethrough) {
            append(text)
        } else {
            pushStyle(
                SpanStyle(
                    color = if (color != Color.Unspecified) color else Color.Unspecified,
                    fontWeight = if (isBold) androidx.compose.ui.text.font.FontWeight.Bold else null,
                    fontStyle = if (isItalic) androidx.compose.ui.text.font.FontStyle.Italic else null,
                    textDecoration = buildTextDecoration(isUnderline, isStrikethrough)
                )
            )
            append(text)
            pop()
        }
    }

    private fun buildTextDecoration(
        isUnderline: Boolean,
        isStrikethrough: Boolean
    ): androidx.compose.ui.text.style.TextDecoration? {
        if (!isUnderline && !isStrikethrough) return null
        val list = mutableListOf<androidx.compose.ui.text.style.TextDecoration>()
        if (isUnderline) list.add(androidx.compose.ui.text.style.TextDecoration.Underline)
        if (isStrikethrough) list.add(androidx.compose.ui.text.style.TextDecoration.LineThrough)
        return list.reduce { acc, td -> acc + td }
    }

    fun stripAllAnsi(text: String): String {
        return OSC_SEQUENCE.replace(CSI_SEQUENCE.replace(text, ""), "")
    }

    private fun resolveColor(codes: List<Int>, defaultColor: Color): Color {
        val colorCode = codes.lastOrNull { it in 30..37 || it in 90..97 } ?: return defaultColor
        return ANSI_COLORS[colorCode] ?: defaultColor
    }
}
