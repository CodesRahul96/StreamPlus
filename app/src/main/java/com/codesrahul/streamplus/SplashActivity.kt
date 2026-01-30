package com.codesrahul.streamplus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.codesrahul.streamplus.ui.theme.StreamPlusTheme
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StreamPlusTheme {
                val scale = remember { Animatable(0f) }

                LaunchedEffect(key1 = true) {
                    scale.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 800,
                            easing = { androidx.compose.animation.core.OvershootInterpolator(2f).getInterpolation(it) }
                        )
                    )
                    delay(1200L)
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0F172A), // Slate 900
                                    Color(0xFF1E293B)  // Slate 800
                                )
                            )
                        )
                ) {
                    // Using the app logo drawable we just set up
                    // We assume R.drawable.app_logo exists or use ic_launcher_foreground if not, but we saved app_logo.png
                    // Since R class isn't generated in my context, I'll refer to it by name in code assuming valid build
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(150.dp)
                            .scale(scale.value)
                    )
                }
            }
        }
    }
}
