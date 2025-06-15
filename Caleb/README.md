# Caleb - AI Appointments Manager

Un'app Android che utilizza l'intelligenza artificiale per creare appuntamenti automaticamente dal linguaggio naturale.

## 🚀 Funzionalità

- **Input naturale**: Scrivi "Riunione con Mario domani alle 15" e l'AI crea l'appuntamento completo
- **Elaborazione AI**: Utilizza Groq API (Llama 3) per estrarre informazioni strutturate
- **Salvataggio sicuro**: API key crittografate con Android EncryptedSharedPreferences
- **Server backend**: Salva gli appuntamenti su server Ktor

## 🛠️ Tecnologie

### Android App (Kotlin)
- **Jetpack Compose** - UI moderna e reattiva
- **Retrofit** - Chiamate API REST
- **Coroutines** - Programmazione asincrona
- **EncryptedSharedPreferences** - Storage sicuro delle chiavi API

### Backend (Kotlin)
- **Ktor** - Server web leggero
- **Gson** - Serializzazione JSON

### AI Integration
- **Groq API** - Elaborazione linguaggio naturale con Llama 3
- **JSON parsing** - Conversione automatica testo → appuntamento strutturato

