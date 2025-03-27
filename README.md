# Android IA Car - Assistente Virtual Estilo KITT

[![Android Auto](https://img.shields.io/badge/Android_Auto-Suportado-green.svg)](https://www.android.com/auto/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg)](https://kotlinlang.org/)
[![Gemini API](https://img.shields.io/badge/Gemini_API-1.0.0-FF6F00.svg)](https://ai.google.dev/)

## 🚗 Descrição do Projeto
Assistente virtual integrado ao Android Auto com funcionalidades:
- Monitoramento de dados do veículo em tempo real
- Controle por comando de voz natural
- Integração com serviços de navegação e entretenimento
- Processamento de linguagem natural via Gemini AI

## ⚙️ Funcionalidades Principais
- **Diagnóstico Veicular**
  - Nível de bateria e status elétrico
  - Localização GPS e dados de telemetria
  - Status da conexão com operadora

- **Controle por Voz**
  - Comandos de navegação ("Navegar para...")
  - Controle de mídia (play/pause/skip)
  - Consultas contextualizadas ao Gemini AI

- **Interface Adaptativa**
  - Scanner luminoso estilo Knight Rider
  - Feedback visual por LEDs
  - Sintetização de voz para respostas

## 📋 Pré-requisitos
- Android Auto compatível
- Android SDK 34+
- Chave de API Gemini (adicionar no local.properties usando o template do local.properties.example)

## 🔧 Configuração da API Gemini
1. Obtenha sua API key em [Google AI Studio](https://aistudio.google.com/)
2. Adicione no arquivo `local.properties`:
```properties
gemini.api.key=SUA_CHAVE_API_AQUI
```

## 🚀 Execução
```bash
./gradlew assembleDebug
```

## 📸 Demonstração
![Interface do Assistente](docs/interface-preview.png)

## 🤝 Contribuição
Contribuições são bem-vindas! Siga o [guia de contribuição](CONTRIBUTING.md)

## Licença
[MIT](LICENSE)