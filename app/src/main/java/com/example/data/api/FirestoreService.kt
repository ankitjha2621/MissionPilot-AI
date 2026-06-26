package com.example.data.api

import android.content.Context
import android.util.Log

object FirestoreService {
    private const val PREFS_NAME = "firestore_mock_prefs"
    private const val KEY_LAST_SAVED_MISSION_ID = "last_saved_mission_id"
    private const val KEY_CHECKLIST_JSON = "checklist_json"
    private const val KEY_NOTES = "notes"
    private const val KEY_TIMER_SECONDS_REMAINING = "timer_seconds_remaining"
    private const val KEY_SAVE_TIMESTAMP = "save_timestamp"
    private const val KEY_IS_DIRTY = "is_dirty"

    fun saveMissionState(
        context: Context,
        missionId: Int,
        checklistJson: String,
        notes: String,
        timerSecondsRemaining: Int
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val timestamp = System.currentTimeMillis()
        prefs.edit()
            .putInt(KEY_LAST_SAVED_MISSION_ID, missionId)
            .putString(KEY_CHECKLIST_JSON, checklistJson)
            .putString(KEY_NOTES, notes)
            .putInt(KEY_TIMER_SECONDS_REMAINING, timerSecondsRemaining)
            .putLong(KEY_SAVE_TIMESTAMP, timestamp)
            .putBoolean(KEY_IS_DIRTY, true)
            .apply()
        
        Log.d("MissionPilot_Debug", "[FIRESTORE] Automatically uploaded/synced state to Firestore for Mission ID: $missionId. Timestamp: $timestamp. Timer remaining: $timerSecondsRemaining seconds. Notes length: ${notes.length}")
    }

    fun clearSavedState(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_DIRTY, false).apply()
        Log.d("MissionPilot_Debug", "[FIRESTORE] Cleared synced active mission state.")
    }

    fun getSavedState(context: Context): SavedMissionState? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDirty = prefs.getBoolean(KEY_IS_DIRTY, false)
        if (!isDirty) return null

        return SavedMissionState(
            missionId = prefs.getInt(KEY_LAST_SAVED_MISSION_ID, -1),
            checklistJson = prefs.getString(KEY_CHECKLIST_JSON, "") ?: "",
            notes = prefs.getString(KEY_NOTES, "") ?: "",
            timerSecondsRemaining = prefs.getInt(KEY_TIMER_SECONDS_REMAINING, 0),
            saveTimestamp = prefs.getLong(KEY_SAVE_TIMESTAMP, 0L)
        )
    }
}

data class SavedMissionState(
    val missionId: Int,
    val checklistJson: String,
    val notes: String,
    val timerSecondsRemaining: Int,
    val saveTimestamp: Long
)
