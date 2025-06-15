#!/usr/bin/env python3
"""
Script per testare l'estrazione di dati appuntamento con Groq API
Uso: python3 test_groq.py <API_KEY> [prompt_personalizzato]
"""

import requests
import json
import sys
import argparse
from datetime import datetime, timedelta
from typing import Dict, Any

class Colors:
    """Colori per output terminale"""
    RED = '\033[0;31m'
    GREEN = '\033[0;32m'
    YELLOW = '\033[1;33m'
    BLUE = '\033[0;34m'
    PURPLE = '\033[0;35m'
    CYAN = '\033[0;36m'
    NC = '\033[0m'  # No Color

def print_status(message: str):
    print(f"{Colors.BLUE}[INFO]{Colors.NC} {message}")

def print_success(message: str):
    print(f"{Colors.GREEN}[SUCCESS]{Colors.NC} {message}")

def print_error(message: str):
    print(f"{Colors.RED}[ERROR]{Colors.NC} {message}")

def print_warning(message: str):
    print(f"{Colors.YELLOW}[WARNING]{Colors.NC} {message}")

def print_json(data: Dict[Any, Any], title: str = "JSON"):
    """Stampa JSON formattato con colori"""
    print(f"{Colors.CYAN}{title}:{Colors.NC}")
    print(json.dumps(data, indent=2, ensure_ascii=False))

def create_system_prompt() -> str:
    """Crea il prompt di sistema per l'AI"""
    return """Sei un assistente che estrae informazioni da testi in linguaggio naturale e le converte in formato JSON per appuntamenti.

Estrai dal testo dell'utente i seguenti campi e restituisci SOLO un JSON valido:
- id: genera un ID univoco (formato APP-XXX con numeri casuali)
- title: titolo dell'appuntamento
- description: descrizione dettagliata basata sul contesto
- startTime: data e ora inizio (formato ISO: YYYY-MM-DDTHH:MM:SS)
- endTime: data e ora fine (formato ISO: YYYY-MM-DDTHH:MM:SS, aggiungi durata ragionevole)
- location: luogo dell'appuntamento (se non specificato, usa "Da definire")
- participants: array di email dei partecipanti (genera email realistiche basate sui nomi)
- status: sempre "CONFIRMED"
- notes: note aggiuntive o istruzioni pertinenti

Regole per le date:
- Se non specificato il giorno, usa domani
- Se non specificata l'ora, usa orari lavorativi (9:00-18:00)
- Se non specificata la durata, stima in base al tipo di riunione

Rispondi ESCLUSIVAMENTE con il JSON valido, senza markdown, backticks o altre spiegazioni."""

def call_groq_api(api_key: str, user_prompt: str) -> Dict[Any, Any]:
    """Effettua la chiamata all'API Groq"""
    
    url = "https://api.groq.com/openai/v1/chat/completions"
    
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json"
    }
    
    payload = {
        "messages": [
            {
                "role": "system",
                "content": create_system_prompt()
            },
            {
                "role": "user",
                "content": user_prompt
            }
        ],
        "model": "llama3-8b-8192",
        "temperature": 0.1,
        "max_tokens": 1000,
        "top_p": 1,
        "stream": False
    }
    
    print_status("Inviando richiesta a Groq API...")
    print_status(f"Prompt: {user_prompt}")
    print()
    
    try:
        response = requests.post(url, headers=headers, json=payload, timeout=30)
        
        print_status(f"Status HTTP: {response.status_code}")
        
        if response.status_code == 200:
            return response.json()
        else:
            print_error(f"Errore API: {response.status_code}")
            try:
                error_data = response.json()
                print_json(error_data, "Dettagli errore")
            except:
                print_error(f"Risposta raw: {response.text}")
            return None
            
    except requests.exceptions.Timeout:
        print_error("Timeout della richiesta")
        return None
    except requests.exceptions.RequestException as e:
        print_error(f"Errore di rete: {e}")
        return None
    except Exception as e:
        print_error(f"Errore generico: {e}")
        return None

def validate_and_parse_json(ai_response: str) -> Dict[Any, Any]:
    """Valida e parsa la risposta JSON dell'AI"""
    
    print_status("Validando la risposta dell'AI...")
    
    # Pulisci la risposta da eventuali caratteri extra
    cleaned_response = ai_response.strip()
    
    # Rimuovi eventuali backticks markdown
    if cleaned_response.startswith('```'):
        lines = cleaned_response.split('\n')
        cleaned_response = '\n'.join(lines[1:-1])
    
    try:
        parsed_json = json.loads(cleaned_response)
        print_success("✓ JSON valido!")
        return parsed_json
        
    except json.JSONDecodeError as e:
        print_error(f"✗ JSON non valido: {e}")
        print_warning("Risposta AI raw:")
        print(cleaned_response)
        return None

def test_with_appointment_server(json_data: Dict[Any, Any], server_url: str = "http://192.168.168.93:8079/appointments") -> bool:
    """Testa il JSON generato con il server degli appuntamenti"""
    
    print_status(f"Testando con server appuntamenti: {server_url}")
    
    try:
        headers = {"Content-Type": "application/json"}
        response = requests.post(server_url, json=json_data, timeout=10)
        
        print_status(f"Server response status: {response.status_code}")
        
        if response.status_code in [200, 201]:
            print_success("✓ Server ha accettato l'appuntamento!")
            try:
                response_data = response.json()
                print_json(response_data, "Risposta server")
            except:
                print_success(f"Risposta server: {response.text}")
            return True
        else:
            print_warning(f"Server ha risposto con codice {response.status_code}")
            print_warning(f"Risposta: {response.text}")
            return False
            
    except requests.exceptions.ConnectionError:
        print_warning("Non riesco a connettermi al server (normale se non è avviato)")
        return False
    except Exception as e:
        print_error(f"Errore nella chiamata al server: {e}")
        return False

def main():
    parser = argparse.ArgumentParser(description="Test Groq API per estrazione dati appuntamento")
    parser.add_argument("api_key", help="Groq API key (inizia con gsk-)")
    parser.add_argument("prompt", nargs='?', 
                       help="Prompt personalizzato (opzionale)",
                       default="Riunione marketing domani alle 14:30 con Mario e Luigi in sala conferenze per discutere la campagna estiva")
    parser.add_argument("--server", "-s", 
                       help="URL del server appuntamenti",
                       default="http://192.168.168.93:8079/appointments")
    parser.add_argument("--no-server-test", action="store_true",
                       help="Salta il test con il server")
    
    args = parser.parse_args()
    
    print_status("🚀 Test Groq API per estrazione dati appuntamento")
    print_status("=" * 60)
    print()
    
    # Chiamata API
    response_data = call_groq_api(args.api_key, args.prompt)
    
    if not response_data:
        print_error("Chiamata API fallita!")
        sys.exit(1)
    
    print_success("Chiamata API riuscita!")
    print()
    
    # Estrai risposta AI
    try:
        ai_content = response_data['choices'][0]['message']['content']
        usage = response_data.get('usage', {})
        
        print_success("Risposta dell'AI:")
        print(f"{Colors.GREEN}{ai_content}{Colors.NC}")
        print()
        
        # Mostra statistiche
        if usage:
            print_status("Statistiche utilizzo:")
            print_json(usage)
            print()
        
    except KeyError as e:
        print_error(f"Formato risposta API inatteso: {e}")
        print_json(response_data, "Risposta completa")
        sys.exit(1)
    
    # Valida JSON
    appointment_data = validate_and_parse_json(ai_content)
    
    if not appointment_data:
        print_error("Impossibile parsare la risposta dell'AI")
        sys.exit(1)
    
    print()
    print_json(appointment_data, "JSON appuntamento generato")
    print()
    
    # Salva risultato
    output_file = "groq_appointment.json"
    try:
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(appointment_data, f, indent=2, ensure_ascii=False)
        print_success(f"Risultato salvato in '{output_file}'")
    except Exception as e:
        print_warning(f"Impossibile salvare file: {e}")
    
    # Test con server (opzionale)
    if not args.no_server_test:
        print()
        print_status("=" * 60)
        test_with_appointment_server(appointment_data, args.server)
    
    print()
    print_success("🎉 Test completato!")

if __name__ == "__main__":
    main()