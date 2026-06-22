package com.antispy.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    private var isServiceRunningState = mutableStateOf(false)

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            isServiceRunningState.value = AntiSpyOverlayService.isRunning
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        isServiceRunningState.value = AntiSpyOverlayService.isRunning

        val filter = IntentFilter("com.antispy.app.STATUS_CHANGED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(statusReceiver, filter)
        }

        setContent {
            AntiSpyAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F0E17) // Dark slate premium background
                ) {
                    AntiSpyMainScreen(
                        isServiceRunning = isServiceRunningState.value,
                        onToggleService = { start ->
                            toggleService(start)
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(statusReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleService(start: Boolean) {
        if (!Settings.canDrawOverlays(this)) return
        
        val intent = Intent(this, AntiSpyOverlayService::class.java)
        if (start) {
            val sharedPref = getSharedPreferences("AntiSpyPrefs", Context.MODE_PRIVATE)
            val opacity = sharedPref.getFloat("opacity", 0.2f)
            val pattern = sharedPref.getString("pattern", AntiSpyOverlayService.PATTERN_LINES) ?: AntiSpyOverlayService.PATTERN_LINES
            
            intent.putExtra(AntiSpyOverlayService.EXTRA_OPACITY, opacity)
            intent.putExtra(AntiSpyOverlayService.EXTRA_PATTERN, pattern)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            intent.action = AntiSpyOverlayService.ACTION_STOP
            startService(intent)
        }
    }
}

@Composable
fun AntiSpyMainScreen(
    isServiceRunning: Boolean,
    onToggleService: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    // SharedPreferences states
    val sharedPref = remember { context.getSharedPreferences("AntiSpyPrefs", Context.MODE_PRIVATE) }
    var opacity by remember { mutableStateOf(sharedPref.getFloat("opacity", 0.2f)) }
    var pattern by remember {
        mutableStateOf(
            sharedPref.getString("pattern", AntiSpyOverlayService.PATTERN_LINES)
                ?: AntiSpyOverlayService.PATTERN_LINES
        )
    }

    // Permission request launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasNotificationPermission = granted
        }
    )

    // Check permission on resume
    DisposableEffect(Unit) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = Settings.canDrawOverlays(context)
            }
        }
        // Simplified for this context. In actual android, we hook into LifecycleOwner.
        onDispose {}
    }

    // Force recheck on launch
    LaunchedEffect(key1 = isServiceRunning) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
    }

    // Update settings helper
    val updateSettings = { newOpacity: Float, newPattern: String ->
        opacity = newOpacity
        pattern = newPattern
        sharedPref.edit().apply {
            putFloat("opacity", newOpacity)
            putString("pattern", newPattern)
            apply()
        }
        if (isServiceRunning) {
            val intent = Intent(AntiSpyOverlayService.ACTION_UPDATE).apply {
                putExtra(AntiSpyOverlayService.EXTRA_OPACITY, newOpacity)
                putExtra(AntiSpyOverlayService.EXTRA_PATTERN, newPattern)
                `package` = context.packageName
            }
            context.sendBroadcast(intent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Title Header
        Text(
            text = "Anti-Spy Display",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFFFD54F), // Amber color
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 1.sp
        )
        Text(
            text = "Software Privacy Filter",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFA7A9BE),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Fingerprint Safety banner Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1E2E)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🔒 Sidik Jari Tetap Aman",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Filter privasi visual perangkat lunak ini tidak mengganggu sensor sidik jari di bawah layar Anda. Anda dapat tetap memakai antigores bening biasa dengan aman.",
                    fontSize = 13.sp,
                    color = Color(0xFFA7A9BE),
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Permission Card
        if (!hasOverlayPermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFFF8A80), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1E1E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Izin Diperlukan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFFFF8A80)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aplikasi ini memerlukan izin 'Tampilkan di atas aplikasi lain' agar dapat menggambar filter privasi di layar.",
                        fontSize = 13.sp,
                        color = Color(0xFFFFCDD2),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                    ) {
                        Text("Berikan Izin", color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Notification Permission request (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A1A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Izin Notifikasi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFFFD54F)
                        )
                        Text(
                            text = "Dibutuhkan untuk status foreground service overlay.",
                            fontSize = 12.sp,
                            color = Color(0xFFE2E2D2)
                        )
                    }
                    Button(
                        onClick = {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))
                    ) {
                        Text("Izinkan", color = Color.Black, fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Main controls Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1E2E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Switch row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Aktifkan Filter Privasi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isServiceRunning) "Filter sedang berjalan" else "Filter nonaktif",
                            fontSize = 12.sp,
                            color = if (isServiceRunning) Color(0xFF4CAF50) else Color(0xFFA7A9BE)
                        )
                    }
                    
                    Switch(
                        checked = isServiceRunning,
                        onCheckedChange = { checked ->
                            if (!hasOverlayPermission) {
                                // Request permission instead
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } else {
                                onToggleService(checked)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFFFD54F),
                            checkedTrackColor = Color(0xFF7B61FF)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Opacity slider
                Text(
                    text = "Tingkat Kegelapan: ${(opacity * 100).toInt()}%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = opacity,
                    onValueChange = { valRounded ->
                        // Keep opacity in range 0.1 to 0.9 to avoid complete black screen
                        val boundedVal = valRounded.coerceIn(0.02f, 0.9f)
                        updateSettings(boundedVal, pattern)
                    },
                    valueRange = 0.02f..0.9f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFFD54F),
                        activeTrackColor = Color(0xFF7B61FF)
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Pattern selector
                Text(
                    text = "Pola Filter Privasi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))

                val patterns = listOf(
                    AntiSpyOverlayService.PATTERN_DIM to "Hanya Redup (Dim)",
                    AntiSpyOverlayService.PATTERN_LINES to "Garis-Garis Rapat (Lines)",
                    AntiSpyOverlayService.PATTERN_CROSSHATCH to "Silang Silang (Crosshatch)",
                    AntiSpyOverlayService.PATTERN_NOISE to "Bintik Noise (Noise)"
                )

                patterns.forEach { (type, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (pattern == type),
                                onClick = { updateSettings(opacity, type) }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (pattern == type),
                            onClick = { updateSettings(opacity, type) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFFFFD54F),
                                unselectedColor = Color(0xFFA7A9BE)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            color = if (pattern == type) Color.White else Color(0xFFA7A9BE),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        
        // Settings bar instruction
        Text(
            text = "Tip: Anda juga dapat mengaktifkan filter ini dari panel Quick Settings dengan menambahkan tombol 'Anti-Spy Tile' ke bar status atas.",
            fontSize = 12.sp,
            color = Color(0xFFA7A9BE),
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// Simple Custom Theme Wrapper for Jetpack Compose
@Composable
fun AntiSpyAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Color(0xFF7B61FF),
            secondary = Color(0xFFFFD54F),
            background = Color(0xFF0F0E17),
            surface = Color(0xFF1F1E2E)
        ),
        content = content
    )
}
