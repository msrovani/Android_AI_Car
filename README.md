# KITT - Assistente de Carro com Android Auto

Um assistente de carro inteligente inspirado no KITT do Knight Rider, desenvolvido para Android Auto com integração com a API Gemini da Google.

## 🚀 Funcionalidades

- Interface inspirada no KITT do Knight Rider
- Reconhecimento de voz para comandos
- Integração com a API Gemini para processamento de linguagem natural
- Navegação
- Controle de mídia
- Leitura de notificações
- Informações meteorológicas
- Controle de volume

## 🛠️ Tecnologias Utilizadas

- Kotlin
- Android Auto SDK
- Google Gemini API
- Text-to-Speech
- Speech Recognition
- Coroutines
- Hilt (Dependency Injection)

## 📋 Pré-requisitos

- Android Studio
- Android SDK
- Dispositivo Android com Android Auto
- Chave de API do Gemini

## 🔧 Configuração

1. Clone o repositório:
```bash
git clone https://github.com/seu-usuario/android-ia-car.git
```

2. Abra o projeto no Android Studio

3. Configure sua chave da API Gemini:
   - Crie um arquivo `local.properties` na raiz do projeto
   - Adicione sua chave: `GEMINI_API_KEY=sua_chave_aqui`

4. Compile o projeto:
```bash
./gradlew clean assembleDebug
```

## 📱 Instalação

1. Instale o APK gerado em `app/build/outputs/apk/debug/app-debug.apk`
2. Conecte seu dispositivo ao carro ou inicie o Android Auto no modo desenvolvedor
3. O aplicativo KITT estará disponível na interface do Android Auto

## 🎯 Como Usar

1. Diga "KITT" para ativar o assistente
2. Faça comandos como:
   - "Qual é a temperatura hoje?"
   - "Navegue até o shopping"
   - "Toque uma música"
   - "Ajuste o volume"
   - "Leia minhas notificações"

## 📝 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

## 🤝 Contribuindo

Contribuições são bem-vindas! Por favor, leia as [diretrizes de contribuição](CONTRIBUTING.md) para detalhes sobre o processo de submissão de pull requests.

## 📞 Suporte

Para suporte, abra uma issue no GitHub ou entre em contato através do email: seu-email@exemplo.com