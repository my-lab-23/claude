// app.js
const appContainer = document.getElementById('app-container');
let currentQuestionIndex = 0;
let questionnaireResponses = {
    oreTrascorseDaSolo: 0,
    pauraDelPalcoscenico: false,
    partecipazioneEventiSociali: 0,
    usciteSettimanali: 0,
    stanchezzaDopoSocializzazione: false,
    numeroAmiciStretti: 0,
    frequenzaPostSocial: 0
};

// Define questions similarly to Kotlin
const questions = [
    { text: "Quante ore passi da solo/a ogni giorno?", type: "SLIDER", min: 0, max: 11 },
    { text: "Hai paura del palcoscenico/di parlare in pubblico?", type: "YES_NO" },
    { text: "Con che frequenza partecipi ad eventi sociali?", type: "SLIDER", min: 0, max: 10, minLabel: "mai", maxLabel: "sempre" },
    { text: "Quante volte esci di casa a settimana?", type: "SLIDER", min: 0, max: 7 },
    { text: "Ti senti scarico/a dopo aver socializzato?", type: "YES_NO" },
    { text: "Quanti amici stretti hai?", type: "SLIDER", min: 0, max: 15 },
    { text: "Con che frequenza pubblichi sui social media?", type: "SLIDER", min: 0, max: 10, minLabel: "mai", maxLabel: "continuamente" }
];

const predictor = new OnnxPersonalityPredictor();

function showWelcomeScreen() {
    currentQuestionIndex = 0; // Reset index for new questionnaire
    questionnaireResponses = { // Reset responses
        oreTrascorseDaSolo: 0,
        pauraDelPalcoscenico: false,
        partecipazioneEventiSociali: 0,
        usciteSettimanali: 0,
        stanchezzaDopoSocializzazione: false,
        numeroAmiciStretti: 0,
        frequenzaPostSocial: 0
    };

    appContainer.innerHTML = `
        <div class="welcome-screen">
            <h1>🧠 Sistema di Previsione Personalità</h1>
            <h2>Scopri se sei introverso o estroverso attraverso l'intelligenza artificiale</h2>
            <p>
                Questo questionario utilizza algoritmi di machine learning per analizzare
                le tue abitudini sociali e predire il tuo tipo di personalità.
                <br><br>
                Ti faremo 7 domande e poi ti mostreremo i risultati con le probabilità.
            </p>
            <button class="button start-button">Inizia Questionario</button>
        </div>
    `;

    document.querySelector('.start-button').addEventListener('click', showQuestionScreen);
}

function showQuestionScreen() {
    if (currentQuestionIndex >= questions.length) {
        showResultsScreen();
        return;
    }

    const question = questions[currentQuestionIndex];
    const isLastQuestion = currentQuestionIndex === questions.length - 1;

    let inputHtml = '';
    let userDataAttribute = '';

    if (question.type === "SLIDER") {
        const value = questionnaireResponses[getKeyForQuestion(currentQuestionIndex)] || question.min;
        inputHtml = `
            <div class="slider-container">
                <input type="range" min="${question.min}" max="${question.max}" value="${value}">
                <label class="slider-value-label">${value}</label>
                <div class="slider-labels">
                    ${question.minLabel ? `<span>${question.minLabel}</span>` : ''}
                    ${question.maxLabel ? `<span>${question.maxLabel}</span>` : ''}
                </div>
            </div>
        `;
        userDataAttribute = `data-input-type="slider"`;
    } else if (question.type === "YES_NO") {
        const isYesSelected = questionnaireResponses[getKeyForQuestion(currentQuestionIndex)] === true;
        const isNoSelected = questionnaireResponses[getKeyForQuestion(currentQuestionIndex)] === false;
        inputHtml = `
            <div class="radio-group">
                <label>
                    <input type="radio" name="yesNoQuestion" value="yes" ${isYesSelected ? 'checked' : ''}>
                    Sì
                </label>
                <label>
                    <input type="radio" name="yesNoQuestion" value="no" ${isNoSelected ? 'checked' : ''}>
                    No
                </label>
            </div>
        `;
        userDataAttribute = `data-input-type="yes_no"`;
    }

    appContainer.innerHTML = `
        <div class="question-screen">
            <div class="progress-label">Domanda ${currentQuestionIndex + 1} di ${questions.length}</div>
            <div class="progress-bar-container">
                <div class="progress-bar" style="width: ${((currentQuestionIndex + 1) / questions.length) * 100}%;"></div>
            </div>
            <h2 class="question-title">${question.text}</h2>
            <div class="input-container" ${userDataAttribute}>
                ${inputHtml}
            </div>
            <div class="navigation-buttons">
                <button class="button nav-button back-button" ${currentQuestionIndex === 0 ? 'disabled' : ''}>Indietro</button>
                <button class="button nav-button next-button">${isLastQuestion ? 'Risultato' : 'Avanti'}</button>
            </div>
        </div>
    `;

    const inputContainer = document.querySelector('.input-container');

    if (question.type === "SLIDER") {
        const slider = inputContainer.querySelector('input[type="range"]');
        const valueLabel = inputContainer.querySelector('.slider-value-label');
        slider.addEventListener('input', (event) => {
            valueLabel.textContent = event.target.value;
        });
        // Restore previous value if available, or set to min
        if (questionnaireResponses[getKeyForQuestion(currentQuestionIndex)] !== 0) {
            slider.value = questionnaireResponses[getKeyForQuestion(currentQuestionIndex)];
            valueLabel.textContent = slider.value;
        } else {
             slider.value = question.min; // Ensure slider starts at min if no prior response
             valueLabel.textContent = question.min;
        }

    } else if (question.type === "YES_NO") {
        // Radio buttons are handled by their checked state in the initial HTML rendering
    }

    document.querySelector('.back-button').addEventListener('click', () => {
        currentQuestionIndex--;
        showQuestionScreen();
    });

    document.querySelector('.next-button').addEventListener('click', () => {
        if (saveCurrentAnswer(question, inputContainer)) {
            currentQuestionIndex++;
            showQuestionScreen();
        }
    });
}

function getKeyForQuestion(index) {
    switch (index) {
        case 0: return "oreTrascorseDaSolo";
        case 1: return "pauraDelPalcoscenico";
        case 2: return "partecipazioneEventiSociali";
        case 3: return "usciteSettimanali";
        case 4: return "stanchezzaDopoSocializzazione";
        case 5: return "numeroAmiciStretti";
        case 6: return "frequenzaPostSocial";
        default: return null;
    }
}

function saveCurrentAnswer(question, inputContainer) {
    const key = getKeyForQuestion(currentQuestionIndex);
    if (!key) return false;

    if (question.type === "SLIDER") {
        const slider = inputContainer.querySelector('input[type="range"]');
        questionnaireResponses[key] = parseInt(slider.value, 10);
    } else if (question.type === "YES_NO") {
        const selectedRadio = inputContainer.querySelector('input[name="yesNoQuestion"]:checked');
        if (!selectedRadio) {
            showAlert("Attenzione", "Per favore seleziona una risposta prima di continuare.");
            return false;
        }
        questionnaireResponses[key] = selectedRadio.value === 'yes';
    }
    return true;
}

async function showResultsScreen() {
    let prediction = "N/A";
    let probabilities = new Map();

    try {
        const result = await predictor.predict(questionnaireResponses);
        prediction = result.prediction;
        probabilities = result.probabilities;
    } catch (error) {
        console.error("Error during prediction:", error);
        showAlert("Errore di Previsione", error.message || "Si è verificato un errore durante la previsione.");
        // Fallback to default or empty display
        probabilities.set("Extrovert", 0.33);
        probabilities.set("Introvert", 0.33);
        probabilities.set("Ambivert", 0.34);
        prediction = "Errore";
    }

    let probabilitiesHtml = '';
    probabilities.forEach((prob, type) => {
        const percentage = (prob * 100).toFixed(1);
        const isPredictedType = type === prediction;
        probabilitiesHtml += `
            <div class="probability-row">
                <span class="percentage-label ${isPredictedType ? 'highlighted' : ''}">${type}: ${percentage}%</span>
                <div class="progress-bar-inner">
                    <div class="progress-fill ${isPredictedType ? 'highlighted' : ''}" style="width: ${percentage}%;"></div>
                </div>
            </div>
        `;
    });

    const summaryHtml = `
        <div class="summary-box">
            <h3 class="summary-title">Riepilogo delle tue risposte:</h3>
            <p class="summary-item">• Ore trascorse da solo al giorno: ${questionnaireResponses.oreTrascorseDaSolo}</p>
            <p class="summary-item">• Paura del palcoscenico: ${questionnaireResponses.pauraDelPalcoscenico ? "Sì" : "No"}</p>
            <p class="summary-item">• Partecipazione eventi sociali: ${questionnaireResponses.partecipazioneEventiSociali}/10</p>
            <p class="summary-item">• Uscite settimanali: ${questionnaireResponses.usciteSettimanali}</p>
            <p class="summary-item">• Stanchezza dopo socializzazione: ${questionnaireResponses.stanchezzaDopoSocializzazione ? "Sì" : "No"}</p>
            <p class="summary-item">• Numero amici stretti: ${questionnaireResponses.numeroAmiciStretti}</p>
            <p class="summary-item">• Frequenza post social: ${questionnaireResponses.frequenzaPostSocial}/10</p>
        </div>
    `;


    appContainer.innerHTML = `
        <div class="results-screen">
            <h1>🔮 Risultati della Previsione</h1>
            <div class="result-card">
                <p class="prediction-label">Il tuo tipo di personalità previsto è:</p>
                <p class="predicted-type">${prediction}</p>
            </div>
            <h2 class="probabilities-title">Probabilità:</h2>
            <div class="probabilities-box">
                ${probabilitiesHtml}
            </div>
            ${summaryHtml}
            <div class="buttons-box">
                <button class="button restart-button">Nuovo</button>
                <button class="button exit-button">Esci</button>
            </div>
        </div>
    `;

    document.querySelector('.restart-button').addEventListener('click', showWelcomeScreen);
    document.querySelector('.exit-button').addEventListener('click', () => {
        // In a web context, "exit" usually means closing the tab or navigating away.
        // For a simple app, we might just show a message or redirect.
        // Or, more practically, just don't do anything as the user can close the tab.
        // For this example, let's just make it a no-op or a simple alert.
        showAlert("Uscita", "Grazie per aver usato il sistema di previsione!");
        // setTimeout(() => window.close(), 1000); // This might not work in all browsers
    });
}

function showAlert(title, message) {
    const overlay = document.createElement('div');
    overlay.className = 'overlay';

    const alertBox = document.createElement('div');
    alertBox.className = 'custom-alert';
    alertBox.innerHTML = `
        <h3>${title}</h3>
        <p>${message}</p>
        <button id="alert-ok-button">OK</button>
    `;

    document.body.appendChild(overlay);
    document.body.appendChild(alertBox);

    document.getElementById('alert-ok-button').addEventListener('click', () => {
        document.body.removeChild(alertBox);
        document.body.removeChild(overlay);
    });
}


// Initial load
document.addEventListener('DOMContentLoaded', showWelcomeScreen);
