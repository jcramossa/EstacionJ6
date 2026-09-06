package com.example.estacionj6.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*

class VoiceAssistantManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private var tts: TextToSpeech? = TextToSpeech(context, this)

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript

    private var modelName = "gemini-1.5-flash-latest"
    private var generativeModel = GenerativeModel(
        modelName = modelName,
        apiKey = "AIzaSyB3LPEmn_4dyOri0-o-TJkz9U7IlhWK8UY"
    )

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("es", "ES")
        }
    }

    fun speak(text: String) {
        Log.e("SH_LOG", "Hub dice: $text")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun startListening(onResult: (String) -> Unit) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { _isListening.value = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { _isListening.value = false }
            override fun onError(error: Int) { _isListening.value = false }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                _transcript.value = text
                onResult(text)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { _transcript.value = it }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer.startListening(intent)
    }

    fun processIntent(text: String, onCommand: (AssistantCommand) -> Unit) {
        if (text.isEmpty()) return
        _isThinking.value = true
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.e("SH_LOG", "Pidiendo a $modelName: $text")
                val prompt = """
                    Responde SOLO un JSON: {
                      "response": "..",
                      "action": "ALARM_SET" | "MUSIC_PLAY" | "MUSIC_STOP" | "RELAY_ON" | "RELAY_OFF" | "RADIO_PLAY" | "YOUTUBE_PLAY" | "PLEX_PLAY",
                      "hour": 8,
                      "minute": 30,
                      "label": "..",
                      "isReminder": false,
                      "query": "búsqueda",
                      "season": 1,
                      "episode": 5
                    }
                    REGLA: PLEX_PLAY si pide "ver", "película" o "serie" de Plex. 
                    Extrae temporada y episodio si el usuario lo indica.
                    Frase: "$text"
                """.trimIndent()

                val result = generativeModel.generateContent(prompt)
                val fullText = result.text ?: ""
                Log.e("SH_LOG", "Respuesta IA: $fullText")

                val startIndex = fullText.indexOf("{")
                val endIndex = fullText.lastIndexOf("}")
                
                if (startIndex != -1 && endIndex != -1) {
                    val json = JSONObject(fullText.substring(startIndex, endIndex + 1))
                    val action = json.optString("action", "NONE").uppercase()
                    val spoken = json.optString("response", "Hecho")
                    
                    speak(spoken)
                    
                    when (action) {
                        "ALARM_SET" -> {
                            val h = try { json.get("hour").toString().toInt() } catch(e: Exception) { -1 }
                            val m = try { json.get("minute").toString().toInt() } catch(e: Exception) { 0 }
                            val label = json.optString("label", "").ifEmpty { null }
                            val isRem = json.optBoolean("isReminder", false)
                            if (h in 0..23) onCommand(AssistantCommand.AlarmSet(h, m, null, null, label, isRem))
                        }
                        "MUSIC_PLAY" -> onCommand(AssistantCommand.MusicPlay(json.optString("query", text)))
                        "RADIO_PLAY" -> onCommand(AssistantCommand.RadioPlay(json.optString("query", text)))
                        "YOUTUBE_PLAY" -> onCommand(AssistantCommand.YouTubePlay(json.optString("query", text)))
                        "PLEX_PLAY" -> {
                            val q = json.optString("query", text)
                            val s = json.optInt("season", -1).takeIf { it != -1 }
                            val e = json.optInt("episode", -1).takeIf { it != -1 }
                            onCommand(AssistantCommand.PlexPlay(q, s, e))
                        }
                        "MUSIC_STOP" -> onCommand(AssistantCommand.MusicStop)
                        "RELAY_ON" -> onCommand(AssistantCommand.RelayOn)
                        "RELAY_OFF" -> onCommand(AssistantCommand.RelayOff)
                        else -> onCommand(AssistantCommand.None)
                    }
                } else {
                    throw Exception("No JSON")
                }
            } catch (e: Exception) {
                Log.e("SH_LOG", "IA falló (${e.message}). Usando cerebro de emergencia.")
                val upper = text.uppercase()
                
                when {
                    upper.contains("ALARMA") || upper.contains("RECUÉRDAME") -> {
                        // Extractor de números en palabras (Básico)
                        val wordMap = mapOf(
                            "CERO" to 0, "UNA" to 1, "UNO" to 1, "DOS" to 2, "TRES" to 3, "CUATRO" to 4, "CINCO" to 5,
                            "SEIS" to 6, "SIETE" to 7, "OCHO" to 8, "NUEVE" to 9, "DIEZ" to 10, "ONCE" to 11, "DOCE" to 12,
                            "TRECE" to 13, "CATORCE" to 14, "QUINCE" to 15, "DIECISÉIS" to 16, "DIECISIETE" to 17,
                            "DIECIOCHO" to 18, "DIECINUEVE" to 19, "VEINTE" to 20, "TREINTA" to 30, "CUARENTA" to 40, "CINCUENTA" to 50
                        )
                        
                        val numbers = "\\d+".toRegex().findAll(text).map { it.value.toInt() }.toMutableList()
                        if (numbers.isEmpty()) {
                            val words = text.uppercase().split(" ")
                            var hour = -1
                            var minute = 0
                            words.forEachIndexed { i, word ->
                                wordMap[word]?.let { valNum ->
                                    if (hour == -1) hour = valNum
                                    else if (words.getOrNull(i-1) == "Y" || words.getOrNull(i-1) == "CON") minute += valNum
                                    else if (minute == 0) minute = valNum
                                }
                            }
                            if (hour != -1) { numbers.add(hour); numbers.add(minute) }
                        }

                        val h = numbers.getOrNull(0) ?: 8
                        val m = numbers.getOrNull(1) ?: 0
                        val isRem = upper.contains("RECUÉRDAME")
                        speak(if (isRem) "Recordatorio a las $h y $m" else "Alarma a las $h y $m")
                        onCommand(AssistantCommand.AlarmSet(h, m, null, null, text, isRem))
                    }
                    upper.contains("RADIO") || upper.contains("EMISORA") -> {
                        val q = text.lowercase().replace("pon la radio", "").replace("pon la emisora", "").replace("radio ", "").trim()
                        speak("Buscando emisora $q")
                        onCommand(AssistantCommand.RadioPlay(q))
                    }
                    upper.contains("QUITAR") || upper.contains("PARAR") || upper.contains("DETENER") || upper.contains("SILENCIO") || upper.contains("PARA LA") -> {
                        speak("Vale, me callo")
                        onCommand(AssistantCommand.MusicStop)
                    }
                    upper.contains("MÚSICA") || upper.contains("PON") -> {
                        val q = text.lowercase().replace("pon ", "").replace("música ", "").trim()
                        if (upper.contains("VIDEO") || upper.contains("YOUTUBE")) {
                            speak("Buscando vídeo de $q")
                            onCommand(AssistantCommand.YouTubePlay(q))
                        } else if (upper.contains("PLEX") || upper.contains("PELÍCULA") || upper.contains("SERIE")) {
                            // Intento extraer temporada y episodio del texto
                            val numbers = "\\d+".toRegex().findAll(text).map { it.value.toInt() }.toList()
                            val s = if (upper.contains("TEMPORADA")) numbers.firstOrNull() else null
                            val e = if (upper.contains("CAPÍTULO") || upper.contains("EPISODIO")) numbers.lastOrNull() else null
                            
                            val q = text.lowercase()
                                .replace("pon ", "")
                                .replace("ver ", "")
                                .replace(" en plex", "")
                                .replace("temporada \\d+".toRegex(), "")
                                .replace("capítulo \\d+".toRegex(), "")
                                .trim()
                                
                            speak("Buscando $q en Plex")
                            onCommand(AssistantCommand.PlexPlay(q, s, e))
                        } else {
                            speak("Buscando $q")
                            onCommand(AssistantCommand.MusicPlay(q))
                        }
                    }
                    upper.contains("ENCIENDE") -> { speak("Encendido"); onCommand(AssistantCommand.RelayOn) }
                    upper.contains("APAGA") -> { speak("Apagado"); onCommand(AssistantCommand.RelayOff) }
                }
            } finally {
                _isThinking.value = false
            }
        }
    }

    fun release() {
        speechRecognizer.destroy()
        tts?.stop()
        tts?.shutdown()
    }

    sealed class AssistantCommand {
        data class MusicPlay(val query: String) : AssistantCommand()
        data class RadioPlay(val query: String) : AssistantCommand()
        data class YouTubePlay(val query: String) : AssistantCommand()
        data class PlexPlay(val query: String, val season: Int? = null, val episode: Int? = null) : AssistantCommand()
        object MusicStop : AssistantCommand()
        object RelayOn : AssistantCommand()
        object RelayOff : AssistantCommand()
        data class AlarmSet(
            val hour: Int, val minute: Int, 
            val repeatDays: List<Int>? = null,
            val specificDate: String? = null,
            val label: String? = null,
            val isReminder: Boolean = false
        ) : AssistantCommand()
        object None : AssistantCommand()
    }
}
