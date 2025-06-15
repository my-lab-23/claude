#!/usr/bin/env python3
"""
Script per trascrivere un file audio o una cartella di file audio utilizzando Whisper.
"""

import whisper
import json
from pathlib import Path
from datetime import datetime
import sys
import argparse
import glob
import subprocess
import tempfile
import os

# Definisci le estensioni di file audio/video supportate
SUPPORTED_EXTENSIONS = [".mp3", ".wav", ".m4a", ".flac", ".mp4", ".mov", ".avi"]


class Transcriber:
    """
    Classe per gestire la trascrizione di un file audio.
    """
    def __init__(self, model_size="base", output_dir=None):
        self.model_size = model_size
        self.model = None

        if output_dir:
            self.output_dir = Path(output_dir)
        else:
            home_dir = Path.home()
            self.output_dir = home_dir / "Scrivania" / "Gradle" / "new_fine" / "Korso" / "Youth"

        self.output_dir.mkdir(parents=True, exist_ok=True)
        print(f"📁 Cartella di output: {self.output_dir}")

    def carica_modello(self):
        """Carica il modello Whisper."""
        print(f"🔄 Caricamento del modello Whisper ({self.model_size})...")
        try:
            self.model = whisper.load_model(self.model_size)
            print("✅ Modello caricato con successo!")
        except Exception as e:
            print(f"❌ Errore durante il caricamento del modello: {e}")
            return False
        return True

    def trascrivi_audio(self, audio_path, duration_minutes=None):
        """
        Trascrive l'audio utilizzando Whisper, con un'opzione per limitare la durata
        utilizzando un file temporaneo creato con ffmpeg.
        """
        input_file = audio_path
        temp_file = None

        if duration_minutes:
            try:
                # Crea un file temporaneo con estensione mp4 per compatibilità
                temp_file = tempfile.NamedTemporaryFile(suffix=".mp4", delete=False)
                temp_file_path = temp_file.name
                temp_file.close()

                print(f"⏳ Creazione di un file temporaneo di {duration_minutes} minuti con ffmpeg...")
                duration_seconds = duration_minutes * 60
                
                # Esegui il comando ffmpeg per tagliare il file.
                # Aggiunge il parametro '-y' per sovrascrivere senza chiedere conferma.
                command = [
                    "ffmpeg",
                    "-y",  # Forza la sovrascrittura
                    "-i", str(audio_path),
                    "-t", str(duration_seconds),
                    "-acodec", "copy",
                    "-vcodec", "copy",
                    temp_file_path
                ]
                
                subprocess.run(command, check=True)
                
                print("✅ File temporaneo creato con successo!")
                input_file = Path(temp_file_path)

            except FileNotFoundError:
                print("❌ Errore: ffmpeg non trovato. Assicurati che sia installato e nel tuo PATH.")
                return None
            except subprocess.CalledProcessError as e:
                print(f"❌ Errore durante l'esecuzione di ffmpeg: {e}")
                return None
            except Exception as e:
                print(f"❌ Errore nella gestione del file temporaneo: {e}")
                return None

        result = None
        try:
            print("🔄 Trascrizione dell'audio in corso...")
            result = self.model.transcribe(str(input_file),
                                           task="transcribe",
                                           word_timestamps=True,
                                           verbose=False)
            print("✅ Trascrizione completata!")
        except Exception as e:
            print(f"❌ Errore durante la trascrizione: {e}")
        finally:
            if temp_file:
                # Pulisce il file temporaneo
                os.remove(input_file)
                print("🗑️ File temporaneo rimosso.")
        
        return result


    def salva_trascrizione(self, result, audio_file_name):
        """Salva il risultato della trascrizione in un file JSON."""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        nome_base = Path(audio_file_name).stem
        nome_file = f"TRASCRIZIONE_{nome_base}_{timestamp}.json"
        file_path = self.output_dir / nome_file

        with open(file_path, 'w', encoding='utf-8') as f:
            json.dump(result, f, ensure_ascii=False, indent=4)

        print(f"💾 Trascrizione salvata in: {file_path}")
        return file_path

    def processa_trascrizione(self, audio_path, duration_minutes=None):
        """Processo completo: trascrive e salva."""
        print(f"▶️ Inizio processo per: {audio_path}")

        if not self.model and not self.carica_modello():
            return False

        if not audio_path.exists():
            print(f"❌ File audio non trovato: {audio_path}")
            return False
            
        result = self.trascrivi_audio(audio_path, duration_minutes)
        if not result:
            return False

        self.salva_trascrizione(result, audio_path.name)

        print("🎉 Processo di trascrizione completato!")
        return True


def main():
    """
    Funzione principale per gestire gli argomenti della riga di comando.
    """
    print("="*60)
    print("          AUDIO TRANSCRIBER")
    print("="*60 + "\n")

    parser = argparse.ArgumentParser(description="Trascrive un file o una cartella di file audio utilizzando Whisper.")
    parser.add_argument("path", help="Il percorso del file o della cartella da trascrivere.")
    
    modelli = {"1": "tiny", "2": "base", "3": "small"}
    parser.add_argument("--model", "-m", choices=modelli.values(), default="base",
                        help="Scegli la precisione della trascrizione (tiny, base, small). Predefinito: base.")
    
    parser.add_argument("--duration", "-d", type=int,
                        help="Specifica il numero di minuti dall'inizio che devono essere trascritti.")

    args = parser.parse_args()

    input_path = Path(args.path)
    model_size = args.model
    duration_minutes = args.duration

    # Imposta la cartella di output
    output_directory = Path("/mnt/backup_usb/Youth/")
    
    transcriber = Transcriber(model_size, output_directory)

    if input_path.is_file():
        # Trascrizione di un singolo file
        if input_path.suffix.lower() in SUPPORTED_EXTENSIONS:
            transcriber.processa_trascrizione(input_path, duration_minutes)
        else:
            print(f"❌ Estensione non supportata per il file: {input_path}")
    elif input_path.is_dir():
        # Trascrizione di tutti i file supportati in una cartella
        print(f"📂 Inizio la trascrizione di tutti i file supportati nella cartella: {input_path}")
        found_files = []
        for ext in SUPPORTED_EXTENSIONS:
            found_files.extend(input_path.glob(f"*{ext}"))
        
        if not found_files:
            print(f"⚠️ Nessun file supportato trovato nella cartella: {input_path}")
        else:
            for file_path in found_files:
                transcriber.processa_trascrizione(file_path, duration_minutes)
    else:
        print(f"❌ Percorso non valido: {input_path}. Fornisci un percorso a un file o a una cartella.")

if __name__ == "__main__":
    main()
    