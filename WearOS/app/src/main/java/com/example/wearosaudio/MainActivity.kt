package com.example.wearosaudio

import android.media.AudioDeviceInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.example.wearosaudio.audio.AudioHelper
import com.example.wearosaudio.audio.AudioOutputType
import com.example.wearosaudio.audio.AudioPlayer
import com.example.wearosaudio.audio.BluetoothHelper

/**
 * MainActivity
 *
 * Integra todos os 6 passos do guia de áudio para Wear OS:
 *   1. Configuração do ambiente (Wear OS Activity + Compose)
 *   2. Verificação de saídas de áudio disponíveis
 *   3. Detecção dinâmica via AudioDeviceCallback
 *   4. Botão para abrir configurações Bluetooth quando necessário
 *   5. Reprodução de áudio via AudioPlayer
 *   6. Casos de uso: alarme, fitness, educativo
 */
class MainActivity : ComponentActivity() {

    private lateinit var audioHelper: AudioHelper
    private lateinit var audioPlayer: AudioPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Passo 2 — Inicializa o helper de áudio
        audioHelper = AudioHelper(this)
        audioPlayer = AudioPlayer(this)
        audioPlayer.initTts()

        setContent {
            WearAudioApp(
                audioHelper = audioHelper,
                audioPlayer = audioPlayer,
                onOpenBluetooth = {
                    // Passo 4 — Abre configurações Bluetooth
                    BluetoothHelper.openBluetoothSettings(this)
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Passo 3 — Registra callback ao retornar ao app
        audioHelper.registerAudioCallback()
    }

    override fun onPause() {
        super.onPause()
        // Passo 3 — Remove callback ao sair do app (evita vazamento de memória)
        audioHelper.unregisterAudioCallback()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayer.release()
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Composable principal
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun WearAudioApp(
    audioHelper: AudioHelper,
    audioPlayer: AudioPlayer,
    onOpenBluetooth: () -> Unit
) {
    // Estado reativo: tipo de saída de áudio atual
    var outputType by remember { mutableStateOf(audioHelper.getCurrentOutputType()) }
    var statusMessage by remember { mutableStateOf("Pronto") }

    // Passo 3 — Atualiza o estado quando um dispositivo é conectado/desconectado
    DisposableEffect(audioHelper) {
        audioHelper.onDeviceChanged = { _, type ->
            outputType = type
            statusMessage = when (type) {
                AudioOutputType.BLUETOOTH -> "Bluetooth conectado 🎧"
                AudioOutputType.SPEAKER   -> "Alto-falante ativo 🔊"
                AudioOutputType.NONE      -> "Sem saída de áudio"
            }
        }
        onDispose { audioHelper.onDeviceChanged = null }
    }

    MaterialTheme {
        Scaffold(
            timeText = { TimeText() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ── Indicador de saída de áudio (Passo 2) ──
                AudioStatusBadge(outputType)

                // ── Mensagem de status ──
                Text(
                    text = statusMessage,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                // ── Passo 6: Botão Alarme ──
                ActionButton(label = "⏰ Alarme") {
                    if (outputType != AudioOutputType.NONE) {
                        audioPlayer.playAlarmBeep()
                        statusMessage = "Alarme tocando!"
                    } else {
                        statusMessage = "Conecte um fone de ouvido"
                    }
                }

                // ── Passo 6: Botão Fitness ──
                ActionButton(label = "🏃 Fitness") {
                    if (outputType != AudioOutputType.NONE) {
                        audioPlayer.speakFitnessInstruction("Inicie 10 flexões agora!")
                        statusMessage = "Instrução enviada"
                    } else {
                        statusMessage = "Conecte um fone de ouvido"
                    }
                }

                // ── Passo 6: Botão Educativo ──
                ActionButton(label = "📚 Educativo") {
                    if (outputType != AudioOutputType.NONE) {
                        audioPlayer.speakEducationalFeedback("Resposta correta! Muito bem!")
                        statusMessage = "Feedback enviado"
                    } else {
                        statusMessage = "Conecte um fone de ouvido"
                    }
                }

                // ── Passo 4: Botão Bluetooth (exibido somente quando necessário) ──
                if (outputType == AudioOutputType.NONE) {
                    ActionButton(
                        label = "Conectar Bluetooth",
                        color = Color(0xFF1565C0),
                        onClick = onOpenBluetooth
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Componentes reutilizáveis
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun AudioStatusBadge(type: AudioOutputType) {
    val (icon, label, color) = when (type) {
        AudioOutputType.BLUETOOTH -> Triple("🎧", "Bluetooth A2DP", Color(0xFF42A5F5))
        AudioOutputType.SPEAKER   -> Triple("🔊", "Alto-falante", Color(0xFF66BB6A))
        AudioOutputType.NONE      -> Triple("🔇", "Sem saída", Color(0xFFEF5350))
    }
    Text(
        text = "$icon $label",
        fontSize = 13.sp,
        color = color,
        textAlign = TextAlign.Center
    )
}

@Composable
fun ActionButton(
    label: String,
    color: Color = Color(0xFF37474F),
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = color)
    ) {
        Text(text = label, fontSize = 12.sp, color = Color.White)
    }
}
