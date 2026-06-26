package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reflections")
data class Reflection(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val missionId: Int,
    val mood: String, // e.g. "Energized", "Tired", "Anxious", "Neutral"
    val difficulty: String, // e.g. "Easy", "Challenging", "Hard"
    val learning: String,
    val tomorrowPlan: String,
    val createdAt: Long = System.currentTimeMillis()
)
