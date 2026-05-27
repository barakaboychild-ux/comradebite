package com.comradebite.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comradebite.data.MealCombination
import com.comradebite.data.BaseMeal
import com.comradebite.ui.theme.CyanAccent
import com.comradebite.viewmodel.*
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter
import androidx.core.text.HtmlCompat
import coil.compose.AsyncImage

@Composable
fun DashboardScreen(viewModel: MealViewModel, backgroundRes: Int) {
    val groupSize by viewModel.groupSize.collectAsState()
    val groupCode by viewModel.groupCode.collectAsState()
    val groupName by viewModel.groupName.collectAsState()
    val dailyPlan by viewModel.dailyPlan.collectAsState(initial = emptyMap())
    val allBaseMeals by viewModel.allBaseMeals.collectAsState(initial = emptyList())
    val allCombinations by viewModel.allCombinations.collectAsState(initial = emptyList())
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val dailyHealth by viewModel.dailyHealth.collectAsState()
    val waterToday by viewModel.waterToday.collectAsState()
    val aiHint by viewModel.aiHint.collectAsState()
    val aiThoughts by viewModel.aiThoughts.collectAsState()
    val context = LocalContext.current

    var thoughtIndex by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(aiThoughts) {
        if (thoughtIndex >= aiThoughts.size) {
            thoughtIndex = 0
        }
        while(true) {
            delay(10000)
            if (aiThoughts.isNotEmpty()) {
                thoughtIndex = (thoughtIndex + 1) % aiThoughts.size
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(Modifier.height(16.dp)) }

            // Chef AI Thoughts Header
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(15.dp)),
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.width(6.dp).height(40.dp).background(CyanAccent, RoundedCornerShape(3.dp)))
                        Spacer(Modifier.width(16.dp))
                        
                        val currentThought = if (aiThoughts.isNotEmpty()) aiThoughts[thoughtIndex % aiThoughts.size] else "Welcome, Comrade!"
                        
                        Crossfade(targetState = currentThought, animationSpec = tween(1000), label = "sentiment") { text ->
                            Text(
                                text = "\"$text\"", 
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontStyle = FontStyle.Italic, 
                                    color = if (isDarkTheme) Color.White else Color.Black, 
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }

            // Wellness Summary Row
            item {
                WellnessRow(dailyHealth, waterToday, isDarkTheme, onWaterClick = { viewModel.logWater() })
            }

            // Hero Section with Image Background
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    AsyncImage(
                        model = backgroundRes,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.8f
                    )
                    
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))

                    Column(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Number of people eating:", 
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            style = TextStyle(shadow = Shadow(color = Color.Black, blurRadius = 8f))
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.setGroupSize(groupSize - 1) },
                                modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                            ) { 
                                Text("-", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) 
                            }
                            
                            Text(
                                text = groupSize.toString(), 
                                style = MaterialTheme.typography.headlineLarge, 
                                color = CyanAccent, 
                                modifier = Modifier.padding(horizontal = 32.dp),
                                fontWeight = FontWeight.Black
                            )
                            
                            IconButton(
                                onClick = { viewModel.setGroupSize(groupSize + 1) },
                                modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                            ) { 
                                Icon(Icons.Default.Add, "Increase", tint = Color.White) 
                            }
                        }
                    }
                }
            }

            // AI Suggestion Hint
            aiHint?.let { hint ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CyanAccent.copy(alpha = 0.05f)),
                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.2f))
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(hint.icon, fontSize = 24.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = HtmlCompat.fromHtml(hint.text, HtmlCompat.FROM_HTML_MODE_LEGACY).toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Sync Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (groupCode.isBlank()) CyanAccent.copy(alpha = 0.15f) else Color.Transparent),
                    border = BorderStroke(1.dp, if (groupCode.isBlank()) CyanAccent else Color.Gray.copy(alpha = 0.3f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        if (groupCode.isBlank()) {
                            Text("Sync with Group", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = CyanAccent)
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                var joinCode by remember { mutableStateOf("") }
                                OutlinedTextField(
                                    value = joinCode, 
                                    onValueChange = { if (it.length <= 6) joinCode = it.uppercase() }, 
                                    label = { Text("Group Code") }, 
                                    modifier = Modifier.weight(1f), 
                                    singleLine = true
                                )
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = { viewModel.updateGroupCode(joinCode) }, 
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
                                ) {
                                    Icon(Icons.Default.Refresh, null)
                                }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, null, tint = CyanAccent, modifier = Modifier.size(16.dp))
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

            item { Text("Daily Plan", style = MaterialTheme.typography.headlineSmall, color = if (isDarkTheme) Color.White else Color.Black, fontWeight = FontWeight.Bold) }

            // Meals
            if (allBaseMeals.isEmpty() || allCombinations.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CyanAccent.copy(alpha = 0.1f)), border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f))) {
                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Info, null, tint = CyanAccent, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No suggestions available yet.", fontWeight = FontWeight.Bold)
                            Text("Add ingredients and create Combinations to start.", textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                listOf("Breakfast", "Lunch", "Dinner").forEach { time ->
                    item {
                        MealSlotCard(time, dailyPlan[time], groupSize, viewModel, allBaseMeals, isDarkTheme)
                    }
                }
            }
            
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun WellnessRow(health: DailyHealth, water: Int, isDarkTheme: Boolean, onWaterClick: () -> Unit) {
    val targets = listOf("carb", "protein", "vegetable", "fruit")
    val hitCount = targets.count { t -> health.groups.any { it == t || it == "mixed" } }
    val balancePct = (hitCount.toFloat() / targets.size.toFloat() * 100).toInt()

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        WellnessCell(
            modifier = Modifier.weight(1f),
            value = if (health.kcal > 0) health.kcal.toString() else "—",
            label = "🔥 kcal",
            progress = if (health.kcal > 0) (health.kcal.toFloat() / 2000f).coerceAtMost(1f) else 0f,
            color = if (health.kcal > 2200) Color.Red else if (health.kcal > 1600) Color(0xFF10B981) else Color(0xFF60A5FA),
            isDarkTheme = isDarkTheme
        )
        WellnessCell(
            modifier = Modifier.weight(1f),
            value = if (health.groups.isNotEmpty()) "$balancePct%" else "—",
            label = "🥗 balance",
            progress = balancePct.toFloat() / 100f,
            color = if (balancePct >= 75) Color(0xFF10B981) else if (balancePct >= 50) Color(0xFFF59E0B) else Color.Red,
            isDarkTheme = isDarkTheme
        )
        WellnessCell(
            modifier = Modifier.weight(1f).clickable { onWaterClick() },
            value = "$water/8",
            label = "💧 water",
            progress = (water.toFloat() / 8f).coerceAtMost(1f),
            color = if (water >= 8) Color(0xFF10B981) else CyanAccent,
            isDarkTheme = isDarkTheme
        )
    }
}

@Composable
fun WellnessCell(modifier: Modifier, value: String, label: String, progress: Float, color: Color, isDarkTheme: Boolean) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)),
        border = BorderStroke(0.5.dp, if(isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Black, fontSize = 18.sp, color = color)
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
            )
            Spacer(Modifier.height(4.dp))
            Text(label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isDarkTheme) Color.Gray else Color.DarkGray)
        }
    }
}

@Composable
fun MealSlotCard(time: String, combo: MealCombination?, groupSize: Int, viewModel: MealViewModel, allBaseMeals: List<BaseMeal>, isDarkTheme: Boolean) {
    val mealTime = when(time) { "Breakfast" -> MealViewModel.BREAKFAST_TIME; "Lunch" -> MealViewModel.LUNCH_TIME; else -> MealViewModel.DINNER_TIME }
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Color.Black.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.9f)),
        border = BorderStroke(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = time, style = MaterialTheme.typography.headlineSmall, color = if(time == "Breakfast") (if(isDarkTheme) Color.White else Color.DarkGray) else if(time == "Lunch") CyanAccent else Color(0xFFFACC15))
                    Text(text = "Scheduled: ${mealTime.format(timeFormatter)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                if (combo?.isRare == true) {
                    Surface(color = Color(0xFF8B5CF6).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, Color(0xFF8B5CF6))) {
                        Text("RARE", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            
            if (combo != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(combo.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    if (combo.isSpecial) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.Star, "Special", tint = Color(0xFFFACC15), modifier = Modifier.size(20.dp))
                    }
                }
                
                val share = viewModel.getIndividualShare(combo, allBaseMeals, groupSize)
                
                Column(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()) {
                    combo.baseNames.forEach { name ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                    
                    if (share != null && share > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Group Total", fontWeight = FontWeight.Bold)
                            Text("KSh ${(share * groupSize).toInt()}")
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Your Share", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text("KSh ${share.toInt()}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                        }
                    } else {
                        Text("Price data not available for this combination.", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontStyle = FontStyle.Italic)
                    }
                }
            } else {
                Text("No suggestion available.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        }
    }
}
