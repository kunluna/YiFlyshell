package com.yishell.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.yishell.app.data.model.ConnectionConfig
import com.yishell.app.data.model.PortForwarding
import com.yishell.app.data.model.QuickCommand
import com.yishell.app.data.model.Session

@Database(
    entities = [ConnectionConfig::class, Session::class, PortForwarding::class, QuickCommand::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun connectionDao(): ConnectionDao
    abstract fun quickCommandDao(): QuickCommandDao

    companion object {
        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE connections ADD COLUMN customIconUri TEXT")
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE connections ADD COLUMN favoriteOrder INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE connections ADD COLUMN iconResName TEXT")
            }
        }
    }
}
