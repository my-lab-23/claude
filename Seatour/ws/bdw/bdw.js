let busData = [];

document.getElementById("fileInput").addEventListener("change", function (e) {
  const file = e.target.files[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = function (e) {
    try {
      busData = JSON.parse(e.target.result);
      displayData();
    } catch (error) {
      alert("Errore nel caricamento del file JSON: " + error.message);
    }
  };
  reader.readAsText(file);
});

function displayData() {
  if (!busData || busData.length === 0) {
    return;
  }

  // Nascondi il messaggio "no data" e mostra le statistiche
  document.getElementById("noData").style.display = "none";
  document.getElementById("statsSection").style.display = "grid";
  document.getElementById("dataContent").style.display = "block";

  // Calcola statistiche
  updateStats();

  // Raggruppa i dati per data
  const dataByDate = groupByDate(busData);

  // Genera il grid dei dati
  generateDataGrid(dataByDate);
}

function updateStats() {
  const totalRecords = busData.length;
  const uniqueDays = [...new Set(busData.map((item) => item.data))].length;
  const avgCrowding = (
    busData.reduce((sum, item) => sum + item.livelloAffollamento, 0) /
    totalRecords
  ).toFixed(1);
  const validTemps = busData.filter(
    (item) => item.temperatura !== null && item.temperatura !== undefined
  );
  const avgTemp =
    validTemps.length > 0
      ? (
          validTemps.reduce((sum, item) => sum + item.temperatura, 0) /
          validTemps.length
        ).toFixed(1)
      : "N/A";

  document.getElementById("totalRecords").textContent = totalRecords;
  document.getElementById("uniqueDays").textContent = uniqueDays;
  document.getElementById("avgCrowding").textContent = avgCrowding;
  document.getElementById("avgTemp").textContent =
    avgTemp + (avgTemp !== "N/A" ? "°C" : "");
}

function groupByDate(data) {
  const grouped = {};
  data.forEach((item) => {
    if (!grouped[item.data]) {
      grouped[item.data] = [];
    }
    grouped[item.data].push(item);
  });
  return grouped;
}

function generateDataGrid(dataByDate) {
  const grid = document.getElementById("dataGrid");
  grid.innerHTML = "";

  // Ordina le date
  const sortedDates = Object.keys(dataByDate).sort(
    (a, b) => new Date(b) - new Date(a)
  );

  sortedDates.forEach((date) => {
    const dayData = dataByDate[date];
    const card = createDayCard(date, dayData);
    grid.appendChild(card);
  });
}

function createDayCard(date, dayData) {
  const card = document.createElement("div");
  card.className = "data-card";

  // Prendi la temperatura dal primo record (dovrebbe essere uguale per tutti i record dello stesso giorno)
  const temperature = dayData[0].temperatura;
  const formattedDate = new Date(date).toLocaleDateString("it-IT", {
    weekday: "long",
    year: "numeric",
    month: "long",
    day: "numeric",
  });

  let recordsHtml = "";
  dayData.forEach((record) => {
    recordsHtml += `
                    <div class="info-item">
                        <span class="info-label">Direzione:</span>
                        <span class="direction direction-${record.direzione.toLowerCase()}">${
      record.direzione
    }</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">Affollamento:</span>
                        <span class="crowding-level crowding-${
                          record.livelloAffollamento
                        }">Livello ${record.livelloAffollamento}</span>
                    </div>
                `;
  });

  card.innerHTML = `
                <div class="card-header">
                    <div class="card-date">${formattedDate}</div>
                    <div class="card-temp">${
                      temperature !== null && temperature !== undefined
                        ? temperature + "°C"
                        : "N/A"
                    }</div>
                </div>
                <div class="card-content">
                    ${recordsHtml}
                </div>
            `;

  return card;
}

// Drag and drop support
const container = document.querySelector(".container");

["dragenter", "dragover", "dragleave", "drop"].forEach((eventName) => {
  container.addEventListener(eventName, preventDefaults, false);
});

function preventDefaults(e) {
  e.preventDefault();
  e.stopPropagation();
}

["dragenter", "dragover"].forEach((eventName) => {
  container.addEventListener(eventName, highlight, false);
});

["dragleave", "drop"].forEach((eventName) => {
  container.addEventListener(eventName, unhighlight, false);
});

function highlight(e) {
  container.style.backgroundColor = "rgba(255, 255, 255, 0.98)";
}

function unhighlight(e) {
  container.style.backgroundColor = "rgba(255, 255, 255, 0.95)";
}

container.addEventListener("drop", handleDrop, false);

function handleDrop(e) {
  const dt = e.dataTransfer;
  const files = dt.files;

  if (files.length > 0) {
    document.getElementById("fileInput").files = files;
    const event = new Event("change", { bubbles: true });
    document.getElementById("fileInput").dispatchEvent(event);
  }
}
