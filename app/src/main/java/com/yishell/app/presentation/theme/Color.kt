package com.yishell.app.presentation.theme

import androidx.compose.ui.graphics.Color
import com.yishell.app.data.local.TerminalColorScheme
import com.yishell.app.data.model.ConnectionColor

fun ConnectionColor.toComposeColor(): Color = when (this) {
    ConnectionColor.DEFAULT -> ConnectionGreen
    ConnectionColor.GREEN -> ConnectionGreen
    ConnectionColor.BLUE -> ConnectionBlue
    ConnectionColor.RED -> ConnectionRed
    ConnectionColor.YELLOW -> ConnectionYellow
    ConnectionColor.PURPLE -> ConnectionPurple
    ConnectionColor.CYAN -> ConnectionCyan
}

// Terminal color scheme definitions
object TerminalThemes {
    data class Scheme(
        val background: Color,
        val foreground: Color,
        val cursor: Color,
        val selection: Color,
        val name: String
    )

    val DEFAULT = Scheme(
        background = Color(0xFF0D1117),
        foreground = Color(0xFFE6EDF3),
        cursor = Color(0xFF00E676),
        selection = Color(0x8000E676),
        name = "默认"
    )

    val SOLARIZED_DARK = Scheme(
        background = Color(0xFF002B36),
        foreground = Color(0xFF839496),
        cursor = Color(0xFF839496),
        selection = Color(0x80073642),
        name = "Solarized Dark"
    )

    val SOLARIZED_LIGHT = Scheme(
        background = Color(0xFFFDF6E3),
        foreground = Color(0xFF657B83),
        cursor = Color(0xFF657B83),
        selection = Color(0x80EEE8D5),
        name = "Solarized Light"
    )

    val MONOKAI = Scheme(
        background = Color(0xFF272822),
        foreground = Color(0xFFF8F8F2),
        cursor = Color(0xFFF8F8F0),
        selection = Color(0x8049483E),
        name = "Monokai"
    )

    val DRACULA = Scheme(
        background = Color(0xFF282A36),
        foreground = Color(0xFFF8F8F2),
        cursor = Color(0xFFF8F8F2),
        selection = Color(0x8044475A),
        name = "Dracula"
    )

    // 亮色终端配色（跟随 App 亮色主题时使用）
    val LIGHT = Scheme(
        background = Color(0xFFF7F9FC),
        foreground = Color(0xFF1F2937),
        cursor = Color(0xFF4D8DFF),
        selection = Color(0x804D8DFF),
        name = "亮色"
    )

    fun forScheme(scheme: TerminalColorScheme): Scheme = when (scheme) {
        TerminalColorScheme.AUTO -> DEFAULT
        TerminalColorScheme.DEFAULT -> DEFAULT
        TerminalColorScheme.SOLARIZED_DARK -> SOLARIZED_DARK
        TerminalColorScheme.SOLARIZED_LIGHT -> SOLARIZED_LIGHT
        TerminalColorScheme.MONOKAI -> MONOKAI
        TerminalColorScheme.DRACULA -> DRACULA
    }

    /**
     * 解析最终使用的配色：AUTO 时根据 App 主题明暗自动选择亮/暗终端配色；
     * 其余具体方案直接返回，作为手动覆盖。
     */
    fun resolve(scheme: TerminalColorScheme, isDark: Boolean): Scheme = when (scheme) {
        TerminalColorScheme.AUTO -> if (isDark) DEFAULT else LIGHT
        else -> forScheme(scheme)
    }
}

// ==================== 品牌色 ====================
val PrimaryBlue = Color(0xFF4D8DFF)
val GlowBlue = Color(0xFF6AA8FF)

// ==================== Light Theme ====================
val LightBackground = Color(0xFFF7F9FC)
val LightSurface = Color(0xFFFFFFFF)
val LightCard = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0F0F0)
val LightOnBackground = Color(0xFF1F2937)
val LightOnSurface = Color(0xFF1F2937)
val LightOnSurfaceVariant = Color(0xFF6B7280)
val LightOnPrimary = Color.White
val LightOnSecondary = Color.White
val LightOnTertiary = Color.White
val LightError = Color(0xFFEF4444)

// ==================== Dark Theme ====================
val DarkBackground = Color(0xFF0F1115)
val DarkSurface = Color(0xFF1A1D24)
val DarkCard = Color(0xFF22252E)
val DarkSurfaceVariant = Color(0xFF2D2D2D)
val DarkOnSurfaceVariant = Color(0xFFB0B0B0)
val DarkOnBackground = Color(0xFFE6EDF3)
val DarkOnSurface = Color(0xFFE6EDF3)

// ==================== 文字颜色 ====================
val TextPrimary = Color(0xFF1F2937)
val TextSecondary = Color(0xFF6B7280)
val TextHint = Color(0xFF9CA3AF)
val TextDisabled = Color(0xFFCBD5E1)
val TextTertiary = Color(0xFF888888)

// ==================== 状态颜色 ====================
val Success = Color(0xFF22C55E)
val Warning = Color(0xFFF59E0B)
val Danger = Color(0xFFEF4444)
val Info = Color(0xFF4D8DFF)
val StatusGreen = Color(0xFF22C55E)

// ==================== Glass 颜色 ====================
val GlassBackground = Color(0x2EFFFFFF)   // rgba(255,255,255,0.18)
val GlassBorder = Color(0x99FFFFFF)       // rgba(255,255,255,0.60)
val GlassHighlight = Color(0xD9FFFFFF)    // rgba(255,255,255,0.85)
val GlassShadow = Color(0x14000000)       // rgba(0,0,0,0.08)

// Dark Glass
val DarkGlassBackground = Color(0x2E1A1D24) // 透明深灰
val DarkGlassBorder = Color(0x4D6AA8FF)     // 蓝色折射边缘

// Folder Glass
val FolderBlue = Color(0xFFCFE4FF)

// ==================== 页面辅助色 ====================
val PageBackground = Color(0xFFF7F9FC)
val AppBackground = Color(0xFFF7F9FC)
val AppCardBackground = Color.White
val ShadowColor = Color.Black.copy(alpha = 0.08f)
val CardDivider = Color(0xFFEEEEEE)
val CardBorderColor = Color(0xFFE0E0E0)

// ==================== Terminal Colors ====================
val TerminalBackground = Color(0xFF0D1117)
val TerminalForeground = Color(0xFFE6EDF3)
val TerminalCursor = Color(0xFF00E676)
val TerminalSelection = Color(0x8000E676)
val TerminalButtonBg = Color(0xFFF0F8FF)

// ==================== ANSI Colors ====================
val AnsiRed = Color(0xFFF44747)
val AnsiYellow = Color(0xFFFFD54F)
val AnsiMagenta = Color(0xFFCE93D8)

// ==================== Connection Colors ====================
val ConnectionGreen = Color(0xFF22C55E)
val ConnectionBlue = Color(0xFF4D8DFF)
val ConnectionRed = Color(0xFFEF4444)
val ConnectionYellow = Color(0xFFFFCC00)
val ConnectionPurple = Color(0xFF9C27B0)
val ConnectionCyan = Color(0xFF00BCD4)

// ==================== 品牌辅助色 ====================
val BrandGreen = Color(0xFF22C55E)
val BrandYellow = Color(0xFFFFCC00)
val DesignGreen = Color(0xFF22C55E)
val DesignBlue = Color(0xFF4D8DFF)
val DesignTextPrimary = Color(0xFF1F2937)
val DesignTextSecondary = Color(0xFF6B7280)
val DesignCardDivider = Color(0xFFEEEEEE)
val DesignTerminalBg = Color(0xFFF0F8FF)
val DesignFavoriteButtonBg = Color(0xFFF0F8FF)
val FavoriteYellow = Color(0xFFFFCC00)
val Cyan500 = Color(0xFF00BCD4)
val Cyan700 = Color(0xFF0097A7)
val Green500 = Color(0xFF00E676)
