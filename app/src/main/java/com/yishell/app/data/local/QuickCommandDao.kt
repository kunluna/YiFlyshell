package com.yishell.app.data.local

import androidx.room.*
import com.yishell.app.data.model.QuickCommand
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickCommandDao {
    @Query("SELECT * FROM quick_commands WHERE connectionId = :connectionId ORDER BY sortOrder ASC")
    fun getByConnectionId(connectionId: String): Flow<List<QuickCommand>>

    @Query("SELECT * FROM quick_commands WHERE connectionId = :connectionId ORDER BY sortOrder ASC")
    suspend fun getByConnectionIdSync(connectionId: String): List<QuickCommand>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(command: QuickCommand): Long

    @Update
    suspend fun update(command: QuickCommand)

    @Delete
    suspend fun delete(command: QuickCommand)

    @Query("DELETE FROM quick_commands WHERE connectionId = :connectionId")
    suspend fun deleteByConnectionId(connectionId: String)
}
