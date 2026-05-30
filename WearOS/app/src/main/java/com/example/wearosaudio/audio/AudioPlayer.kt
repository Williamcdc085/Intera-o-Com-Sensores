package com.example.wearosaudio.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * AudioPlayer
 *
 * Passo 5 — Reprodução de Áudio:
 *   Após detectar uma saída válida, a reprodução no Wear OS funciona
 *   exatamente como em dispositivos móveis. Aqui encapsulamos MediaPlayer,
 *   ToneGenerator e TextToSpeech para cobrir os casos de uso do Passo 6.
 *
 * Passo 6 — Casos de uso de alto-falantes em Wear OS:
 *   • Alarme com beep sonoro
 *   • Instruções de voz para exercícios (TTS)
 *   • Feedback auditivo educativo (TTS)
 */
class AudioPlayer(private val context: Context) {

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var mediaPlayer: MediaPlayer? = null
    private var toneGenerator: ToneGenerator? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    // ──────────────────────────────────────────────────────────
    // TextToSpeech — inicialização única
    // ──────────────────────────────────────────────────────────

    fun initTts(onReady: (() -> Unit)? = null) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("pt", "BR")
                isTtsReady = true
                Log.d(TAG, "TTS inicializado com sucesso")
                onReady?.invoke()
            } else {
                Log.e(TAG, "Falha ao inicializar TTS: código=$status")
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // Passo 6 — Alarme: beep sonoro com ToneGenerator
    // ──────────────────────────────────────────────────────────

    /**
     * Reproduz um beep de alarme.
     * Útil para notificações sonoras em relógios Wear OS (ex.: alarme de hora).
     */
    fun playAlarmBeep() {
        try {
            toneGenerator?.release()
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 800)
            Log.d(TAG, "Beep de alarme reproduzido")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao reproduzir beep: ${e.message}")
        }
    }

    // ──────────────────────────────────────────────────────────
    // Passo 6 — App de fitness: instruções de voz via TTS
    // ──────────────────────────────────────────────────────────

    /**
     * Fala uma instrução de exercício em voz alta.
     * Ex.: "Agora faça 10 flexões" ou "Inicie o trote leve"
     */
    fun speakFitnessInstruction(instruction: String) {
        if (!isTtsReady) {
            Log.w(TAG, "TTS ainda não está pronto. Chame initTts() antes.")
            return
        }
        requestAudioFocus(AudioManager.STREAM_MUSIC) {
            tts?.speak(instruction, TextToSpeech.QUEUE_FLUSH, null, "fitness_$instruction")
            Log.d(TAG, "Instrução de fitness falada: $instruction")
        }
    }

    // ──────────────────────────────────────────────────────────
    // Passo 6 — App educativo: feedback auditivo
    // ──────────────────────────────────────────────────────────

    /**
     * Fala um feedback educativo para o usuário.
     * Ex.: "Resposta correta! Muito bem!" ou "Tente novamente."
     */
    fun speakEducationalFeedback(feedback: String) {
        if (!isTtsReady) {
            Log.w(TAG, "TTS ainda não está pronto. Chame initTts() antes.")
            return
        }
        requestAudioFocus(AudioManager.STREAM_MUSIC) {
            tts?.speak(feedback, TextToSpeech.QUEUE_ADD, null, "edu_$feedback")
            Log.d(TAG, "Feedback educativo falado: $feedback")
        }
    }

    // ──────────────────────────────────────────────────────────
    // Passo 5 — Reprodução genérica via MediaPlayer (URI ou res)
    // ──────────────────────────────────────────────────────────

    /**
     * Reproduz um arquivo de áudio a partir de um raw resource.
     * Ex.: R.raw.alarme
     */
    fun playRawAudio(resId: Int, onComplete: (() -> Unit)? = null) {
        stopAudio()
        requestAudioFocus(AudioManager.STREAM_MUSIC) {
            mediaPlayer = MediaPlayer.create(context, resId).apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setOnCompletionListener {
                    Log.d(TAG, "Reprodução concluída")
                    onComplete?.invoke()
                    releaseMediaPlayer()
                }
                start()
            }
            Log.d(TAG, "Reprodução iniciada: resId=$resId")
        }
    }

    fun stopAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            releaseMediaPlayer()
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // ──────────────────────────────────────────────────────────
    // Gerenciamento de foco de áudio
    // ──────────────────────────────────────────────────────────

    private fun requestAudioFocus(streamType: Int, onGranted: () -> Unit) {
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setLegacyStreamType(streamType)
                    .build()
            )
            .setOnAudioFocusChangeListener { focusChange ->
                Log.d(TAG, "Mudança de foco de áudio: $focusChange")
            }
            .build()

        val result = audioManager.requestAudioFocus(focusRequest)
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            onGranted()
        } else {
            Log.w(TAG, "Foco de áudio não concedido")
        }
    }

    // ──────────────────────────────────────────────────────────
    // Liberação de recursos
    // ──────────────────────────────────────────────────────────

    fun release() {
        stopAudio()
        toneGenerator?.release()
        toneGenerator = null
        tts?.shutdown()
        tts = null
        isTtsReady = false
    }

    companion object {
        private const val TAG = "AudioPlayer"
    }
}
