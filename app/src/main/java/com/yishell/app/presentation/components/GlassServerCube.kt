package com.yishell.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yishell.app.data.model.ConnectionColor

@Composable
fun GlassServerCube(
    size: Dp = 64.dp,
    color: ConnectionColor = ConnectionColor.DEFAULT,
    modifier: Modifier = Modifier,
    showStar: Boolean = false,
    showPlusBadge: Boolean = false
) {
    val brandColor = when (color) {
        ConnectionColor.DEFAULT, ConnectionColor.BLUE -> Color(0xFF007AFF)
        ConnectionColor.GREEN -> Color(0xFF00D68F)
        ConnectionColor.YELLOW -> Color(0xFFFFCC00)
        ConnectionColor.RED -> Color(0xFFEF4444)
        ConnectionColor.PURPLE -> Color(0xFF9C27B0)
        ConnectionColor.CYAN -> Color(0xFF00BCD4)
    }

    Canvas(modifier = modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()
        val unit = w / 8f

        val topH = unit * 1.5f
        val sideW = unit * 3f
        val sideH = unit * 2.5f

        val centerX = w / 2f
        val topY = h * 0.15f

        val shadowPath = Path().apply {
            moveTo(centerX - sideW / 2, topY + topH + sideH + unit * 0.3f)
            quadraticBezierTo(
                centerX, topY + topH + sideH + unit * 0.8f,
                centerX + sideW / 2, topY + topH + sideH + unit * 0.3f
            )
        }
        drawPath(
            path = shadowPath,
            brush = Brush.radialGradient(
                colors = listOf(Color.Black.copy(alpha = 0.15f), Color.Transparent),
                center = Offset(centerX, topY + topH + sideH + unit * 0.5f),
                radius = sideW / 2
            )
        )

        val glassBasePath = Path().apply {
            moveTo(centerX - sideW / 2 - unit * 0.2f, topY + topH + sideH + unit * 0.1f)
            quadraticBezierTo(
                centerX, topY + topH + sideH + unit * 0.4f,
                centerX + sideW / 2 + unit * 0.2f, topY + topH + sideH + unit * 0.1f
            )
        }
        drawPath(
            path = glassBasePath,
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.1f)),
                center = Offset(centerX, topY + topH + sideH + unit * 0.2f),
                radius = sideW / 2
            )
        )

        val topPath = Path().apply {
            moveTo(centerX, topY)
            lineTo(centerX + sideW / 2, topY + topH / 2)
            lineTo(centerX, topY + topH)
            lineTo(centerX - sideW / 2, topY + topH / 2)
            close()
        }
        drawPath(
            path = topPath,
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.8f), Color.White.copy(alpha = 0.2f)),
                start = Offset(centerX - sideW / 2, topY),
                end = Offset(centerX + sideW / 2, topY + topH)
            )
        )

        val leftPath = Path().apply {
            moveTo(centerX - sideW / 2, topY + topH / 2)
            lineTo(centerX, topY + topH)
            lineTo(centerX, topY + topH + sideH)
            lineTo(centerX - sideW / 2, topY + topH / 2 + sideH)
            close()
        }
        drawPath(
            path = leftPath,
            color = brandColor.copy(alpha = 0.4f)
        )

        val rightPath = Path().apply {
            moveTo(centerX + sideW / 2, topY + topH / 2)
            lineTo(centerX, topY + topH)
            lineTo(centerX, topY + topH + sideH)
            lineTo(centerX + sideW / 2, topY + topH / 2 + sideH)
            close()
        }
        drawPath(
            path = rightPath,
            color = brandColor.copy(alpha = 0.3f)
        )

        val edgeColor = Color.White.copy(alpha = 0.3f)
        val edgeWidth = unit * 0.08f

        drawLine(edgeColor, Offset(centerX, topY), Offset(centerX + sideW / 2, topY + topH / 2), edgeWidth)
        drawLine(edgeColor, Offset(centerX, topY), Offset(centerX - sideW / 2, topY + topH / 2), edgeWidth)
        drawLine(edgeColor, Offset(centerX + sideW / 2, topY + topH / 2), Offset(centerX, topY + topH), edgeWidth)
        drawLine(edgeColor, Offset(centerX - sideW / 2, topY + topH / 2), Offset(centerX, topY + topH), edgeWidth)

        drawLine(edgeColor, Offset(centerX, topY + topH), Offset(centerX, topY + topH + sideH), edgeWidth)
        drawLine(edgeColor, Offset(centerX - sideW / 2, topY + topH / 2 + sideH), Offset(centerX - sideW / 2, topY + topH / 2), edgeWidth)
        drawLine(edgeColor, Offset(centerX + sideW / 2, topY + topH / 2 + sideH), Offset(centerX + sideW / 2, topY + topH / 2), edgeWidth)

        drawLine(edgeColor, Offset(centerX - sideW / 2, topY + topH / 2 + sideH), Offset(centerX, topY + topH + sideH), edgeWidth)
        drawLine(edgeColor, Offset(centerX + sideW / 2, topY + topH / 2 + sideH), Offset(centerX, topY + topH + sideH), edgeWidth)

        val textCenterX = centerX + sideW / 4
        val textCenterY = topY + topH + sideH / 2

        val symbolSize = unit * 0.8f
        val symbolColor = Color.White.copy(alpha = 0.9f)
        val strokeWidth = unit * 0.12f

        val gtX = textCenterX - symbolSize * 0.3f
        val gtY = textCenterY - symbolSize * 0.3f
        drawLine(symbolColor, Offset(gtX - symbolSize * 0.2f, gtY), Offset(gtX + symbolSize * 0.1f, gtY + symbolSize * 0.2f), strokeWidth)
        drawLine(symbolColor, Offset(gtX + symbolSize * 0.1f, gtY + symbolSize * 0.2f), Offset(gtX - symbolSize * 0.2f, gtY + symbolSize * 0.4f), strokeWidth)

        val usX = textCenterX + symbolSize * 0.15f
        val usY = textCenterY + symbolSize * 0.2f
        drawLine(symbolColor, Offset(usX - symbolSize * 0.15f, usY + symbolSize * 0.2f), Offset(usX + symbolSize * 0.15f, usY + symbolSize * 0.2f), strokeWidth)

        if (showStar) {
            val starCx = centerX + sideW / 2 + unit * 0.2f
            val starCy = topY - unit * 0.1f
            val starR = unit * 0.4f
            val starPath = Path()
            for (i in 0 until 5) {
                val outerAngle = Math.toRadians((i * 72 - 90).toDouble())
                val innerAngle = Math.toRadians((i * 72 + 36 - 90).toDouble())
                val outerX = starCx + starR * kotlin.math.cos(outerAngle).toFloat()
                val outerY = starCy + starR * kotlin.math.sin(outerAngle).toFloat()
                val innerX = starCx + starR * 0.4f * kotlin.math.cos(innerAngle).toFloat()
                val innerY = starCy + starR * 0.4f * kotlin.math.sin(innerAngle).toFloat()
                if (i == 0) starPath.moveTo(outerX, outerY) else starPath.lineTo(outerX, outerY)
                starPath.lineTo(innerX, innerY)
            }
            starPath.close()
            drawPath(starPath, color = Color(0xFFFFCC00))
        }

        if (showPlusBadge) {
            val badgeCx = centerX + sideW / 2 + unit * 0.1f
            val badgeCy = topY + topH + sideH + unit * 0.1f
            val badgeR = unit * 0.35f
            drawCircle(
                color = Color(0xFF007AFF),
                radius = badgeR,
                center = Offset(badgeCx, badgeCy)
            )
            val plusSize = badgeR * 0.6f
            drawLine(
                color = Color.White,
                start = Offset(badgeCx - plusSize, badgeCy),
                end = Offset(badgeCx + plusSize, badgeCy),
                strokeWidth = unit * 0.08f
            )
            drawLine(
                color = Color.White,
                start = Offset(badgeCx, badgeCy - plusSize),
                end = Offset(badgeCx, badgeCy + plusSize),
                strokeWidth = unit * 0.08f
            )
        }
    }
}
