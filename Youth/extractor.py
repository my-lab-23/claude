#!/usr/bin/env python3
"""
Estrattore di link da playlist YouTube
Salva tutti i link dei video di una playlist in un file di testo
"""

import yt_dlp
import sys
from datetime import datetime

def estrai_link_playlist(url_playlist, nome_file_output=None):
    """
    Estrae tutti i link video da una playlist YouTube e li salva in un file
    
    Args:
        url_playlist (str): URL della playlist YouTube
        nome_file_output (str): Nome del file di output (opzionale)
    """
    
    # Configurazione per yt-dlp
    ydl_opts = {
        'quiet': True,  # Riduce l'output verboso
        'no_warnings': True,
        'extract_flat': True,  # Estrae solo le info di base senza scaricare
        'dump_single_json': False,
    }
    
    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            print("Estrazione informazioni playlist in corso...")
            
            # Estrae informazioni dalla playlist
            info = ydl.extract_info(url_playlist, download=False)
            
            if 'entries' not in info:
                print("Errore: Impossibile trovare video nella playlist")
                return
            
            # Nome del file di output
            if not nome_file_output:
                timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
                nome_file_output = f"playlist_links_{timestamp}.txt"
            
            # Estrae i link e le informazioni
            video_info = []
            for entry in info['entries']:
                if entry:  # Verifica che l'entry non sia None
                    video_id = entry.get('id', 'N/A')
                    titolo = entry.get('title', 'Titolo non disponibile')
                    url = f"https://www.youtube.com/watch?v={video_id}"
                    durata = entry.get('duration_string', 'N/A')
                    
                    video_info.append({
                        'url': url,
                        'titolo': titolo,
                        'durata': durata,
                        'id': video_id
                    })
            
            # Salva nel file
            with open(nome_file_output, 'w', encoding='utf-8') as f:
                f.write(f"PLAYLIST: {info.get('title', 'Titolo non disponibile')}\n")
                f.write(f"URL PLAYLIST: {url_playlist}\n")
                f.write(f"NUMERO TOTALE VIDEO: {len(video_info)}\n")
                f.write(f"DATA ESTRAZIONE: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
                f.write("=" * 80 + "\n\n")
                
                for i, video in enumerate(video_info, 1):
                    f.write(f"{i:03d}. {video['titolo']}\n")
                    f.write(f"     URL: {video['url']}\n")
                    f.write(f"     Durata: {video['durata']}\n")
                    f.write(f"     ID: {video['id']}\n")
                    f.write("-" * 50 + "\n")
                
                # Sezione solo link (per copia-incolla facile)
                f.write("\n" + "=" * 80 + "\n")
                f.write("SOLO LINK (per copia-incolla):\n")
                f.write("=" * 80 + "\n")
                for video in video_info:
                    f.write(f"{video['url']}\n")
            
            print(f"✓ Estrazione completata!")
            print(f"✓ Trovati {len(video_info)} video")
            print(f"✓ Link salvati in: {nome_file_output}")
            
            return video_info
            
    except Exception as e:
        print(f"Errore durante l'estrazione: {str(e)}")
        return None

def main():
    """Funzione principale"""
    
    # URL della playlist fornita
    url_playlist = "https://www.youtube.com/playlist?list=PLhEwqlL10MqMSHePf3Kn4T8AaR0ItUUer"
    
    print("Estrattore Link Playlist YouTube")
    print("=" * 50)
    print(f"Playlist da elaborare: {url_playlist}")
    print()
    
    # Opzione per personalizzare il nome del file
    nome_file = input("Nome file di output (invio per nome automatico): ").strip()
    if not nome_file:
        nome_file = None
    
    # Estrae i link
    risultato = estrai_link_playlist(url_playlist, nome_file)
    
    if risultato:
        print("\n" + "=" * 50)
        print("RIEPILOGO:")
        print(f"Video trovati: {len(risultato)}")
        print("File salvato con successo!")
    else:
        print("Estrazione fallita. Verifica l'URL della playlist.")

# Funzione per uso come modulo
def estrai_da_url_personalizzato():
    """Permette di inserire un URL personalizzato"""
    url = input("Inserisci l'URL della playlist YouTube: ").strip()
    if not url:
        print("URL non valido")
        return
    
    nome_file = input("Nome file di output (invio per nome automatico): ").strip()
    if not nome_file:
        nome_file = None
    
    return estrai_link_playlist(url, nome_file)

if __name__ == "__main__":
    print("Scegli un'opzione:")
    print("1. Usa la playlist predefinita")
    print("2. Inserisci URL playlist personalizzato")
    
    scelta = input("Scelta (1 o 2): ").strip()
    
    if scelta == "2":
        estrai_da_url_personalizzato()
    else:
        main()
