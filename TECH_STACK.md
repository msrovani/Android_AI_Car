# Android IA Car - Stack Tecnológico

Esta é a lista das tecnologias principais utilizadas no projeto, após análise e remoção de itens não utilizados:

*   **Linguagem:** Kotlin
*   **Plataforma:** Android Nativo
*   **Interface de Usuário (Celular):** Android Views (XML Layouts, `AppCompatActivity`) - *Potencialmente para configurações ou fallback.*
*   **Interface de Usuário (Carro):** Android Auto (Android for Cars App Library - `androidx.car.app`) com Templates (Message, Pane, List).
*   **Inteligência Artificial:** Google Gemini 2.0 Flash (SDK `com.google.ai.client.generativeai`)
*   **Funcionalidades de Voz:**
    *   **Síntese de Voz (TTS):** API nativa do Android (`android.speech.tts.TextToSpeech`)
    *   **Reconhecimento de Voz (STT):** API nativa do Android (`android.speech.SpeechRecognizer`)
*   **Gerenciamento de Dependências:** Gradle
*   **Injeção de Dependência:** Hilt
*   **Programação Assíncrona:** Kotlin Coroutines
*   **Gerenciamento de Estado:** Kotlin StateFlow (em `ConversationManager`)
*   **Localização:** Google Play Services Location (usada para delegar navegação)
*   **Build System:** Gradle com Kotlin DSL (`.gradle.kts`) 