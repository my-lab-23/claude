let historicalData = [];
let dataLoaded = false;

// Imposta la data di domani come default
document.getElementById("targetDate").valueAsDate = new Date(
  Date.now() + 24 * 60 * 60 * 1000
);

document
  .getElementById("jsonFile")
  .addEventListener("change", function (event) {
    const file = event.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = function (e) {
        try {
          historicalData = JSON.parse(e.target.result);
          dataLoaded = true;
          document.getElementById("predictButton").disabled = false;

          document.getElementById("fileStatus").innerHTML = `
                            <div style="color: #38a169; margin-top: 10px;">
                                ✅ File caricato con successo! ${historicalData.length} record trovati.
                            </div>
                        `;

          displayDataStats();
        } catch (error) {
          document.getElementById("fileStatus").innerHTML = `
                            <div class="error">
                                ❌ Errore nel caricamento del file: ${error.message}
                            </div>
                        `;
        }
      };
      reader.readAsText(file);
    }
  });

function displayDataStats() {
  const dateRange = getDateRange(historicalData);
  const tempRange = getTemperatureRange(historicalData);
  
  // Statistiche separate per andata e ritorno
  const andataData = historicalData.filter(d => d.direzione === "ANDATA");
  const ritornoData = historicalData.filter(d => d.direzione === "RITORNO");
  
  const avgCrowdingAndata = andataData.length > 0 ? getAverageCrowding(andataData) : 0;
  const avgCrowdingRitorno = ritornoData.length > 0 ? getAverageCrowding(ritornoData) : 0;

  const statsHtml = `
                <div class="data-stats">
                    <h4>📊 Statistiche Dati Caricati</h4>
                    <p><strong>Periodo:</strong> ${dateRange.start} - ${dateRange.end}</p>
                    <p><strong>Range temperature:</strong> ${tempRange.min}°C - ${tempRange.max}°C</p>
                    <p><strong>Totale record:</strong> ${historicalData.length} (${andataData.length} andata, ${ritornoData.length} ritorno)</p>
                    
                    <div style="display: flex; gap: 20px; margin-top: 15px;">
                        <div style="flex: 1; padding: 10px; border: 1px solid #ddd; border-radius: 5px;">
                            <h5>🚌 ANDATA</h5>
                            <p><strong>Affollamento medio:</strong> ${avgCrowdingAndata.toFixed(1)}</p>
                            <p><strong>Distribuzione per giorno:</strong></p>
                            ${getDayDistribution(andataData)}
                        </div>
                        
                        <div style="flex: 1; padding: 10px; border: 1px solid #ddd; border-radius: 5px;">
                            <h5>🏠 RITORNO</h5>
                            <p><strong>Affollamento medio:</strong> ${avgCrowdingRitorno.toFixed(1)}</p>
                            <p><strong>Distribuzione per giorno:</strong></p>
                            ${getDayDistribution(ritornoData)}
                        </div>
                    </div>
                </div>
            `;

  document.getElementById("results").innerHTML = statsHtml;
}

function getDateRange(data) {
  const dates = data.map((d) => new Date(d.data)).sort((a, b) => a - b);
  return {
    start: dates[0].toLocaleDateString("it-IT"),
    end: dates[dates.length - 1].toLocaleDateString("it-IT"),
  };
}

function getTemperatureRange(data) {
  const temps = data.map((d) => d.temperatura);
  return {
    min: Math.min(...temps),
    max: Math.max(...temps),
  };
}

function getAverageCrowding(data) {
  if (data.length === 0) return 0;
  const levels = data.map((d) => d.livelloAffollamento);
  return levels.reduce((a, b) => a + b, 0) / levels.length;
}

function getDayDistribution(data) {
  const dayNames = [
    "Dom", "Lun", "Mar", "Mer", "Gio", "Ven", "Sab"
  ];
  const dayCounts = {};

  data.forEach((record) => {
    const dayOfWeek = new Date(record.data).getDay();
    const dayName = dayNames[dayOfWeek];
    dayCounts[dayName] = (dayCounts[dayName] || 0) + 1;
  });

  return Object.entries(dayCounts)
    .map(
      ([day, count]) =>
        `<span style="margin-right: 10px; font-size: 0.9em;"><strong>${day}:</strong> ${count}</span>`
    )
    .join("");
}

function makePrediction() {
  if (!dataLoaded) {
    alert("Carica prima un file JSON con i dati storici");
    return;
  }

  const targetDate = new Date(document.getElementById("targetDate").value);
  const targetTemp = parseFloat(document.getElementById("temperature").value);
  const direzione = document.getElementById("direction").value; // Nuovo campo da aggiungere all'HTML

  if (!targetDate || isNaN(targetTemp) || !direzione) {
    alert("Inserisci una data, una temperatura e una direzione valide");
    return;
  }

  const prediction = calculatePrediction(targetDate, targetTemp, direzione);
  displayPrediction(prediction, targetDate, targetTemp, direzione);
}

function calculatePrediction(targetDate, targetTemp, direzione) {
  const targetDayOfWeek = targetDate.getDay();
  const dayNames = [
    "Domenica", "Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato"
  ];

  // Filtra i dati per direzione E giorno della settimana
  const directionData = historicalData.filter((record) => 
    record.direzione === direzione
  );
  
  const sameDayDirectionData = directionData.filter((record) => {
    const recordDate = new Date(record.data);
    return recordDate.getDay() === targetDayOfWeek;
  });

  // Priorità: stesso giorno + stessa direzione > stessa direzione > tutti i dati
  let relevantData;
  let method;
  
  if (sameDayDirectionData.length > 0) {
    relevantData = sameDayDirectionData;
    method = "same-day-direction";
  } else if (directionData.length > 0) {
    relevantData = directionData;
    method = "same-direction";
  } else {
    relevantData = historicalData;
    method = "general";
  }

  if (relevantData.length === 0) {
    return {
      level: 3,
      confidence: 0,
      method: "default",
      sameDayDirectionCount: 0,
      directionCount: directionData.length,
      totalDataCount: historicalData.length,
      direzione: direzione
    };
  }

  // Calcola correlazione temperatura-affollamento per la direzione specifica
  const tempCorrelation = calculateTemperatureCorrelation(relevantData, targetTemp);

  // Calcola la previsione base
  const baseAvg = relevantData.reduce((sum, record) => sum + record.livelloAffollamento, 0) / relevantData.length;
  let prediction = baseAvg + tempCorrelation.adjustment;

  // Limita la previsione tra 1 e 5
  prediction = Math.max(1, Math.min(5, prediction));

  // Calcola la confidenza
  const confidence = calculateConfidence(
    sameDayDirectionData.length,
    directionData.length,
    relevantData.length,
    tempCorrelation.strength
  );

  return {
    level: Math.round(prediction * 2) / 2, // Arrotonda a 0.5
    confidence: confidence,
    method: method,
    sameDayDirectionCount: sameDayDirectionData.length,
    directionCount: directionData.length,
    totalDataCount: historicalData.length,
    temperatureEffect: tempCorrelation.adjustment,
    dayName: dayNames[targetDayOfWeek],
    direzione: direzione
  };
}

function calculateTemperatureCorrelation(data, targetTemp) {
  if (data.length < 2) {
    return { adjustment: 0, strength: 0 };
  }

  // Calcola la correlazione lineare tra temperatura e affollamento
  const n = data.length;
  const sumX = data.reduce((sum, record) => sum + record.temperatura, 0);
  const sumY = data.reduce((sum, record) => sum + record.livelloAffollamento, 0);
  const sumXY = data.reduce((sum, record) => sum + record.temperatura * record.livelloAffollamento, 0);
  const sumX2 = data.reduce((sum, record) => sum + record.temperatura * record.temperatura, 0);

  const avgTemp = sumX / n;
  const correlation = (n * sumXY - sumX * sumY) / 
    Math.sqrt((n * sumX2 - sumX * sumX) * 
              (n * data.reduce((sum, record) => sum + record.livelloAffollamento * record.livelloAffollamento, 0) - sumY * sumY));

  // Calcola l'aggiustamento basato sulla differenza di temperatura
  const tempDiff = targetTemp - avgTemp;
  const adjustment = (correlation || 0) * tempDiff * 0.05; // Fattore di scala

  return {
    adjustment: adjustment,
    strength: Math.abs(correlation || 0),
  };
}

function calculateConfidence(sameDayDirectionCount, directionCount, totalRelevantCount, tempCorrelation) {
  let confidence = 0;

  // Confidenza basata sui dati dello stesso giorno e stessa direzione (peso maggiore)
  if (sameDayDirectionCount > 0) {
    confidence += Math.min(sameDayDirectionCount * 20, 70); // Max 70% per dati stesso giorno + direzione
  } else if (directionCount > 0) {
    // Confidenza basata sui dati della stessa direzione
    confidence += Math.min(directionCount * 10, 50); // Max 50% per dati stessa direzione
  }

  // Confidenza basata sul totale dei dati rilevanti
  confidence += Math.min(totalRelevantCount * 2, 20); // Max 20% per quantità dati

  // Confidenza basata sulla correlazione temperatura
  confidence += tempCorrelation * 10; // Max 10% per correlazione temperatura

  return Math.min(confidence, 100);
}

function displayPrediction(prediction, targetDate, targetTemp, direzione) {
  const levelDescriptions = {
    1: "Vuoto",
    1.5: "Quasi vuoto",
    2: "Poco affollato",
    2.5: "Moderatamente affollato",
    3: "Normalmente affollato",
    3.5: "Abbastanza affollato",
    4: "Molto affollato",
    4.5: "Quasi pieno",
    5: "Strapieno",
  };

  const directionEmoji = direzione === "ANDATA" ? "🚌" : "🏠";
  const levelClass = `level-${Math.ceil(prediction.level)}`;

  const resultsHtml = `
                <div class="results">
                    <h3>${directionEmoji} Previsione ${direzione} per ${targetDate.toLocaleDateString("it-IT")} (${prediction.dayName})</h3>
                    
                    <div class="prediction-value">
                        <span class="level-indicator ${levelClass}">
                            Livello ${prediction.level} - ${levelDescriptions[prediction.level]}
                        </span>
                    </div>
                    
                    <div class="confidence">
                        <strong>📈 Affidabilità della previsione:</strong> ${prediction.confidence.toFixed(1)}%
                        <br>
                        <strong>🌡️ Temperatura considerata:</strong> ${targetTemp}°C
                        ${prediction.temperatureEffect !== 0 ? 
                          `<br><strong>🔄 Effetto temperatura:</strong> ${prediction.temperatureEffect > 0 ? "+" : ""}${prediction.temperatureEffect.toFixed(2)} livelli` 
                          : ""
                        }
                    </div>
                    
                    <div class="data-stats">
                        <strong>📊 Dati utilizzati per ${direzione}:</strong><br>
                        • Dati per ${prediction.dayName} + ${direzione}: ${prediction.sameDayDirectionCount} record<br>
                        • Totale dati ${direzione}: ${prediction.directionCount} record<br>
                        • Totale dati storici: ${prediction.totalDataCount} record<br>
                        • Metodo: ${getMethodDescription(prediction.method)}
                    </div>
                    
                    ${prediction.confidence < 50 ? 
                      `<div class="error">
                          ⚠️ Attenzione: L'affidabilità della previsione è bassa. 
                          Raccomandato raccogliere più dati storici per la direzione ${direzione}.
                      </div>` 
                      : ""
                    }
                </div>
            `;

  document.getElementById("results").innerHTML = resultsHtml;
}

function getMethodDescription(method) {
  switch(method) {
    case "same-day-direction":
      return "Previsione basata su stesso giorno della settimana e stessa direzione";
    case "same-direction":
      return "Previsione basata su stessa direzione (tutti i giorni)";
    case "general":
      return "Previsione basata su tutti i dati disponibili";
    default:
      return "Previsione di default";
  }
}