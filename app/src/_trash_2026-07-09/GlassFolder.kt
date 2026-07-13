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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassFolder(
    size: Dp = 64.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()

        val shadowPath = androidx.compose.ui.graphics.Path().apply {
            addRoundRect(
                RoundRect(
                    left = w * 0.08f,
                    top = h * 0.3f + h * 0.05f,
                    right = w * 0.92f,
                    bottom = h * 0.95f,
                    cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
                )
            )
        }
        drawPath(
            path = shadowPath,
            brush = Brush.radialGradient(
                colors = listOf(Color.Black.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(w / 2f, h * 0.7f),
                radius = w * 0.5f
            )
        )

        val headerPath = androidx.compose.ui.graphics.Path().apply {
            addRoundRect(
                RoundRect(
                    left = w * 0.08f,
                    top = h * 0.2f,
                    right = w * 0.45f,
                    bottom = h * 0.32f,
                    cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
                )
            )
        }
        drawPath(
            path = headerPath,
            color = Color(0xFFCFE4FF).copy(alpha = 0.5f)
        )

        val bodyPath = androidx.compose.ui.graphics.Path().apply {
            addRoundRect(
                RoundRect(
                    left = w * 0.08f,
                    top = h * 0.28f,
                    right = w * 0.92f,
                    bottom = h * 0.92f,
                    cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
                )
            )
        }
        drawPath(
            path = bodyPath,
            color = Color(0xFFCFE4FF).copy(alpha = 0.4f)
        )

        val highlightPath = androidx.compose.ui.graphics.Path().apply {
            addRoundRect(
                RoundRect(
                    left = w * 0.1f,
                    top = h * 0.3f,
                    right = w * 0.9f,
                    bottom = h * 0.5f,
                    cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
                )
            )
        }
        drawPath(
            path = highlightPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.0f)),
                startY = h * 0.3f,
                endY = h * 0.5f
            )
        )
    }
}
