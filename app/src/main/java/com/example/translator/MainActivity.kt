package com.example.translator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Check permission again after returning
        checkPermissions()
    }

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val intent = Intent(this, CaptureService::class.java).apply {
                action = CaptureService.ACTION_START
                putExtra(CaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(CaptureService.EXTRA_RESULT_DATA, result.data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    private var hasOverlayPermission by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkPermissions()
        val prefs = PreferencesManager(this)

        setContent {
            var currentScreen by remember { mutableStateOf("main") }

            MaterialTheme {
                if (currentScreen == "main") {
                    MainScreen(
                        hasOverlayPermission = hasOverlayPermission,
                        onRequestOverlay = { requestOverlayPermission() },
                        onStartCapture = { requestMediaProjection() },
                        onStopCapture = { stopCaptureService() },
                        onRefresh = { refreshTranslation() },
                        onOpenSettings = { currentScreen = "settings" }
                    )
                } else {
                    SettingsScreen(
                        prefs = prefs,
                        onBack = { currentScreen = "main" }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun checkPermissions() {
        hasOverlayPermission = Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    private fun requestMediaProjection() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            return
        }
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    private fun stopCaptureService() {
        val intent = Intent(this, CaptureService::class.java).apply {
            action = CaptureService.ACTION_STOP
        }
        startService(intent)
    }

    /** Ask the running capture service to re-translate the current selection. */
    private fun refreshTranslation() {
        val intent = Intent(this, CaptureService::class.java).apply {
            action = CaptureService.ACTION_REFRESH
        }
        startService(intent)
    }
}

@Composable
fun MainScreen(
    hasOverlayPermission: Boolean,
    onRequestOverlay: () -> Unit,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF6EC1E4),
                        Color(0xFF8E9EEA),
                        Color(0xFFB98DE2)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "屏幕翻译",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "悬浮窗 · 实时 OCR · 日英译中",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )

            Spacer(Modifier.height(8.dp))

            AppGlassCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("状态", style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.7f))
                    Text(
                        text = if (hasOverlayPermission) "悬浮窗权限：已授予" else "悬浮窗权限：未授予",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (!hasOverlayPermission) {
                GlassButton("授予悬浮窗权限") { onRequestOverlay() }
            } else {
                GlassButton("开始翻译服务") { onStartCapture() }
                GlassButton("刷新翻译", primary = false) { onRefresh() }
                GlassButton("停止服务", primary = false) { onStopCapture() }
            }

            Spacer(Modifier.height(8.dp))
            GlassButton("设置", primary = false) { onOpenSettings() }
        }
    }
}

/** Reusable liquid-glass card for the in-app screens. */
@Composable
fun AppGlassCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.08f))
                )
            )
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)), RoundedCornerShape(24.dp))
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(24.dp), clip = false),
        content = content
    )
}

@Composable
fun GlassButton(text: String, primary: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(50.dp)
            .clip(shape)
            .background(
                if (primary) Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.30f))
                ) else Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.08f))
                )
            )
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)), shape)
            .shadow(elevation = 6.dp, shape = shape, clip = false)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
    }
}

@Composable
fun SettingsScreen(prefs: PreferencesManager, onBack: () -> Unit) {
    var apiKey by remember { mutableStateOf(prefs.apiKey) }
    var modelName by remember { mutableStateOf(prefs.modelName) }
    var baseUrl by remember { mutableStateOf(prefs.baseUrl) }
    var intervalMs by remember { mutableStateOf(prefs.recognitionIntervalMs.toString()) }
    var wifiOnly by remember { mutableStateOf(prefs.wifiOnly) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF6EC1E4),
                        Color(0xFF8E9EEA),
                        Color(0xFFB98DE2)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassButton("返回", primary = false, onClick = onBack)
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "设置",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(20.dp))

            AppGlassCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    GlassTextField("DeepSeek API Key", apiKey) {
                        apiKey = it
                        prefs.apiKey = it
                    }
                    GlassTextField("模型名称（如 deepseek-chat）", modelName) {
                        modelName = it
                        prefs.modelName = it
                    }
                    GlassTextField("Base URL", baseUrl) {
                        baseUrl = it
                        prefs.baseUrl = it
                    }
                    GlassTextField("识别间隔（ms，300-5000）", intervalMs) {
                        intervalMs = it
                        it.toLongOrNull()?.let { ms -> prefs.recognitionIntervalMs = ms }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "仅在 Wi-Fi 下请求",
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = wifiOnly,
                            onCheckedChange = {
                                wifiOnly = it
                                prefs.wifiOnly = it
                            }
                        )
                    }
                    Text(
                        "开启后，移动网络下将跳过翻译请求。",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/** A glass-styled text field. */
@Composable
private fun GlassTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.White.copy(alpha = 0.8f)) },
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.12f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
            focusedIndicatorColor = Color.White.copy(alpha = 0.5f),
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}
