# Android IA Car - Checklist de Melhorias

Esta lista detalha as melhorias e correções sugeridas para o projeto, priorizadas por urgência e impacto.

## 1. Core Components & Injeção de Dependência (Hilt) - Urgente

*   [x] **Resolver Inicializações Duplicadas/Inconsistentes:**
    *   [x] Injetar `GenerativeModel` via Hilt em `MainCarScreen.kt` (remover inicialização manual). (Feito via Service/Session)
    *   [x] Injetar `CoroutineScope` via Hilt em `MainCarScreen.kt` (remover `CoroutineScope(Dispatchers.Main)` manual). (Feito via Service/Session)
    *   [x] **Centralizar Gerenciamento de `TextToSpeech`:**
        *   [x] Remover inicialização do `TextToSpeech` em `MainActivity.kt` (se não for essencial ali).
        *   [x] Garantir que `TextToSpeech` seja inicializado e gerenciado corretamente dentro do ciclo de vida do Android Auto (provavelmente em `MainCarScreen.kt`, passando o `CarContext` e o `UtteranceProgressListener`). (Feito em MainCarScreen)
    *   [x] **Centralizar Gerenciamento de `ConversationManager`:**
        *   [x] Remover inicialização do `ConversationManager` em `MainActivity.kt`.
        *   [x] Inicializar `ConversationManager` em `MainCarScreen.kt` (ou `MainCarSession` se fizer mais sentido), injetando as dependências (`Context`, `TTS`, `CoroutineScope`, `GenerativeModel`) corretamente (usando as instâncias injetadas/gerenciadas). (Feito em MainCarScreen, recebendo deps via construtor)
*   [x] **Revisar Escopos de Coroutines:**
    *   [x] Verificar se o `CoroutineScope` injetado para `ConversationManager` (via `MainCarScreen`) usa `Dispatchers.IO` ou `Dispatchers.Default` para operações de longa duração (Gemini API, processamento JSON). (Confirmado, usa IO via AppModule)
    *   [x] Usar `withContext(Dispatchers.Main)` dentro do `ConversationManager` apenas para interações que *precisam* da thread principal (como chamar o `onResponse` callback, que já está na Main em `MainCarScreen`). (Verificado, não necessário pois MainCarScreen já usa lifecycleScope/Main)
    *   [x] Usar `lifecycleScope` em `MainCarScreen.kt` para coroutines que devem seguir o ciclo de vida da tela. (Feito para updates de UI/TTS)

## 2. Limpeza de Código e Dependências - Importante

*   [x] **Remover Dependências Não Utilizadas (`app/build.gradle.kts`):**
    *   [x] Remover `implementation("com.squareup.retrofit2:retrofit:...")`
    *   [x] Remover `implementation("com.squareup.retrofit2:converter-gson:...")`
    *   [x] Remover `implementation("com.squareup.okhttp3:okhttp:...")`
    *   [x] Remover `implementation("com.squareup.okhttp3:logging-interceptor:...")`
    *   [x] Remover `implementation("androidx.media3:...")` (todas as dependências Media3)
*   [x] **Remover `SettingsActivity.kt` (se não utilizada):**
    *   [x] Excluir o arquivo `SettingsActivity.kt`.
    *   [x] Excluir o layout `activity_settings.xml`.
    *   [x] Remover a declaração da `<activity android:name=".SettingsActivity" ... />` do `AndroidManifest.xml`.
*   [x] **Revisar e Remover Permissões Não Utilizadas (`AndroidManifest.xml`):**
    *   [x] Avaliar a necessidade real e remover (se não usadas):
        *   [x] `android.permission.READ_EXTERNAL_STORAGE`
        *   [x] `android.permission.WRITE_EXTERNAL_STORAGE`
        *   [x] `android.permission.ACCESS_BACKGROUND_LOCATION`
        *   [x] `android.permission.TTS_ENGINE` (provavelmente desnecessária)
        *   [x] `android.permission.READ_PHONE_STATE` (Removido na revisão final)
*   [x] **Atualizar `README.md`:**
    *   [x] Remover menção a `GeminiService.kt`.
    *   [x] Corrigir a descrição das funcionalidades para refletir o estado atual (ex: remover "Controle de direção do carro").
    *   [x] Verificar se a estrutura do projeto descrita ainda é válida.

## 3. Ações, Estado e Tratamento de Erros - Médio

*   [x] **Melhorar Ação `controlMusic` (`ConversationManager.kt`):**
    *   [ ] (Opcional, Avançado) Investigar o uso de `MediaSessionCompat` ou `MediaBrowserServiceCompat` para interagir com players de mídia de forma mais robusta e obter o estado real (`isPlayingMusic`, `currentSong`, `currentArtist`).
    *   [x] Atualizar `SystemState` com base no estado real obtido (se usando MediaSession) ou manter a heurística atual com um comentário sobre suas limitações. (Feito, mantido heurística com comentário)
*   [x] **Melhorar Ação `startNavigation` (`ConversationManager.kt`):**
    *   [x] Adicionar `try-catch (ActivityNotFoundException)` em volta de `context.startActivity(intent)`.
    *   [x] No `catch`, usar `speak()` para informar ao usuário que o app de navegação não foi encontrado.
*   [x] **Implementar (ou manter como não implementado) `readNotifications` (`ConversationManager.kt`):**
    *   [ ] Decidir se a funcionalidade é essencial. Se sim, implementar `NotificationListenerService` (complexo, requer permissão especial).
    *   [x] Se não, manter a resposta atual "Ainda não consigo..." (Mantido)
*   [x] **Refinar Tratamento de Erros:**
    *   [x] Em `processGeminiResponse`: Capturar exceções JSON específicas e fornecer `spokenResponse` mais detalhadas (ex: "Erro ao processar a resposta do assistente"). (Feito para JSONException crítica)
    *   [ ] Em `processGeminiResponse`: No `catch` de execução de ações, tentar identificar a ação que falhou e informar o usuário (ex: "Não consegui iniciar a navegação"). (Decidido manter log apenas)
    *   [x] Em `MainCarScreen`: Usar `CarToast.makeText(...)` em `showTemporaryError` para feedback menos intrusivo.
*   [x] **Gerenciamento de Estado (`SystemState`):**
    *   [x] Revisar como e quando o `SystemState` é atualizado para garantir que reflete o melhor possível o estado real (considerar limitações de obter estado de apps externos). (Revisado, mantido heurísticas)

## 4. Permissões e UI - Baixo

*   [x] **Centralizar e Otimizar Solicitação de Permissões:**
    *   [x] Remover verificações/solicitações duplicadas em `MainActivity.kt` (se não essencial).
    *   [x] Focar a solicitação de permissões em `MainCarScreen.kt` (ou onde a funcionalidade é usada).
    *   [x] (Opcional) Implementar solicitação de permissão "just-in-time" (ex: pedir `RECORD_AUDIO` apenas ao clicar no microfone pela primeira vez). (Feito para RECORD_AUDIO)
*   [x] **Refinar UI do Android Auto (`MainCarScreen.kt`):**
    *   [x] Implementar `handleListItemClick` se o template `LIST` for usado com itens clicáveis no futuro. (Estrutura implementada)
    *   [x] Revisar a lógica de `isProcessing` vs `isLoading` para garantir clareza e robustez, assegurando que o botão de microfone sempre volte ao estado correto. Considerar unificar em `isBusy`. (Revisado, unificado em isBusy e ícone dinâmico implementado)