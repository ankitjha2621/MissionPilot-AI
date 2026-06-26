package com.example.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.MissionPilotViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGoalScreen(
    viewModel: MissionPilotViewModel,
    onBackClick: () -> Unit,
    onGoalCreated: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Coding") }
    var deadline by remember { mutableStateOf("3 Days") }
    var priority by remember { mutableStateOf("High") }
    var beginCommitment by remember { mutableStateOf("7:00 PM") }

    val isGeneratingMissions by viewModel.isGeneratingMissions.collectAsState()

    val categories = listOf("Coding", "Creative", "Writing", "Design", "Academics")
    val priorities = listOf("High", "Medium", "Low")
    val deadlines = listOf("Today", "Tomorrow", "3 Days", "1 Week", "Custom")
    val commitmentShortcuts = listOf("7:00 PM", "After dinner", "Tomorrow morning", "Now")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "DECLARE CAMPAIGN",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AeroBlue,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        enabled = !isGeneratingMissions,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = ElectricBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberBlack)
            )
        },
        containerColor = CyberBlack
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Goal Input
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "WHAT IS YOUR STRATEGIC GOAL?",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("e.g., Prepare for DSA Contest or Write Thesis Draft") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("goal_title_input"),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = ElectricBlue
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = CyberBorder,
                            focusedContainerColor = CyberSlate,
                            unfocusedContainerColor = CyberSlate,
                            focusedPlaceholderColor = TextMuted,
                            unfocusedPlaceholderColor = TextMuted
                        ),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )
                }

                // Category Selection Cards
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "CAMPAIGN CATEGORY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = category == cat
                            Card(
                                modifier = Modifier
                                    .clickable { category = cat }
                                    .testTag("category_card_$cat"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) NeonPurple.copy(alpha = 0.2f) else CyberSlate
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) NeonPurple else CyberBorder
                                ),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSelected) ElectricBlue else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

                // Deadline Selection Row
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "DEADLINE MATRIX",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        deadlines.forEach { dead ->
                            val isSelected = deadline == dead
                            Card(
                                modifier = Modifier
                                    .clickable { deadline = dead }
                                    .testTag("deadline_card_$dead"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) ElectricBlue.copy(alpha = 0.15f) else CyberSlate
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) ElectricBlue else CyberBorder
                                ),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = if (isSelected) ElectricBlue else TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = dead,
                                        color = if (isSelected) ElectricBlue else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Priority Selection Cards
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "PRIORITY VECTOR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        priorities.forEach { prio ->
                            val isSelected = priority == prio
                            val activeBorderColor = when (prio) {
                                "High" -> NeonPink
                                "Medium" -> CyberOrange
                                else -> CyberGreen
                            }
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { priority = prio }
                                    .testTag("priority_card_$prio"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) activeBorderColor.copy(alpha = 0.15f) else CyberSlate
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) activeBorderColor else CyberBorder
                                ),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = prio,
                                        color = if (isSelected) activeBorderColor else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Realistic Start Commitment Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "WHEN DO YOU REALISTICALLY WANT TO BEGIN?",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "We capture this to intercept 'I'll start later' friction before it stalls your momentum.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    
                    // Shortcut chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        commitmentShortcuts.forEach { comm ->
                            val isSelected = beginCommitment == comm
                            Card(
                                modifier = Modifier
                                    .clickable { beginCommitment = comm }
                                    .testTag("commitment_shortcut_$comm"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) ElectricBlue.copy(alpha = 0.15f) else CyberSlate
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) ElectricBlue else CyberBorder
                                ),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = if (isSelected) ElectricBlue else TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = comm,
                                        color = if (isSelected) ElectricBlue else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                    
                    // Text Field for custom/detailed commitments
                    OutlinedTextField(
                        value = beginCommitment,
                        onValueChange = { beginCommitment = it },
                        placeholder = { Text("Or specify another custom trigger (e.g., 'At 8:30 PM', 'After a 10m walk')") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("commitment_custom_input"),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = ElectricBlue
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = CyberBorder,
                            focusedContainerColor = CyberSlate,
                            unfocusedContainerColor = CyberSlate,
                            focusedPlaceholderColor = TextMuted,
                            unfocusedPlaceholderColor = TextMuted
                        ),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Launch Campaign Button
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            Log.d("MissionPilot_Debug", "[STAGE 1] Button clicked: Creating strategic campaign '$title' (Category: $category, Priority: $priority, Deadline: $deadline, Commitment: $beginCommitment)")
                            viewModel.createGoal(
                                title = title,
                                category = category,
                                deadline = deadline,
                                priority = priority,
                                beginCommitment = beginCommitment,
                                onFinished = onGoalCreated
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("generate_blueprint_button"),
                    enabled = title.isNotBlank() && !isGeneratingMissions,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        disabledContainerColor = CyberBorder
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        "GENERATE AI BLUEPRINT PLAN",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = if (title.isNotBlank()) CyberBlack else TextMuted
                    )
                }
            }

            // AI Agent Generation Loading Layer overlay
            AnimatedVisibility(
                visible = isGeneratingMissions,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                var currentStepIndex by remember { mutableStateOf(0) }
                
                LaunchedEffect(isGeneratingMissions) {
                    if (isGeneratingMissions) {
                        currentStepIndex = 0
                        while (true) {
                            delay(1600)
                            if (currentStepIndex < 3) {
                                currentStepIndex++
                            } else {
                                currentStepIndex = 3
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CyberBlack.copy(alpha = 0.95f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(100.dp)
                        ) {
                            CircularProgressIndicator(
                                color = when (currentStepIndex) {
                                    0 -> NeonPurple
                                    1 -> ElectricBlue
                                    2 -> NeonPink
                                    else -> CyberGreen
                                },
                                strokeWidth = 5.dp,
                                modifier = Modifier.fillMaxSize()
                            )
                            Icon(
                                imageVector = when (currentStepIndex) {
                                    0 -> Icons.Default.Edit
                                    1 -> Icons.Default.PlayArrow
                                    2 -> Icons.Default.Warning
                                    else -> Icons.Default.Star
                                },
                                contentDescription = null,
                                tint = when (currentStepIndex) {
                                    0 -> NeonPurple
                                    1 -> ElectricBlue
                                    2 -> NeonPink
                                    else -> CyberGreen
                                },
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Text(
                            text = "AUTONOMOUS COGNITIVE ENGINE",
                            color = AeroBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "MissionPilot Orchestrator",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Timeline of 4 Agents
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CyberSlate, shape = MaterialTheme.shapes.large)
                                .border(BorderStroke(1.dp, CyberBorder), shape = MaterialTheme.shapes.large)
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Step 1: Planner Agent
                            AgentStatusRow(
                                agentName = "Planner Agent",
                                agentColor = NeonPurple,
                                statusText = if (currentStepIndex == 0) "Analyzing goal..." else "Goal analyzed & partitioned.",
                                isActive = currentStepIndex == 0,
                                isCompleted = currentStepIndex > 0
                            )

                            // Step 2: Mission Agent
                            AgentStatusRow(
                                agentName = "Mission Agent",
                                agentColor = ElectricBlue,
                                statusText = if (currentStepIndex < 1) "Awaiting blueprint..." else if (currentStepIndex == 1) "Creating roadmap..." else "Milestones and checklist generated.",
                                isActive = currentStepIndex == 1,
                                isCompleted = currentStepIndex > 1
                            )

                            // Step 3: Recovery Agent
                            AgentStatusRow(
                                agentName = "Recovery Agent",
                                agentColor = NeonPink,
                                statusText = if (currentStepIndex < 2) "Awaiting parameters..." else if (currentStepIndex == 2) "Preparing fallback plans..." else "Psychological backup protocols armed.",
                                isActive = currentStepIndex == 2,
                                isCompleted = currentStepIndex > 2
                            )

                            // Step 4: Reflection Agent
                            AgentStatusRow(
                                agentName = "Reflection Agent",
                                agentColor = CyberGreen,
                                statusText = if (currentStepIndex < 3) "Awaiting launch..." else if (currentStepIndex == 3) "Building learning profile..." else "Performance audit systems active.",
                                isActive = currentStepIndex == 3,
                                isCompleted = currentStepIndex > 3
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Active campaign subtitle
                        Text(
                            text = "Orchestrating campaign: \"$title\"",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgentStatusRow(
    agentName: String,
    agentColor: Color,
    statusText: String,
    isActive: Boolean,
    isCompleted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = CyberGreen,
                    modifier = Modifier.size(20.dp)
                )
            } else if (isActive) {
                CircularProgressIndicator(
                    color = agentColor,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(TextMuted, shape = MaterialTheme.shapes.extraSmall)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = agentName.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = if (isActive || isCompleted) agentColor else TextMuted,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = statusText,
                fontSize = 12.sp,
                color = if (isActive) TextPrimary else if (isCompleted) TextSecondary else TextMuted
            )
        }
    }
}
