package com.comradebite.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comradebite.ui.theme.CyanAccent

data class GuideStep(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
fun UserGuideScreen(isDarkTheme: Boolean) {
    val steps = listOf(
        GuideStep(
            "1. Build Your Inventory",
            "Go to 'Meal Inventory' and add the ingredients you usually have (e.g., Rice, Beans, Ugali) and their prices.",
            Icons.Default.List
        ),
        GuideStep(
            "2. Create Combinations",
            "In 'Combo Builder', combine your inventory items into full meals (e.g., Rice + Beans = 'Rice & Beans'). Set them for Breakfast, Lunch, or Dinner.",
            Icons.Default.AddCircle
        ),
        GuideStep(
            "3. Sync with Comrades",
            "Use 'Join/Switch Group' to link with your roommates. Share your 6-digit code so everyone sees the same inventory and plan!",
            Icons.Default.Share
        ),
        GuideStep(
            "4. Follow the Plan",
            "The 'Daily Plan' automatically suggests meals based on your budget and variety. Meals are marked as 'Eaten' automatically at set times.",
            Icons.Default.DateRange
        ),
        GuideStep(
            "5. Grocery Assistant",
            "Check the 'Grocery Assistant' to see what's missing for your weekly plan so you only buy what you need.",
            Icons.Default.ShoppingCart
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Welcome to ComradeBite!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                "Master your student budget and never guess what to eat again.",
                fontSize = 16.sp,
                color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Gray
            )
            Spacer(Modifier.height(8.dp))
        }

        items(steps) { step ->
            GuideItem(step, isDarkTheme)
        }
        
        item {
            Spacer(Modifier.height(32.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = CyanAccent.copy(alpha = 0.1f)
                ),
                border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = CyanAccent)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = CyanAccent)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Pro Tip: Special meals like 'Friday Night Pizza' can be marked as 'Special' in the Combo Builder to ensure they appear on the right days!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun GuideItem(step: GuideStep, isDarkTheme: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = CyanAccent.copy(alpha = 0.2f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(step.icon, contentDescription = null, tint = CyanAccent)
            }
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column {
            Text(
                step.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkTheme) Color.White else Color.Black
            )
            Text(
                step.description,
                fontSize = 14.sp,
                color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Gray,
                lineHeight = 20.sp
            )
        }
    }
}
