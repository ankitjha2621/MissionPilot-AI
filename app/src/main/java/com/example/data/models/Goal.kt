package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String, // e.g., "DSA", "Design", "Project", "Writing"
    val deadline: String,
    val priority: String, // e.g., "High", "Medium", "Low"
    val createdAt: Long = System.currentTimeMillis(),
    val beginCommitment: String = "Now"
)
