package com.vanshika.focusflow.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val durationInSeconds: Long,
    val date: String,
    val distractions: Int = 0
)