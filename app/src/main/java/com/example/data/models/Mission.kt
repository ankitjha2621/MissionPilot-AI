package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "missions")
data class Mission(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val goalId: Int,
    val title: String,
    val description: String,
    val estimatedMinutes: Int,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val checklistJson: String = "", // JSON or simple format like "Task 1:false|Task 2:true"
    val notes: String = ""
) {
    // Helper to get list of tasks from the checklist string
    fun getChecklistItems(): List<ChecklistItem> {
        if (checklistJson.isBlank()) return emptyList()
        return try {
            checklistJson.split("|").mapNotNull {
                val parts = it.split("::")
                if (parts.size >= 2) {
                    ChecklistItem(parts[0], parts[1].toBoolean())
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        // Helper to convert list of checklist items back to checklist string
        fun createChecklistJson(items: List<ChecklistItem>): String {
            return items.joinToString("|") { "${it.task}::${it.isDone}" }
        }
    }
}

data class ChecklistItem(
    val task: String,
    val isDone: Boolean = false
)
