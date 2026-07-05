package com.yishell.app.presentation.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.*
import org.junit.Test

class AnsiParserTest {

    private val ESC = "\u001B"

    private fun parse(text: String, defaultColor: Color = Color.Unspecified) =
        AnsiParserOptimized.parse(text, defaultColor)

    private fun findSpan(text: String, substring: String): SpanStyle {
        val result = parse(text)
        val start = result.text.indexOf(substring)
        assertTrue("'$substring' not found in '${result.text}'", start >= 0)
        val span = result.spanStyles.find { it.start <= start && it.end >= start + substring.length }
        assertNotNull("No span for '$substring'", span)
        return span!!.item
    }

    private fun getAllSpans(text: String) = parse(text).spanStyles

    @Test
    fun testResetCode() {
        val result = parse("hello${ESC}[0mworld")
        assertEquals("helloworld", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun testBold() {
        val style = findSpan("${ESC}[1mBOLD${ESC}[22m", "BOLD")
        assertEquals(FontWeight.Bold, style.fontWeight)
    }

    @Test
    fun testItalic() {
        val style = findSpan("${ESC}[3mITALIC${ESC}[23m", "ITALIC")
        assertEquals(FontStyle.Italic, style.fontStyle)
    }

    @Test
    fun testUnderline() {
        val style = findSpan("${ESC}[4mUNDER${ESC}[24m", "UNDER")
        assertEquals(TextDecoration.Underline, style.textDecoration)
    }

    @Test
    fun testStrikethrough() {
        val style = findSpan("${ESC}[9mDEL${ESC}[29m", "DEL")
        assertEquals(TextDecoration.LineThrough, style.textDecoration)
    }

    @Test
    fun testInverse() {
        val result = parse("${ESC}[7m${ESC}[31mINV${ESC}[27m")
        assertEquals("INV", result.text)
        assertTrue(result.spanStyles.isNotEmpty())
    }

    @Test
    fun testHidden() {
        val result = parse("${ESC}[8mHID${ESC}[28m")
        assertEquals("HID", result.text)
    }

    @Test
    fun testFgColor8() {
        val style = findSpan("${ESC}[31mRED", "RED")
        assertEquals(Color(0xFFCC0000), style.color)
    }

    @Test
    fun testBgColor8() {
        val result = parse("${ESC}[41mBG")
        assertEquals("BG", result.text)
        val span = result.spanStyles.firstOrNull()
        assertNotNull(span)
    }

    @Test
    fun testFgColor256() {
        val style = findSpan("${ESC}[38;5;196mRED", "RED")
        assertNotEquals(Color.Unspecified, style.color)
        val r = ((style.color.value.toLong() shr 16) and 0xFF).toInt()
        assertTrue("Expected red-ish, got r=$r", r > 150)
    }

    @Test
    fun testFgColorTrueColor() {
        val style = findSpan("${ESC}[38;2;255;0;0mt", "t")
        assertEquals(Color(0xFFFF0000), style.color)
    }

    @Test
    fun testBgColorTrueColor() {
        val result = parse("${ESC}[48;2;0;255;0mG")
        assertEquals("G", result.text)
        val span = result.spanStyles.first()
        assertEquals(Color(0xFF00FF00), span.item.color)
    }

    @Test
    fun testOscSequence() {
        val result = "hello${ESC}]0;title\u0007world"
        val parsed = parse(result)
        assertEquals("helloworld", parsed.text)
    }

    @Test
    fun testCursorMovementIgnored() {
        val result = parse("${ESC}[2Ahello")
        assertEquals("hello", result.text)
    }

    @Test
    fun testClearScreenIgnored() {
        val result = parse("${ESC}[2Jhello")
        assertEquals("hello", result.text)
    }

    @Test
    fun testScrollIgnored() {
        val result = parse("${ESC}[1Shello")
        assertEquals("hello", result.text)
    }

    @Test
    fun testMixedTextAndAnsi() {
        val result = parse("abc${ESC}[31mdef${ESC}[0mghi")
        assertEquals("abcdefghi", result.text)
        val defSpan = findSpan("abc${ESC}[31mdef${ESC}[0mghi", "def")
        assertEquals(Color(0xFFCC0000), defSpan.color)
    }

    @Test
    fun testStripAllAnsi() {
        val stripped = AnsiParserOptimized.stripAllAnsi("hello${ESC}[31mworld${ESC}[0m")
        assertEquals("helloworld", stripped)
    }

    @Test
    fun testDefaultColor() {
        val result = parse("hello", defaultColor = Color.Gray)
        val span = result.spanStyles.firstOrNull()
        assertNotNull(span)
        assertEquals(Color.Gray, span!!.item.color)
    }

    @Test
    fun testMultipleSGRCodes() {
        val style = findSpan("${ESC}[1;31mBOLD RED", "BOLD RED")
        assertEquals(FontWeight.Bold, style.fontWeight)
        assertEquals(Color(0xFFCC0000), style.color)
    }

    @Test
    fun testBrightFgColors() {
        val style = findSpan("${ESC}[91mBRIGHT RED", "BRIGHT RED")
        assertEquals(Color(0xFFEF2929), style.color)
    }

    @Test
    fun testBrightBgColors() {
        val result = parse("${ESC}[104mBG")
        assertEquals("BG", result.text)
        assertTrue(result.spanStyles.isNotEmpty())
    }

    @Test
    fun testFgDefaultReset() {
        val result = parse("${ESC}[31mRED${ESC}[39mDEFAULT")
        assertEquals("REDDEFAULT", result.text)
        val redSpan = findSpan("${ESC}[31mRED${ESC}[39mDEFAULT", "RED")
        assertEquals(Color(0xFFCC0000), redSpan.color)
    }

    @Test
    fun testBgDefaultReset() {
        val result = parse("${ESC}[41mBG${ESC}[49mNORMAL")
        assertEquals("BGNORMAL", result.text)
    }

    @Test
    fun testEmptyText() {
        val result = parse("")
        assertEquals("", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun testPlainTextNoAnsi() {
        val result = parse("hello world")
        assertEquals("hello world", result.text)
    }

    @Test
    fun testStripOscSequence() {
        val stripped = AnsiParserOptimized.stripAllAnsi("hello${ESC}]0;title\u0007world")
        assertEquals("helloworld", stripped)
    }

    @Test
    fun testStripEscBackslash() {
        val stripped = AnsiParserOptimized.stripAllAnsi("a${ESC}]0;title${ESC}\\b")
        assertEquals("ab", stripped)
    }

    @Test
    fun test256ColorPaletteRange() {
        // Test grayscale section (232-255)
        val style = findSpan("${ESC}[38;5;232mX", "X")
        assertNotEquals(Color.Unspecified, style.color)
        // Grayscale index 232 = gray value 8
        val g = ((style.color.value.toLong() shr 8) and 0xFF).toInt()
        assertTrue("Expected gray-ish, got g=$g", g < 20)
    }

    @Test
    fun testBoldAndColorCombined() {
        val style = findSpan("${ESC}[1;34mBOLD BLUE", "BOLD BLUE")
        assertEquals(FontWeight.Bold, style.fontWeight)
        assertEquals(Color(0xFF3465A4), style.color)
    }

    @Test
    fun testUnderlineAndStrikethroughCombined() {
        val style = findSpan("${ESC}[4;9mCOMBO", "COMBO")
        val expected = TextDecoration.Underline + TextDecoration.LineThrough
        assertEquals(expected, style.textDecoration)
    }

    @Test
    fun testConsecutiveAnsiSequences() {
        val result = parse("${ESC}[31mR${ESC}[32mG${ESC}[33mY")
        assertEquals("RGY", result.text)
        assertEquals(3, result.spanStyles.size)
    }

    @Test
    fun testInvertedColors() {
        // Inverse with fg red and no bg → effective fg becomes black (default bg)
        val result = parse("${ESC}[7m${ESC}[31mTEXT${ESC}[27m")
        assertEquals("TEXT", result.text)
        assertTrue(result.spanStyles.isNotEmpty())
    }

    @Test
    fun testStripPrivateModeSequence() {
        val stripped = AnsiParserOptimized.stripAllAnsi("a${ESC}[?25hb")
        assertEquals("ab", stripped)
    }

    @Test
    fun testIncompleteSequenceAtEnd() {
        val result = parse("hello${ESC}[")
        assertEquals("hello", result.text)
    }

    @Test
    fun testOscWithEscBackslashTerminator() {
        val result = "hello${ESC}]0;title${ESC}\\world"
        val parsed = parse(result)
        assertEquals("helloworld", parsed.text)
    }
}
