package com.yishell.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.yishell.app.data.model.ConnectionConfig
import com.yishell.app.data.model.PortForwarding
import com.yishell.app.data.model.QuickCommand
import com.yishell.app.data.model.Session

@Database(
    entities = [ConnectionConfig::class, Session::class, PortForwarding::class, QuickCommand::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun connectionDao(): ConnectionDao
    abstract fun quickCommandDao(): QuickCommandDao
}
