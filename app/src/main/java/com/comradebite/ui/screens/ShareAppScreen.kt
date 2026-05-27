package com.comradebite.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comradebite.ui.theme.CyanAccent

@Composable
fun ShareAppScreen(isDarkTheme: Boolean) {
    val context = LocalContext.current
    // The exact Vercel link and message provided by you
    val downloadLink = "https://comradebite.vercel.app/"
    val shareText = "Hey! Check out ComradeBite to manage your meals perfectly: $downloadLink"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CyanAccent.copy(alpha = 0.1f),
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(60.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "Spread the Love!",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isDarkTheme) Color.White else Color.Black,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "ComradeBite is better with friends. Share it with your roommates and classmates to help them master their budget too!",
            fontSize = 16.sp,
            color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        
        Spacer(Modifier.height(24.dp))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = CyanAccent.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Download Link:", fontSize = 12.sp, color = CyanAccent, fontWeight = FontWeight.Bold)
                Text(downloadLink, fontSize = 14.sp, color = if(isDarkTheme) Color.White else Color.Black)
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Share ComradeBite via")
                context.startActivity(shareIntent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text("Share with Comrades", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(Modifier.height(16.dp))
        
        Text(
            "Together, we conquer the hunger!",
            fontSize = 12.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            color = CyanAccent.copy(alpha = 0.6f)
        )
    }
}
