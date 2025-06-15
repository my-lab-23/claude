const fs = require('fs');
const https = require('https');

// Costante per il percorso del file
const FILE_PATH = '/home/ema/annotazioni_bus.json';
const OUTPUT_PATH = '/home/ema/annotazioni_bus_con_temperature.json';

// Funzione per fare richieste HTTP
function httpsGet(url) {
  return new Promise((resolve, reject) => {
    https.get(url, (res) => {
      let data = '';
      res.on('data', (chunk) => {
        data += chunk;
      });
      res.on('end', () => {
        try {
          resolve(JSON.parse(data));
        } catch (error) {
          reject(error);
        }
      });
    }).on('error', (error) => {
      reject(error);
    });
  });
}

// Funzione per ottenere la temperatura media del giorno da Open-Meteo
async function getTemperatureForDate(date) {
  try {
    // API Open-Meteo per Roma (assumendo che sia la località)
    const url = `https://api.open-meteo.com/v1/forecast?latitude=41.9028&longitude=12.4964&daily=temperature_2m_mean&start_date=${date}&end_date=${date}&timezone=Europe%2FRome`;
    
    const response = await httpsGet(url);
    
    if (response.daily && response.daily.temperature_2m_mean && response.daily.temperature_2m_mean[0] !== null) {
      return Math.round(response.daily.temperature_2m_mean[0] * 10) / 10; // Arrotonda a 1 decimale
    } else {
      console.warn(`Temperatura non disponibile per la data ${date}`);
      return null;
    }
  } catch (error) {
    console.error(`Errore nel recuperare la temperatura per ${date}:`, error.message);
    return null;
  }
}

// Funzione principale
async function processAnnotations() {
  try {
    // Carica il file JSON originale
    console.log('Caricamento del file:', FILE_PATH);
    const rawData = fs.readFileSync(FILE_PATH, 'utf8');
    const annotazioni = JSON.parse(rawData);
    
    console.log(`Trovate ${annotazioni.length} annotazioni`);
    
    // Ottieni le date uniche
    const dateUniche = [...new Set(annotazioni.map(item => item.data))];
    console.log('Date da processare:', dateUniche);
    
    // Controlla se esiste già il file con le temperature
    let temperatureEsistenti = {};
    if (fs.existsSync(OUTPUT_PATH)) {
      console.log('\n--- CONTROLLO FILE ESISTENTE ---');
      console.log('File con temperature già esistente:', OUTPUT_PATH);
      try {
        const datiEsistenti = JSON.parse(fs.readFileSync(OUTPUT_PATH, 'utf8'));
        console.log(`Trovate ${datiEsistenti.length} annotazioni esistenti`);
        
        // Estrai le temperature già presenti per data
        datiEsistenti.forEach(item => {
          if (item.temperatura !== null && item.temperatura !== undefined) {
            temperatureEsistenti[item.data] = item.temperatura;
          }
        });
        
        const dateConTemperatura = Object.keys(temperatureEsistenti);
        console.log(`Temperature già disponibili per ${dateConTemperatura.length} date:`, dateConTemperatura);
        
        if (dateConTemperatura.length > 0) {
          dateConTemperatura.forEach(data => {
            console.log(`  ${data}: ${temperatureEsistenti[data]}°C`);
          });
        }
      } catch (error) {
        console.warn('Errore nella lettura del file esistente:', error.message);
        console.log('Procedo senza utilizzare i dati esistenti');
      }
    } else {
      console.log('\n--- PRIMO AVVIO ---');
      console.log('File con temperature non esistente, sarà creato nuovo');
    }
    
    // Identifica le date per cui serve recuperare la temperatura
    const dateMancanti = dateUniche.filter(data => !temperatureEsistenti.hasOwnProperty(data));
    
    console.log('\n--- RECUPERO TEMPERATURE ---');
    if (dateMancanti.length === 0) {
      console.log('✓ Tutte le temperature sono già disponibili, nessuna richiesta a Open-Meteo necessaria');
    } else {
      console.log(`Temperature da recuperare per ${dateMancanti.length} date:`, dateMancanti);
    }
    
    // Carica le temperature solo per le date mancanti
    const temperaturePerData = { ...temperatureEsistenti };
    for (const data of dateMancanti) {
      console.log(`📡 Recupero temperatura da Open-Meteo per ${data}...`);
      const temperatura = await getTemperatureForDate(data);
      temperaturePerData[data] = temperatura;
      
      if (temperatura !== null) {
        console.log(`✓ ${data}: ${temperatura}°C`);
      } else {
        console.log(`✗ ${data}: temperatura non disponibile`);
      }
      
      // Pausa di 100ms tra le richieste per essere gentili con l'API
      await new Promise(resolve => setTimeout(resolve, 100));
    }
    
    // Processa le annotazioni: rimuovi orario e aggiungi temperatura
    const annotazioniProcessate = annotazioni.map(item => {
      const nuovaAnnotazione = {
        data: item.data,
        livelloAffollamento: item.livelloAffollamento,
        direzione: item.direzione,
        linea: item.linea,
        temperatura: temperaturePerData[item.data]
      };
      
      return nuovaAnnotazione;
    });
    
    // Salva il nuovo file JSON
    console.log('\n--- SALVATAGGIO ---');
    console.log('Salvataggio del nuovo file:', OUTPUT_PATH);
    fs.writeFileSync(OUTPUT_PATH, JSON.stringify(annotazioniProcessate, null, 2));
    
    console.log('✓ Operazione completata con successo!');
    console.log(`Nuovo file salvato in: ${OUTPUT_PATH}`);
    
    // Mostra un riassunto finale
    console.log('\n--- RIASSUNTO FINALE ---');
    const temperatureValide = Object.values(temperaturePerData).filter(t => t !== null).length;
    const temperatureTotali = Object.keys(temperaturePerData).length;
    console.log(`Temperature processate: ${temperatureValide}/${temperatureTotali} date`);
    
    for (const [data, temp] of Object.entries(temperaturePerData)) {
      const status = temperatureEsistenti.hasOwnProperty(data) ? '(esistente)' : '(nuova)';
      console.log(`  ${data}: ${temp !== null ? temp + '°C' : 'Non disponibile'} ${status}`);
    }
    
  } catch (error) {
    console.error('Errore durante l\'elaborazione:', error.message);
    process.exit(1);
  }
}

// Esegui lo script
processAnnotations();