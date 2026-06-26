package com.example.data.repository

import com.example.data.database.GoalDao
import com.example.data.database.MissionDao
import com.example.data.database.ReflectionDao
import com.example.data.models.Goal
import com.example.data.models.Mission
import com.example.data.models.Reflection
import com.example.data.api.GeminiAgentManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MissionRepository(
    private val goalDao: GoalDao,
    private val missionDao: MissionDao,
    private val reflectionDao: ReflectionDao
) {
    val allGoals: Flow<List<Goal>> = goalDao.getAllGoals()
    val allMissions: Flow<List<Mission>> = missionDao.getAllMissions()
    val allReflections: Flow<List<Reflection>> = reflectionDao.getAllReflections()

    fun getMissionsForGoal(goalId: Int): Flow<List<Mission>> {
        return missionDao.getMissionsForGoal(goalId)
    }

    suspend fun getGoalById(id: Int): Goal? {
        return goalDao.getGoalById(id)
    }

    suspend fun getMissionById(id: Int): Mission? {
        return missionDao.getMissionById(id)
    }

    suspend fun insertGoal(goal: Goal): Long {
        return goalDao.insertGoal(goal)
    }

    suspend fun updateGoal(goal: Goal) {
        goalDao.updateGoal(goal)
    }

    suspend fun deleteGoal(goal: Goal) {
        goalDao.deleteGoal(goal)
        missionDao.deleteMissionsForGoal(goal.id)
    }

    suspend fun updateMission(mission: Mission) {
        missionDao.updateMission(mission)
    }

    suspend fun insertReflection(reflection: Reflection): Long {
        return reflectionDao.insertReflection(reflection)
    }

    suspend fun getReflectionForMission(missionId: Int): Reflection? {
        return reflectionDao.getReflectionForMission(missionId)
    }

    /**
     * Agentic creation flow:
     * 1. Inserts the Goal into database to get its auto-generated ID.
     * 2. Calls GeminiAgentManager to plan missions.
     * 3. Inserts those missions in Room.
     */
    suspend fun createGoalWithMissions(title: String, category: String, deadline: String, priority: String, beginCommitment: String = "7:00 PM"): Goal {
        val tempGoal = Goal(
            title = title,
            category = category,
            deadline = deadline,
            priority = priority,
            beginCommitment = beginCommitment
        )
        val generatedId = goalDao.insertGoal(tempGoal).toInt()
        val realGoal = tempGoal.copy(id = generatedId)

        // Call Gemini to plan
        val plannedMissions = GeminiAgentManager.generateMissions(realGoal)
        missionDao.insertMissions(plannedMissions)

        return realGoal
    }

    suspend fun deleteAllGoals() {
        goalDao.deleteAllGoals()
    }
}
