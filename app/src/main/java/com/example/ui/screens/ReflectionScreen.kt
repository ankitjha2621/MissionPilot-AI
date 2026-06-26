package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.MissionPilotViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReflectionScreen(
    viewModel: MissionPilotViewModel,
    onBackClick: () -> Unit,
    onFinished: () -> Unit
) {
    val reflections by viewModel.reflections.collectAsState()
    val isGeneratingReflection by viewModel.isGeneratingReflection.collectAsState()
    val reflectionResponse by viewModel.reflectionResponse.collectAsState()

    var mood by remember { mutableStateOf("Energized") }
    var difficulty by remember { mutableStateOf("Challenging") }
    var learning by remember { mutableStateOf("") }
    var tomorrowPlan by remember { mutableStateOf("") }

    val moods = listOf("Energized", "Neutral", "Tired", "Anxious")
    val difficulties = listOf("Easy", "Challenging", "Hard")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "CYCLE AUDIT",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberGreen,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
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
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (reflectionResponse == null) {
                    // Header
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "PERFORMANCE AUDIT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberGreen,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Analyze Your Active Cycle",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AeroBlue
                        )
                        Text(
                            text = "Introspection is the pilot's final defense against stalling. Log your state to calculate custom launchpad velocity for tomorrow.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }

                    // Mood selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "COGNITIVE ENERGY STATE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            moods.forEach { item ->
                                val isSelected = mood == item
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { mood = item }
                                        .testTag("mood_card_$item"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) CyberGreen.copy(alpha = 0.15f) else CyberSlate
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) CyberGreen else CyberBorder
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
                                            text = item,
                                            color = if (isSelected) CyberGreen else TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Difficulty Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "PERCEIVED COMPASS VECTOR (DIFFICULTY)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            difficulties.forEach { item ->
                                val isSelected = difficulty == item
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { difficulty = item }
                                        .testTag("difficulty_card_$item"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) ElectricBlue.copy(alpha = 0.15f) else CyberSlate
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) ElectricBlue else CyberBorder
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
                                            text = item,
                                            color = if (isSelected) ElectricBlue else TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Key learning input
                    OutlinedTextField(
                        value = learning,
                        onValueChange = { learning = it },
                        label = { Text("What is your major learning or core victory today?") },
                        placeholder = { Text("e.g. Brute force was easy but recursion constraints got confusing.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(95.dp)
                            .testTag("learning_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberGreen,
                            unfocusedBorderColor = CyberBorder,
                            focusedContainerColor = CyberSlate,
                            unfocusedContainerColor = CyberSlate
                        ),
                        shape = MaterialTheme.shapes.medium
                    )

                    // Tomorrow plan input
                    OutlinedTextField(
                        value = tomorrowPlan,
                        onValueChange = { tomorrowPlan = it },
                        label = { Text("What is your single launch focus for tomorrow?") },
                        placeholder = { Text("e.g. Research memoization or complete 2 array problems.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(95.dp)
                            .testTag("tomorrow_plan_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberGreen,
                            unfocusedBorderColor = CyberBorder,
                            focusedContainerColor = CyberSlate,
                            unfocusedContainerColor = CyberSlate
                        ),
                        shape = MaterialTheme.shapes.medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isGeneratingReflection) {
                        CircularProgressIndicator(
                            color = CyberGreen,
                            strokeWidth = 4.dp,
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                    } else {
                        Button(
                            onClick = {
                                if (learning.isNotBlank() && tomorrowPlan.isNotBlank()) {
                                    viewModel.generateAndSaveReflection(mood, difficulty, learning, tomorrowPlan) {}
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_audit_button"),
                            enabled = learning.isNotBlank() && tomorrowPlan.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberGreen,
                                disabledContainerColor = CyberBorder
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                "SUBMIT AUDIT LOG",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = if (learning.isNotBlank() && tomorrowPlan.isNotBlank()) CyberBlack else TextMuted
                            )
                        }
                    }

                    // --- Historically saved reflections logs list ---
                    if (reflections.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "HISTORICAL AUDIT LOGS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        reflections.forEach { log ->
                            val formatter = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
                            val dateString = formatter.format(Date(log.createdAt))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CyberSlate),
                                border = BorderStroke(1.dp, CyberBorder),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Energy: ${log.mood} | ${log.difficulty}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricBlue
                                        )
                                        Text(
                                            text = dateString,
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Victory: ${log.learning}",
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tomorrow's Launch: ${log.tomorrowPlan}",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Reflection Agent Output view
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CyberSlate),
                            border = BorderStroke(1.dp, CyberGreen.copy(alpha = 0.5f)),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = CyberGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        "📈 REFLECTION AUDIT INSIGHTS",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberGreen,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = reflectionResponse ?: "",
                                    fontSize = 14.sp,
                                    color = TextPrimary,
                                    lineHeight = 20.sp
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = {
                                        viewModel.clearReflectionState()
                                        onFinished()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("complete_cycle_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreen),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text(
                                        "COMPLETE CYCLE & RETURN TO DECK",
                                        fontWeight = FontWeight.Black,
                                        color = CyberBlack,
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
}
