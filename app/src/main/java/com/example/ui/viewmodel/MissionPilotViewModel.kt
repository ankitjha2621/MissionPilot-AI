package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.models.ChecklistItem
import com.example.data.models.Goal
import com.example.data.models.Mission
import com.example.data.models.Reflection
import com.example.data.repository.MissionRepository
import com.example.data.api.GeminiAgentManager
import com.example.data.api.FirestoreService
import com.example.data.api.SavedMissionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AIAgentState {
    IDLE,
    PLANNING,     // 🧠 Planner Agent is breaking down goal
    EXECUTING,    // 🚀 Mission Agent is guiding execution
    RECOVERY,     // 🛟 Recovery Agent is rescuing
    REFLECTING    // 📈 Reflection Agent is auditing
}

class MissionPilotViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MissionPilotViewModel"
    private val repository: MissionRepository

    // Database flows
    val goals: StateFlow<List<Goal>>
    val missions: StateFlow<List<Mission>>
    val reflections: StateFlow<List<Reflection>>

    // UI States
    private val _activeAgent = MutableStateFlow(AIAgentState.IDLE)
    val activeAgent: StateFlow<AIAgentState> = _activeAgent.asStateFlow()

    private val _isGeneratingMissions = MutableStateFlow(false)
    val isGeneratingMissions: StateFlow<Boolean> = _isGeneratingMissions.asStateFlow()

    // Sticky / active selection states
    private val _selectedGoalId = MutableStateFlow<Int?>(null)
    val selectedGoalId: StateFlow<Int?> = _selectedGoalId.asStateFlow()

    private val _activeMissionId = MutableStateFlow<Int?>(null)
    val activeMissionId: StateFlow<Int?> = _activeMissionId.asStateFlow()

    // Stuck Mode state
    private val _rescueStrategy = MutableStateFlow<String?>(null)
    val rescueStrategy: StateFlow<String?> = _rescueStrategy.asStateFlow()

    private val _isRescuing = MutableStateFlow(false)
    val isRescuing: StateFlow<Boolean> = _isRescuing.asStateFlow()

    // Next micro-step state
    private val _nextMicroStep = MutableStateFlow<String?>(null)
    val nextMicroStep: StateFlow<String?> = _nextMicroStep.asStateFlow()

    private val _isFetchingMicroStep = MutableStateFlow(false)
    val isFetchingMicroStep: StateFlow<Boolean> = _isFetchingMicroStep.asStateFlow()

    // Proactive Briefing State
    private val _proactiveBriefing = MutableStateFlow<String?>(null)
    val proactiveBriefing: StateFlow<String?> = _proactiveBriefing.asStateFlow()

    private val _isFetchingBriefing = MutableStateFlow(false)
    val isFetchingBriefing: StateFlow<Boolean> = _isFetchingBriefing.asStateFlow()

    // Reflection screen working states
    private val _reflectionResponse = MutableStateFlow<String?>(null)
    val reflectionResponse: StateFlow<String?> = _reflectionResponse.asStateFlow()

    private val _isGeneratingReflection = MutableStateFlow(false)
    val isGeneratingReflection: StateFlow<Boolean> = _isGeneratingReflection.asStateFlow()

    // Adaptive Recovery States
    private val _difficultyLevels = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val difficultyLevels: StateFlow<Map<Int, Int>> = _difficultyLevels.asStateFlow()

    private val _reducedDifficultyTexts = MutableStateFlow<Map<Int, String>>(emptyMap())
    val reducedDifficultyTexts: StateFlow<Map<Int, String>> = _reducedDifficultyTexts.asStateFlow()

    private val _isFetchingDifficultyReduction = MutableStateFlow(false)
    val isFetchingDifficultyReduction: StateFlow<Boolean> = _isFetchingDifficultyReduction.asStateFlow()

    // Focus Mode Timer State
    private val _timerSecondsRemaining = MutableStateFlow(1500) // Default 25 mins
    val timerSecondsRemaining: StateFlow<Int> = _timerSecondsRemaining.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private var timerJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MissionRepository(
            goalDao = database.goalDao(),
            missionDao = database.missionDao(),
            reflectionDao = database.reflectionDao()
        )

        goals = repository.allGoals.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        missions = repository.allMissions.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        reflections = repository.allReflections.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        // Auto-select latest goal and mission if available
        viewModelScope.launch {
            combine(goals, missions) { goalList, missionList ->
                Pair(goalList, missionList)
            }.collect { (goalList, missionList) ->
                if (_selectedGoalId.value == null && goalList.isNotEmpty()) {
                    _selectedGoalId.value = goalList.first().id
                }
                if (_activeMissionId.value == null && missionList.isNotEmpty()) {
                    val active = missionList.find { !it.isCompleted } ?: missionList.firstOrNull()
                    if (active != null) {
                        _activeMissionId.value = active.id
                    }
                }
            }
        }

        // Proactive Briefing Auto-Updater with safe manual delay debouncing
        viewModelScope.launch {
            var lastUpdateJob: Job? = null
            combine(goals, missions, reflections) { goalList, missionList, reflectionList ->
                Triple(goalList, missionList, reflectionList)
            }.collect { (goalList, missionList, reflectionList) ->
                lastUpdateJob?.cancel()
                lastUpdateJob = launch {
                    delay(800) // Safe manual debounce delay
                    _isFetchingBriefing.value = true
                    Log.d("MissionPilot_Debug", "[PROACTIVE ENGINE] Automatically evaluating current state for proactive advice...")
                    try {
                        val brief = GeminiAgentManager.generateProactiveBriefing(goalList, missionList, reflectionList)
                        _proactiveBriefing.value = brief
                    } catch (e: Exception) {
                        Log.e("MissionPilotViewModel", "Failed to retrieve proactive briefing: ${e.message}")
                        _proactiveBriefing.value = GeminiAgentManager.getLocalProactiveBriefing(goalList, missionList, reflectionList)
                    } finally {
                        _isFetchingBriefing.value = false
                    }
                }
            }
        }
    }

    // Computed: active goal & active mission
    val activeGoal: StateFlow<Goal?> = combine(goals, selectedGoalId) { goalList, gId ->
        if (gId != null) goalList.find { it.id == gId } else goalList.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeMission: StateFlow<Mission?> = combine(missions, selectedGoalId, activeMissionId) { missionList, gId, mId ->
        val goalMissions = if (gId != null) missionList.filter { it.goalId == gId } else missionList
        val target = if (mId != null) goalMissions.find { it.id == mId } else null
        target ?: goalMissions.find { !it.isCompleted } ?: goalMissions.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Momentum score dynamic calculation
    val momentumScore: StateFlow<Int> = combine(missions, reflections) { mList, rList ->
        var score = 30 // Starting baseline momentum
        mList.forEach { mission ->
            if (mission.isCompleted) {
                score += 15 // +15 for each completed mission
                val checklist = mission.getChecklistItems()
                val completedCount = checklist.count { it.isDone }
                score += completedCount * 3 // +3 for each completed subtask
            }
        }
        score += rList.size * 10 // +10 for each recorded reflection
        if (score > 100) 100 else score // Cap at 100 for visual gaming scale
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)

    fun selectGoal(goalId: Int) {
        _selectedGoalId.value = goalId
        viewModelScope.launch {
            val goalMissions = missions.value.filter { it.goalId == goalId }
            val firstUncompleted = goalMissions.find { !it.isCompleted } ?: goalMissions.firstOrNull()
            if (firstUncompleted != null) {
                _activeMissionId.value = firstUncompleted.id
            }
        }
    }

    fun selectMission(missionId: Int) {
        _activeMissionId.value = missionId
        // Also update corresponding goal selection
        val mission = missions.value.find { it.id == missionId }
        if (mission != null) {
            _selectedGoalId.value = mission.goalId
        }
        resetTimer(mission?.estimatedMinutes ?: 25)
    }

    /**
     * Goal screen action: Create a new goal and generate missions with 🧠 Planner Agent.
     */
    fun createGoal(title: String, category: String, deadline: String, priority: String, beginCommitment: String = "7:00 PM", onFinished: () -> Unit) {
        viewModelScope.launch {
            _activeAgent.value = AIAgentState.PLANNING
            _isGeneratingMissions.value = true
            Log.d("MissionPilot_Debug", "[STAGE 2] Planner Agent started for goal: '$title' with commitment to start: '$beginCommitment'")
            try {
                val newGoal = repository.createGoalWithMissions(title, category, deadline, priority, beginCommitment)
                Log.d("MissionPilot_Debug", "[STAGE 6] Planner Agent: Storing goal and missions in local Room database.")
                
                // Explicitly retrieve the newly generated missions to set the first one as active
                val goalMissions = repository.getMissionsForGoal(newGoal.id).first()
                val firstUncompleted = goalMissions.find { !it.isCompleted } ?: goalMissions.firstOrNull()
                
                _selectedGoalId.value = newGoal.id
                if (firstUncompleted != null) {
                    _activeMissionId.value = firstUncompleted.id
                }
                
                onFinished()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create goal: ${e.message}")
            } finally {
                _isGeneratingMissions.value = false
                _activeAgent.value = AIAgentState.IDLE
            }
        }
    }

    /**
     * Checkbox toggling inside the Mission Mode Checklist.
     */
    fun toggleChecklistItem(mission: Mission, itemIndex: Int) {
        viewModelScope.launch {
            val checklist = mission.getChecklistItems().toMutableList()
            if (itemIndex in checklist.indices) {
                val current = checklist[itemIndex]
                checklist[itemIndex] = current.copy(isDone = !current.isDone)
                val updatedMission = mission.copy(
                    checklistJson = Mission.createChecklistJson(checklist)
                )
                repository.updateMission(updatedMission)
            }
        }
    }

    /**
     * Auto-save mission notes and checklist progress on screen exit.
     */
    fun saveMissionProgress(mission: Mission, notes: String) {
        viewModelScope.launch {
            val updated = mission.copy(notes = notes)
            repository.updateMission(updated)
            Log.d("MissionPilot_Debug", "[MISSION AGENT] Auto-saved progress for mission '${mission.id}' (Notes length: ${notes.length}).")
        }
    }

    /**
     * Complete current mission and log it.
     */
    fun completeMission(mission: Mission, notes: String, onFinished: () -> Unit) {
        viewModelScope.launch {
            val updated = mission.copy(
                isCompleted = true,
                completedAt = System.currentTimeMillis(),
                notes = notes
            )
            repository.updateMission(updated)
            stopTimer()
            onFinished()
        }
    }

    /**
     * 🚀 MISSION AGENT: Next smallest micro-step request.
     */
    fun getNextMicroStepForCurrentMission() {
        val mission = activeMission.value ?: return
        viewModelScope.launch {
            _activeAgent.value = AIAgentState.EXECUTING
            _isFetchingMicroStep.value = true
            _nextMicroStep.value = null
            try {
                val step = GeminiAgentManager.generateNextMicroStep(mission, mission.getChecklistItems())
                _nextMicroStep.value = step
            } catch (e: Exception) {
                _nextMicroStep.value = "Error generating micro-step. Just open your materials and do the very first task."
            } finally {
                _isFetchingMicroStep.value = false
            }
        }
    }

    fun clearMicroStep() {
        _nextMicroStep.value = null
        _activeAgent.value = AIAgentState.IDLE
    }

    /**
     * 🛟 RECOVERY AGENT: Help me, I'm stuck!
     */
    fun activateRescueMode(reason: String) {
        val mission = activeMission.value ?: return
        viewModelScope.launch {
            _activeAgent.value = AIAgentState.RECOVERY
            _isRescuing.value = true
            _rescueStrategy.value = null
            try {
                val strategy = GeminiAgentManager.generateRescueStrategy(mission, reason)
                _rescueStrategy.value = strategy
            } catch (e: Exception) {
                _rescueStrategy.value = "Error contacting Rescue Agent. Take 5 deep breaths, put your phone in another room, and write one word on your sheet."
            } finally {
                _isRescuing.value = false
            }
        }
    }

    fun clearRescue() {
        _rescueStrategy.value = null
        _activeAgent.value = AIAgentState.IDLE
    }

    /**
     * 📈 REFLECTION AGENT: Save today's reflection and plan.
     */
    fun generateAndSaveReflection(mood: String, difficulty: String, learning: String, tomorrowPlan: String, onFinished: () -> Unit) {
        val mission = activeMission.value ?: return
        viewModelScope.launch {
            _activeAgent.value = AIAgentState.REFLECTING
            _isGeneratingReflection.value = true
            _reflectionResponse.value = null
            try {
                // Get all completed missions to show details
                val allCompleted = missions.value.filter { it.isCompleted }
                val reflectionText = GeminiAgentManager.generateDailyReflection(allCompleted, mood, difficulty, learning, tomorrowPlan)
                _reflectionResponse.value = reflectionText

                val reflection = Reflection(
                    missionId = mission.id,
                    mood = mood,
                    difficulty = difficulty,
                    learning = learning,
                    tomorrowPlan = tomorrowPlan
                )
                repository.insertReflection(reflection)
            } catch (e: Exception) {
                Log.e(TAG, "Error in reflection saving: ${e.message}")
            } finally {
                _isGeneratingReflection.value = false
            }
        }
    }

    fun clearReflectionState() {
        _reflectionResponse.value = null
        _activeAgent.value = AIAgentState.IDLE
    }

    // --- Focus Timer Controls ---
    fun startTimer() {
        if (_isTimerRunning.value) return
        _isTimerRunning.value = true
        timerJob = viewModelScope.launch {
            while (_timerSecondsRemaining.value > 0 && _isTimerRunning.value) {
                delay(1000)
                _timerSecondsRemaining.value -= 1
            }
            if (_timerSecondsRemaining.value == 0) {
                _isTimerRunning.value = false
            }
        }
    }

    fun stopTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer(minutes: Int = 25) {
        stopTimer()
        _timerSecondsRemaining.value = minutes * 60
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
            _selectedGoalId.value = null
            _activeMissionId.value = null
        }
    }

    fun resetDatabase() {
        viewModelScope.launch {
            repository.deleteAllGoals()
            _selectedGoalId.value = null
            _activeMissionId.value = null
        }
    }

    fun refreshProactiveBriefing() {
        viewModelScope.launch {
            _isFetchingBriefing.value = true
            try {
                _proactiveBriefing.value = GeminiAgentManager.generateProactiveBriefing(goals.value, missions.value, reflections.value)
            } catch (e: Exception) {
                _proactiveBriefing.value = GeminiAgentManager.getLocalProactiveBriefing(goals.value, missions.value, reflections.value)
            } finally {
                _isFetchingBriefing.value = false
            }
        }
    }

    fun reduceDifficultyForActiveMission() {
        val mission = activeMission.value ?: return
        val currentLevels = _difficultyLevels.value.toMutableMap()
        val nextLevel = (currentLevels[mission.id] ?: 0) + 1
        currentLevels[mission.id] = nextLevel
        _difficultyLevels.value = currentLevels

        viewModelScope.launch {
            _isFetchingDifficultyReduction.value = true
            Log.d("MissionPilot_Debug", "[RECOVERY AGENT] Reducing difficulty level to $nextLevel for mission '${mission.title}'")
            try {
                val reducedTaskText = GeminiAgentManager.generateReducedDifficultyTask(mission.title, nextLevel)
                val currentTexts = _reducedDifficultyTexts.value.toMutableMap()
                currentTexts[mission.id] = reducedTaskText
                _reducedDifficultyTexts.value = currentTexts
                
                // Refresh the proactive briefing as well to integrate this dynamic recovery
                refreshProactiveBriefing()
            } catch (e: Exception) {
                Log.e("MissionPilotViewModel", "Failed to reduce task difficulty: ${e.message}")
            } finally {
                _isFetchingDifficultyReduction.value = false
            }
        }
    }

    /**
     * Automatically uploads/syncs state to Firestore when leaving or inactive.
     */
    fun autoSaveToFirestore(mission: Mission, notes: String) {
        val timerSec = _timerSecondsRemaining.value
        FirestoreService.saveMissionState(
            getApplication(),
            mission.id,
            mission.checklistJson,
            notes,
            timerSec
        )
    }

    /**
     * Clear the synced active state from Firestore when completed or abandoned.
     */
    fun clearFirestoreState() {
        FirestoreService.clearSavedState(getApplication())
    }

    /**
     * Resumes the active mission from Firestore synced state.
     */
    fun resumeMissionState(savedState: SavedMissionState) {
        viewModelScope.launch {
            try {
                val mission = repository.getMissionById(savedState.missionId)
                if (mission != null) {
                    val updatedMission = mission.copy(
                        checklistJson = savedState.checklistJson,
                        notes = savedState.notes
                    )
                    repository.updateMission(updatedMission)
                    
                    _selectedGoalId.value = mission.goalId
                    _activeMissionId.value = mission.id
                    _timerSecondsRemaining.value = savedState.timerSecondsRemaining
                    
                    Log.d("MissionPilot_Debug", "[MISSION AGENT] Successfully restored and resumed mission ${mission.id} from Firestore state.")
                }
            } catch (e: Exception) {
                Log.e("MissionPilotViewModel", "Failed to resume mission state: ${e.message}")
            }
        }
    }
}
