package com.example.estacionj6

import android.Manifest
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.exoplayer.ExoPlayer
import com.example.estacionj6.data.remote.NetworkClient
import com.example.estacionj6.data.repository.HubRepositoryImpl
import com.example.estacionj6.data.repository.MusicRepository
import com.example.estacionj6.data.repository.PlexRepository
import com.example.estacionj6.data.repository.RadioRepository
import com.example.estacionj6.ui.components.SmartHubDashboard
import com.example.estacionj6.ui.main.MainViewModel
import com.example.estacionj6.util.VoiceAssistantManager

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repository = HubRepositoryImpl(
                    api = NetworkClient.api,
                    context = applicationContext
                )
                
                val musicRepository = MusicRepository(
                    api = NetworkClient.navidromeApi,
                    user = "admin",
                    pass = "admin",
                    baseUrl = "http://192.168.1.30:4533/"
                )
                
                val radioRepository = RadioRepository(
                    api = NetworkClient.radioApi
                )
                
                val plexRepository = PlexRepository(
                    api = NetworkClient.plexApi,
                    token = "Sz1L76j34TQPsX5CV7Tw",
                    baseUrl = "http://192.168.1.30:32400"
                )
                
                val voiceManager = VoiceAssistantManager(applicationContext)
                val exoPlayer = ExoPlayer.Builder(applicationContext).build()

                @Suppress("UNCHECKED_CAST")
                return MainViewModel(repository, musicRepository, radioRepository, plexRepository, voiceManager, exoPlayer) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicitar permisos de audio
        requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 0)

        // Permitir orientación horizontal en ambos sentidos (Landscape y Reverse Landscape)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        // Configurar pantalla completa total (Modo Inmersivo)
        hideSystemUI()

        // Mantener la pantalla encendida
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val state by viewModel.uiState.collectAsState()
            SmartHubDashboard(
                state = state,
                onToggleRelay = { turnOn -> viewModel.toggleRelay(turnOn) },
                onManualWake = { viewModel.manualWake() },
                onVoiceMicClick = { viewModel.startVoiceAssistant() },
                onToggleMusic = { viewModel.toggleMusic() },
                onDeleteAlarm = { id -> viewModel.deleteAlarm(id) },
                onToggleAlarm = { id, enabled -> viewModel.toggleAlarm(id, enabled) },
                onDismissAlarm = { viewModel.dismissAlarm() },
                player = viewModel.player // Necesitamos exponer el player en el ViewModel
            )
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            val controller = window.insetsController
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Soporte para versiones antiguas (como el J6 original)
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }
}