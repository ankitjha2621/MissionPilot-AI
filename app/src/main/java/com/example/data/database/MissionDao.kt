package com.example.data.database

import androidx.room.*
import com.example.data.models.Mission
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionDao {
    @Query("SELECT * FROM missions WHERE goalId = :goalId ORDER BY id ASC")
    fun getMissionsForGoal(goalId: Int): Flow<List<Mission>>

    @Query("SELECT * FROM missions ORDER BY id DESC")
    fun getAllMissions(): Flow<List<Mission>>

    @Query("SELECT * FROM missions WHERE id = :id")
    suspend fun getMissionById(id: Int): Mission?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMissions(missions: List<Mission>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMission(mission: Mission): Long

    @Update
    suspend fun updateMission(mission: Mission)

    @Query("DELETE FROM missions WHERE goalId = :goalId")
    suspend fun deleteMissionsForGoal(goalId: Int)
}
