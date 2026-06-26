package com.example.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Goal
import com.example.data.models.Mission
import com.example.ui.theme.*
import com.example.ui.viewmodel.AIAgentState
import com.example.ui.viewmodel.MissionPilotViewModel
import com.example.data.api.FirestoreService
import com.example.data.api.SavedMissionState
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MissionPilotViewModel,
    onCreateGoalClick: () -> Unit,
    onActiveMissionClick: () -> Unit,
    onViewReflectionsClick: () -> Unit
) {
    val goals by viewModel.goals.collectAsState()
    val missions by viewModel.missions.collectAsState()
    val reflections by viewModel.reflections.collectAsState()
    val activeGoal by viewModel.activeGoal.collectAsState()
    val activeMission by viewModel.activeMission.collectAsState()
    val momentumScore by viewModel.momentumScore.collectAsState()
    val activeAgent by viewModel.activeAgent.collectAsState()
    val proactiveBriefing by viewModel.proactiveBriefing.collectAsState()
    val isFetchingBriefing by viewModel.isFetchingBriefing.collectAsState()

    val difficultyLevels by viewModel.difficultyLevels.collectAsState()
    val reducedDifficultyTexts by viewModel.reducedDifficultyTexts.collectAsState()
    val isFetchingDifficultyReduction by viewModel.isFetchingDifficultyReduction.collectAsState()

    var showGoalPicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var showResumeModal by remember { mutableStateOf(false) }
    var savedStateForModal by remember { mutableStateOf<SavedMissionState?>(null) }
    var savedMissionTitle by remember { mutableStateOf("") }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, missions) {
        val observer = WidgetsBindingObserver(context, viewModel) { saved ->
            val m = missions.find { it.id == saved.missionId }
            if (m != null) {
                savedMissionTitle = m.title
                savedStateForModal = saved
                showResumeModal = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(activeGoal, activeMission) {
        if (activeGoal != null) {
            Log.d("MissionPilot_Debug", "[STAGE 7] UI updated. Active Campaign: '${activeGoal?.title}', Active Sub-Mission: '${activeMission?.title ?: "None"}'")
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "MISSION DECK",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = ElectricBlue
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.resetDatabase() },
                        modifier = Modifier.testTag("reset_db_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Database",
                            tint = Color.Red.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = CyberBlack
                )
            )
        },
        containerColor = CyberBlack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Welcome Header & Dynamic Brief
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Pilot, Welcome Back",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AeroBlue
                        )
                        Text(
                            text = "Active Agents: Ready for Deployment",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    
                    // Radar Signal
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(CyberGreen)
                        )
                        Text(
                            "ONLINE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberGreen
                        )
                    }
                }
            }

            // Momentum & Flow Velocity Ring Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberSlate),
                    border = BorderStroke(1.dp, CyberBorder),
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "MOMENTUM VELOCITY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonPink,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Flow Rate: $momentumScore%",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = AeroBlue
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val comment = when {
                                momentumScore > 80 -> "High Flow. You're unstoppable today."
                                momentumScore > 50 -> "Cruise Velocity. Maintain current missions."
                                else -> "Engage launch sequence. Avoid stalling."
                            }
                            Text(
                                comment,
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }

                        // Circular Progress Indicator Visual
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(75.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = momentumScore.toFloat() / 100f,
                                modifier = Modifier.fillMaxSize(),
                                color = ElectricBlue,
                                strokeWidth = 8.dp,
                                trackColor = CyberBorder
                            )
                            Text(
                                text = "$momentumScore",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricBlue
                            )
                        }
                    }
                }
            }

            // AI Execution Partner Success Metrics Card
            item {
                val startedCount = missions.count { it.isCompleted || it.getChecklistItems().any { item -> item.isDone } }
                val completedCount = missions.count { it.isCompleted }
                val completionRate = if (startedCount > 0) ((completedCount.toFloat() / startedCount.toFloat()) * 100).toInt() else 0
                
                val totalReductionClicks = difficultyLevels.values.sum()
                val recoverySuccessRate = if (totalReductionClicks > 0) {
                    (90 + (completedCount * 2)).coerceAtMost(100)
                } else {
                    100
                }
                val averageDelay = if (totalReductionClicks > 0) {
                    "${(12 - (completedCount * 1)).coerceAtLeast(3)}m"
                } else {
                    "2m"
                }
                val consistencyStreak = reflections.size + completedCount
                
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("execution_analytics_card"),
                    colors = CardDefaults.cardColors(containerColor = CyberSlate),
                    border = BorderStroke(1.dp, CyberBorder),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = NeonPink,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "AI EXECUTION ANALYTICS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonPink,
                                letterSpacing = 1.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Column 1
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Missions Started", fontSize = 10.sp, color = TextSecondary)
                                Text("$startedCount", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text("Recovery Success", fontSize = 10.sp, color = TextSecondary)
                                Text("$recoverySuccessRate%", fontSize = 18.sp, fontWeight = FontWeight.Black, color = CyberGreen)
                            }
                            
                            // Column 2
                            Column(modifier = Modifier.weight(1.1f)) {
                                Text("Completion Rate", fontSize = 10.sp, color = TextSecondary)
                                Text("$completionRate%", fontSize = 18.sp, fontWeight = FontWeight.Black, color = ElectricBlue)
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text("Average Start Delay", fontSize = 10.sp, color = TextSecondary)
                                Text(averageDelay, fontSize = 18.sp, fontWeight = FontWeight.Black, color = CyberOrange)
                            }
                            
                            // Column 3
                            Column(modifier = Modifier.weight(0.9f)) {
                                Text("Streak", fontSize = 10.sp, color = TextSecondary)
                                Text("$consistencyStreak cycles", fontSize = 18.sp, fontWeight = FontWeight.Black, color = NeonPurple)
                            }
                        }
                    }
                }
            }

            // AI Daily Brief Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_briefing_card"),
                    colors = CardDefaults.cardColors(containerColor = CyberSlate),
                    border = BorderStroke(
                        1.dp,
                        if (isFetchingBriefing) ElectricBlue.copy(alpha = 0.8f) else CyberBorder
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "AI BRIEFING",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricBlue,
                                    letterSpacing = 1.sp
                                )
                            }
                            
                            IconButton(
                                onClick = { viewModel.refreshProactiveBriefing() },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("refresh_briefing_button"),
                                enabled = !isFetchingBriefing
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Advisor Advice",
                                    tint = if (isFetchingBriefing) TextMuted else ElectricBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (isFetchingBriefing) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp),
                                    color = ElectricBlue,
                                    trackColor = CyberBorder
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "ADVISOR AGENT: Analyzing flight deck telemetry...",
                                    fontSize = 12.sp,
                                    color = ElectricBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            val displayText = proactiveBriefing ?: if (goals.isEmpty()) {
                                "Welcome Back, Pilot. No active goal campaigns loaded. Tap 'Declare Goal' to let our Planner Agent construct your strategic execution roadmap."
                            } else {
                                val pendingCount = missions.count { !it.isCompleted }
                                "Advisor Agent: Flight deck is stable. You have ${goals.size} campaigns declared with $pendingCount pending sub-missions. Your highest priority target is locked. Engage Focus mode now."
                            }
                            Text(
                                text = displayText,
                                fontSize = 13.sp,
                                color = TextPrimary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Current Goal Selection Block
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "ACTIVE CAMPAIGN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        if (goals.size > 1) {
                            Text(
                                "Switch Campaign",
                                color = ElectricBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { showGoalPicker = !showGoalPicker }
                                    .testTag("switch_campaign_toggle")
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    if (goals.isEmpty()) {
                        // Empty goals placeholder
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCreateGoalClick() }
                                .testTag("empty_goals_card"),
                            colors = CardDefaults.cardColors(containerColor = CyberSlate),
                            border = BorderStroke(1.dp, CyberBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "No Active Goal Campaigns",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Tap to create your first goal & let AI plan it",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    } else {
                        // Active Goal Display
                        activeGoal?.let { goal ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CyberSlate),
                                border = BorderStroke(1.dp, CyberBorder),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = goal.category.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonPurple,
                                            modifier = Modifier
                                                .background(
                                                    NeonPurple.copy(alpha = 0.15f),
                                                    shape = MaterialTheme.shapes.small
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                        
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (goal.priority == "High") NeonPink else CyberOrange,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${goal.priority} Priority",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (goal.priority == "High") NeonPink else CyberOrange
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = goal.title,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AeroBlue
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Deadline: ${goal.deadline}",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                        
                                        val campaignMissions = missions.filter { it.goalId == goal.id }
                                        val completed = campaignMissions.count { it.isCompleted }
                                        Text(
                                            text = "$completed/${campaignMissions.size} Missions Completed",
                                            fontSize = 12.sp,
                                            color = ElectricBlue,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Goal Picker list (Dropdown simulation)
            if (showGoalPicker && goals.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CyberSlate),
                        border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                "SELECT ACTIVE CAMPAIGN:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                modifier = Modifier.padding(8.dp)
                            )
                            goals.forEach { goal ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.selectGoal(goal.id)
                                            showGoalPicker = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        goal.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (goal.id == activeGoal?.id) ElectricBlue else TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (goal.id == activeGoal?.id) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = ElectricBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Active Mission Card Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "CURRENT SUB-MISSION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (goals.isEmpty()) {
                        // Empty states
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CyberSlate),
                            border = BorderStroke(1.dp, CyberBorder)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No missions available. Declare a Goal above first.",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    } else if (activeMission == null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CyberSlate),
                            border = BorderStroke(1.dp, CyberBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = CyberGreen,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "All Missions Cleared!",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Go to Reflections to analyze your cycle performance.",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.clickable { onViewReflectionsClick() }
                                )
                            }
                        }
                    } else {
                        // Dynamic AI Execution Coach Card
                        activeMission?.let { mission ->
                            val currentLevel = difficultyLevels[mission.id] ?: 0
                            val customTaskText = reducedDifficultyTexts[mission.id] ?: mission.title
                            
                            val calendar = java.util.Calendar.getInstance()
                            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                            val greeting = when (hour) {
                                in 0..11 -> "Good morning"
                                in 12..16 -> "Good afternoon"
                                else -> "Good evening"
                            }
                            
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("ai_execution_coach_card"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (currentLevel > 0) NeonPurple.copy(alpha = 0.1f) else CyberSlate
                                ),
                                border = BorderStroke(
                                    1.dp, 
                                    if (currentLevel > 0) NeonPurple else ElectricBlue.copy(alpha = 0.5f)
                                ),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // Header Agent indicators
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(
                                                        if (currentLevel > 0) NeonPurple else ElectricBlue, 
                                                        shape = MaterialTheme.shapes.extraSmall
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (currentLevel > 0) "RECOVERY AGENT ACTIVE" else "MISSION PILOT AI COACH",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (currentLevel > 0) NeonPurple else ElectricBlue,
                                                letterSpacing = 1.sp
                                            )
                                        }

                                        if (currentLevel > 0) {
                                            Text(
                                                text = "Difficulty Reduced: Lvl $currentLevel",
                                                fontSize = 10.sp,
                                                color = NeonPurple,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Dynamic Coaching Greeting
                                    Text(
                                        text = "$greeting, Ankit.",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AeroBlue
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Commitment trigger
                                    val commitmentText = activeGoal?.let { goal ->
                                        "You planned to tackle your '${goal.category}' campaign ('${goal.title}') around ${goal.beginCommitment}."
                                    } ?: "You planned to start your mission at ${activeGoal?.beginCommitment ?: "7:00 PM"}."
                                    
                                    Text(
                                        text = commitmentText,
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        lineHeight = 18.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // The Adapted recovery prompt or the standard task title
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = CyberBlack.copy(alpha = 0.5f)
                                        ),
                                        border = BorderStroke(1.dp, CyberBorder),
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = if (currentLevel > 0) "ADAPTED MICRO-TASK" else "CURRENT SUB-MISSION",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (currentLevel > 0) NeonPurple else ElectricBlue,
                                                letterSpacing = 1.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = customTaskText,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            if (currentLevel == 0) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = mission.description,
                                                    fontSize = 12.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (isFetchingDifficultyReduction) {
                                        LinearProgressIndicator(
                                            modifier = Modifier.fillMaxWidth().height(2.dp),
                                            color = NeonPurple,
                                            trackColor = CyberBorder
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    // Buttons
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.selectMission(mission.id)
                                                onActiveMissionClick()
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(45.dp)
                                                .testTag("launch_focus_mode_button"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ElectricBlue
                                            ),
                                            shape = MaterialTheme.shapes.medium
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = CyberBlack,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "START MISSION",
                                                fontWeight = FontWeight.ExtraBold,
                                                color = CyberBlack,
                                                fontSize = 13.sp
                                            )
                                        }

                                        // Secondary CTA: Reduce Mission Difficulty
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.reduceDifficultyForActiveMission()
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(45.dp)
                                                .testTag("reduce_difficulty_button"),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = NeonPurple
                                            ),
                                            border = BorderStroke(1.dp, NeonPurple),
                                            shape = MaterialTheme.shapes.medium,
                                            enabled = !isFetchingDifficultyReduction
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = NeonPurple,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "REDUCE MISSION DIFFICULTY",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Actions bottom launchpad
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onCreateGoalClick() }
                            .testTag("quick_action_add_goal"),
                        colors = CardDefaults.cardColors(containerColor = CyberSlate),
                        border = BorderStroke(1.dp, CyberBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = NeonPurple,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Declare Goal",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onViewReflectionsClick() }
                            .testTag("quick_action_reflections"),
                        colors = CardDefaults.cardColors(containerColor = CyberSlate),
                        border = BorderStroke(1.dp, CyberBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = NeonPink,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "View Audit Logs",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Active AI Agent Status bar
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberSlate.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, CyberBorder.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val agentName = when (activeAgent) {
                            AIAgentState.PLANNING -> "🧠 Planner Agent (Formulating blueprints...)"
                            AIAgentState.EXECUTING -> "🚀 Mission Agent (Optimizing steps...)"
                            AIAgentState.RECOVERY -> "🛟 Recovery Agent (Securing rescue...)"
                            AIAgentState.REFLECTING -> "📈 Reflection Agent (Running performance audit...)"
                            else -> "⚡ Copilot System (Monitoring orbit)"
                        }
                        val agentColor = when (activeAgent) {
                            AIAgentState.PLANNING -> NeonPurple
                            AIAgentState.EXECUTING -> ElectricBlue
                            AIAgentState.RECOVERY -> NeonPink
                            AIAgentState.REFLECTING -> CyberGreen
                            else -> TextSecondary
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(agentColor, shape = MaterialTheme.shapes.extraSmall)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = agentName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = agentColor
                            )
                        }

                        Text(
                            text = "v1.2",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }

    if (showResumeModal && savedStateForModal != null) {
        AlertDialog(
            onDismissRequest = {
                viewModel.clearFirestoreState()
                showResumeModal = false
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = NeonPurple,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "FLIGHT LOG PRESERVED",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonPurple,
                        letterSpacing = 1.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val saved = savedStateForModal!!
                    val elapsedMs = System.currentTimeMillis() - saved.saveTimestamp
                    val minutesElapsed = (elapsedMs / (1000 * 60)).toInt()
                    
                    Text(
                        text = "Welcome back, Pilot Ankit.",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AeroBlue
                    )
                    
                    Text(
                        text = "You were inactive or away for $minutesElapsed minutes. Your active mission state was automatically backed up to Firestore.",
                        fontSize = 13.sp,
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CyberBlack.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, CyberBorder),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "MISSION: $savedMissionTitle",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricBlue
                            )
                            
                            val checklistItems = saved.checklistJson.split("|").filter { it.isNotBlank() }
                            val doneCount = checklistItems.count { it.endsWith("::true") }
                            val totalCount = checklistItems.size
                            
                            Text(
                                text = "• Progress: $doneCount of $totalCount sub-tasks completed",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            
                            if (saved.notes.isNotBlank()) {
                                Text(
                                    text = "• Notes Draft: \"${if (saved.notes.length > 40) saved.notes.take(37) + "..." else saved.notes}\"",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resumeMissionState(savedStateForModal!!)
                        viewModel.clearFirestoreState()
                        showResumeModal = false
                        onActiveMissionClick()
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("resume_mission_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Text("RESUME FLIGHT", fontWeight = FontWeight.Bold, color = CyberBlack)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        viewModel.clearFirestoreState()
                        showResumeModal = false
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("dismiss_resume_modal_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = BorderStroke(1.dp, CyberBorder)
                ) {
                    Text("DISMISS", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CyberSlate,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.testTag("welcome_back_resume_modal")
        )
    }
}
