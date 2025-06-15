# Progetto Youth

Questo progetto è una suite di script Python progettati per automatizzare il processo di download, estrazione, trascrizione e analisi di contenuti video da YouTube, con un focus specifico sulle lezioni di probabilità.

## Funzionalità

### Download e Acquisizione
- **Estrazione di link da playlist**: Estrae tutti i link dei video da una playlist di YouTube e li salva in un file di testo.
- **Download di video**: Scarica i video da una lista di link.

### Elaborazione e Analisi
- **Controllo delle trascrizioni**: Verifica se per un video di YouTube è disponibile una trascrizione.
- **Trascrizione audio/video**: Trascrive file audio o video in testo utilizzando il modello Whisper di OpenAI.
- **Conversione di trascrizioni**: Converte i file di trascrizione JSON in formati di testo leggibili, con o senza timestamp.
- **Generazione di report**: Analizza le trascrizioni per identificare e riassumere gli argomenti principali trattati in ogni lezione.

## Script

### Download e Acquisizione

#### `extractor.py`

Questo script prende in input l'URL di una playlist di YouTube ed estrae le informazioni di tutti i video contenuti in essa. Per ogni video, salva in un file di testo l'URL, il titolo, la durata e l'ID. Il file di output è formattato per essere facilmente leggibile e contiene anche una sezione con i soli link per un comodo copia-incolla.

#### `downloader.py`

Questo script legge il file di testo contenente la lista di URL di video di YouTube (generato da `extractor.py`) e li scarica in una cartella dedicata. La cartella di download viene nominata con il titolo della playlist e un timestamp per garantire l'unicità. Lo script gestisce gli errori di download e crea un file di riepilogo con le statistiche del processo.

### Elaborazione e Analisi

#### `checker.py`

Questo script verifica se per un dato URL di un video di YouTube è disponibile una trascrizione. È in grado di distinguere tra trascrizioni generate automaticamente e quelle create manualmente, fornendo un riscontro immediato sulla disponibilità di sottotitoli o trascrizioni.

#### `transcriber.py`

Questo script utilizza il modello di riconoscimento vocale Whisper di OpenAI per trascrivere i file audio o video scaricati. Può processare un singolo file o un'intera cartella di file. La trascrizione viene salvata in un file JSON che include il testo completo e i timestamp per ogni parola. È possibile specificare il modello di Whisper da utilizzare (tiny, base, small) e limitare la trascrizione a una durata specifica del file.

#### `converter.py`

Questo script converte i file JSON generati da `transcriber.py` in file di testo `.txt` facilmente leggibili. Offre la possibilità di includere i timestamp per ogni segmento di testo, rendendo più semplice seguire la trascrizione sincronizzata con l'audio originale.

#### `reporter.py`

Questo script analizza la cartella di file di trascrizione in formato JSON. Utilizzando un dizionario di parole chiave relative alla teoria della probabilità, identifica gli argomenti principali di ogni lezione. Infine, genera un file di report in formato testo che elenca, per ogni file analizzato, l'argomento principale trattato.

## Note Legali

**IMPORTANTE**: Questo software è destinato esclusivamente a scopi educativi e di ricerca personale. L'utilizzo di questi script è soggetto alle seguenti limitazioni legali:

### Contenuti Autorizzati
- Video di dominio pubblico
- Contenuti con licenza Creative Commons o altre licenze aperte
- Video di cui si possiede il copyright
- Contenuti per i quali si dispone di esplicita autorizzazione scritta dal proprietario
- Materiale educativo reso disponibile gratuitamente dall'autore per scopi didattici

### Contenuti NON Autorizzati
- Video protetti da copyright senza autorizzazione esplicita
- Contenuti commerciali o premium
- Materiale soggetto a restrizioni di distribuzione
- Video con licenze che vietano esplicitamente il download o la trascrizione

### Responsabilità dell'Utente
L'utente è l'unico responsabile di:
- Verificare i diritti di utilizzo dei contenuti prima del download
- Rispettare i termini di servizio di YouTube
- Rispettare le leggi sul copyright applicabili nella propria giurisdizione
- Utilizzare i contenuti scaricati nel rispetto dei diritti d'autore

### Disclaimer
Gli sviluppatori di questo software non si assumono alcuna responsabilità per l'uso improprio del software o per eventuali violazioni del copyright da parte degli utenti. Il software viene fornito "così com'è" senza garanzie di alcun tipo.

