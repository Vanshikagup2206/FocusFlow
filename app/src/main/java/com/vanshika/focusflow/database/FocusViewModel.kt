package com.vanshika.focusflow.database

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val focusDao: FocusSessionDao = FocusDatabase.getDatabase(application).sessionDao()

    fun saveFocusSession(durationInSeconds: Long, distractions: Int = 0) {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val session = FocusSession(
            durationInSeconds = durationInSeconds,
            date = currentDate,
            distractions = distractions
        )

        viewModelScope.launch(Dispatchers.IO) {
            focusDao.insertSession(session)
        }
    }

    fun getAllSessions(): Flow<List<FocusSession>> = focusDao.getAllSessions()

    fun clearSessions() {
        viewModelScope.launch(Dispatchers.IO) {
            focusDao.clearAllSessions()
        }
    }

    fun updateSessionDuration(startTime: Long, onDurationUpdate: (Long) -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            var elapsedTime = 0L
            while (true) {
                delay(1000)
                elapsedTime = (System.currentTimeMillis() - startTime) / 1000
                withContext(Dispatchers.Main) {
                    onDurationUpdate(elapsedTime)
                }
            }
        }
    }

    fun saveSessionWithDistractions(duration: Long, distractions: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val session = FocusSession(
                    durationInSeconds = duration,
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    distractions = distractions
                )
                focusDao.insertSession(session)
                Log.d("FocusViewModel", "Session saved successfully")
            } catch (e: Exception) {
                Log.e("FocusViewModel", "Failed to save session", e)
            }
        }
    }
}