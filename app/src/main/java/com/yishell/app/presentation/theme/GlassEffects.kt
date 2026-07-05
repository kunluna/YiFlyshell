package com.yishell.app.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Blue acrylic glass (server cubes, folders) - VisionOS style
fun Modifier.blueAcrylicGlass(): Modifier = this
    .shadow(
        elevation = 8.dp,
        shape = RoundedCornerShape(24.dp),
        ambientColor = Color.Black.copy(alpha = 0.08f),
        spotColor = Color.Black.copy(alpha = 0.08f)
    )

    .clip(RoundedCornerShape(24.dp))
    .background(FolderBlue.copy(alpha = 0.4f))
    .border(width = 1.dp, brush = Brush.linearGradient(listOf(FolderBlue.copy(alpha = 0.5f), FolderBlue.copy(alpha = 0.1f), FolderBlue.copy(alpha = 0.5f))), shape = RoundedCornerShape(24.dp))

// White glass card (monitor cards) - frosted glass
fun Modifier.whiteGlassCard(): Modifier = this
    .shadow(
        elevation = 8.dp,
        shape = RoundedCornerShape(20.dp),
        ambientColor = Color.Black.copy(alpha = 0.08f),
        spotColor = Color.Black.copy(alpha = 0.08f)
    )

    .clip(RoundedCornerShape(20.dp))
    .background(Color.White.copy(alpha = 0.92f))
    .border(width = 1.dp, brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.8f))), shape = RoundedCornerShape(20.dp))

// Light glass button (Ctrl, Alt, Tab, Esc keys)
fun Modifier.lightGlassButton(): Modifier = this
    .shadow(
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        ambientColor = Color.Black.copy(alpha = 0.08f),
        spotColor = Color.Black.copy(alpha = 0.08f)
    )

    .clip(RoundedCornerShape(12.dp))
    .background(Color.White.copy(alpha = 0.82f))
    .border(width = 1.dp, color = Color.White.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
