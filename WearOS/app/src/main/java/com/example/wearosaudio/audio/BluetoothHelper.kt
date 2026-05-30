package com.example.wearosaudio.audio

import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * BluetoothHelper
 *
 * Passo 4 — Facilitando a Conexão Bluetooth:
 *   Em vez de exibir uma mensagem de erro quando não há saída de áudio,
 *   oferece ao usuário a opção de ir direto às configurações de Bluetooth.
 */
object BluetoothHelper {

    /**
     * Abre as configurações de Bluetooth do sistema com filtro para
     * dispositivos de áudio (headsets), facilitando o pareamento rápido.
     *
     * Extras utilizados:
     *   • EXTRA_CONNECTION_ONLY    — mostra apenas a tela de conexão
     *   • EXTRA_CLOSE_ON_CONNECT   — fecha a tela ao conectar
     *   • FILTER_TYPE = 1          — filtra apenas dispositivos de áudio
     */
    fun openBluetoothSettings(context: Context) {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("EXTRA_CONNECTION_ONLY", true)
            putExtra("EXTRA_CLOSE_ON_CONNECT", true)
            putExtra("android.bluetooth.devicepicker.extra.FILTER_TYPE", 1)
        }
        context.startActivity(intent)
    }
}
