package com.codesrahul.streamplus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codesrahul.streamplus.ui.theme.StreamPlusTheme
import com.codesrahul.streamplus.ui.components.PremiumBackground
import com.codesrahul.streamplus.ui.player.PlayerActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StreamPlusTheme {
                val scale = remember { Animatable(0f) }
                val alphaAnim = remember { Animatable(0f) }
                val pulseScale = remember { Animatable(1f) }

                LaunchedEffect(Unit) {
                    launch {
                        scale.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = 1000,
                                easing = { android.view.animation.OvershootInterpolator(2f).getInterpolation(it) }
                            )
                        )
                    }
                    launch {
                        delay(400)
                        alphaAnim.animateTo(1f, tween(800))
                    }
                    launch {
                        pulseScale.animateTo(
                            targetValue = 1.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                    }

                    delay(2500L)

                    val splashAction = intent?.action
                    if (splashAction == "com.codesrahul.streamplus.ACTION_PLAY_LAST") {
                        val playerIntent = Intent(this@SplashActivity, PlayerActivity::class.java)
                        playerIntent.putExtras(intent.extras ?: Bundle())
                        startActivity(playerIntent)
                    } else {
                        val mainIntent = Intent(this@SplashActivity, MainActivity::class.java)
                        when (splashAction) {
                            "com.codesrahul.streamplus.ACTION_OPEN_PLAYLIST" -> mainIntent.putExtra("START_TAB", "playlist")
                            "com.codesrahul.streamplus.ACTION_OPEN_GALLERY" -> mainIntent.putExtra("START_TAB", "local")
                            "com.codesrahul.streamplus.ACTION_OPEN_DIRECT_STREAM" -> mainIntent.putExtra("START_TAB", "direct_play")
                        }
                        startActivity(mainIntent)
                    }
                    @Suppress("DEPRECATION")
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    PremiumBackground(isSplash = true) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                // Outer glow ring
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    modifier = Modifier
                                        .size(150.dp)
                                        .scale(pulseScale.value * scale.value)
                                ) {}

                                // Logo circle
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSystemInDarkTheme()) Color.Black.copy(alpha = 0.3f)
                                            else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .size(160.dp)
                                        .scale(scale.value),
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Box(
                                            modifier = Modifier
                                                .size(115.dp)
                                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(32.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(
                                                            MaterialTheme.colorScheme.primary,
                                                            MaterialTheme.colorScheme.tertiary
                                                        )
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayCircle,
                                                contentDescription = "StreamPlus Logo",
                                                tint = Color.White,
                                                modifier = Modifier.size(65.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Text(
                                text = "StreamPlus",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 4.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .alpha(alphaAnim.value)
                                    .scale(alphaAnim.value * 0.5f + 0.5f)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Your Premium Streaming Hub",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    letterSpacing = 2.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.alpha(alphaAnim.value)
                            )
                        }
                    }
                }
            }
        }
    }
}
