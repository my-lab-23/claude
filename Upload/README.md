# File Downloader

Sistema per scaricare file da remoto tramite app Android.

## 📱 Cosa fa

Un server espone i file di una cartella e un'app Android permette di:
- Visualizzare la lista dei file disponibili
- Scaricare qualsiasi file con un semplice tap
- Salvare i file nella cartella Download del telefono

## 🛠️ Tecnologie

**Server:**
- Kotlin + Ktor (REST API)

**Client:**  
- Android + Jetpack Compose
- Retrofit per le chiamate HTTP
- Material Design 3

## 🚀 Come funziona

1. Il server Ktor serve i file da `/home/ema/upload`
2. L'app Android si connette al server  
3. Mostra la lista dei file con un'interfaccia moderna
4. Ogni file ha un pulsante download
5. I file vengono scaricati mantenendo il nome originale