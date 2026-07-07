package com.yishell.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yishell.app.R
import com.yishell.app.data.model.ConnectionColor
import kotlin.math.min

/* ============================================================
 * GlassIcons.kt — v7 PNG 资源版
 *
 * 核心图标（Logo / Server / 收藏 / ActionCard）使用从 design_ref.png
 * 精确抠取的 PNG 资源，保证 1:1 视觉一致。
 * 辅助小图标（Search/Settings/Clock 等）保留 Canvas 绘制。
 * ============================================================ */

private val BrandBlue = Color(0xFF4D8DFF)
private val BrandYellow = Color(0xFFFFCC00)

/** Logo 图标 — 直接用 PNG */
@Composable
fun GlassLogoIcon(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Int = 40
) {
    androidx.compose.foundation.Image(
        painter = painterResource(R.drawable.ic_logo),
        contentDescription = contentDescription,
        modifier = modifier.size(size.dp)
    )
}

/** 大 Server 图标 — PNG 资源 + 颜色映射 */
@Composable
fun GlassServerIcon(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    color: ConnectionColor = ConnectionColor.DEFAULT,
    showStar: Boolean = false,
    showPlus: Boolean = false,
    useTerminal: Boolean = false,
    customIconUri: String? = null,
    size: Int = 80
) {
    // 自定义图标优先
    if (customIconUri != null) {
        androidx.compose.foundation.Image(
            painter = coil.compose.rememberAsyncImagePainter(customIconUri),
            contentDescription = contentDescription,
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            modifier = modifier.size(size.dp)
        )
        return
    }
    val resId = when {
        showPlus -> R.drawable.ic_server_action
        useTerminal -> when (color) {
            ConnectionColor.DEFAULT, ConnectionColor.BLUE -> R.drawable.ic_server_term_blue
            ConnectionColor.GREEN -> R.drawable.ic_server_term_green
            ConnectionColor.YELLOW -> R.drawable.ic_server_term_yellow
            ConnectionColor.PURPLE -> R.drawable.ic_server_term_purple
            ConnectionColor.CYAN -> R.drawable.ic_server_term_orange
            ConnectionColor.RED -> R.drawable.ic_server_term_red
        }
        showStar -> when (color) {
            ConnectionColor.DEFAULT, ConnectionColor.BLUE -> R.drawable.ic_server_fav_blue
            ConnectionColor.GREEN -> R.drawable.ic_server_fav_green
            ConnectionColor.YELLOW -> R.drawable.ic_server_fav_yellow
            ConnectionColor.PURPLE -> R.drawable.ic_server_fav_purple
            ConnectionColor.CYAN -> R.drawable.ic_server_fav_cyan
            ConnectionColor.RED -> R.drawable.ic_server_fav_red
        }
        else -> when (color) {
            ConnectionColor.DEFAULT, ConnectionColor.BLUE -> R.drawable.ic_server_large_blue
            ConnectionColor.GREEN -> R.drawable.ic_server_large_green
            ConnectionColor.YELLOW -> R.drawable.ic_server_large_yellow
            ConnectionColor.PURPLE -> R.drawable.ic_server_large_purple
            ConnectionColor.CYAN -> R.drawable.ic_server_large_cyan
            ConnectionColor.RED -> R.drawable.ic_server_large_red
        }
    }
    androidx.compose.foundation.Image(
        painter = painterResource(resId),
        contentDescription = contentDescription,
        modifier = modifier.size(size.dp)
    )
}

/** 小 Server 图标 — PNG 资源 */
@Composable
fun GlassSmallServerIcon(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    color: ConnectionColor = ConnectionColor.DEFAULT,
    showStar: Boolean = false,
    size: Int = 48
) {
    val resId = when {
        showStar -> when (color) {
            ConnectionColor.DEFAULT, ConnectionColor.BLUE -> R.drawable.ic_server_fav_blue
            ConnectionColor.GREEN -> R.drawable.ic_server_fav_green
            ConnectionColor.YELLOW -> R.drawable.ic_server_fav_yellow
            ConnectionColor.PURPLE -> R.drawable.ic_server_fav_purple
            ConnectionColor.CYAN -> R.drawable.ic_server_fav_cyan
            ConnectionColor.RED -> R.drawable.ic_server_fav_red
        }
        else -> when (color) {
            ConnectionColor.DEFAULT, ConnectionColor.BLUE -> R.drawable.ic_server_small_blue
            ConnectionColor.GREEN -> R.drawable.ic_server_small_green
            ConnectionColor.YELLOW -> R.drawable.ic_server_small_yellow
            ConnectionColor.PURPLE -> R.drawable.ic_server_small_purple
            ConnectionColor.CYAN -> R.drawable.ic_server_small_cyan
            ConnectionColor.RED -> R.drawable.ic_server_small_red
        }
    }
    androidx.compose.foundation.Image(
        painter = painterResource(resId),
        contentDescription = contentDescription,
        modifier = modifier.size(size.dp)
    )
}

/** ActionCard 大立方体 — PNG 资源 */
@Composable
fun GlassActionCubeIcon(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Int = 96
) {
    androidx.compose.foundation.Image(
        painter = painterResource(R.drawable.ic_server_action),
        contentDescription = contentDescription,
        modifier = modifier.size(size.dp)
    )
}

/* ==================== 辅助小图标（保留 Canvas） ==================== */

@Composable
fun GlassSearchIcon(modifier: Modifier = Modifier, size: Int = 22, tint: Color = Color(0xFF1A1A1A)) {
    Canvas(modifier.size(size.dp)) {
        val ds = this.size
        val s = min(ds.width, ds.height)
        val cx = s * 0.42f; val cy = s * 0.42f; val rr = s * 0.28f
        drawCircle(color = Color.Black.copy(alpha = 0.08f), center = Offset(cx + s * 0.035f, cy + s * 0.035f), radius = rr)
        drawCircle(color = tint, center = Offset(cx, cy), radius = rr, style = Stroke(width = s * 0.095f, cap = StrokeCap.Round))
        drawArc(color = Color.White.copy(alpha = 0.50f), startAngle = 200f, sweepAngle = 85f, useCenter = false, topLeft = Offset(cx - rr, cy - rr), size = Size(rr * 2, rr * 2), style = Stroke(width = s * 0.035f, cap = StrokeCap.Round))
        drawLine(tint, Offset(cx + rr * 0.68f, cy + rr * 0.68f), Offset(cx + rr * 1.52f, cy + rr * 1.52f), s * 0.115f, StrokeCap.Round)
    }
}

@Composable
fun GlassSettingsIcon(modifier: Modifier = Modifier, size: Int = 22, tint: Color = Color(0xFF1A1A1A)) {
    Canvas(modifier.size(size.dp)) {
        val ds = this.size; val s = min(ds.width, ds.height); val cx = s / 2f; val cy = s / 2f; val r = s * 0.37f
        drawCircle(color = Color.Black.copy(alpha = 0.06f), center = Offset(cx + s * 0.035f, cy + s * 0.035f), radius = r)
        for (i in 0 until 8) {
            val angle = (360f / 8) * i - 90f; val rad = Math.toRadians(angle.toDouble())
            val cosA = kotlin.math.cos(rad).toFloat(); val sinA = kotlin.math.sin(rad).toFloat()
            drawLine(tint, Offset(cx + cosA * r, cy + sinA * r), Offset(cx + cosA * (r + s * 0.095f), cy + sinA * (r + s * 0.095f)), s * 0.125f, StrokeCap.Round)
        }
        drawCircle(color = tint, center = Offset(cx, cy), radius = r)
        drawCircle(color = Color.White, center = Offset(cx, cy), radius = r * 0.35f)
    }
}

@Composable
fun GlassClockIcon(modifier: Modifier = Modifier, size: Int = 16, tint: Color = Color(0xFF1A1A1A)) {
    Canvas(modifier.size(size.dp)) {
        val ds = this.size; val s = min(ds.width, ds.height); val cx = s / 2f; val cy = s / 2f; val r = s * 0.42f
        drawCircle(color = tint, center = Offset(cx, cy), radius = r, style = Stroke(width = s * 0.095f, cap = StrokeCap.Round))
        drawLine(tint, Offset(cx, cy), Offset(cx, cy - r * 0.48f), s * 0.075f, StrokeCap.Round)
        drawLine(tint, Offset(cx, cy), Offset(cx + r * 0.38f, cy + r * 0.04f), s * 0.062f, StrokeCap.Round)
        drawCircle(color = tint, center = Offset(cx, cy), radius = s * 0.048f)
    }
}

@Composable
fun GlassStarIcon(modifier: Modifier = Modifier, size: Int = 16, tint: Color = Color(0xFFFFCC00)) {
    Canvas(modifier.size(size.dp)) {
        val ds = this.size; val s = min(ds.width, ds.height)
        val path = Path(); val center = Offset(s / 2, s / 2); val radius = s * 0.43f; val ir = radius * 0.42f
        for (i in 0 until 10) {
            val a = (Math.PI / 5 * i) - Math.PI / 2; val r = if (i % 2 == 0) radius else ir
            val x = center.x + r * kotlin.math.cos(a).toFloat(); val y = center.y + r * kotlin.math.sin(a).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color = tint)
    }
}

@Composable
fun GlassEditIcon(size: Int = 18, tint: Color = Color(0xFF1A1A1A)) {
    Canvas(Modifier.size(size.dp)) {
        val ds = this.size; val s = min(ds.width, ds.height)
        val p = Path().apply { moveTo(s * 0.20f, s * 0.80f); lineTo(s * 0.70f, s * 0.30f); lineTo(s * 0.80f, s * 0.40f); lineTo(s * 0.30f, s * 0.90f); close() }
        drawPath(p, color = tint)
        drawPath(Path().apply { moveTo(s * 0.15f, s * 0.85f); lineTo(s * 0.20f, s * 0.80f); lineTo(s * 0.30f, s * 0.90f); lineTo(s * 0.25f, s * 0.95f); close() }, color = Color(0xFF4D8DFF))
    }
}

@Composable
fun GlassCopyIcon(size: Int = 18, tint: Color = Color(0xFF1A1A1A)) {
    Canvas(Modifier.size(size.dp)) {
        val ds = this.size; val s = min(ds.width, ds.height)
        drawRoundRect(color = tint.copy(alpha = 0.4f), topLeft = Offset(s * 0.15f, s * 0.20f), size = Size(s * 0.55f, s * 0.55f), cornerRadius = CornerRadius(s * 0.10f, s * 0.10f))
        drawRoundRect(color = tint, topLeft = Offset(s * 0.30f, s * 0.35f), size = Size(s * 0.55f, s * 0.55f), cornerRadius = CornerRadius(s * 0.10f, s * 0.10f))
    }
}

@Composable
fun GlassDeleteIcon(size: Int = 18, tint: Color = Color(0xFFEF4444)) {
    Canvas(Modifier.size(size.dp)) {
        val ds = this.size; val s = min(ds.width, ds.height)
        drawRoundRect(color = tint, topLeft = Offset(s * 0.18f, s * 0.22f), size = Size(s * 0.64f, s * 0.10f), cornerRadius = CornerRadius(s * 0.04f, s * 0.04f))
        drawRoundRect(color = tint, topLeft = Offset(s * 0.36f, s * 0.13f), size = Size(s * 0.28f, s * 0.10f), cornerRadius = CornerRadius(s * 0.04f, s * 0.04f))
        drawPath(Path().apply { moveTo(s * 0.25f, s * 0.32f); lineTo(s * 0.30f, s * 0.88f); lineTo(s * 0.70f, s * 0.88f); lineTo(s * 0.75f, s * 0.32f); close() }, color = tint)
    }
}

@Composable
fun GlassMoreVertIcon(modifier: Modifier = Modifier, size: Int = 20, tint: Color = Color(0xFF999999)) {
    Canvas(modifier.size(size.dp)) {
        val ds = this.size; val s = min(ds.width, ds.height)
        for (i in 0..2) { drawCircle(color = tint, center = Offset(s * 0.5f, s * (0.20f + i * 0.30f)), radius = s * 0.10f) }
    }
}

@Composable
fun GlassLinkOffIcon(size: Int = 18, tint: Color = Color(0xFFEF4444)) {
    Canvas(Modifier.size(size.dp)) {
        val ds = this.size; val s = min(ds.width, ds.height)
        drawPath(Path().apply { addRoundRect(RoundRect(s * 0.10f, s * 0.30f, s * 0.48f, s * 0.68f, CornerRadius(s * 0.20f, s * 0.20f))) }, color = tint, style = Stroke(width = s * 0.095f, cap = StrokeCap.Round))
        drawPath(Path().apply { addRoundRect(RoundRect(s * 0.50f, s * 0.30f, s * 0.90f, s * 0.68f, CornerRadius(s * 0.20f, s * 0.20f))) }, color = tint, style = Stroke(width = s * 0.095f, cap = StrokeCap.Round))
        drawLine(color = tint, start = Offset(s * 0.20f, s * 0.20f), end = Offset(s * 0.80f, s * 0.80f), strokeWidth = s * 0.095f, cap = StrokeCap.Round)
    }
}

@Composable
fun GlassChevronRightIcon(modifier: Modifier = Modifier, size: Int = 14, tint: Color = Color(0xFFCCCCCC)) {
    Canvas(modifier.size(size.dp)) {
        val ds = this.size; val s = min(ds.width, ds.height)
        drawPath(Path().apply { moveTo(s * 0.40f, s * 0.20f); lineTo(s * 0.70f, s * 0.50f); lineTo(s * 0.40f, s * 0.80f) }, color = tint, style = Stroke(width = s * 0.135f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}
