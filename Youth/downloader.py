#!/usr/bin/env python3
"""
Scaricatore Video da File Playlist
Legge i file di playlist generati e scarica tutti i video in una cartella dedicata
"""

import yt_dlp
import os
import glob
import re
from datetime import datetime
import sys

def trova_file_playlist():
    """
    Trova tutti i file playlist nella directory corrente
    
    Returns:
        list: Lista dei file playlist trovati
    """
    pattern = "./playlist_links_*.txt"
    file_trovati = glob.glob(pattern)
    return sorted(file_trovati)

def mostra_menu_file(file_playlist):
    """
    Mostra il menu per selezionare il file playlist
    
    Args:
        file_playlist (list): Lista dei file playlist
    
    Returns:
        str: Percorso del file selezionato o None
    """
    if not file_playlist:
        print("❌ Nessun file playlist trovato nella directory corrente!")
        print("   Assicurati che ci siano file con nome 'playlist_links_*.txt'")
        return None
    
    print("📁 FILE PLAYLIST DISPONIBILI:")
    print("=" * 60)
    
    for i, file in enumerate(file_playlist, 1):
        # Estrae informazioni dal file
        nome_file = os.path.basename(file)
        try:
            with open(file, 'r', encoding='utf-8') as f:
                prime_righe = f.readlines()[:4]
                
            titolo_playlist = "N/A"
            numero_video = "N/A"
            data_creazione = "N/A"
            
            for riga in prime_righe:
                if riga.startswith("PLAYLIST:"):
                    titolo_playlist = riga.replace("PLAYLIST:", "").strip()
                elif riga.startswith("NUMERO TOTALE VIDEO:"):
                    numero_video = riga.replace("NUMERO TOTALE VIDEO:", "").strip()
                elif riga.startswith("DATA ESTRAZIONE:"):
                    data_creazione = riga.replace("DATA ESTRAZIONE:", "").strip()
            
            print(f"{i:2d}. {nome_file}")
            print(f"    📺 Playlist: {titolo_playlist[:50]}...")
            print(f"    🎬 Video: {numero_video}")
            print(f"    📅 Creato: {data_creazione}")
            print("-" * 60)
            
        except Exception as e:
            print(f"{i:2d}. {nome_file} (errore lettura: {str(e)})")
            print("-" * 60)
    
    return file_playlist

def estrai_link_da_file(percorso_file):
    """
    Estrae i link YouTube dal file playlist
    
    Args:
        percorso_file (str): Percorso del file playlist
    
    Returns:
        tuple: (lista_link, info_playlist)
    """
    link = []
    info_playlist = {
        'titolo': 'N/A',
        'url_originale': 'N/A',
        'numero_video': 0,
        'data_estrazione': 'N/A'
    }
    
    try:
        with open(percorso_file, 'r', encoding='utf-8') as f:
            contenuto = f.read()
        
        # Estrae informazioni header
        righe = contenuto.split('\n')
        for riga in righe[:10]:  # Controlla prime 10 righe per info
            if riga.startswith("PLAYLIST:"):
                info_playlist['titolo'] = riga.replace("PLAYLIST:", "").strip()
            elif riga.startswith("URL PLAYLIST:"):
                info_playlist['url_originale'] = riga.replace("URL PLAYLIST:", "").strip()
            elif riga.startswith("NUMERO TOTALE VIDEO:"):
                try:
                    info_playlist['numero_video'] = int(riga.replace("NUMERO TOTALE VIDEO:", "").strip())
                except:
                    pass
            elif riga.startswith("DATA ESTRAZIONE:"):
                info_playlist['data_estrazione'] = riga.replace("DATA ESTRAZIONE:", "").strip()
        
        # Estrae link con regex
        pattern_link = r'https://www\.youtube\.com/watch\?v=[a-zA-Z0-9_-]+'
        link_trovati = re.findall(pattern_link, contenuto)
        
        # Rimuove duplicati mantenendo l'ordine
        link = list(dict.fromkeys(link_trovati))
        
        print(f"✓ Link estratti dal file: {len(link)}")
        
        return link, info_playlist
        
    except Exception as e:
        print(f"❌ Errore nella lettura del file: {str(e)}")
        return [], info_playlist

def crea_cartella_download(info_playlist):
    """
    Crea una cartella per il download basata sul titolo della playlist
    
    Args:
        info_playlist (dict): Informazioni sulla playlist
    
    Returns:
        str: Percorso della cartella creata
    """
    # Pulisce il titolo per nome cartella
    titolo_pulito = re.sub(r'[<>:"/\\|?*]', '_', info_playlist['titolo'])
    titolo_pulito = titolo_pulito.strip()[:50]  # Limita lunghezza
    
    # Timestamp per unicità
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    nome_cartella = f"Download_{titolo_pulito}_{timestamp}"
    
    # Crea la cartella
    try:
        os.makedirs(nome_cartella, exist_ok=True)
        print(f"📁 Cartella creata: {nome_cartella}")
        return nome_cartella
    except Exception as e:
        print(f"❌ Errore creazione cartella: {str(e)}")
        return None

def scarica_video(link_list, cartella_download, info_playlist):
    """
    Scarica tutti i video dai link forniti
    
    Args:
        link_list (list): Lista dei link da scaricare
        cartella_download (str): Cartella di destinazione
        info_playlist (dict): Informazioni playlist per log
    """
    
    # Configurazione yt-dlp per download
    ydl_opts = {
        'outtmpl': os.path.join(cartella_download, '%(playlist_index)03d - %(title)s.%(ext)s'),
        'format': 'best[height<=720]/best',  # Qualità buona ma non eccessiva
        'writeinfojson': False,  # Non salva metadati JSON
        'writesubtitles': False,  # Non scarica sottotitoli
        'writeautomaticsub': False,
        'ignoreerrors': True,  # Continua anche se un video fallisce
    }
    
    print(f"\n🚀 INIZIO DOWNLOAD")
    print(f"📺 Playlist: {info_playlist['titolo']}")
    print(f"🎬 Video da scaricare: {len(link_list)}")
    print(f"📁 Cartella: {cartella_download}")
    print("=" * 80)
    
    successi = 0
    errori = 0
    
    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            for i, link in enumerate(link_list, 1):
                try:
                    print(f"\n[{i:03d}/{len(link_list):03d}] Scaricando: {link}")
                    ydl.download([link])
                    successi += 1
                    print(f"✓ Download completato ({i}/{len(link_list)})")
                    
                except Exception as e:
                    errori += 1
                    print(f"❌ Errore download: {str(e)}")
                    
                    # Salva errori in un file log
                    with open(os.path.join(cartella_download, "errori_download.log"), "a", encoding="utf-8") as log:
                        log.write(f"{datetime.now()}: Errore su {link} - {str(e)}\n")
    
    except KeyboardInterrupt:
        print(f"\n\n⏹️ Download interrotto dall'utente")
    except Exception as e:
        print(f"\n❌ Errore generale: {str(e)}")
    
    # Riepilogo finale
    print(f"\n" + "=" * 80)
    print(f"📊 RIEPILOGO DOWNLOAD")
    print(f"✅ Successi: {successi}")
    print(f"❌ Errori: {errori}")
    print(f"📁 Cartella: {cartella_download}")
    
    # Crea file riepilogo
    try:
        with open(os.path.join(cartella_download, "riepilogo_download.txt"), "w", encoding="utf-8") as f:
            f.write(f"RIEPILOGO DOWNLOAD\n")
            f.write(f"Playlist: {info_playlist['titolo']}\n")
            f.write(f"Data download: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
            f.write(f"Video scaricati con successo: {successi}\n")
            f.write(f"Errori: {errori}\n")
            f.write(f"Totale link processati: {len(link_list)}\n")
    except:
        pass
    
    return successi, errori

def main():
    """Funzione principale"""
    
    print("🎬 SCARICATORE VIDEO DA FILE PLAYLIST")
    print("=" * 80)
    
    # Trova file playlist
    file_playlist = trova_file_playlist()
    if not file_playlist:
        return
    
    # Mostra menu e selezione
    file_disponibili = mostra_menu_file(file_playlist)
    
    try:
        scelta = input(f"\nScegli il file playlist (1-{len(file_disponibili)}): ").strip()
        indice = int(scelta) - 1
        
        if indice < 0 or indice >= len(file_disponibili):
            print("❌ Scelta non valida!")
            return
        
        file_selezionato = file_disponibili[indice]
        print(f"\n✓ File selezionato: {file_selezionato}")
        
    except (ValueError, KeyboardInterrupt):
        print("❌ Operazione annullata")
        return
    
    # Estrae link dal file
    print(f"\n📖 Lettura file playlist...")
    link_video, info_playlist = estrai_link_da_file(file_selezionato)
    
    if not link_video:
        print("❌ Nessun link trovato nel file!")
        return
    
    print(f"✓ Trovati {len(link_video)} link video")
    
    # Conferma download
    print(f"\n📋 ANTEPRIMA DOWNLOAD:")
    print(f"📺 Playlist: {info_playlist['titolo']}")
    print(f"🎬 Video da scaricare: {len(link_video)}")
    
    conferma = input(f"\n🤔 Procedere con il download? (s/N): ").strip().lower()
    if conferma not in ['s', 'si', 'y', 'yes']:
        print("⏹️ Download annullato")
        return
    
    # Crea cartella download
    cartella = crea_cartella_download(info_playlist)
    if not cartella:
        return
    
    # Avvia download
    successi, errori = scarica_video(link_video, cartella, info_playlist)
    
    print(f"\n🎉 OPERAZIONE COMPLETATA!")
    if successi > 0:
        print(f"✅ {successi} video scaricati in '{cartella}'")
    if errori > 0:
        print(f"⚠️  {errori} errori (vedi log nella cartella)")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print(f"\n\n👋 Arrivederci!")
    except Exception as e:
        print(f"\n❌ Errore imprevisto: {str(e)}")
