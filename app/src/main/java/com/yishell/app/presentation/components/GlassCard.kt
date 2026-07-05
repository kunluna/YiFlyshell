package com.yishell.app.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Stable
fun Modifier.glassCard(): Modifier = this
    .shadow(
        elevation = 10.dp,
        shape = RoundedCornerShape(24.dp),
        ambientColor = Color.Black.copy(alpha = 0.05f),
        spotColor = Color.Black.copy(alpha = 0.1f)
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.5f),
                Color.White.copy(alpha = 0.1f),
                Color.White.copy(alpha = 0.8f)
            )
        ),
        shape = RoundedCornerShape(24.dp)
    )
