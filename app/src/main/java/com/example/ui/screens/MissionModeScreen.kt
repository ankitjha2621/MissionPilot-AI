package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Mission
import com.example.ui.theme.*
import com.example.ui.viewmodel.MissionPilotViewModel
import com.example.data.api.FirestoreService
import com.example.data.api.SavedMissionState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionModeScreen(
    viewModel: MissionPilotViewModel,
    onBackClick: () -> Unit,
    onStuckClick: () -> Unit,
    onCompleteSuccess: () -> Unit
) {
    val activeMission by viewModel.activeMission.collectAsState()
    val timerSecondsRemaining by viewModel.timerSecondsRemaining.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()

    val nextMicroStep by viewModel.nextMicroStep.collectAsState()
    val isFetchingMicroStep by viewModel.isFetchingMicroStep.collectAsState()

    var notesText by remember { mutableStateOf("") }

    val context = LocalContext.current
    var showWelcomeBackModal by remember { mutableStateOf(false) }
    var savedStateForModal by remember { mutableStateOf<SavedMissionState?>(null) }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Lifecycle Observer to detect when the user leaves or returns to the app
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val saveObserver = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP || event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                activeMission?.let { mission ->
                    viewModel.autoSaveToFirestore(mission, notesText)
                }
            }
        }
        val resumeObserver = WidgetsBindingObserver(context, viewModel) { saved ->
            savedStateForModal = saved
            showWelcomeBackModal = true
        }
        lifecycleOwner.lifecycle.addObserver(saveObserver)
        lifecycleOwner.lifecycle.addObserver(resumeObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(saveObserver)
            lifecycleOwner.lifecycle.removeObserver(resumeObserver)
        }
    }

    // In-app inactivity tracking: triggers if inactive for >5 minutes
    LaunchedEffect(lastInteractionTime) {
        kotlinx.coroutines.delay(5 * 60 * 1000) // 5 minutes inactivity
        activeMission?.let { mission ->
            viewModel.autoSaveToFirestore(mission, notesText)
            val saved = FirestoreService.getSavedState(context)
            if (saved != null) {
                savedStateForModal = saved
                showWelcomeBackModal = true
            }
        }
    }

    // Synchronize default timer when entering
    LaunchedEffect(activeMission) {
        activeMission?.let {
            notesText = it.notes
            if (!isTimerRunning) {
                viewModel.resetTimer(it.estimatedMinutes)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "MISSION FLIGHT",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AeroBlue,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.saveMissionProgress(mission = activeMission ?: return@IconButton, notes = notesText)
                            onBackClick()
                        },
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
        if (activeMission == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(CyberBlack),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No active mission. Launch one from the Mission Deck.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            val mission = activeMission!!
            val checklist = mission.getChecklistItems()
            val totalItems = checklist.size
            val doneItems = checklist.count { it.isDone }
            val progressPercent = if (totalItems > 0) (doneItems.toFloat() / totalItems.toFloat()) else 0f

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()
                                lastInteractionTime = System.currentTimeMillis()
                            }
                        }
                    }
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Welcome Back Banner if progress > 0
                if (doneItems > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("welcome_back_progress_banner"),
                        colors = CardDefaults.cardColors(containerColor = CyberGreen.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, CyberGreen),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = CyberGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Welcome back, Pilot.",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberGreen
                                )
                                Text(
                                    text = "You already completed ${(progressPercent * 100).toInt()}%. Let's continue from where you stopped.",
                                    fontSize = 12.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                // Header Details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberSlate),
                    border = BorderStroke(1.dp, CyberBorder),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(ElectricBlue, shape = MaterialTheme.shapes.extraSmall)
                            )
                            Text(
                                "ACTIVE SUB-MISSION IN FOCUS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricBlue,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = mission.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AeroBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = mission.description,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Focus Timer Display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberSlate),
                    border = BorderStroke(1.dp, CyberBorder),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "FOCUS TIMER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonPurple,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Timer Text
                        val minutes = timerSecondsRemaining / 60
                        val seconds = timerSecondsRemaining % 60
                        val timerString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                        Text(
                            text = timerString,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            color = ElectricBlue,
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.resetTimer(mission.estimatedMinutes) },
                                modifier = Modifier.testTag("reset_timer_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset",
                                    tint = TextSecondary
                                )
                            }

                            Button(
                                onClick = {
                                    if (isTimerRunning) viewModel.stopTimer() else viewModel.startTimer()
                                },
                                modifier = Modifier
                                    .width(140.dp)
                                    .testTag("toggle_timer_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isTimerRunning) NeonPink else ElectricBlue
                                ),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(
                                    imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = CyberBlack,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isTimerRunning) "PAUSE" else "START FOCUS",
                                    fontWeight = FontWeight.Bold,
                                    color = CyberBlack,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Checklist Card
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
                                "LAUNCHPAD CHECKLIST",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "${(progressPercent * 100).toInt()}% Done",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricBlue
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LinearProgressIndicator(
                            progress = progressPercent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(MaterialTheme.shapes.small),
                            color = ElectricBlue,
                            trackColor = CyberBorder
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (checklist.isEmpty()) {
                            Text(
                                "No tasks generated for this mission.",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        } else {
                            checklist.forEachIndexed { idx, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.toggleChecklistItem(mission, idx) }
                                        .padding(vertical = 8.dp)
                                        .testTag("checklist_item_$idx"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (item.isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (item.isDone) CyberGreen else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = item.task,
                                        color = if (item.isDone) TextSecondary else TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = if (item.isDone) FontWeight.Normal else FontWeight.Medium,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                // 🚀 MISSION AGENT: Next Small Actionable Step Button
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberSlate),
                    border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.4f)),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = NeonPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "🚀 MISSION AGENT: STARTER ENGINE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonPurple,
                                letterSpacing = 1.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Procrastinating starting the first step? Click below. The Mission Agent will calculate the single absolute smallest friction-free micro-action.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        // Generated micro step box if available
                        nextMicroStep?.let { step ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CyberBlack),
                                border = BorderStroke(1.dp, CyberBorder),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "MICRO-STEP LAUNCHPAD:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricBlue
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = step,
                                        fontSize = 13.sp,
                                        color = TextPrimary,
                                        lineHeight = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Click 'Clear' once done to continue.",
                                        fontSize = 10.sp,
                                        color = TextMuted,
                                        modifier = Modifier
                                            .clickable { viewModel.clearMicroStep() }
                                            .testTag("clear_microstep_button")
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isFetchingMicroStep) {
                            CircularProgressIndicator(
                                color = NeonPurple,
                                strokeWidth = 3.dp,
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                        } else {
                            Button(
                                onClick = { viewModel.getNextMicroStepForCurrentMission() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("get_microstep_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonPurple
                                ),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(
                                    "CALCULATE NEXT MICRO-STEP",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                // Mission Notes Entry Area
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Mission Notes & Journal Insights") },
                    placeholder = { Text("What resources did you use? What are your notes/findings?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("mission_notes_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = CyberBorder,
                        focusedContainerColor = CyberSlate,
                        unfocusedContainerColor = CyberSlate
                    ),
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Core Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Stuck button
                    OutlinedButton(
                        onClick = onStuckClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("im_stuck_button"),
                        border = BorderStroke(1.dp, NeonPink),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPink),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = NeonPink,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "I'M STUCK",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Complete Button
                    Button(
                        onClick = {
                            viewModel.completeMission(mission, notesText, onCompleteSuccess)
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("complete_mission_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGreen),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = CyberBlack,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "COMPLETE MISSION",
                            fontWeight = FontWeight.Black,
                            color = CyberBlack,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    if (showWelcomeBackModal && savedStateForModal != null) {
        AlertDialog(
            onDismissRequest = {
                viewModel.clearFirestoreState()
                showWelcomeBackModal = false
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
                            val activeTitle = activeMission?.title ?: "Current Mission"
                            Text(
                                text = "MISSION: $activeTitle",
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
                        showWelcomeBackModal = false
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
                        showWelcomeBackModal = false
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
