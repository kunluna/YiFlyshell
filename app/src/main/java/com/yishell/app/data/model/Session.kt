package com.yishell.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = ConnectionConfig::class,
            parentColumns = ["id"],
            childColumns = ["configId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Session(
    @PrimaryKey
    val id: String,
    val configId: String?,
    val isActive: Boolean,
    val startedAt: Long,
    val lastActivity: Long
)
