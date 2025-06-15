import csv
import os

def conta_campi_mancanti(nome_file):
    campi_mancanti = 0
    with open(nome_file, mode='r', newline='', encoding='utf-8') as file:
        reader = csv.reader(file)
        intestazione = next(reader)  # Salta l'intestazione
        for riga_num, riga in enumerate(reader, start=2):
            for valore in riga:
                if valore.strip() == '':
                    campi_mancanti += 1
            if len(riga) < len(intestazione):
                campi_mancanti += len(intestazione) - len(riga)
    return campi_mancanti

# Richiesta input all'utente
nome_file = input("Inserisci il percorso del file CSV: ").strip()

# Controllo se il file esiste
if not os.path.isfile(nome_file):
    print("Errore: il file specificato non esiste.")
else:
    mancanti = conta_campi_mancanti(nome_file)
    print(f"Numero totale di campi mancanti: {mancanti}")
