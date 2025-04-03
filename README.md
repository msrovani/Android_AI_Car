# Android IA Car

Aplicativo Android para controle de carro com IA integrada.

## Funcionalidades

- 🚗 Controle de direção do carro
- 🎤 Reconhecimento de voz
- 🗣️ Respostas por voz (TTS)
- 📍 Localização em tempo real
- 🤖 Integração com IA (Gemini)
- ⚙️ Gerenciamento de permissões
- 🔄 Atualizações automáticas

## Requisitos

- Android 7.0 (API 24) ou superior
- Permissões necessárias:
  - Localização
  - Microfone
  - Telefone
  - Armazenamento
  - Internet

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
│   │   │       ├── MainActivity.kt
│   │   │       ├── SettingsActivity.kt
│   │   │       ├── MainCarSession.kt
│   │   │       ├── MainCarAppService.kt
│   │   │       └── ai/
│   │   │           ├── ConversationManager.kt
│   │   │           └── GeminiService.kt
│   │   └── res/
│   │       ├── layout/
│   │       │   ├── activity_main.xml
│   │       │   └── activity_settings.xml
│   │       └── values/
│   │           └── colors.xml
└── build.gradle.kts
```

## Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

## Contribuição

Contribuições são bem-vindas! Por favor, leia as [diretrizes de contribuição](CONTRIBUTING.md) antes de enviar um pull request.