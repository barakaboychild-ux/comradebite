package com.comradebite.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comradebite.ui.theme.CyanAccent
import com.comradebite.viewmodel.MealViewModel

@Composable
fun LoginScreen(viewModel: MealViewModel, onLoginSuccess: () -> Unit) {
    // Default to Register mode for new users
    var isRegisterMode by remember { mutableStateOf(true) }
    
    val context = LocalContext.current
    val functionalSiteUrl = "https://comradebite-c0265c98.firebaseapp.com/"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isRegisterMode) "Register" else "Login",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = CyanAccent
        )
        
        Spacer(Modifier.height(16.dp))

        Text(
            text = if (isRegisterMode) {
                "Registering allows you to create your group, invite roommates, and sync your meal plans in real-time on the ComradeBite site."
            } else {
                "Welcome back! Log in to the ComradeBite site to access your shared group data and meal inventory."
            },
            fontSize = 15.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        
        Spacer(Modifier.height(48.dp))
        
        // Main Action Button - Redirects IMMEDIATELY to the functional site
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(functionalSiteUrl))
                context.startActivity(intent)
                onLoginSuccess()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
        ) {
            Text(if (isRegisterMode) "Register" else "Login", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        
        Spacer(Modifier.height(24.dp))

        // Toggle path with requested wording
        TextButton(onClick = { 
            isRegisterMode = !isRegisterMode
        }) {
            Text(
                text = if (isRegisterMode) "login instead if you have an account" else "register instead if you don't have an account",
                color = CyanAccent,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
        }
    }
}
