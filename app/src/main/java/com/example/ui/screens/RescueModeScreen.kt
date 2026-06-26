package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.MissionPilotViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescueModeScreen(
    viewModel: MissionPilotViewModel,
    onBackClick: () -> Unit
) {
    val activeMission by viewModel.activeMission.collectAsState()
    val rescueStrategy by viewModel.rescueStrategy.collectAsState()
    val isRescuing by viewModel.isRescuing.collectAsState()

    var selectedReason by remember { mutableStateOf<String?>(null) }

    val obstacles = listOf(
        Pair("Difficult", "🧠 Too Hard / Complex"),
        Pair("Distracted", "📵 Sidetracked / Distracted"),
        Pair("Tired", "🔋 Exhausted / No Energy"),
        Pair("Bored", "🥱 Boring / Tedious"),
        Pair("No Motivation", "🔥 Resistant to Start")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "RECOVERY PROTOCOL",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonPink,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.clearRescue()
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "PILOT RESCUE ACTIVE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonPink,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Why Have You Stalled?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AeroBlue
                    )
                    Text(
                        text = "Select your current obstacle. The Recovery Agent will instantly formulate a personalized bypass strategy to get you moving.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }

                // Grid selection of reasons
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    obstacles.forEach { (id, label) ->
                        val isSelected = selectedReason == id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedReason = id
                                    viewModel.activateRescueMode(id)
                                }
                                .testTag("rescue_reason_card_$id"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) NeonPink.copy(alpha = 0.15f) else CyberSlate
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) NeonPink else CyberBorder
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when (id) {
                                            "Difficult" -> Icons.Default.Warning
                                            "Distracted" -> Icons.Default.PhoneAndroid
                                            "Tired" -> Icons.Default.BatteryAlert
                                            "Bored" -> Icons.Default.Face
                                            else -> Icons.Default.OfflineBolt
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) NeonPink else ElectricBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = label,
                                        color = if (isSelected) NeonPink else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = NeonPink,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Gemini Rescue Result Box
                AnimatedVisibility(
                    visible = selectedReason != null,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CyberSlate),
                        border = BorderStroke(1.dp, NeonPink.copy(alpha = 0.6f)),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OfflineBolt,
                                    contentDescription = null,
                                    tint = NeonPink,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "🛟 RECOVERY ENGINE BYPASS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonPink,
                                    letterSpacing = 1.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))

                            if (isRescuing) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(
                                            color = NeonPink,
                                            strokeWidth = 4.dp,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            "Calculating reframe metrics...",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            } else {
                                rescueStrategy?.let { strategy ->
                                    Text(
                                        text = strategy,
                                        fontSize = 14.sp,
                                        color = TextPrimary,
                                        lineHeight = 20.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Button(
                                        onClick = {
                                            viewModel.clearRescue()
                                            onBackClick()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("execute_recovery_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NeonPink
                                        ),
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Text(
                                            "EXECUTE RECOVERY PLAN",
                                            fontWeight = FontWeight.ExtraBold,
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
}
