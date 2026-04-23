package com.comradebite.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comradebite.ui.theme.CyanAccent
import com.comradebite.viewmodel.MealViewModel

@Composable
fun TimetableScreen(viewModel: MealViewModel) {
    val weeklyPlan by viewModel.weeklyTimetable.collectAsState(initial = emptyList())
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Weekly Meal Plan", 
                style = MaterialTheme.typography.headlineSmall, 
                color = if (isDarkTheme) Color.White else Color.Black,
                fontWeight = FontWeight.Bold
            )
        }

        itemsIndexed(weeklyPlan) { index, dayPlan ->
            if (index < days.size) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = days[index],
                            style = MaterialTheme.typography.titleLarge,
                            color = CyanAccent,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(8.dp))
                        
                        dayPlan.forEach { (time, combo) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = time, 
                                    style = MaterialTheme.typography.bodyMedium, 
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = combo?.name ?: "No suggestion",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isDarkTheme) Color.White else Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
