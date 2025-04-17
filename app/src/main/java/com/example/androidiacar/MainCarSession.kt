package com.example.androidiacar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.model.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.androidiacar.ai.*
import com.example.androidiacar.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import androidx.core.graphics.drawable.IconCompat
import javax.inject.Inject
import androidx.car.app.CarToast

class MainCarScreen(
    carContext: CarContext,
    private val generativeModel: GenerativeModel,
    private val coroutineScope: CoroutineScope
) : Screen(carContext), DefaultLifecycleObserver {
    private val TAG = "MainCarScreen"
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var conversationManager: ConversationManager
    
    // Estado da UI a ser renderizada
    private var currentUiUpdate: UiUpdate? = 
        UiUpdate(TemplateType.MESSAGE, primaryText = "KITT pronto. Toque para interagir.")
    private var lastSpokenText: String = ""
    private var isBusy: Boolean = false

    init {
        lifecycle.addObserver(this)
        // Inicializa TTS. O ConversationManager será inicializado no callback do TTS.
        setupTTS()
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        Log.d(TAG, "onCreate")
        // Solicitar permissões de localização no início
        requestLocationPermissionsIfNeeded()
        // Configurar o SpeechRecognizer (mas sem pedir permissão de áudio aqui)
        setupSpeechRecognizerInitially()
    }
    
    // Renomeada - Pede apenas Localização
    private fun requestLocationPermissionsIfNeeded() {
        val permissionsNeeded = mutableListOf<String>()
        // Remover checagem de RECORD_AUDIO daqui
        if (ContextCompat.checkSelfPermission(carContext, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
         if (ContextCompat.checkSelfPermission(carContext, Manifest.permission.ACCESS_COARSE_LOCATION) 
             != PackageManager.PERMISSION_GRANTED) {
             permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION)
         }

        if (permissionsNeeded.isNotEmpty()) {
            Log.i(TAG, "Solicitando permissões de localização: ${permissionsNeeded.joinToString()}")
            carContext.requestPermissions(permissionsNeeded) { granted, rejected ->
                // Remover lógica de RECORD_AUDIO daqui
                if (granted.contains(Manifest.permission.ACCESS_FINE_LOCATION) || granted.contains(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                     Log.i(TAG, "Permissão de localização concedida.")
                     // TODO: Poderia forçar atualização do contexto aqui se necessário
                }
                 if (rejected.isNotEmpty()) {
                     Log.w(TAG, "Permissões de localização negadas: ${rejected.joinToString()}")
                     // Mostrar erro específico para localização, se desejado
                     showPermissionError(rejected) // Ou uma mensagem mais específica
                 }
            }
        } else {
            Log.i(TAG, "Permissões de localização já concedidas.")
             // TODO: Atualizar contexto de localização se necessário
        }
    }

    // Renomeada - Apenas tenta configurar, não pede permissão
    private fun setupSpeechRecognizerInitially() {
        // NÃO verificar permissão aqui, será feito just-in-time
        try {
            if (SpeechRecognizer.isRecognitionAvailable(carContext)) {
                // Criar o listener uma vez
                val recognitionListener = object : RecognitionListener { 
                    override fun onReadyForSpeech(params: android.os.Bundle?) { Log.d(TAG, "SR: Pronto") }
                    override fun onBeginningOfSpeech() { Log.d(TAG, "SR: Começou a falar") ; setBusyState(true) } // Usar isBusy
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { Log.d(TAG, "SR: Fim da fala") ; /* setBusyState(false) será chamado em onResults ou onError */ }
                    override fun onError(error: Int) {
                        Log.e(TAG, "SR Error: $error")
                        setBusyState(false) // Usar isBusy
                    }
                    override fun onResults(results: android.os.Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            Log.i(TAG, "SR Result: ${matches[0]}")
                            processVoiceCommand(matches[0]) // setBusyState(true) é chamado aqui
                        } else {
                            Log.w(TAG, "SR: Nenhum resultado encontrado.")
                            setBusyState(false) // Garante que volte ao normal se não houver resultado
                        }
                    }
                    override fun onPartialResults(partialResults: android.os.Bundle?) {}
                    @Deprecated("Deprecated in Java")
                    override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
                 }

                // Apenas criar a instância se não existir, mas não garante funcionalidade sem permissão
                if (speechRecognizer == null) {
                     speechRecognizer = SpeechRecognizer.createSpeechRecognizer(carContext).apply {
                        setRecognitionListener(recognitionListener)
                     }
                     Log.i(TAG, "Instância de SpeechRecognizer criada (permissão será verificada no uso).")
                } else {
                     // Se já existe, talvez reatribuir o listener? Ou não fazer nada.
                     // Por segurança, vamos garantir que o listener está atualizado.
                     speechRecognizer?.setRecognitionListener(recognitionListener)
                }
            } else {
                 Log.e(TAG, "Reconhecimento de voz não disponível neste dispositivo.")
                 // Não mostrar erro aqui, pois o usuário ainda não tentou usar
            }
        } catch (e: Exception) {
             Log.e(TAG, "Erro ao criar instância de SpeechRecognizer: ${e.message}", e)
              // Não mostrar erro aqui
        }
    }

    private fun setupTTS() {
        Log.d(TAG, "Iniciando TTS")
        tts = TextToSpeech(carContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.i(TAG, "TTS inicializado com sucesso.")
                tts?.language = Locale("pt", "BR")
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { setBusyState(true) }
                    override fun onDone(utteranceId: String?) { setBusyState(false) }
                    override fun onError(utteranceId: String?) { setBusyState(false) }
                })

                // A verificação ::generativeModel.isInitialized não é mais necessária
                // pois recebemos a instância pelo construtor.
                initializeConversationManager()

            } else {
                Log.e(TAG, "Falha na inicialização do TTS. Status: $status")
                showInitializationError("Erro no sistema de voz.")
            }
        }
    }

     private fun initializeConversationManager() {
        // A verificação ::generativeModel.isInitialized não é mais necessária
        // pois recebemos a instância pelo construtor.
        if (tts != null) { // Verificar apenas TTS
            conversationManager = ConversationManager(
                carContext,
                tts!!,
                coroutineScope, // Usar a instância do construtor
                generativeModel // Usar a instância do construtor
            )
            Log.i(TAG, "ConversationManager inicializado.")
            invalidateScreen() // Garante que a tela inicial seja exibida
        } else {
             Log.e(TAG, "Tentativa de inicializar ConversationManager sem TTS.")
             showInitializationError("Falha ao carregar assistente.")
        }
    }

    private fun processVoiceCommand(command: String) {
        if (!::conversationManager.isInitialized) {
             Log.e(TAG, "ConversationManager não inicializado ao processar comando.")
             showTemporaryError("Assistente não está pronto.")
             setBusyState(false)
             return
        }
        setBusyState(true)
        conversationManager.processInput(command) { kittResponse ->
            lifecycleScope.launch(Dispatchers.Main) {
                lastSpokenText = kittResponse.spokenResponse
                speak(lastSpokenText)
                currentUiUpdate = kittResponse.uiUpdate ?: 
                    UiUpdate(TemplateType.MESSAGE, primaryText = lastSpokenText)
                invalidateScreen()
            }
        }
    }
    
    private fun speak(text: String) {
        if (tts == null) {
             Log.e(TAG, "TTS não inicializado ao tentar falar.")
             showTemporaryError("Erro no sistema de voz.")
             setBusyState(false)
             return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
        Log.i(TAG, "Falando: $text")
    }

    // Nova função para pedir permissão de áudio just-in-time
    private fun requestAudioPermissionIfNeeded(onGranted: () -> Unit) {
        if (ContextCompat.checkSelfPermission(carContext, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permissão de áudio já concedida.")
            onGranted()
        } else {
            Log.i(TAG, "Solicitando permissão de áudio...")
            carContext.requestPermissions(listOf(Manifest.permission.RECORD_AUDIO)) { granted, _ ->
                if (granted.contains(Manifest.permission.RECORD_AUDIO)) {
                    Log.i(TAG, "Permissão de áudio concedida pelo usuário.")
                    onGranted()
                } else {
                    Log.w(TAG, "Permissão de áudio negada pelo usuário.")
                    CarToast.makeText(carContext, "Permissão de microfone necessária para usar a voz", CarToast.LENGTH_LONG).show()
                    setBusyState(false)
                }
            }
        }
    }

    private fun startVoiceRecognition() {
        requestAudioPermissionIfNeeded { 
            Log.d(TAG, "Permissão de áudio OK, iniciando reconhecimento...")
            if (speechRecognizer == null) {
                 Log.e(TAG, "SpeechRecognizer NULO ao tentar ouvir, mesmo após permissão.")
                 setupSpeechRecognizerInitially()
                 if (speechRecognizer == null) {
                    showTemporaryError("Erro ao iniciar microfone.")
                    setBusyState(false)
                    return@requestAudioPermissionIfNeeded
                 }
            }
            if (isBusy) {
                Log.w(TAG, "Tentativa de iniciar reconhecimento enquanto já está ocupado (após permissão).")
                return@requestAudioPermissionIfNeeded
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Ouvindo...")
            }
            Log.d(TAG, "Iniciando speechRecognizer.startListening(intent)...")
            try {
                 speechRecognizer?.startListening(intent)
             } catch (e: Exception) {
                 Log.e(TAG, "Erro ao chamar startListening: ${e.message}", e)
                 showTemporaryError("Erro ao ativar microfone.")
                 setBusyState(false)
             }
        }
    }

    private fun setBusyState(busy: Boolean) {
        if (isBusy != busy) {
            isBusy = busy
            Log.d(TAG, "Busy state: $isBusy")
            invalidateScreen()
        }
    }

    private fun invalidateScreen() {
        // Garante que invalidate() seja chamado na thread principal
        lifecycleScope.launch(Dispatchers.Main) {
            invalidate()
        }
    }
    
    // --- Funções de UI Error --- 
    
    private fun showPermissionError(rejectedPermissions: List<String>) {
        val message = "KITT precisa das seguintes permissões para funcionar corretamente: ${rejectedPermissions.joinToString()}"
        currentUiUpdate = UiUpdate(TemplateType.MESSAGE, primaryText = message)
        lastSpokenText = ""
        setBusyState(false)
        invalidateScreen()
    }

    private fun showInitializationError(message: String) {
        currentUiUpdate = UiUpdate(TemplateType.MESSAGE, primaryText = "Erro na inicialização: $message")
        lastSpokenText = ""
        setBusyState(false)
        invalidateScreen()
    }
    
     private fun showTemporaryError(message: String) {
         Log.e(TAG, "Erro temporário: $message")
         setBusyState(false)
         CarToast.makeText(carContext, message, CarToast.LENGTH_SHORT).show()
     }

    // --- Construção do Template --- 

    override fun onGetTemplate(): Template {
        Log.d(TAG, "onGetTemplate chamado. Busy: $isBusy, UI Update: ${currentUiUpdate?.templateType}")
        val actionStrip = ActionStrip.Builder()
            .addAction(createTalkAction())
            .build()
        return when (currentUiUpdate?.templateType) {
            TemplateType.PANE -> buildPaneTemplate(currentUiUpdate!!, actionStrip)
            TemplateType.LIST -> buildListTemplate(currentUiUpdate!!, actionStrip)
            TemplateType.MESSAGE -> buildMessageTemplate(currentUiUpdate!!, actionStrip)
            else -> buildMessageTemplate( 
                UiUpdate(TemplateType.MESSAGE, primaryText = lastSpokenText.ifEmpty { "KITT pronto." }), 
                actionStrip
            )
        }
    }

    private fun buildMessageTemplate(uiUpdate: UiUpdate, actionStrip: ActionStrip): MessageTemplate {
        return MessageTemplate.Builder(uiUpdate.primaryText ?: "...")
            .setTitle("KITT")
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(actionStrip)
            .build()
    }

    private fun buildPaneTemplate(uiUpdate: UiUpdate, actionStrip: ActionStrip): PaneTemplate {
        val paneBuilder = Pane.Builder()
        uiUpdate.primaryText?.let { paneBuilder.addRow(Row.Builder().setTitle(it).build()) }
        uiUpdate.secondaryText?.let { paneBuilder.addRow(Row.Builder().setTitle(it).build()) }
        // TODO: Adicionar botões de ação no Pane se uiUpdate especificar

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle(uiUpdate.title ?: "KITT")
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(actionStrip)
            .build()
    }

    private fun buildListTemplate(uiUpdate: UiUpdate, actionStrip: ActionStrip): ListTemplate {
        val itemListBuilder = ItemList.Builder()
        uiUpdate.items?.forEach { item ->
            val rowBuilder = Row.Builder().setTitle(item.title)
            item.description?.let { rowBuilder.addText(it) }
            // Descomentar e definir onClickListener
            rowBuilder.setOnClickListener { 
                handleListItemClick(item.actionId, item.title)
            }
            itemListBuilder.addItem(rowBuilder.build())
        }

        return ListTemplate.Builder()
            .setSingleList(itemListBuilder.build())
            .setTitle(uiUpdate.title ?: "KITT")
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(actionStrip)
            .build()
    }
    
    // Cria a Ação para o botão de falar/microfone
    private fun createTalkAction(): Action {
        val iconCompat = IconCompat.createWithResource(carContext, R.drawable.ic_mic)
        return Action.Builder()
            .setIcon(CarIcon.Builder(iconCompat).build()) // Usar IconCompat
            .setOnClickListener(ParkedOnlyOnClickListener.create {
                Log.d(TAG, "Botão Interagir clicado.")
                startVoiceRecognition()
            })
            .setFlags(Action.FLAG_PRIMARY)
            .setEnabled(!isBusy)
            .build()
    }
    
    // Descomentar a função e implementar lógica básica
    private fun handleListItemClick(actionId: String?, title: String) { 
        val id = actionId ?: "(nenhum id)"
        Log.i(TAG, "Item da lista clicado: Título='$title', ActionID='$id'")
        
        // Lógica Inicial: Tratar o clique como um comando de voz usando o título do item.
        // Isso permite ações como clicar em um nome de local sugerido para iniciar a navegação.
        if (title.isNotEmpty()) {
            CarToast.makeText(carContext, "Processando: $title", CarToast.LENGTH_SHORT).show()
            processVoiceCommand(title) 
        } else {
            Log.w(TAG, "Item da lista clicado sem título válido.")
            CarToast.makeText(carContext, "Ação inválida para este item", CarToast.LENGTH_SHORT).show()
        }
        
        /* Lógica futura poderia ser mais sofisticada:
        if (actionId != null) {
             if (actionId.startsWith("http")) { // Exemplo: Abrir URL?
                 // ... 
             } else if (actionId.startsWith("COMMAND:")) { // Exemplo: Comando específico?
                 processVoiceCommand(actionId.removePrefix("COMMAND:"))
             } else { // Tratar como comando de voz (título ou ID)
                 processVoiceCommand(actionId) 
             }
        } else if (title.isNotEmpty()) { // Fallback para o título
             processVoiceCommand(title)
        } else { ... }
        */
    }

    // --- Ciclo de Vida --- 

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Log.d(TAG, "onDestroy")
        tts?.stop()
        tts?.shutdown()
        tts = null
        speechRecognizer?.destroy()
        speechRecognizer = null
        // Não precisamos remover o observer manualmente com DefaultLifecycleObserver
    }
}

// Modificar construtor da Session para receber dependências
class MainCarSession(
    private val generativeModel: GenerativeModel,
    private val coroutineScope: CoroutineScope
) : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        // Passar dependências para a Screen
        return MainCarScreen(carContext, generativeModel, coroutineScope)
    }
} 