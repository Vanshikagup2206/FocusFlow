package com.vanshika.focusflow.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Insert
    suspend fun insertSession(session: FocusSession)

    @Query("SELECT * FROM sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<FocusSession>>

    @Query("DELETE FROM sessions")
    suspend fun clearAllSessions()
}