package com.example.androidiacar.ai

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.androidiacar.BuildConfig // Importar BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.asTextOrNull
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedList
import java.util.Locale
import java.util.UUID

// --- Estruturas de Dados para Resposta --- 

data class KittResponse(
    val spokenResponse: String,
    val uiUpdate: UiUpdate? = null
)

data class UiUpdate(
    val templateType: TemplateType,
    val title: String? = null,
    val primaryText: String? = null,
    val secondaryText: String? = null,
    val items: List<UiListItem>? = null
)

enum class TemplateType { MESSAGE, PANE, LIST }

data class UiListItem(
    val title: String,
    val description: String? = null,
    val actionId: String? = null // Pode ser útil para identificar o clique na UI
)

// --- Classe Principal ---

class ConversationManager(
    private val context: Context,
    private val tts: TextToSpeech,
    private val coroutineScope: CoroutineScope,
    private val generativeModel: GenerativeModel
) {
    private val TAG = "ConversationManager"
    private val conversationHistory = LinkedList<Content>()
    private val maxHistory = 10
    private var currentContext = mutableMapOf<String, Any>()
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val chat = generativeModel.startChat()

    // Definir a data class com um default simples para volume
    data class SystemState(
        val isNavigating: Boolean = false,
        val currentDestination: String? = null,
        val isPlayingMusic: Boolean = false,
        val currentSong: String? = null,
        val currentArtist: String? = null,
        val volume: Int = 0, // Default simples
        val hasUnreadNotifications: Boolean = false
    )
    
    // Inicializa o StateFlow obtendo o volume atual primeiro
    private val _systemState: MutableStateFlow<SystemState>
    val systemState: StateFlow<SystemState>

    init { // Usar um bloco init para inicializar _systemState
        val initialVolume = try {
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter volume inicial", e)
            0 // Fallback para 0 em caso de erro
        }
        _systemState = MutableStateFlow(SystemState(volume = initialVolume))
        systemState = _systemState // Expor como StateFlow
    }

    // Modificar a assinatura para usar o novo callback
    fun processInput(input: String, onResponse: (KittResponse) -> Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            val defaultErrorResponse = KittResponse("Ocorreu um erro ao processar. Tente novamente.")
            try {
                // 1. Adicionar entrada do usuário ao histórico LOCAL primeiro
                val userContent = content("user") { text(input) } // Apenas a fala do usuário vai para o histórico
                updateHistory(userContent, null) // Adiciona só usuário por enquanto

                // 2. Construir o prompt com contexto ATUAL + entrada do usuário
                val fullPrompt = buildPrompt(input) // Passa o input bruto para o prompt builder
                Log.d(TAG, "Enviando prompt para Gemini: $fullPrompt")
                val promptContent = content("user") { text(fullPrompt) } // CONTEÚDO A SER ENVIADO para Gemini (NÃO adicionado ao histórico principal)

                // 3. Enviar prompt para o Gemini
                // Usamos sendMessage com o prompt completo, mas o histórico interno do chat é gerenciado pelo SDK
                val response = chat.sendMessage(promptContent) 
                val geminiRawResponse = response.text
                Log.d(TAG, "Resposta bruta do Gemini: $geminiRawResponse")

                if (geminiRawResponse != null) {
                    // 4. Adicionar resposta do modelo ao histórico LOCAL
                    val modelContent = content("model") { text(geminiRawResponse) } // Resposta JSON bruta
                    updateHistory(null, modelContent) // Atualiza histórico com a resposta

                    // 5. Processar JSON, executar ações, obter KittResponse
                    val kittResponse = processGeminiResponse(geminiRawResponse)
                    onResponse(kittResponse)
                } else {
                    Log.e(TAG, "Resposta do Gemini foi nula.")
                    onResponse(KittResponse("Desculpe, não recebi uma resposta válida."))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar entrada ou chamar Gemini: ${e.message}", e)
                onResponse(defaultErrorResponse)
            }
        }
    }

    // Atualiza o histórico local que usamos para reconstruir o chat se necessário
    private fun updateHistory(userContent: Content?, modelContent: Content?) {
        userContent?.let { conversationHistory.add(it) }
        modelContent?.let { conversationHistory.add(it) }
        while (conversationHistory.size > maxHistory * 2) {
            conversationHistory.removeFirst()
            conversationHistory.removeFirst()
        }
    }

    fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    private fun buildPrompt(input: String): String {
         val historyString = conversationHistory.takeLast(maxHistory).joinToString("\n") {
             val role = it.role?.replaceFirstChar { char -> char.uppercase() } ?: "Desconhecido"
             "$role: ${it.parts.joinToString { p -> p.asTextOrNull() ?: "[Conteúdo não textual]" }}"
         }
         val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        return """
        === Contexto do Sistema Atual ===
        Navegação Ativa: ${systemState.value.isNavigating}
        Destino Atual: ${systemState.value.currentDestination ?: "Nenhum"}
        Tocando Música: ${systemState.value.isPlayingMusic}
        Música/Artista: ${systemState.value.currentSong ?: "Nenhuma"} / ${systemState.value.currentArtist ?: "Nenhum"}
        Volume Atual (0-15): ${systemState.value.volume}
        Notificações Não Lidas: ${systemState.value.hasUnreadNotifications} (Sempre false por enquanto)
        Hora Atual: $currentTime
        
        === Histórico Recente da Conversa ===
        $historyString
        
        === Entrada Atual do Usuário ===
        Usuário: "$input"
        
        === Instruções para KITT (Você) ===
        1.  **Persona:** Você é KITT, um assistente de IA para carros. Responda em português do Brasil. Seja útil, mas mantenha um tom ligeiramente formal e tecnológico.
        2.  **Análise:** Analise a Entrada Atual do Usuário, o Histórico e o Contexto do Sistema.
        3.  **Formato JSON:** Sua resposta DEVE ser um JSON VÁLIDO com QUATRO chaves: "response", "actions", "context_updates", "ui_update".
        4.  **"response" (String):** A resposta curta e direta para o usuário ouvir. *Seja conciso!* Confirme ações importantes (navegação, mensagens).
        5.  **"actions" (JSONArray):** Ações a serem executadas pelo sistema. Lista vazia `[]` se nenhuma.
        6.  **"context_updates" (JSONObject):** Atualizações para o contexto. `{}` se não houver. (Não implementado ainda).
        7.  **"ui_update" (JSONObject ou null):** Instruções para atualizar a UI do carro.
            *   Use `null` se a UI não deve mudar.
            *   Se não for `null`, deve ter `"template_type"`.
            *   **Quando usar templates:**
                *   `MESSAGE`: Padrão para a maioria das respostas faladas simples.
                *   `PANE`: Para exibir informações relacionadas (ex: música + artista, detalhes de um local).
                *   `LIST`: *Apenas* se você estiver apresentando múltiplas opções claras para o usuário escolher (ex: resultados de busca).
        8.  **Segurança:** Se uma solicitação parecer complexa ou exigir muita interação visual (ex: ler uma lista longa), prefira uma resposta falada simples e sugira que o usuário tente novamente quando parado.
        9.  **Uso do Contexto:**
            *   Se o usuário pedir algo que já está acontecendo (ex: 'navegar para X' quando `isNavigating` é `true` e `currentDestination` é `X`), informe que a ação já está em andamento.
            *   Se o usuário pedir para pausar música e `isPlayingMusic` for `false`, informe que a música já está pausada.
            *   Mencione o `currentDestination` ou a `currentTime` na sua `response` se for relevante para a conversa.
        10. **Ambiguidade:** Peça esclarecimentos se a solicitação do usuário for ambígua (ex: 'me leve para casa' sem saber onde é casa).
        11. **Tom de Conversa (Avançado):** Se o usuário fizer perguntas gerais ou comentários não relacionados a ações do carro (resultando em action `SPEAK_ONLY`), você pode ser um pouco mais elaborado na sua "response", mas mantenha a concisão geral.
        
        === Tipos de Ação ("actions" -> "type") ===
        - NAVIGATE: {"type": "NAVIGATE", "destination": "endereço ou nome do local"}
        - PLAY_MUSIC: {"type": "PLAY_MUSIC", "command": "play/pause/next/previous/stop", "song": "(opcional)", "artist": "(opcional)"}
        - ADJUST_VOLUME: {"type": "ADJUST_VOLUME", "level": 0-15} // Usar apenas se o usuário pedir especificamente
        - READ_NOTIFICATIONS: {"type": "READ_NOTIFICATIONS"} // Responda que ainda não consegue
        - CALL: {"type": "CALL", "number": "número ou nome do contato"} // Abrirá o discador
        - SEND_MESSAGE: {"type": "SEND_MESSAGE", "to": "contato/número", "message": "texto"} // Abrirá app de SMS
        - SPEAK_ONLY: {"type": "SPEAK_ONLY"} // Use se nenhuma outra ação for necessária (perguntas gerais, etc.)

        === Tipos de UI ("ui_update" -> "template_type") ===
        - MESSAGE: {"template_type": "MESSAGE", "primary_text": "texto conciso"}
        - PANE: {"template_type": "PANE", "title": "título opcional", "primary_text": "texto principal", "secondary_text": "texto secundário opcional"}
        - LIST: {"template_type": "LIST", "title": "título da lista", "items": [{"title": "Item 1", "description": "descrição opcional", "actionId": "ID único ou comando relacionado ao item"}, ...]} // Use com moderação! Para actionId, tente usar o próprio título do item se for uma entidade (ex: nome de local), ou um comando curto relacionado.

        === Exemplo de Resposta JSON ===
        {
          "response": "Ok, iniciando navegação para o Shopping Central.",
          "actions": [{"type": "NAVIGATE", "destination": "Shopping Central"}],
          "context_updates": {},
          "ui_update": {"template_type": "MESSAGE", "primary_text": "Navegando para Shopping Central..."}
        }
        
        Agora, gere o JSON para a entrada "$input".
        """
    }

    // Modificar o tipo de retorno e a lógica de parsing
    private fun processGeminiResponse(jsonResponse: String): KittResponse {
        var spokenResponse = "Desculpe, tive dificuldade em processar a resposta." 
        var uiUpdate: UiUpdate? = null

        try {
            Log.d(TAG, "Processando JSON: $jsonResponse")
            val json = JSONObject(jsonResponse)
            spokenResponse = json.optString("response", spokenResponse)
            val actions = json.optJSONArray("actions") ?: JSONArray()
            val contextUpdates = json.optJSONObject("context_updates") // Extrair o objeto de atualizações
            val uiUpdateJson = json.optJSONObject("ui_update")

            // --- Processar context_updates PRIMEIRO --- 
            if (contextUpdates != null && contextUpdates.length() > 0) {
                Log.d(TAG, "Processando context_updates: ${contextUpdates}")
                var currentState = _systemState.value // Pegar o estado atual
                
                // Tentar atualizar cada campo conhecido
                if (contextUpdates.has("isNavigating")) {
                    currentState = currentState.copy(isNavigating = contextUpdates.optBoolean("isNavigating", currentState.isNavigating))
                }
                if (contextUpdates.has("currentDestination")) {
                    // optString retorna string vazia se não for string ou nulo se for explicitamente null
                    val dest = contextUpdates.optString("currentDestination", currentState.currentDestination)
                    // Tratar string vazia como null aqui, pois queremos null se não houver destino
                    currentState = currentState.copy(currentDestination = dest.ifEmpty { null })
                }
                if (contextUpdates.has("isPlayingMusic")) {
                    currentState = currentState.copy(isPlayingMusic = contextUpdates.optBoolean("isPlayingMusic", currentState.isPlayingMusic))
                }
                 if (contextUpdates.has("currentSong")) {
                     val song = contextUpdates.optString("currentSong", currentState.currentSong)
                     currentState = currentState.copy(currentSong = song.ifEmpty { null })
                 }
                 if (contextUpdates.has("currentArtist")) {
                     val artist = contextUpdates.optString("currentArtist", currentState.currentArtist)
                     currentState = currentState.copy(currentArtist = artist.ifEmpty { null })
                 }
                 // TODO: Poderia adicionar volume e hasUnreadNotifications se Gemini precisar controlá-los

                // Atualizar o StateFlow APENAS se houve mudanças
                if (currentState != _systemState.value) {
                    Log.i(TAG, "SystemState atualizado por Gemini: $currentState")
                    _systemState.value = currentState
                } else {
                    Log.d(TAG, "context_updates não resultou em mudanças no SystemState.")
                }
            }

            // --- Processar ui_update --- 
            if (uiUpdateJson != null) {
                try {
                    val templateStr = uiUpdateJson.optString("template_type").uppercase()                    
                    val templateType = try { TemplateType.valueOf(templateStr) } catch (e: IllegalArgumentException) { null }
                    
                    if (templateType != null) {
                        val title = uiUpdateJson.optString("title", null)
                        val primaryText = uiUpdateJson.optString("primary_text", null)
                        val secondaryText = uiUpdateJson.optString("secondary_text", null)
                        val itemsJson = uiUpdateJson.optJSONArray("items")
                        var itemsList: List<UiListItem>? = null

                        if (itemsJson != null && templateType == TemplateType.LIST) {
                            itemsList = mutableListOf()
                            for (i in 0 until itemsJson.length()) {
                                val itemJson = itemsJson.optJSONObject(i)
                                if (itemJson != null) {
                                    itemsList.add(UiListItem(
                                        title = itemJson.optString("title") ?: "Item sem título",
                                        description = itemJson.optString("description", null),
                                        actionId = itemJson.optString("actionId", null)
                                    ))
                                }
                            }
                        }
                        
                        uiUpdate = UiUpdate(templateType, title, primaryText, secondaryText, itemsList)
                         Log.d(TAG, "UI Update processado: $uiUpdate")
                    } else {
                         Log.w(TAG, "Tipo de template UI desconhecido ou ausente: $templateStr")
                    }
                } catch (e: JSONException) {
                     Log.e(TAG, "Erro ao processar ui_update JSON: ${uiUpdateJson.toString()}", e)
                     // Continua sem uiUpdate
                }
            }

            // --- Processar actions DEPOIS das atualizações de contexto --- 
            if (actions.length() == 0 && uiUpdate == null) {
                 Log.d(TAG, "Nenhuma ação ou UI update para executar. Apenas falando.")
            }

            for (i in 0 until actions.length()) {
                try {
                    val action = actions.getJSONObject(i)
                    val actionType = action.optString("type") ?: ""
                    Log.d(TAG, "Executando ação: $actionType")

                    when (actionType) {
                        "NAVIGATE" -> {
                            val destination = action.optString("destination") ?: ""
                            if (destination.isNotEmpty()) {
                                // Feedback falado já está na 'spokenResponse' do JSON principal
                                startNavigation(destination)
                            } else {
                                Log.w(TAG, "Ação NAVIGATE sem destino.")
                                // A resposta falada já deve pedir esclarecimento
                            }
                        }
                        "PLAY_MUSIC" -> {
                            val command = action.optString("command") ?: "play"
                            val song = action.optString("song", null)
                            val artist = action.optString("artist", null)
                            controlMusic(command, song, artist)
                        }
                        "ADJUST_VOLUME" -> {
                            val level = action.optInt("level", -1)
                            adjustVolume(level) // Validação dentro da função
                        }
                        "READ_NOTIFICATIONS" -> readNotifications()
                        "CALL" -> {
                            val number = action.optString("number") ?: ""
                            if (number.isNotEmpty()) makeCall(number)
                            else Log.w(TAG, "Ação CALL sem número.")
                        }
                        "SEND_MESSAGE" -> {
                            val to = action.optString("to") ?: ""
                            val message = action.optString("message") ?: ""
                            if (to.isNotEmpty() && message.isNotEmpty()) sendMessage(to, message)
                            else Log.w(TAG, "Ação SEND_MESSAGE incompleta.")
                        }
                         "SPEAK_ONLY" -> Log.d(TAG, "Ação SPEAK_ONLY.")
                        else -> Log.w(TAG, "Tipo de ação desconhecido: $actionType")
                    }
                } catch (e: JSONException) {
                    Log.e(TAG, "Erro ao processar action JSON: ${actions.optJSONObject(i)?.toString()}", e)
                } catch (e: Exception) {
                     Log.e(TAG, "Erro inesperado ao executar ação ${actions.optJSONObject(i)?.toString()}: ${e.message}", e)
                     // A resposta falada geral já foi definida
                }
            }

        } catch (e: JSONException) {
            Log.e(TAG, "Erro CRÍTICO ao processar JSON principal: $jsonResponse", e)
            // Se o JSON principal falhar, retorna o erro padrão com mensagem mais específica
            spokenResponse = "Desculpe, não entendi a resposta do assistente."
            return KittResponse(spokenResponse, null) // Retorna imediatamente
        } catch (e: Exception) {
             Log.e(TAG, "Erro inesperado CRÍTICO no processGeminiResponse: ${e.message}", e)
              // Erro genérico inesperado
              spokenResponse = "Ocorreu um erro inesperado ao processar a resposta."
              return KittResponse(spokenResponse, null) // Retorna imediatamente
        }
        
        // Retorna a resposta falada e as instruções de UI processadas
        return KittResponse(spokenResponse, uiUpdate)
    }

    // --- Funções de Ação (sem grandes mudanças, apenas logging e validação interna) ---

    private fun startNavigation(destination: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("google.navigation:q=${Uri.encode(destination)}")
                // Tentar direcionar para o Maps, mas pode falhar se não instalado
                setPackage("com.google.android.apps.maps") 
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            _systemState.value = systemState.value.copy(isNavigating = true, currentDestination = destination)
            Log.i(TAG, "Navegação iniciada para: $destination")
        } catch (e: android.content.ActivityNotFoundException) {
            Log.e(TAG, "Erro ao iniciar navegação (ActivityNotFound): ${e.message}", e)
            // Tentar sem setPackage como fallback
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("google.navigation:q=${Uri.encode(destination)}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                 _systemState.value = systemState.value.copy(isNavigating = true, currentDestination = destination)
                 Log.i(TAG, "Navegação iniciada (fallback) para: $destination")
            } catch (e2: android.content.ActivityNotFoundException) {
                 Log.e(TAG, "Erro ao iniciar navegação (Fallback ActivityNotFound): ${e2.message}", e2)
                 speak("Não consegui encontrar um aplicativo de navegação para iniciar. O Google Maps está instalado?")
            } catch (e2: Exception) {
                 Log.e(TAG, "Erro inesperado ao iniciar navegação (Fallback): ${e2.message}", e2)
                 speak("Ocorreu um erro inesperado ao tentar iniciar a navegação.")
            }
        } catch (e: Exception) {
             Log.e(TAG, "Erro inesperado ao iniciar navegação para $destination: ${e.message}", e)
             speak("Ocorreu um erro inesperado ao tentar iniciar a navegação.")
        }
    }

    private fun controlMusic(command: String, song: String? = null, artist: String? = null) {
        val action = when (command.lowercase()) {
            "play" -> "com.android.music.musicservicecommand.togglepause" // Pode ser play ou pause
            "pause" -> "com.android.music.musicservicecommand.pause"
            "next" -> "com.android.music.musicservicecommand.next"
            "previous" -> "com.android.music.musicservicecommand.previous"
            "stop" -> "com.android.music.musicservicecommand.stop"
            else -> null
        }
        if (action != null) {
            try {
                val intent = Intent(action)
                context.sendBroadcast(intent)
                Log.i(TAG, "Broadcast de música enviado: $action")

                // ATENÇÃO: Atualização do estado da música é uma HEURÍSTICA.
                // Assume que o broadcast foi recebido e o estado mudou conforme o comando.
                // Não há garantia de que o estado real corresponde a isso.
                // Ações como 'togglepause' também tornam a previsão de 'isPlayingMusic' incerta.
                val newIsPlayingMusic = when (command.lowercase()) {
                    "play" -> true // Suposição
                    "pause", "stop" -> false // Suposição
                    else -> systemState.value.isPlayingMusic // Mantém para next/previous
                }
                _systemState.value = systemState.value.copy(
                    isPlayingMusic = newIsPlayingMusic,
                    currentSong = if (command == "play" && song != null) song else systemState.value.currentSong, // Atualiza só se for play com info
                    currentArtist = if (command == "play" && artist != null) artist else systemState.value.currentArtist // Atualiza só se for play com info
                )
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao enviar broadcast de música ($action): ${e.message}", e)
                speak("Tive um problema ao controlar a música.")
            }
        } else {
             Log.w(TAG, "Comando de música inválido: $command")
        }
    }

    private fun adjustVolume(level: Int) {
        try {
             val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
             if (level in 0..maxVolume) {
                 val safeLevel = level.coerceIn(0, maxVolume)
                 audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, safeLevel, AudioManager.FLAG_SHOW_UI)
                 _systemState.value = systemState.value.copy(volume = safeLevel)
                 Log.i(TAG, "Volume ajustado para: $safeLevel")
             } else {
                  Log.w(TAG, "Nível de volume inválido: $level (Max: $maxVolume)")
                  speak("Não posso ajustar para esse nível de volume.")
             }
        } catch (e: Exception) {
             Log.e(TAG, "Erro ao ajustar volume para $level: ${e.message}", e)
             speak("Não consegui ajustar o volume.")
        }
    }

    private fun readNotifications() {
        Log.i(TAG, "Função readNotifications chamada, mas não implementada.")
        speak("Ainda não consigo ler notificações.")
        _systemState.value = systemState.value.copy(hasUnreadNotifications = false) 
    }

    private fun makeCall(number: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${Uri.encode(number)}")
                 addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.i(TAG, "Discador aberto para: $number")
        } catch (e: Exception) {
             Log.e(TAG, "Erro ao abrir discador para $number: ${e.message}", e)
             speak("Não consegui iniciar a chamada.")
        }
    }

    private fun sendMessage(to: String, message: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(to)}")
                putExtra("sms_body", message)
                 addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.i(TAG, "App de SMS aberto para: $to")
        } catch (e: Exception) {
             Log.e(TAG, "Erro ao abrir app de SMS para $to: ${e.message}", e)
             speak("Não consegui preparar a mensagem.")
        }
    }
} 