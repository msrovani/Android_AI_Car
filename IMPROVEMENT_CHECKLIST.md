# Android IA Car - Checklist de Melhorias

Esta lista detalha as melhorias e correções sugeridas para o projeto, priorizadas por urgência e impacto.

## 1. Core Components & Injeção de Dependência (Hilt) - Urgente

*   [ ] **Resolver Inicializações Duplicadas/Inconsistentes:**
    *   [ ] Injetar `GenerativeModel` via Hilt em `MainCarScreen.kt` (remover inicialização manual).
    *   [ ] Injetar `CoroutineScope` via Hilt em `MainCarScreen.kt` (remover `CoroutineScope(Dispatchers.Main)` manual).
    *   [ ] **Centralizar Gerenciamento de `TextToSpeech`:**
        *   [ ] Remover inicialização do `TextToSpeech` em `MainActivity.kt` (se não for essencial ali).
        *   [ ] Garantir que `TextToSpeech` seja inicializado e gerenciado corretamente dentro do ciclo de vida do Android Auto (provavelmente em `MainCarScreen.kt`, passando o `CarContext` e o `UtteranceProgressListener`). Considerar se Hilt pode ajudar (pode ser complexo devido ao listener e contexto).
    *   [ ] **Centralizar Gerenciamento de `ConversationManager`:**
        *   [ ] Remover inicialização do `ConversationManager` em `MainActivity.kt`.
        *   [ ] Inicializar `ConversationManager` em `MainCarScreen.kt` (ou `MainCarSession` se fizer mais sentido), injetando as dependências (`Context`, `TTS`, `CoroutineScope`, `GenerativeModel`) corretamente (usando as instâncias injetadas/gerenciadas).
*   [ ] **Revisar Escopos de Coroutines:**
    *   [ ] Verificar se o `CoroutineScope` injetado para `ConversationManager` (via `MainCarScreen`) usa `Dispatchers.IO` ou `Dispatchers.Default` para operações de longa duração (Gemini API, processamento JSON).
    *   [ ] Usar `withContext(Dispatchers.Main)` dentro do `ConversationManager` apenas para interações que *precisam* da thread principal (como chamar o `onResponse` callback, que já está na Main em `MainCarScreen`).
    *   [ ] Usar `lifecycleScope` em `MainCarScreen.kt` para coroutines que devem seguir o ciclo de vida da tela.

## 2. Limpeza de Código e Dependências - Importante

*   [ ] **Remover Dependências Não Utilizadas (`app/build.gradle.kts`):**
    *   [ ] Remover `implementation("com.squareup.retrofit2:retrofit:...")`
    *   [ ] Remover `implementation("com.squareup.retrofit2:converter-gson:...")`
    *   [ ] Remover `implementation("com.squareup.okhttp3:okhttp:...")`
    *   [ ] Remover `implementation("com.squareup.okhttp3:logging-interceptor:...")`
    *   [ ] Remover `implementation("androidx.media3:...")` (todas as dependências Media3)
*   [ ] **Remover `SettingsActivity.kt` (se não utilizada):**
    *   [ ] Excluir o arquivo `SettingsActivity.kt`.
    *   [ ] Excluir o layout `activity_settings.xml`.
    *   [ ] Remover a declaração da `<activity android:name=".SettingsActivity" ... />` do `AndroidManifest.xml`.
*   [ ] **Revisar e Remover Permissões Não Utilizadas (`AndroidManifest.xml`):**
    *   [ ] Avaliar a necessidade real e remover (se não usadas):
        *   [ ] `android.permission.READ_EXTERNAL_STORAGE`
        *   [ ] `android.permission.WRITE_EXTERNAL_STORAGE`
        *   [ ] `android.permission.ACCESS_BACKGROUND_LOCATION`
        *   [ ] `android.permission.TTS_ENGINE` (provavelmente desnecessária)
*   [ ] **Atualizar `README.md`:**
    *   [ ] Remover menção a `GeminiService.kt`.
    *   [ ] Corrigir a descrição das funcionalidades para refletir o estado atual (ex: remover "Controle de direção do carro").
    *   [ ] Verificar se a estrutura do projeto descrita ainda é válida.

## 3. Ações, Estado e Tratamento de Erros - Médio

*   [ ] **Melhorar Ação `controlMusic` (`ConversationManager.kt`):**
    *   [ ] (Opcional, Avançado) Investigar o uso de `MediaSessionCompat` ou `MediaBrowserServiceCompat` para interagir com players de mídia de forma mais robusta e obter o estado real (`isPlayingMusic`, `currentSong`, `currentArtist`).
    *   [ ] Atualizar `SystemState` com base no estado real obtido (se usando MediaSession) ou manter a heurística atual com um comentário sobre suas limitações.
*   [ ] **Melhorar Ação `startNavigation` (`ConversationManager.kt`):**
    *   [ ] Adicionar `try-catch (ActivityNotFoundException)` em volta de `context.startActivity(intent)`.
    *   [ ] No `catch`, usar `speak()` para informar ao usuário que o app de navegação não foi encontrado.
*   [ ] **Implementar (ou manter como não implementado) `readNotifications` (`ConversationManager.kt`):**
    *   [ ] Decidir se a funcionalidade é essencial. Se sim, implementar `NotificationListenerService` (complexo, requer permissão especial).
    *   [ ] Se não, manter a resposta atual "Ainda não consigo..."
*   [ ] **Refinar Tratamento de Erros:**
    *   [ ] Em `processGeminiResponse`: Capturar exceções JSON específicas e fornecer `spokenResponse` mais detalhadas (ex: "Erro ao processar a resposta do assistente").
    *   [ ] Em `processGeminiResponse`: No `catch` de execução de ações, tentar identificar a ação que falhou e informar o usuário (ex: "Não consegui iniciar a navegação").
    *   [ ] Em `MainCarScreen`: Usar `CarToast.makeText(...)` em `showTemporaryError` para feedback menos intrusivo.
*   [ ] **Gerenciamento de Estado (`SystemState`):**
    *   [ ] Revisar como e quando o `SystemState` é atualizado para garantir que reflete o melhor possível o estado real (considerar limitações de obter estado de apps externos).

## 4. Permissões e UI - Baixo

*   [ ] **Centralizar e Otimizar Solicitação de Permissões:**
    *   [ ] Remover verificações/solicitações duplicadas em `MainActivity.kt` (se não essencial).
    *   [ ] Focar a solicitação de permissões em `MainCarScreen.kt` (ou onde a funcionalidade é usada).
    *   [ ] (Opcional) Implementar solicitação de permissão "just-in-time" (ex: pedir `RECORD_AUDIO` apenas ao clicar no microfone pela primeira vez).
*   [ ] **Refinar UI do Android Auto (`MainCarScreen.kt`):**
    *   [ ] Implementar `handleListItemClick` se o template `LIST` for usado com itens clicáveis no futuro.
    *   [ ] Revisar a lógica de `isProcessing` vs `isLoading` para garantir clareza e robustez, assegurando que o botão de microfone sempre volte ao estado correto. Considerar unificar em `isBusy` se simplificar. 