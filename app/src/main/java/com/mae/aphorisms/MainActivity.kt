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
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.abs
import org.json.JSONArray

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val interFontFamily = FontFamily(
    Font(googleFont = GoogleFont("Inter"), fontProvider = fontProvider)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                AphorismsScreen()
            }
        }
    }
}

@Composable
private fun AphorismsScreen() {
    val ctx = LocalContext.current
    val aphorisms = remember { loadAphorisms(ctx) }
    val vm: AphorismsViewModel = viewModel { AphorismsViewModel(aphorisms) }
    val current by vm.current.collectAsState()
    val navEnabled by vm.navigationEnabled.collectAsState()
    val view = LocalView.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = current,
                    transitionSpec = {
                        fadeIn(tween(300)).togetherWith(fadeOut(tween(300)))
                    },
                    label = "aphorism"
                ) { text ->
                    Text(
                        text = text,
                        style = TextStyle(
                            fontFamily = interFontFamily,
                            fontSize = 26.sp,
                            textAlign = TextAlign.Center,
                            color = Color.White,
                            lineHeight = 36.sp
                        ),
                        maxLines = 10,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(text) {
                                detectTapGestures(onLongPress = {
                                    copyToClipboard(ctx, text)
                                })
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(56.dp))

            OutlinedButton(
                onClick = {
                    if (navEnabled && !vm.isTransitioning) {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        vm.advance(+1)
                    }
                },
                enabled = navEnabled,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
            ) {
                Text(
                    text = "NEW",
                    fontFamily = interFontFamily,
                    letterSpacing = 4.sp
                )
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
