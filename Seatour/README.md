# 🚌 Seatour - Sistema di Monitoraggio Affollamento Bus

Un'applicazione completa per raccogliere, analizzare e predire i livelli di affollamento dei mezzi pubblici, sviluppata con tecnologie moderne e un approccio data-driven.

## 📋 Descrizione del Progetto

Seatour è un sistema integrato composto da:
- **Applicazione Desktop** (Kotlin/JavaFX) per la raccolta dati
- **Script di Arricchimento Dati** (Node.js) per integrare informazioni meteorologiche
- **Dashboard Web** per visualizzare e analizzare i dati raccolti
- **Sistema di Predizione Statistica** basato su correlazioni e pattern per prevedere l'affollamento

## 🏗️ Architettura del Sistema

```
Seatour/
├── src/main/kotlin/         # Applicazione desktop JavaFX
├── bus_temperature_script.js # Script Node.js per dati meteo
└── ws/                      # Applicazioni web
    ├── bdw/                 # Bus Data Viewer (visualizzazione dati)
    └── bp/                  # Bus Predictor (predizioni)
```

## 🚀 Funzionalità Principali

### 📱 Applicazione Desktop
- **Interfaccia Intuitiva**: Selezione rapida del livello di affollamento (1-5)
- **Doppia Direzione**: Registrazione separata per andata e ritorno
- **Prevenzione Duplicati**: Controllo automatico per evitare inserimenti multipli
- **Persistenza Dati**: Salvataggio in formato JSON locale
- **Report Integrato**: Visualizzazione statistiche e storico

### 🌤️ Arricchimento Dati Meteorologici
- **Integrazione Open-Meteo**: Recupero automatico temperature giornaliere
- **Ottimizzazione Richieste**: Cache intelligente per evitare chiamate API duplicate
- **Gestione Errori**: Robustezza nel caso di dati meteo non disponibili
- **Logging Dettagliato**: Monitoraggio completo del processo di arricchimento

### 📊 Dashboard di Visualizzazione
- **Vista Cronologica**: Organizzazione dati per data con informazioni dettagliate
- **Statistiche Aggregate**: Medie di affollamento e temperatura per direzione
- **Design Responsivo**: Interfaccia ottimizzata per tutti i dispositivi
- **Caricamento Drag & Drop**: Importazione file JSON semplificata

### 🎯 Sistema di Predizione Intelligente
- **Algoritmi Statistici**: Correlazione lineare temperatura-affollamento e analisi dei pattern settimanali
- **Predizione Multi-Direzione**: Previsioni separate per andata e ritorno
- **Calcolo Affidabilità**: Valutazione dinamica della confidenza delle previsioni
- **Analisi Temporale Avanzata**: Considerazione specifica del giorno della settimana
- **Fattori Meteorologici**: Integrazione intelligente dei dati climatici nelle predizioni
- **Metodi Adattivi**: Utilizzo gerarchico dei dati (stesso giorno+direzione → stessa direzione → tutti i dati)

## 🛠️ Tecnologie Utilizzate

- **Backend**: Kotlin, JavaFX
- **Data Processing**: Node.js, JavaScript
- **Frontend**: HTML5, CSS3, JavaScript vanilla
- **APIs**: Open-Meteo API per dati meteorologici
- **Storage**: JSON file-based persistence
- **Serializzazione**: Gson per gestione JSON in Kotlin
- **Algoritmi**: Correlazione statistica, analisi dei pattern temporali

