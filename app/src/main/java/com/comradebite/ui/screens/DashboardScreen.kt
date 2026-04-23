package com.comradebite.ui.screens

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comradebite.data.MealCombination
import com.comradebite.ui.theme.CyanAccent
import com.comradebite.viewmodel.MealViewModel
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(viewModel: MealViewModel) {
    val groupSize by viewModel.groupSize.collectAsState()
    val groupCode by viewModel.groupCode.collectAsState()
    val groupName by viewModel.groupName.collectAsState()
    val dailyPlan by viewModel.dailyPlan.collectAsState(initial = emptyMap())
    val allBaseMeals by viewModel.allBaseMeals.collectAsState(initial = emptyList())
    val allCombinations by viewModel.allCombinations.collectAsState(initial = emptyList())
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val context = LocalContext.current

    var joinCode by remember { mutableStateOf("") }
    var isJoining by remember { mutableStateOf(false) }

    val sentiments = listOf(
        "A hungry comrade is a weary comrade. Fuel up for excellence!",
        "Sharing a meal is the ultimate bond. Enjoy your food together!",
        "Good food is the foundation of genuine happiness and grades!",
        "Every meal on a budget is a victory. Stay strong, comrade!",
        "Fueling your dreams, one bite at a time. Eat well!"
    )
    
    var sentimentIndex by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        while(true) {
            delay(8000)
            sentimentIndex = (sentimentIndex + 1) % sentiments.size
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Crystal Sentiment Box
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(15.dp)),
                color = if (isDarkTheme) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .blur(if (android.os.Build.VERSION.SDK_INT >= 31) 25.dp else 0.dp)
                )
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.width(6.dp).height(40.dp).background(CyanAccent, RoundedCornerShape(3.dp)))
                    Spacer(Modifier.width(16.dp))
                    Crossfade(targetState = sentiments[sentimentIndex], animationSpec = tween(1000), label = "sentiment") { text ->
                        Text(
                            text = "\"$text\"",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontStyle = FontStyle.Italic,
                                color = if (isDarkTheme) Color.White else Color.Black,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Start
                            )
                        )
                    }
                }
            }
        }

        // 2. Group Sync Section (Dynamic)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (groupCode.isBlank()) CyanAccent.copy(alpha = 0.15f) else Color.Transparent
                ),
                border = BorderStroke(1.dp, if (groupCode.isBlank()) CyanAccent else Color.Gray.copy(alpha = 0.3f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    if (groupCode.isBlank()) {
                        Text("Sync with Group", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = CyanAccent)
                        Text("Enter the 6-character code from the website to join your team.", fontSize = 12.sp, color = Color.Gray)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = joinCode,
                                onValueChange = { if (it.length <= 6) joinCode = it.uppercase() },
                                label = { Text("Group Code") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent)
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (joinCode.length == 6) {
                                        isJoining = true
                                        viewModel.joinGroup(joinCode) { success, message ->
                                            isJoining = false
                                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                enabled = joinCode.length == 6 && !isJoining,
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
                            ) {
                                if (isJoining) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.Black)
                                else Icon(Icons.Default.Refresh, contentDescription = null)
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Linked to: ", fontSize = 12.sp, color = Color.Gray)
                            Text(groupName, fontWeight = FontWeight.Bold, color = CyanAccent)
                            Spacer(Modifier.weight(1f))
                            Text("CODE: $groupCode", fontSize = 10.sp, color = Color.Gray, fontStyle = FontStyle.Italic)
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Number of people eating:", 
                style = MaterialTheme.typography.headlineSmall, 
                color = if (isDarkTheme) Color.White else Color.Black,
                modifier = if (isDarkTheme) Modifier.shadow(2.dp, ambientColor = Color.Black) else Modifier
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                IconButton(onClick = { viewModel.setGroupSize(groupSize - 1) }) {
                    Text("-", color = if (isDarkTheme) Color.White else Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = groupSize.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (isDarkTheme) Color.White else Color.Black,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                IconButton(onClick = { viewModel.setGroupSize(groupSize + 1) }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = if (isDarkTheme) Color.White else Color.Black)
                }
            }
        }

        item {
            Text(
                "Daily Plan", 
                style = MaterialTheme.typography.headlineSmall, 
                color = if (isDarkTheme) Color.White else Color.Black,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        // Empty State Guidance
        if (allBaseMeals.isEmpty() || allCombinations.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyanAccent.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f))
                ) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No suggestions available yet.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Add ingredients to your Inventory and create Combinations to start getting meal plans.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        } else {
            listOf("Breakfast", "Lunch", "Dinner").forEach { time ->
                item {
                    MealSlotCard(
                        time = time,
                        combo = dailyPlan[time],
                        groupSize = groupSize,
                        viewModel = viewModel,
                        allBaseMeals = allBaseMeals,
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }
    }
}

@Composable
fun MealSlotCard(
    time: String,
    combo: MealCombination?,
    groupSize: Int,
    viewModel: MealViewModel,
    allBaseMeals: List<com.comradebite.data.BaseMeal>,
    isDarkTheme: Boolean
) {
    val mealTime = when(time) {
        "Breakfast" -> MealViewModel.BREAKFAST_TIME
        "Lunch" -> MealViewModel.LUNCH_TIME
        else -> MealViewModel.DINNER_TIME
    }
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Color.Black.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.9f)),
        border = BorderStroke(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        text = time, 
                        style = MaterialTheme.typography.headlineSmall, 
                        color = if(time == "Breakfast") (if(isDarkTheme) Color.White else Color.DarkGray) else if(time == "Lunch") CyanAccent else Color(0xFFFACC15)
                    )
                    Text(
                        text = "Scheduled: ${mealTime.format(timeFormatter)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                if (combo?.isRare == true) {
                    Surface(
                        color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, Color(0xFF8B5CF6))
                    ) {
                        Text(
                            "RARE", 
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8B5CF6),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            
            if (combo != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(combo.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    if (combo.isSpecial) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.Star, contentDescription = "Special", tint = Color(0xFFFACC15), modifier = Modifier.size(20.dp))
                    }
                }
                
                val share = viewModel.getIndividualShare(combo, allBaseMeals, groupSize) ?: 0.0
                val total = share * groupSize

                Column(
                    modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()
                ) {
                    combo.baseNames.forEach { name ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Group Total ($groupSize p)", fontWeight = FontWeight.Bold)
                        Text("KSh ${total.toInt()}")
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Your Share", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text("KSh ${share.toInt()}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                    }
                }
            } else {
                Text("No unique suggestion available.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
    }
}
