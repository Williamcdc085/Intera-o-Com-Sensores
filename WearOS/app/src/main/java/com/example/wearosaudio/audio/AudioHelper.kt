package com.example.wearosaudio.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * AudioHelper
 *
 * Passo 2 — Implementação de saídas de áudio:
 *   Verifica disponibilidade de alto-falante integrado e fone Bluetooth A2DP.
 *
 * Passo 3 — Detecção dinâmica de dispositivos de áudio:
 *   Registra AudioDeviceCallback para monitorar conexões/desconexões em tempo real.
 */
class AudioHelper(private val context: Context) {

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /** Listener exposto para a UI reagir a mudanças de dispositivo */
    var onDeviceChanged: ((hasOutput: Boolean, type: AudioOutputType) -> Unit)? = null

    // ──────────────────────────────────────────────────────────
    // Passo 2 — Verificação de saídas disponíveis
    // ──────────────────────────────────────────────────────────

    /**
     * Retorna true se o tipo de saída de áudio informado estiver disponível.
     *
     * Tipos úteis para Wear OS:
     *   • [AudioDeviceInfo.TYPE_BUILTIN_SPEAKER] — alto-falante integrado
     *   • [AudioDeviceInfo.TYPE_BLUETOOTH_A2DP]  — fone Bluetooth A2DP
     */
    fun audioOutputAvailable(type: Int): Boolean {
        // Verifica se o dispositivo possui hardware de saída de áudio
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)) {
            Log.d(TAG, "Dispositivo sem suporte a saída de áudio")
            return false
        }

        return audioManager
            .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .any { it.type == type }
    }

    /** Retorna true se o alto-falante integrado estiver disponível */
    fun isSpeakerAvailable(): Boolean =
        audioOutputAvailable(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)

    /** Retorna true se um fone de ouvido Bluetooth A2DP estiver conectado */
    fun isBluetoothHeadsetConnected(): Boolean =
        audioOutputAvailable(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)

    /** Retorna o tipo atual de saída de áudio preferida */
    fun getCurrentOutputType(): AudioOutputType = when {
        isBluetoothHeadsetConnected() -> AudioOutputType.BLUETOOTH
        isSpeakerAvailable()          -> AudioOutputType.SPEAKER
        else                          -> AudioOutputType.NONE
    }

    // ──────────────────────────────────────────────────────────
    // Passo 3 — Detecção dinâmica de dispositivos
    // ──────────────────────────────────────────────────────────

    private val audioDeviceCallback = object : AudioDeviceCallback() {

        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            super.onAudioDevicesAdded(addedDevices)
            addedDevices?.forEach { device ->
                Log.d(TAG, "Dispositivo conectado: tipo=${device.type} | ${device.productName}")
            }

            if (isBluetoothHeadsetConnected()) {
                Log.d(TAG, "Fone Bluetooth A2DP conectado — redirecionando áudio")
                onDeviceChanged?.invoke(true, AudioOutputType.BLUETOOTH)
            } else if (isSpeakerAvailable()) {
                onDeviceChanged?.invoke(true, AudioOutputType.SPEAKER)
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            super.onAudioDevicesRemoved(removedDevices)
            removedDevices?.forEach { device ->
                Log.d(TAG, "Dispositivo desconectado: tipo=${device.type} | ${device.productName}")
            }

            val currentType = getCurrentOutputType()
            onDeviceChanged?.invoke(currentType != AudioOutputType.NONE, currentType)
        }
    }

    /**
     * Inicia o monitoramento de dispositivos de áudio.
     * Chame em onResume() ou no início do ciclo de vida relevante.
     */
    fun registerAudioCallback() {
        audioManager.registerAudioDeviceCallback(
            audioDeviceCallback,
            Handler(Looper.getMainLooper())
        )
        Log.d(TAG, "AudioDeviceCallback registrado")
    }

    /**
     * Para o monitoramento de dispositivos de áudio.
     * Chame em onPause() para evitar vazamentos de memória.
     */
    fun unregisterAudioCallback() {
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        Log.d(TAG, "AudioDeviceCallback removido")
    }

    companion object {
        private const val TAG = "AudioHelper"
    }
}

/** Representa os tipos de saída de áudio reconhecidos pelo app */
enum class AudioOutputType {
    SPEAKER,   // Alto-falante integrado do Wear OS
    BLUETOOTH, // Fone de ouvido Bluetooth A2DP
    NONE       // Nenhuma saída disponível
}
