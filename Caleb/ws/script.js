// script.js
document.addEventListener('DOMContentLoaded', function() {
    const uploadArea = document.getElementById('uploadArea');
    const fileInput = document.getElementById('fileInput');
    const uploadBtn = document.getElementById('uploadBtn');
    const appointmentsSection = document.getElementById('appointmentsSection');
    const appointmentsGrid = document.getElementById('appointmentsGrid');
    const errorMessage = document.getElementById('errorMessage');
    const stats = document.getElementById('stats');

    let loadedAppointments = [];
    let currentFilter = 'all'; // 'all' o 'future'

    // Event listeners
    uploadBtn.addEventListener('click', () => fileInput.click());
    fileInput.addEventListener('change', handleFileSelect);
    
    // Drag and drop functionality
    uploadArea.addEventListener('dragover', handleDragOver);
    uploadArea.addEventListener('dragleave', handleDragLeave);
    uploadArea.addEventListener('drop', handleDrop);

    function handleDragOver(e) {
        e.preventDefault();
        uploadArea.classList.add('dragover');
    }

    function handleDragLeave(e) {
        e.preventDefault();
        uploadArea.classList.remove('dragover');
    }

    function handleDrop(e) {
        e.preventDefault();
        uploadArea.classList.remove('dragover');
        
        const files = e.dataTransfer.files;
        if (files.length > 0) {
            processFile(files[0]);
        }
    }

    function handleFileSelect(e) {
        const file = e.target.files[0];
        if (file) {
            processFile(file);
        }
    }

    function processFile(file) {
        if (!file.name.endsWith('.json')) {
            showError('Per favore seleziona un file JSON valido.');
            return;
        }

        const reader = new FileReader();
        reader.onload = function(e) {
            try {
                const jsonData = JSON.parse(e.target.result);
                loadedAppointments = jsonData;
                currentFilter = 'all';
                displayAppointments(jsonData);
                hideError();
                setTimeout(setStatsListeners, 0);
                setActiveStat('statTotali');
            } catch (error) {
                showError('Errore nel parsing del file JSON. Assicurati che il file sia valido.');
            }
        };
        reader.readAsText(file);
    }

    function displayAppointments(appointments) {
        if (!Array.isArray(appointments)) {
            showError('Il file JSON deve contenere un array di appuntamenti.');
            return;
        }

        let filteredAppointments = appointments;
        if (currentFilter === 'future') {
            const now = new Date();
            filteredAppointments = appointments.filter(app => new Date(app.startTime) > now && app.status === 'CONFIRMED');
        } else if (currentFilter === 'confirmed') {
            filteredAppointments = appointments.filter(app => app.status === 'CONFIRMED');
        }

        appointmentsGrid.innerHTML = '';
        const unconfirmedGrid = document.getElementById('unconfirmedGrid');
        unconfirmedGrid.innerHTML = '';
        
        // Mostra statistiche
        displayStats(appointments);
        setStatsListeners();
        
        // Separa gli appuntamenti confermati e non confermati
        const confirmedAppointments = filteredAppointments.filter(app => app.status === 'CONFIRMED');
        // Gli unconfirmed sono sempre tutti (non filtrati)
        const unconfirmedAppointments = appointments.filter(app => app.status !== 'CONFIRMED');
        
        // Ordina appuntamenti confermati per data
        confirmedAppointments.sort((a, b) => new Date(a.startTime) - new Date(b.startTime));
        
        // Visualizza appuntamenti confermati
        confirmedAppointments.forEach(appointment => {
            const card = createAppointmentCard(appointment);
            appointmentsGrid.appendChild(card);
        });

        // Visualizza appuntamenti non confermati
        unconfirmedAppointments.forEach(appointment => {
            const card = createUnconfirmedAppointmentCard(appointment);
            unconfirmedGrid.appendChild(card);
        });

        appointmentsSection.style.display = 'block';
        document.getElementById('unconfirmedSection').style.display = unconfirmedAppointments.length > 0 ? 'block' : 'none';
    }

    function displayStats(appointments) {
        const totalAppointments = appointments.length;
        const confirmedAppointments = appointments.filter(app => app.status === 'CONFIRMED').length;
        const upcomingAppointments = appointments.filter(app => new Date(app.startTime) > new Date() && app.status === 'CONFIRMED').length;

        stats.innerHTML = `
            <div class="stat-item" id="statTotali" style="cursor:pointer;">
                <span class="stat-number">${totalAppointments}</span>
                <span class="stat-label">Totali</span>
            </div>
            <div class="stat-item" id="statConfermati" style="cursor:default; opacity:0.7;">
                <span class="stat-number">${confirmedAppointments}</span>
                <span class="stat-label">Confermati</span>
            </div>
            <div class="stat-item" id="statFuturi" style="cursor:pointer;">
                <span class="stat-number">${upcomingAppointments}</span>
                <span class="stat-label">Futuri</span>
            </div>
        `;
    }

    function createAppointmentCard(appointment) {
        const card = document.createElement('div');
        card.className = 'appointment-card';

        const startDate = new Date(appointment.startTime);
        const endDate = new Date(appointment.endTime);
        const duration = Math.round((endDate - startDate) / (1000 * 60 * 60) * 10) / 10; // ore con 1 decimale

        const statusClass = appointment.status === 'CONFIRMED' ? 'status-confirmed' : 'status-pending';
        const statusText = appointment.status === 'CONFIRMED' ? 'Confermato' : 'In Attesa';

        card.innerHTML = `
            <div class="appointment-header">
                <h3 class="appointment-title">${appointment.title}</h3>
                <span class="appointment-status ${statusClass}">${statusText}</span>
            </div>
            
            <div class="appointment-description">
                ${capitalizeFirstLetter(appointment.description)}
            </div>
            
            <div class="appointment-info">
                <div class="info-item">
                    <span class="info-icon">📅</span>
                    <span>${formatDate(startDate)}</span>
                </div>
                <div class="info-item">
                    <span class="info-icon">🕒</span>
                    <span>${formatTime(startDate)} - ${formatTime(endDate)} (${duration}h)</span>
                </div>
                <div class="info-item">
                    <span class="info-icon">📍</span>
                    <span>${capitalizeFirstLetter(appointment.location)}</span>
                </div>
            </div>
            
            ${appointment.notes ? `<div class="appointment-notes">
                <strong>Note:</strong> ${appointment.notes}
            </div>` : ''}
        `;

        return card;
    }

    function createUnconfirmedAppointmentCard(appointment) {
        const card = document.createElement('div');
        card.className = 'appointment-card unconfirmed';

        card.innerHTML = `
            <div class="appointment-header">
                <h3 class="appointment-title">${appointment.title}</h3>
                <span class="appointment-status status-pending">Da Confermare</span>
            </div>
            
            <div class="appointment-description">
                ${capitalizeFirstLetter(appointment.description)}
            </div>
            
            <div class="appointment-info">
                <div class="info-item">
                    <span class="info-icon">📍</span>
                    <span>${capitalizeFirstLetter(appointment.location)}</span>
                </div>
            </div>
            
            ${appointment.notes ? `<div class="appointment-notes">
                <strong>Note:</strong> ${appointment.notes}
            </div>` : ''}
        `;

        return card;
    }

    function capitalizeFirstLetter(string) {
        if (!string) return '';
        return string.charAt(0).toUpperCase() + string.slice(1);
    }

    function formatDate(date) {
        const now = new Date();
        const isPast = date < now;
        const formattedDate = date.toLocaleDateString('it-IT', {
            weekday: 'long',
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });
        
        return isPast ? `<span class="past-date">${formattedDate}</span>` : formattedDate;
    }

    function formatTime(date) {
        return date.toLocaleTimeString('it-IT', {
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    function showError(message) {
        errorMessage.textContent = message;
        errorMessage.style.display = 'block';
        appointmentsSection.style.display = 'none';
    }

    function hideError() {
        errorMessage.style.display = 'none';
    }

    // Pulsanti filtro tramite stat-item
    function setStatsListeners() {
        const statTotali = document.getElementById('statTotali');
        const statFuturi = document.getElementById('statFuturi');
        if (statTotali && statFuturi) {
            statTotali.addEventListener('click', () => {
                currentFilter = 'all';
                displayAppointments(loadedAppointments);
                setActiveStat('statTotali');
            });
            statFuturi.addEventListener('click', () => {
                currentFilter = 'future';
                displayAppointments(loadedAppointments);
                setActiveStat('statFuturi');
            });
        }
    }
    function setActiveStat(id) {
        ['statTotali','statFuturi'].forEach(statId => {
            const el = document.getElementById(statId);
            if (el) el.classList.toggle('active', statId === id);
        });
        // Confermati mai attivo né cliccabile
        const statConfermati = document.getElementById('statConfermati');
        if (statConfermati) {
            statConfermati.classList.remove('active');
            statConfermati.style.cursor = 'default';
        }
    }
});