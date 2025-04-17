# Android IA Car

Aplicativo Android com assistente de IA para uso em carros via Android Auto.

## Funcionalidades

- 🚗 Interface para Android Auto
- 🎤 Reconhecimento de voz para comandos
- 🗣️ Respostas por voz (TTS)
- 📍 Iniciar Navegação via Google Maps
- 🎵 Controle de música (Play/Pause/Next/Previous via Broadcast)
- 🤖 Integração com IA (Google Gemini)

## Requisitos

- Android 7.0 (API 24) ou superior
- Android Auto instalado no dispositivo ou unidade principal do carro
- Permissões necessárias:
  - Localização (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`)
  - Microfone (`RECORD_AUDIO`)
  - Internet (`INTERNET`)

## Configuração

1. Clone o repositório
2. Adicione sua chave da API Gemini no arquivo `local.properties`:
   ```
   GEMINI_API_KEY=sua_chave_aqui
   ```
3. Compile o projeto:
   ```bash
   ./gradlew clean assembleDebug
   ```

## Estrutura do Projeto

```
app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/androidiacar/
│   │   │       ├── MainActivity.kt         # Activity principal (launcher)
│   │   │       ├── MainCarAppService.kt    # Serviço do Android Auto
│   │   │       ├── MainCarSession.kt       # Gerencia a sessão e a tela do Android Auto
│   │   │       ├── MainApplication.kt      # Configuração Hilt
│   │   │       ├── ai/
│   │   │       │   └── ConversationManager.kt # Lógica da conversa com Gemini e ações
│   │   │       └── di/
│   │   │           └── AppModule.kt          # Módulo Hilt para dependências
│   │   └── res/
│   │       ├── layout/
│   │       │   └── activity_main.xml    # Layout da MainActivity
│   │       └── values/
│   │           ├── colors.xml
│   │           └── strings.xml
└── build.gradle.kts
```

## Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

## Contribuição

Contribuições são bem-vindas! Por favor, leia as [diretrizes de contribuição](CONTRIBUTING.md) antes de enviar um pull request.