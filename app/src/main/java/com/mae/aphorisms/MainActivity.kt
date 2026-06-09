package com.mae.aphorisms

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.abs
import org.json.JSONArray

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AphorismsApp()
        }
    }
}

@Composable
private fun AphorismsApp() {
    val ctx = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    } else {
        if (isDark) darkColorScheme(
            background = Color(0xFF1C1B1F),
            surface = Color(0xFF2B2930)
        ) else lightColorScheme(
            background = Color(0xFFFAF6EF),
            onBackground = Color(0xFF1C1B1F)
        )
    }

    MaterialTheme(colorScheme = colorScheme) {
        AphorismsScreen()
    }
}

@Composable
private fun AphorismsScreen() {
    val ctx = LocalContext.current
    val aphorisms = remember { loadAphorisms(ctx) }
    val vm: AphorismsViewModel = viewModel { AphorismsViewModel(aphorisms) }
    val current by vm.current.collectAsState()
    val counter by vm.counter.collectAsState()
    val navEnabled by vm.navigationEnabled.collectAsState()
    val view = LocalView.current

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp)
                    .pointerInput(navEnabled) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onDragEnd = { totalDrag = 0f },
                            onDragCancel = { totalDrag = 0f }
                        ) { _, dragAmount ->
                            totalDrag += dragAmount
                            if (navEnabled && !vm.isTransitioning && abs(totalDrag) > 80f) {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                vm.advance(if (totalDrag < 0) +1 else -1)
                                totalDrag = 0f
                            }
                        }
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = current,
                        transitionSpec = {
                            fadeIn(animationSpec = androidx.compose.animation.core.tween(300))
                                .togetherWith(fadeOut(animationSpec = androidx.compose.animation.core.tween(300)))
                        },
                        label = "aphorism"
                    ) { text ->
                        Text(
                            text = text,
                            style = TextStyle(
                                fontFamily = FontFamily.Serif,
                                fontSize = 24.sp,
                                textAlign = TextAlign.Center
                            ),
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.pointerInput(text) {
                                detectTapGestures(onLongPress = {
                                    copyToClipboard(ctx, text)
                                })
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (counter.isNotEmpty()) {
                Text(
                    text = counter,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    if (navEnabled && !vm.isTransitioning) {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        vm.advance(+1)
                    }
                },
                enabled = navEnabled
            ) {
                Text("Next")
            }
        }
    }
}

private fun loadAphorisms(ctx: Context): List<String> {
    return try {
        val json = ctx.assets.open("aphorisms.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        List(arr.length()) { i -> arr.getString(i) }.filter { it.isNotEmpty() }
    } catch (e: Exception) {
        Log.w("AphorismsMAE", "Failed to load aphorisms.json: ${e.message}")
        AphorismsViewModel.FALLBACK_APHORISMS
    }
}

private fun copyToClipboard(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("aphorism", text))
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
    }
}
