#!/usr/bin/env python3
"""
Script per verificare se un video YouTube ha una trascrizione disponibile.
Richiede l'installazione di: pip install youtube-transcript-api
"""

import sys
import re
from youtube_transcript_api import YouTubeTranscriptApi
from youtube_transcript_api._errors import TranscriptsDisabled, NoTranscriptFound

def estrai_video_id(url):
    """
    Estrae l'ID del video da un URL di YouTube
    """
    # Pattern per diversi formati di URL YouTube
    patterns = [
        r'(?:https?://)?(?:www\.)?youtube\.com/watch\?v=([^&\n?#]+)',
        r'(?:https?://)?(?:www\.)?youtube\.com/embed/([^&\n?#]+)',
        r'(?:https?://)?(?:www\.)?youtube\.com/v/([^&\n?#]+)',
        r'(?:https?://)?youtu\.be/([^&\n?#]+)',
    ]
    
    for pattern in patterns:
        match = re.search(pattern, url)
        if match:
            return match.group(1)
    
    return None

def verifica_trascrizione(video_url):
    """
    Verifica se un video YouTube ha una trascrizione disponibile
    """
    # Estrai l'ID del video dall'URL
    video_id = estrai_video_id(video_url)
    
    if not video_id:
        return False, "URL non valido o ID video non trovato"
    
    try:
        # Prova a ottenere la lista delle trascrizioni disponibili
        transcript_list = YouTubeTranscriptApi.list_transcripts(video_id)
        
        # Verifica se ci sono trascrizioni disponibili
        trascrizioni_disponibili = []
        
        # Controlla trascrizioni manuali
        for transcript in transcript_list:
            if not transcript.is_generated:
                trascrizioni_disponibili.append(f"Manuale ({transcript.language})")
        
        # Controlla trascrizioni automatiche
        for transcript in transcript_list:
            if transcript.is_generated:
                trascrizioni_disponibili.append(f"Automatica ({transcript.language})")
        
        if trascrizioni_disponibili:
            return True, trascrizioni_disponibili
        else:
            return False, "Nessuna trascrizione trovata"
            
    except TranscriptsDisabled:
        return False, "Le trascrizioni sono disabilitate per questo video"
    except NoTranscriptFound:
        return False, "Nessuna trascrizione disponibile"
    except Exception as e:
        return False, f"Errore durante la verifica: {str(e)}"

def main():
    """
    Funzione principale dello script
    """
    print("=== Verifica Trascrizione YouTube ===\n")
    
    # Se viene passato un argomento da linea di comando
    if len(sys.argv) > 1:
        video_url = sys.argv[1]
    else:
        # Altrimenti chiedi l'input all'utente
        video_url = input("Inserisci l'URL del video YouTube: ").strip()
    
    if not video_url:
        print("❌ URL non fornito!")
        return
    
    print(f"🔍 Verifico il video: {video_url}")
    print("-" * 50)
    
    # Verifica la trascrizione
    ha_trascrizione, dettagli = verifica_trascrizione(video_url)
    
    if ha_trascrizione:
        print("✅ TRASCRIZIONE DISPONIBILE!")
        print("\nTipi di trascrizione trovati:")
        for trascrizione in dettagli:
            print(f"  • {trascrizione}")
    else:
        print("❌ TRASCRIZIONE NON DISPONIBILE")
        print(f"Motivo: {dettagli}")

if __name__ == "__main__":
    main()