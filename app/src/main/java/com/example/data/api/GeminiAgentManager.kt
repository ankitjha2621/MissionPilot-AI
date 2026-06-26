package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.models.Goal
import com.example.data.models.Mission
import com.example.data.models.ChecklistItem
import com.example.data.models.Reflection
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiAgentManager {
    private const val TAG = "GeminiAgentManager"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Data structure for Planner Agent response parsing
    data class PlannedMission(
        val title: String,
        val description: String,
        val estimatedMinutes: Int,
        val checklist: List<String>
    )

    private val listMyType = Types.newParameterizedType(List::class.java, PlannedMission::class.java)
    private val jsonAdapter = moshi.adapter<List<PlannedMission>>(listMyType)

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    private fun isKeyValid(key: String): Boolean {
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY" && !key.startsWith("placeholder", ignoreCase = true)
    }

    /**
     * 🧠 PLANNER AGENT
     * Takes a goal and returns 3-4 structured missions.
     */
    suspend fun generateMissions(goal: Goal): List<Mission> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!isKeyValid(apiKey)) {
            Log.w(TAG, "[STAGE 3 (FALLBACK)] Gemini API key is not valid/configured. Activating local autonomous fallback engine.")
            Log.d("MissionPilot_Debug", "[STAGE 3 (FALLBACK)] Gemini API key is not valid/configured. Activating local autonomous fallback engine.")
            val mockMissions = getMockMissions(goal.id, goal.title, goal.category)
            Log.d("MissionPilot_Debug", "[STAGE 5 (FALLBACK)] Generated ${mockMissions.size} local autonomous blueprint missions.")
            return@withContext mockMissions
        }

        val prompt = """
            You are the 🧠 Planner Agent for 'MissionPilot AI' — an AI execution partner that beats procrastination.
            Your job is to break down the user's high-level goal into a series of 3 to 4 sequential "Missions".
            Each Mission should have a specific focus, estimated completion time in minutes (between 15 and 90 mins),
            and a step-by-step checklist of 3-5 subtasks that are extremely actionable.
            
            Goal Details:
            Title: "${goal.title}"
            Category: "${goal.category}"
            Deadline/Timeline: "${goal.deadline}"
            Priority: "${goal.priority}"
            
            Format your response strictly as a JSON array of objects with the following schema:
            [
               {
                 "title": "Mission Name (concise, action-oriented)",
                 "description": "Clear description of the mission objective and outcome",
                 "estimatedMinutes": 45,
                 "checklist": ["Subtask 1", "Subtask 2", "Subtask 3"]
               }
            ]
            
            Ensure the missions are ordered sequentially from first step to final victory.
            Make the first mission extremely low-friction to start!
        """.trimIndent()

        val systemInstruction = "You are an elite productivity psychologist and Scrum Master. Always reply in clean, valid JSON."

        val request = MoshiGenerateContentRequest(
            contents = listOf(
                MoshiContent(parts = listOf(MoshiPart(text = prompt)))
            ),
            generationConfig = MoshiGenerationConfig(
                responseFormat = MoshiResponseFormat(text = MoshiResponseFormatText(mimeType = "application/json")),
                temperature = 0.2f
            ),
            systemInstruction = MoshiContent(parts = listOf(MoshiPart(text = systemInstruction)))
        )

        Log.d("MissionPilot_Debug", "[STAGE 3] Planner Agent: Sending API request to Gemini model 'gemini-3.5-flash' for goal: '${goal.title}'")
        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            Log.d("MissionPilot_Debug", "[STAGE 4] Planner Agent: Received response from Gemini API. Content length: ${jsonText?.length ?: 0} characters.")
            if (jsonText != null) {
                val cleanedJson = cleanJson(jsonText)
                val planned = jsonAdapter.fromJson(cleanedJson)
                if (!planned.isNullOrEmpty()) {
                    Log.d("MissionPilot_Debug", "[STAGE 5] Planner Agent: JSON parsed successfully. Generated ${planned.size} custom missions.")
                    return@withContext planned.map { p ->
                        val checklistItems = p.checklist.map { ChecklistItem(it, false) }
                        Mission(
                            goalId = goal.id,
                            title = p.title,
                            description = p.description,
                            estimatedMinutes = p.estimatedMinutes,
                            checklistJson = Mission.createChecklistJson(checklistItems)
                        )
                    }
                } else {
                    Log.w(TAG, "[STAGE 5] Parsed mission list was empty. Falling back to local template generator.")
                    Log.d("MissionPilot_Debug", "[STAGE 5] Parsed mission list was empty. Falling back to local template generator.")
                }
            } else {
                Log.w(TAG, "[STAGE 4] Received empty text candidate from Gemini API.")
                Log.d("MissionPilot_Debug", "[STAGE 4] Received empty text candidate from Gemini API.")
            }
            val mockMissions = getMockMissions(goal.id, goal.title, goal.category)
            Log.d("MissionPilot_Debug", "[STAGE 5 (FALLBACK)] Generated ${mockMissions.size} fallback missions.")
            mockMissions
        } catch (e: Exception) {
            Log.e(TAG, "Error generating missions with Gemini: ${e.message}", e)
            Log.d("MissionPilot_Debug", "[STAGE 4 (ERROR)] Gemini API call failed: ${e.message}. Launching local safety recovery protocol.")
            val mockMissions = getMockMissions(goal.id, goal.title, goal.category)
            Log.d("MissionPilot_Debug", "[STAGE 5 (FALLBACK)] Generated ${mockMissions.size} fallback missions.")
            mockMissions
        }
    }

    private fun cleanJson(rawJson: String): String {
        var clean = rawJson.trim()
        if (clean.startsWith("```")) {
            // Remove the opening block (e.g. ```json or ```)
            clean = clean.substringAfter("\n")
            // Remove the closing block (```)
            if (clean.endsWith("```")) {
                clean = clean.substringBeforeLast("```")
            }
        }
        return clean.trim()
    }

    /**
     * 🚀 MISSION AGENT
     * Generates the single next smallest micro-step to beat startup friction.
     */
    suspend fun generateNextMicroStep(mission: Mission, currentChecklist: List<ChecklistItem>): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!isKeyValid(apiKey)) {
            return@withContext "Mock Next Micro-Step: Open your workspace, lay out your notebook/IDE, and write down just the first sentence of your work to build momentum."
        }

        val remainingTasks = currentChecklist.filter { !it.isDone }.joinToString("\n- ") { it.task }
        val prompt = """
            You are the 🚀 Mission Agent. Your single objective is to get the user moving on their current mission.
            They are feeling stuck or having friction starting.
            
            Mission: "${mission.title}"
            Description: "${mission.description}"
            Remaining Subtasks:
            - $remainingTasks
            
            Generate the "NEXT SMALLEST ACTIONABLE MICRO-STEP" to beat procrastination.
            It must be so simple that it's literally impossible to fail.
            For example, instead of "write code", say "Open your IDE, create a file, and write the first function signature."
            Instead of "study", say "Open the book to page 45 and read the bold headings."
            
            Keep your response short (2-3 sentences max) and highly encouraging!
        """.trimIndent()

        val request = MoshiGenerateContentRequest(
            contents = listOf(
                MoshiContent(parts = listOf(MoshiPart(text = prompt)))
            ),
            generationConfig = MoshiGenerationConfig(temperature = 0.7f),
            systemInstruction = MoshiContent(parts = listOf(MoshiPart(text = "You are an action-oriented executive coach who speaks with clarity and precision.")))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Open your materials, set a timer for 5 minutes, and focus on doing just the first item on your checklist."
        } catch (e: Exception) {
            Log.e(TAG, "Error generating next micro-step: ${e.message}")
            "Locate your materials, clear your desk, and do the first micro-action right now for just 2 minutes."
        }
    }

    /**
     * 🛟 RECOVERY AGENT
     * Rescues the user when they hit a wall.
     */
    suspend fun generateRescueStrategy(mission: Mission, reason: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!isKeyValid(apiKey)) {
            return@withContext when (reason) {
                "Difficult" -> "🧠 [Difficult Fallback] Let's break this down. Do NOT try to solve the whole problem. Solve the absolute easiest, most trivial part of it first. Write down a messy outline of the difficulty to externalize it from your brain."
                "Distracted" -> "📵 [Distracted Fallback] Put your phone in another room or turn on Do Not Disturb. Set a physical kitchen timer for exactly 10 minutes. Tell yourself you will work for only 10 minutes, and then you can stop."
                "Tired" -> "🔋 [Tired Fallback] Your energy is low. Stand up, stretch, and drink a glass of ice-cold water. We will do a 'low-energy mission version': just review your work for 5 minutes without writing anything new."
                "Bored" -> "⚡ [Bored Fallback] Let's gamify this. Set a timer for 15 minutes and see if you can complete at least 1 checklist item before the bell rings. Put on some fast instrumental music to speed up your brain."
                else -> "🔥 [Motivation Fallback] Remember WHY you started this. The pain of starting is only 2 minutes; the pain of regret lasts all week. Close your eyes, take a deep breath, and count down: 5 - 4 - 3 - 2 - 1 - GO!"
            }
        }

        val prompt = """
            You are the 🛟 Recovery Agent for MissionPilot AI. The user is currently in active "Mission Mode" but pressed "I'M STUCK".
            They provided their primary reason: "$reason".
            
            Current Mission: "${mission.title}"
            Mission Description: "${mission.description}"
            
            Write a custom, highly psychological, action-oriented recovery rescue strategy.
            Customize it heavily for the reason "$reason".
            
            Structure your response with:
            1. 🛑 COGNITIVE REFRAME: A compassionate but firm 1-sentence reframe of their state.
            2. 🛠️ THE ULTRA-MICRO PATH: A 3-step action path designed to take less than 120 seconds.
            3. 🔥 BOOST: A short sentence of high-octane motivational coaching.
            
            Keep the tone punchy, clean, futuristic, and highly motivating. No bullet lists of generic advice. Give them a specific action!
        """.trimIndent()

        val request = MoshiGenerateContentRequest(
            contents = listOf(
                MoshiContent(parts = listOf(MoshiPart(text = prompt)))
            ),
            generationConfig = MoshiGenerationConfig(temperature = 0.8f),
            systemInstruction = MoshiContent(parts = listOf(MoshiPart(text = "You are J.A.R.V.I.S. meets David Goggins meets a cognitive behavioral therapist. Be concise and sharp.")))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Take a 2-minute breath, break the first task into something even simpler, and make a start right now."
        } catch (e: Exception) {
            Log.e(TAG, "Error in recovery agent: ${e.message}")
            "Refocus your mind. Set a timer for exactly 5 minutes, put all distractions away, and execute the first checklist item. You've got this."
        }
    }

    /**
     * 📈 REFLECTION AGENT
     * Analyzes mission success and generates tomorrow's plan.
     */
    suspend fun generateDailyReflection(
        completedMissions: List<Mission>,
        mood: String,
        difficulty: String,
        learning: String,
        tomorrowPlan: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!isKeyValid(apiKey)) {
            return@withContext """
                📈 REFLECTION INSIGHTS
                Awesome work! You completed ${completedMissions.size} missions today with a "$mood" energy level and found the challenge level to be "$difficulty".
                
                Tomorrow's Launch Strategy:
                Your primary focus tomorrow is: "$tomorrowPlan".
                
                Pro-Tip for Momentum:
                You learned: "$learning". Carry this win into tomorrow's first mission. Your streak is intact. Keep pilot mode active!
            """.trimIndent()
        }

        val missionTitles = completedMissions.joinToString(", ") { it.title }
        val prompt = """
            You are the 📈 Reflection Agent for MissionPilot AI.
            The user has finished their productivity cycle for the day.
            
            Here is the data for today:
            Missions Completed Today: $missionTitles (${completedMissions.size} total)
            Energy/Mood: $mood
            Perceived Difficulty: $difficulty
            User's Key Learning: "$learning"
            User's Intended Plan for Tomorrow: "$tomorrowPlan"
            
            Generate a highly personalized productivity audit and "Tomorrow's Launch Strategy".
            
            Include:
            1. 📊 MOMENTUM ANALYSIS: How their energy ($mood) affected their execution, and a rating of their cognitive output.
            2. 💡 COGNITIVE WIN: Highlight the value of what they learned ($learning) and how to apply it.
            3. 🚀 TOMORROW'S FIRST MISSION: Give them an exact name and description for tomorrow's first action to prevent tomorrow's startup friction.
            
            Make it look like a high-end personal executive analysis. Beautiful, scannable spacing, ultra-professional.
        """.trimIndent()

        val request = MoshiGenerateContentRequest(
            contents = listOf(
                MoshiContent(parts = listOf(MoshiPart(text = prompt)))
            ),
            generationConfig = MoshiGenerationConfig(temperature = 0.6f),
            systemInstruction = MoshiContent(parts = listOf(MoshiPart(text = "You are an elite performance scientist. Write structured, professional, inspiring insights.")))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Reflection successfully recorded. Tomorrow is a new slate for execution."
        } catch (e: Exception) {
            Log.e(TAG, "Error in reflection agent: ${e.message}")
            "Reflection analyzed. You are making steady progress. Tomorrow's plan: $tomorrowPlan. Prepare your workspace tonight to reduce friction in the morning."
        }
    }

    /**
     * 🧠 AUTONOMOUS ADVISOR AGENT
     * Generates a proactive briefing based on current goals, sub-missions, and reflections.
     */
    suspend fun generateProactiveBriefing(
        goals: List<Goal>,
        missions: List<Mission>,
        reflections: List<Reflection>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!isKeyValid(apiKey)) {
            Log.d("MissionPilot_Debug", "[PROACTIVE ENGINE] Gemini key not valid/configured. Using local proactive briefing engine.")
            return@withContext getLocalProactiveBriefing(goals, missions, reflections)
        }

        if (goals.isEmpty()) {
            return@withContext "👋 Welcome Back, Pilot. Systems are green and on standby. Tap 'Declare Goal' to draft a campaign. Once declared, my cognitive sub-agents will immediately partition it into actionable sub-missions."
        }

        val pendingMissions = missions.filter { !it.isCompleted }
        val goalsDescription = goals.joinToString("; ") { "Campaign: '${it.title}', Priority: ${it.priority}, Category: ${it.category}, Deadline: ${it.deadline}" }
        val pendingDescription = if (pendingMissions.isEmpty()) "All missions completed." else pendingMissions.joinToString("; ") { "'${it.title}' (Est: ${it.estimatedMinutes}m)" }
        val pastReflectionsDesc = if (reflections.isEmpty()) "None recorded yet." else reflections.take(3).joinToString("; ") { "Mood: ${it.mood}, Learning: ${it.learning}" }

        val prompt = """
            You are the Autonomous Advisor Agent of MissionPilot AI, working alongside the Planner, Mission, Recovery, and Reflection sub-agents.
            Provide a concise, proactive status briefing and action plan for the user based on their data:
            
            ACTIVE CAMPAIGNS:
            $goalsDescription
            
            PENDING SUB-MISSIONS:
            $pendingDescription
            
            HISTORICAL REFLECTIONS (LAST 3):
            $pastReflectionsDesc
            
            Analyze this situation and write a concise, highly strategic, proactive AI Briefing.
            
            RULES FOR COGNITIVE INITIATIVE:
            1. If they have pending sub-missions, identify the first/easiest sub-mission to tackle immediately to beat startup inertia.
            2. If any deadline is close (or priority is High), highlight that campaign specifically and recommend reprioritizing today's schedule.
            3. If they have been inactive (or past reflections show high difficulty), act as the Recovery Agent to suggest an ultra-low friction micro-start.
            4. Keep the briefing dense, direct, and tactical. Max 3-4 sentences. Format it with a clean, high-contrast, space-pilot tone. Do not use conversational fluff like "Hey there!" or "As an AI..."
        """.trimIndent()

        val request = MoshiGenerateContentRequest(
            contents = listOf(
                MoshiContent(parts = listOf(MoshiPart(text = prompt)))
            ),
            generationConfig = MoshiGenerationConfig(temperature = 0.7f),
            systemInstruction = MoshiContent(parts = listOf(MoshiPart(text = "You are an elite tactical flight commander and peak-performance advisor. Write concise, direct, motivational briefings.")))
        )

        try {
            Log.d("MissionPilot_Debug", "[PROACTIVE ENGINE] Requesting real-time AI insight from Gemini model.")
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val result = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!result.isNullOrBlank()) {
                Log.d("MissionPilot_Debug", "[PROACTIVE ENGINE] Real-time AI briefing generated successfully.")
                result.trim()
            } else {
                Log.w(TAG, "[PROACTIVE ENGINE] Received empty text candidate from Gemini API.")
                getLocalProactiveBriefing(goals, missions, reflections)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[PROACTIVE ENGINE] Gemini API call failed: ${e.message}")
            getLocalProactiveBriefing(goals, missions, reflections)
        }
    }

    fun getLocalProactiveBriefing(
        goals: List<Goal>,
        missions: List<Mission>,
        reflections: List<Reflection>
    ): String {
        if (goals.isEmpty()) {
            return "👋 Welcome Back, Pilot. All systems are green and on standby. Declare a strategic goal campaign below, and our autonomous agents will instantly partition it into actionable, low-friction micro-missions."
        }
        val pendingMissions = missions.filter { !it.isCompleted }
        if (pendingMissions.isEmpty()) {
            return "🏆 Mission complete! All tactical sub-missions are cleared. Reflection Agent recommends performing a daily performance audit or declaring your next strategic goal."
        }
        
        // Find high priority goals or impending deadlines
        val urgentGoal = goals.find { it.priority == "High" } ?: goals.firstOrNull()
        val nextMission = pendingMissions.find { it.goalId == urgentGoal?.id } ?: pendingMissions.first()
        
        val sb = StringBuilder()
        sb.append("🧠 ADVISOR ENGINE BRIEFING: ")
        
        if (urgentGoal != null && urgentGoal.priority == "High") {
            sb.append("Priority alert on Campaign \"${urgentGoal.title}\". Deadline is critical. ")
        }
        
        sb.append("Mission Agent recommends initiating sub-mission \"${nextMission.title}\" (${nextMission.estimatedMinutes} mins). ")
        
        if (pendingMissions.size > 3) {
            sb.append("⚠️ Cognitive load is rising (backlog density: ${pendingMissions.size}). Recovery Agent suggests starting focus timer now to beat startup friction.")
        } else {
            sb.append("Lock in your momentum velocity and commence launch sequence.")
        }
        
        return sb.toString()
    }

    /**
     * 🧠 RECOVERY AGENT
     * Progressive Task Difficulty Reduction:
     * Automatically generates extremely low-friction micro-actions when users struggle to begin.
     */
    suspend fun generateReducedDifficultyTask(
        originalTask: String,
        level: Int
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!isKeyValid(apiKey)) {
            Log.d("MissionPilot_Debug", "[RECOVERY AGENT] Gemini API Key not valid. Using local progressive reduction engine.")
            return@withContext getLocalReducedDifficultyTask(originalTask, level)
        }
        
        val prompt = """
            You are the Recovery Agent of MissionPilot AI, designed to defeat start-up friction and procrastination.
            The user is procrastinating starting this task: "$originalTask".
            They have requested a task difficulty reduction (Current Reduction Level: $level).
            
            COGNITIVE PROGRESSIVE REDUCTION RULES:
            - LEVEL 1: Convert the task into a simple, physically tangible, low-friction setup action taking < 2 minutes. (e.g., "Open LeetCode", "Create a blank Google Doc 'Thesis Draft'", "Put on your study headphones").
            - LEVEL 2: Reduce even further. Make it a single microscopic setup or passive reading action. (e.g., "Read just one Graph problem statement", "Type a single-line title header", "Review yesterday's notes for 60 seconds").
            - LEVEL 3+: Extreme micro-start. Reduce to exactly 2 minutes of low-friction passive engagement. (e.g., "Spend only two minutes reading the first page", "Look at one sample diagram for 60 seconds", "Brain-dump words in a document for 2 minutes").
            
            Current Target Level: $level
            Write ONLY the action statement. Be highly encouraging and direct. Do not write conversational prefixes, quotes, explanations, or numbers. Max 10-12 words.
        """.trimIndent()
        
        val request = MoshiGenerateContentRequest(
            contents = listOf(
                MoshiContent(parts = listOf(MoshiPart(text = prompt)))
            ),
            generationConfig = MoshiGenerationConfig(temperature = 0.5f),
            systemInstruction = MoshiContent(parts = listOf(MoshiPart(text = "You are a cognitive behavioral therapist and high-performance recovery coach. Write short, ultra-low-friction action prompts.")))
        )
        
        try {
            Log.d("MissionPilot_Debug", "[RECOVERY AGENT] Asking Gemini to reduce difficulty for: '$originalTask' at Level $level")
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val result = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!result.isNullOrBlank()) {
                val formatted = result.trim().removeSurrounding("\"")
                Log.d("MissionPilot_Debug", "[RECOVERY AGENT] Gemini reduced task to: '$formatted'")
                formatted
            } else {
                getLocalReducedDifficultyTask(originalTask, level)
            }
        } catch (e: Exception) {
            Log.e("GeminiAgentManager", "[RECOVERY AGENT] Failed: ${e.message}")
            getLocalReducedDifficultyTask(originalTask, level)
        }
    }
    
    fun getLocalReducedDifficultyTask(originalTask: String, level: Int): String {
        val lower = originalTask.lowercase()
        return when (level) {
            1 -> {
                if (lower.contains("graph") || lower.contains("dsa") || lower.contains("code") || lower.contains("leetcode") || lower.contains("setup")) {
                    "Open LeetCode."
                } else if (lower.contains("write") || lower.contains("thesis") || lower.contains("essay") || lower.contains("draft")) {
                    "Open your word processor and create a blank draft document."
                } else {
                    "Open your primary work application or IDE."
                }
            }
            2 -> {
                if (lower.contains("graph") || lower.contains("dsa") || lower.contains("code") || lower.contains("leetcode") || lower.contains("setup")) {
                    "Read just one Graph problem statement."
                } else if (lower.contains("write") || lower.contains("thesis") || lower.contains("essay") || lower.contains("draft")) {
                    "Write down just a single header or first sentence."
                } else {
                    "Read the first line of your study material or project board."
                }
            }
            else -> {
                if (lower.contains("graph") || lower.contains("dsa") || lower.contains("code") || lower.contains("leetcode") || lower.contains("setup")) {
                    "Spend only two minutes reading the problem description."
                } else if (lower.contains("write") || lower.contains("thesis") || lower.contains("essay") || lower.contains("draft")) {
                    "Brain-dump random thoughts into the editor for just 2 minutes."
                } else {
                    "Commit to working on your environment for exactly two minutes."
                }
            }
        }
    }

    private fun getMockMissions(goalId: Int, goalTitle: String, category: String): List<Mission> {
        val missions = mutableListOf<Mission>()
        when (category.lowercase()) {
            "dsa", "coding", "tech" -> {
                missions.add(
                    Mission(
                        goalId = goalId,
                        title = "Setup Environment & Select 3 Problems",
                        description = "Reduce initial friction by finding exactly what 3 questions to solve and opening your editor.",
                        estimatedMinutes = 20,
                        checklistJson = Mission.createChecklistJson(
                            listOf(
                                ChecklistItem("Open your IDE and create a package 'DSA_Contest'", false),
                                ChecklistItem("Select 3 Easy-Medium LeetCode problems (e.g. Arrays, Two Pointer)", false),
                                ChecklistItem("Write out the problem statements as comments in your editor", false)
                            )
                        )
                    )
                )
                missions.add(
                    Mission(
                        goalId = goalId,
                        title = "Deconstruct the First Problem (Interactive Coding)",
                        description = "Analyze constraints, draw the logic, and write a brute-force approach.",
                        estimatedMinutes = 40,
                        checklistJson = Mission.createChecklistJson(
                            listOf(
                                ChecklistItem("Write out the test cases on paper", false),
                                ChecklistItem("Implement the brute-force solution", false),
                                ChecklistItem("Submit and analyze time/space complexity", false)
                            )
                        )
                    )
                )
                missions.add(
                    Mission(
                        goalId = goalId,
                        title = "Optimized Solutions & Edge Cases",
                        description = "Optimize the brute-force solutions to optimal time complexity and test boundary conditions.",
                        estimatedMinutes = 45,
                        checklistJson = Mission.createChecklistJson(
                            listOf(
                                ChecklistItem("Identify bottlenecks in your first solution", false),
                                ChecklistItem("Implement an optimal hash map or two-pointer strategy", false),
                                ChecklistItem("Verify behavior with empty, negative, or duplicate arrays", false)
                            )
                        )
                    )
                )
            }
            else -> {
                missions.add(
                    Mission(
                        goalId = goalId,
                        title = "Launch Pad Prep (First 5% Victory)",
                        description = "Clear your desk, find your core resource, and outline your roadmap to conquer '$goalTitle'.",
                        estimatedMinutes = 15,
                        checklistJson = Mission.createChecklistJson(
                            listOf(
                                ChecklistItem("Clear all background browser tabs and put phone on silent", false),
                                ChecklistItem("Find the exact chapter, doc, or file needed", false),
                                ChecklistItem("Write down a 3-point bullet list of what success looks like today", false)
                            )
                        )
                    )
                )
                missions.add(
                    Mission(
                        goalId = goalId,
                        title = "Deep-Work Execution Phase",
                        description = "Engage in uninterrupted focus to complete the core components of the work.",
                        estimatedMinutes = 45,
                        checklistJson = Mission.createChecklistJson(
                            listOf(
                                ChecklistItem("Draft the first major component", false),
                                ChecklistItem("Review and refine details", false),
                                ChecklistItem("Perform an intermediate self-audit", false)
                            )
                        )
                    )
                )
                missions.add(
                    Mission(
                        goalId = goalId,
                        title = "Polish & Final Submission",
                        description = "Inspect the results, correct any errors, and formally log your victory.",
                        estimatedMinutes = 30,
                        checklistJson = Mission.createChecklistJson(
                            listOf(
                                ChecklistItem("Self-test against your initial criteria", false),
                                ChecklistItem("Fix minor formatting or stylistic flaws", false),
                                ChecklistItem("Package or submit the completed asset", false)
                            )
                        )
                    )
                )
            }
        }
        return missions
    }
}
