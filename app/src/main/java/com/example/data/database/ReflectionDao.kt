package com.example.data.database

import androidx.room.*
import com.example.data.models.Reflection
import kotlinx.coroutines.flow.Flow

@Dao
interface ReflectionDao {
    @Query("SELECT * FROM reflections ORDER BY createdAt DESC")
    fun getAllReflections(): Flow<List<Reflection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReflection(reflection: Reflection): Long

    @Query("SELECT * FROM reflections WHERE missionId = :missionId LIMIT 1")
    suspend fun getReflectionForMission(missionId: Int): Reflection?
}
