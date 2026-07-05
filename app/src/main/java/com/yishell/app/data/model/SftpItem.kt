package com.yishell.app.data.model

data class SftpItem(
    val name: String,
    val path: String,
    val isDir: Boolean,
    val size: Long = 0,
    val modTime: Long = 0
)
