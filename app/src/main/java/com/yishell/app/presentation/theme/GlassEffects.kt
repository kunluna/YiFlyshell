package com.yishell.app.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

// 玻璃效果开关：由 MainActivity 根据设置提供，玻璃修饰符读取它决定是否启用玻璃
val LocalGlassEffect = compositionLocalOf { true }

// Blue acrylic glass (server cubes, folders) - VisionOS style
@Composable
fun Modifier.blueAcrylicGlass(): Modifier {
    if (!LocalGlassEffect.current) {
        return this.background(MaterialTheme.colorScheme.surface)
    }
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    return this
        .shadow(
            elevation = 8.dp,
            shape = RoundedCornerShape(24.dp),
            ambientColor = Color.Black.copy(alpha = 0.08f),
            spotColor = Color.Black.copy(alpha = 0.08f)
        )
        .clip(RoundedCornerShape(24.dp))
        .then(
            if (isLight) {
                background(FolderBlue.copy(alpha = 0.4f))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                FolderBlue.copy(alpha = 0.5f),
                                FolderBlue.copy(alpha = 0.1f),
                                FolderBlue.copy(alpha = 0.5f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            } else {
                background(DarkGlassBackground)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(DarkGlassBorder, DarkGlassBorder.copy(alpha = 0.2f), DarkGlassBorder)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            }
        )
}

// White glass card (monitor cards) - frosted glass
@Composable
fun Modifier.whiteGlassCard(): Modifier {
    if (!LocalGlassEffect.current) {
        return this.background(MaterialTheme.colorScheme.surface)
    }
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    return this
        .shadow(
            elevation = 8.dp,
            shape = RoundedCornerShape(20.dp),
            ambientColor = Color.Black.copy(alpha = 0.08f),
            spotColor = Color.Black.copy(alpha = 0.08f)
        )
        .clip(RoundedCornerShape(20.dp))
        .then(
            if (isLight) {
                background(Color.White.copy(alpha = 0.92f))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.5f),
                                Color.White.copy(alpha = 0.1f),
                                Color.White.copy(alpha = 0.8f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
            } else {
                background(DarkGlassBackground)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                DarkGlassBorder.copy(alpha = 0.6f),
                                DarkGlassBorder.copy(alpha = 0.1f),
                                DarkGlassBorder.copy(alpha = 0.6f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
            }
        )
}

// Light glass button (Ctrl, Alt, Tab, Esc keys)
@Composable
fun Modifier.lightGlassButton(): Modifier {
    if (!LocalGlassEffect.current) {
        return this.background(MaterialTheme.colorScheme.surfaceVariant)
    }
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    return this
        .shadow(
            elevation = 2.dp,
            shape = RoundedCornerShape(12.dp),
            ambientColor = Color.Black.copy(alpha = 0.08f),
            spotColor = Color.Black.copy(alpha = 0.08f)
        )
        .clip(RoundedCornerShape(12.dp))
        .then(
            if (isLight) {
                background(Color.White.copy(alpha = 0.82f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
            } else {
                background(DarkGlassBackground)
                    .border(
                        width = 1.dp,
                        color = DarkGlassBorder.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
            }
        )
}
