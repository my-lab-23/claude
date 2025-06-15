#!/usr/bin/env python3
"""
Script per convertire file JSON di trascrizione in file di testo leggibili.
"""

import json
from pathlib import Path
from datetime import datetime
import sys
import argparse


class JsonToTextConverter:
    """
    Classe per convertire file JSON di trascrizione in testo.
    """
    def __init__(self, output_dir=None):
        if output_dir:
            self.output_dir = Path(output_dir)
        else:
            # Usa lo stesso percorso di output del transcriber
            self.output_dir = Path("/mnt/backup_usb/Youth/")
        
        self.output_dir.mkdir(parents=True, exist_ok=True)
        print(f"📁 Cartella di output: {self.output_dir}")

    def leggi_json_trascrizione(self, json_path):
        """Legge il file JSON della trascrizione."""
        try:
            with open(json_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            print(f"✅ File JSON caricato: {json_path}")
            return data
        except Exception as e:
            print(f"❌ Errore durante la lettura del file JSON: {e}")
            return None

    def estrai_testo(self, data):
        """Estrae il testo dalla struttura JSON di Whisper."""
        try:
            # Il testo completo è disponibile nel campo 'text'
            testo_completo = data.get('text', '')
            
            # Estrae anche i segmenti per una formattazione migliore
            segmenti = data.get('segments', [])
            
            return testo_completo, segmenti
        except Exception as e:
            print(f"❌ Errore durante l'estrazione del testo: {e}")
            return None, None

    def formatta_testo(self, testo_completo, segmenti, include_timestamps=False):
        """Formatta il testo per la scrittura su file."""
        output = []
        
        # Aggiungi intestazione
        output.append("=" * 60)
        output.append("           TRASCRIZIONE AUDIO")
        output.append("=" * 60)
        output.append(f"Data generazione: {datetime.now().strftime('%d/%m/%Y %H:%M:%S')}")
        output.append("-" * 60)
        output.append("")
        
        if include_timestamps and segmenti:
            # Versione con timestamp per ogni segmento
            output.append("TRASCRIZIONE CON TIMESTAMP:")
            output.append("")
            
            for i, segmento in enumerate(segmenti, 1):
                start = segmento.get('start', 0)
                end = segmento.get('end', 0)
                text = segmento.get('text', '').strip()
                
                # Converte i secondi in formato mm:ss
                start_min = int(start // 60)
                start_sec = int(start % 60)
                end_min = int(end // 60)
                end_sec = int(end % 60)
                
                timestamp = f"[{start_min:02d}:{start_sec:02d} - {end_min:02d}:{end_sec:02d}]"
                output.append(f"{timestamp} {text}")
        else:
            # Versione senza timestamp (testo continuo)
            output.append("TRASCRIZIONE:")
            output.append("")
            output.append(testo_completo.strip())
        
        output.append("")
        output.append("-" * 60)
        output.append("Fine trascrizione")
        
        return "\n".join(output)

    def salva_testo(self, testo_formattato, nome_file_originale, include_timestamps=False):
        """Salva il testo formattato in un file .txt."""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        
        # Rimuove il prefisso "TRASCRIZIONE_" se presente e l'estensione
        nome_base = Path(nome_file_originale).stem
        if nome_base.startswith("TRASCRIZIONE_"):
            nome_base = nome_base[13:]  # Rimuove "TRASCRIZIONE_"
        
        # Rimuove il timestamp esistente se presente
        parti = nome_base.split('_')
        if len(parti) > 1 and parti[-1].isdigit():
            nome_base = '_'.join(parti[:-1])
        
        suffix = "_CON_TIMESTAMP" if include_timestamps else ""
        nome_file_txt = f"TESTO_{nome_base}{suffix}_{timestamp}.txt"
        
        file_path = self.output_dir / nome_file_txt
        
        try:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(testo_formattato)
            
            print(f"💾 File di testo salvato: {file_path}")
            return file_path
        except Exception as e:
            print(f"❌ Errore durante il salvataggio: {e}")
            return None

    def converti_json_in_testo(self, json_path, include_timestamps=False):
        """Processo completo: legge JSON e salva come testo."""
        print(f"▶️ Conversione in corso per: {json_path}")
        
        # Verifica che il file esista
        if not json_path.exists():
            print(f"❌ File JSON non trovato: {json_path}")
            return False
        
        # Leggi il JSON
        data = self.leggi_json_trascrizione(json_path)
        if not data:
            return False
        
        # Estrai il testo
        testo_completo, segmenti = self.estrai_testo(data)
        if not testo_completo:
            return False
        
        # Formatta il testo
        testo_formattato = self.formatta_testo(testo_completo, segmenti, include_timestamps)
        
        # Salva il file
        file_salvato = self.salva_testo(testo_formattato, json_path.name, include_timestamps)
        
        if file_salvato:
            print("🎉 Conversione completata con successo!")
            return True
        
        return False


def main():
    """Funzione principale per gestire gli argomenti della riga di comando."""
    print("=" * 60)
    print("      JSON TO TEXT CONVERTER")
    print("=" * 60 + "\n")
    
    parser = argparse.ArgumentParser(description="Converte file JSON di trascrizione in file di testo leggibili.")
    parser.add_argument("path", help="Il percorso del file JSON o della cartella contenente i file JSON.")
    parser.add_argument("--timestamps", "-t", action="store_true",
                        help="Includi i timestamp nella trascrizione.")
    parser.add_argument("--output-dir", "-o", help="Cartella di output personalizzata.")
    
    args = parser.parse_args()
    
    input_path = Path(args.path)
    include_timestamps = args.timestamps
    output_dir = args.output_dir
    
    converter = JsonToTextConverter(output_dir)
    
    if input_path.is_file():
        # Conversione di un singolo file
        if input_path.suffix.lower() == '.json':
            converter.converti_json_in_testo(input_path, include_timestamps)
        else:
            print(f"❌ Il file deve avere estensione .json: {input_path}")
    elif input_path.is_dir():
        # Conversione di tutti i file JSON in una cartella
        print(f"📂 Conversione di tutti i file JSON nella cartella: {input_path}")
        json_files = list(input_path.glob("*.json"))
        
        if not json_files:
            print(f"⚠️ Nessun file JSON trovato nella cartella: {input_path}")
        else:
            for json_file in json_files:
                print(f"\n{'='*40}")
                converter.converti_json_in_testo(json_file, include_timestamps)
    else:
        print(f"❌ Percorso non valido: {input_path}. Fornisci un percorso a un file JSON o a una cartella.")


if __name__ == "__main__":
    main()
