package com.yishell.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 每个连接独立的快捷命令列表。
 * 新建连接时默认插入 13 条硬编码命令。
 */
@Entity(tableName = "quick_commands")
data class QuickCommand(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val connectionId: String,
    val label: String,
    val command: String,
    val sortOrder: Int = 0
)
