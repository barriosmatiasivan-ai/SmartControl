package com.oscar.smartcontrol

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.oscar.smartcontrol.ui.RemoteScreen
import com.oscar.smartcontrol.ui.RemoteViewModel

private val SmartDarkColors = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF00202E),
    primaryContainer = Color(0xFF143647),
    onPrimaryContainer = Color(0xFFCBEFFF),
    secondary = Color(0xFF80CBC4),
    background = Color(0xFF0B0E12),
    surface = Color(0xFF131820),
    surfaceVariant = Color(0xFF1C232E),
    onBackground = Color(0xFFECEFF4),
    onSurface = Color(0xFFECEFF4),
    onSurfaceVariant = Color(0xFF9AA7B8),
    error = Color(0xFFFF6B6B)
)

class MainActivity : ComponentActivity() {

    private val viewModel: RemoteViewModel by viewModels()

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startVoice()
        }
    }

    private val mediaPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.castMedia(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleShareIntent(intent)

        setContent {
            MaterialTheme(colorScheme = SmartDarkColors) {
                RemoteScreen(
                    viewModel = viewModel,
                    onRequestVoice = {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) {
                            viewModel.startVoice()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            false
                        }
                    },
                    onPickMediaToCast = {
                        mediaPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    onOpenSystemCast = {
                        try {
                            startActivity(Intent(Settings.ACTION_CAST_SETTINGS))
                        } catch (_: Exception) {
                        }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onForeground()
    }

    @Suppress("DEPRECATION")
    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            if (uri != null) {
                viewModel.castMedia(uri)
            }
        }
    }
}
