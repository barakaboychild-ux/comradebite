package com.comradebite.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comradebite.ui.theme.LogoGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Premium Code-Only Splash Screen for ComradeBite.
 * Built entirely with icons, shapes, and words as requested.
 */
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.95f) }
    val scope = rememberCoroutineScope()

    // Ambient floating motion for code symbols
    val infiniteTransition = rememberInfiniteTransition(label = "motion")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y_offset"
    )

    LaunchedEffect(Unit) {
        scope.launch {
            alpha.animateTo(1f, tween(1500, easing = LinearOutSlowInEasing))
        }
        scope.launch {
            scale.animateTo(1f, tween(1500, easing = { fraction ->
                val tension = 2f
                val t = fraction - 1.0f
                t * t * ((tension + 1) * t + tension) + 1.0f
            }))
        }
        delay(3500)
        onTimeout()
    }

    SplashContent(alpha = alpha.value, scale = scale.value, floatOffset = floatOffset)
}

@Composable
private fun SplashContent(alpha: Float, scale: Float, floatOffset: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF03070B)), // Ultra dark background for contrast
    ) {
        // --- TOP BRANDING GROUP ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
                .scale(scale)
                .alpha(alpha)
        ) {
            // 1. The Logo Icon: Bowl + Code + Leaf (Built with shapes)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Leaf sprig + digital pixel bits
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.offset(x = 10.dp, y = (-28).dp)
                    ) {
                        Text("🌿", fontSize = 28.sp)
                        Column(modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)) {
                            Box(Modifier.size(6.dp).background(LogoGreen).offset(x = 4.dp))
                            Box(Modifier.size(4.dp).background(LogoGreen.copy(alpha = 0.6f)).offset(x = 10.dp, y = (-4).dp))
                        }
                    }
                    
                    // The White Bowl with </> Code Tag
                    Surface(
                        modifier = Modifier.size(width = 72.dp, height = 52.dp),
                        color = Color.White,
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp, topStart = 6.dp, topEnd = 6.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "</>", 
                                color = Color.Black, 
                                fontWeight = FontWeight.Black, 
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 2. App Name: ComradeBite
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold, color = Color.White)) {
                        append("Comrade")
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold, color = LogoGreen)) {
                        append("Bite")
                    }
                    append(" 🌿")
                },
                fontSize = 54.sp,
                letterSpacing = (-2).sp
            )

            // 3. Green Separator
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(2.5.dp)
                    .background(LogoGreen)
            )

            // 4. Tagline
            Text(
                text = "Program your meals with ease.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )
        }

        // --- BOTTOM HERO GROUP (Icons & Words Only) ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(450.dp)
                .alpha(alpha)
        ) {
            // Hero Radial Glow
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(340.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(LogoGreen.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
            )

            // Floating code symbols mimicking the reference
            Text("{", color = LogoGreen.copy(alpha = 0.25f), fontSize = 52.sp, modifier = Modifier.offset(x = 60.dp, y = (160 + floatOffset).dp))
            Text("</>", color = LogoGreen.copy(alpha = 0.2f), fontSize = 38.sp, modifier = Modifier.offset(x = 300.dp, y = (180 - floatOffset).dp))
            Text("}", color = LogoGreen.copy(alpha = 0.15f), fontSize = 46.sp, modifier = Modifier.offset(x = 120.dp, y = (320 + floatOffset).dp))

            // Stylized Salad Bowl representation using Icon/Emoji
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🥗", 
                    fontSize = 180.sp,
                    modifier = Modifier.scale(1.2f)
                )
            }
            
            // Bottom fade to tie into the background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF03070B).copy(alpha = 0.7f), Color(0xFF03070B))
                        )
                    )
            )
        }
    }
}

@Preview
@Composable
fun SplashPreview() {
    SplashContent(alpha = 1f, scale = 1f, floatOffset = 10f)
}
