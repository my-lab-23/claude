def valida_input(valore, min_val, max_val):
    """Valida l'input dell'utente per assicurarsi che sia nel range corretto"""
    try:
        num = int(valore)
        if min_val <= num <= max_val:
            return num
        else:
            return None
    except ValueError:
        return None

def valida_si_no(risposta):
    """Valida le risposte Si/No"""
    risposta = risposta.lower().strip()
    if risposta in ['si', 'sì', 's', 'yes', 'y']:
        return 'Sì'
    elif risposta in ['no', 'n']:
        return 'No'
    else:
        return None

def questionario_personalita():
    """Funzione principale per il questionario sulla personalità"""
    print("=== QUESTIONARIO SULLA PERSONALITÀ ===")
    print("Rispondi alle seguenti domande con sincerità.\n")
    
    risposte = {}
    
    # 1. Time_spent_Alone (0-11 ore)
    while True:
        risposta = input("1. Quante ore passi da solo/a ogni giorno? (0-11): ")
        valore = valida_input(risposta, 0, 11)
        if valore is not None:
            risposte['Ore_trascorse_da_solo'] = valore
            break
        else:
            print("   Inserisci un numero valido tra 0 e 11.")
    
    # 2. Stage_fear (Yes/No)
    while True:
        risposta = input("2. Hai paura del palcoscenico/di parlare in pubblico? (Sì/No): ")
        valore = valida_si_no(risposta)
        if valore is not None:
            risposte['Paura_del_palcoscenico'] = valore
            break
        else:
            print("   Rispondi con 'Sì' o 'No'.")
    
    # 3. Social_event_attendance (0-10)
    while True:
        risposta = input("3. Con che frequenza partecipi ad eventi sociali? (0=mai, 10=sempre): ")
        valore = valida_input(risposta, 0, 10)
        if valore is not None:
            risposte['Partecipazione_eventi_sociali'] = valore
            break
        else:
            print("   Inserisci un numero valido tra 0 e 10.")
    
    # 4. Going_outside (0-7)
    while True:
        risposta = input("4. Quante volte esci di casa a settimana? (0-7): ")
        valore = valida_input(risposta, 0, 7)
        if valore is not None:
            risposte['Uscite_settimanali'] = valore
            break
        else:
            print("   Inserisci un numero valido tra 0 e 7.")
    
    # 5. Drained_after_socializing (Yes/No)
    while True:
        risposta = input("5. Ti senti scarico/a dopo aver socializzato? (Sì/No): ")
        valore = valida_si_no(risposta)
        if valore is not None:
            risposte['Stanchezza_dopo_socializzazione'] = valore
            break
        else:
            print("   Rispondi con 'Sì' o 'No'.")
    
    # 6. Friends_circle_size (0-15)
    while True:
        risposta = input("6. Quanti amici stretti hai? (0-15): ")
        valore = valida_input(risposta, 0, 15)
        if valore is not None:
            risposte['Numero_amici_stretti'] = valore
            break
        else:
            print("   Inserisci un numero valido tra 0 e 15.")
    
    # 7. Post_frequency (0-10)
    while True:
        risposta = input("7. Con che frequenza pubblichi sui social media? (0=mai, 10=continuamente): ")
        valore = valida_input(risposta, 0, 10)
        if valore is not None:
            risposte['Frequenza_post_social'] = valore
            break
        else:
            print("   Inserisci un numero valido tra 0 e 10.")
    
    return risposte

def mostra_risultati(risposte):
    """Mostra i risultati del questionario"""
    print("\n" + "="*50)
    print("RIEPILOGO DELLE TUE RISPOSTE:")
    print("="*50)
    
    for domanda, risposta in risposte.items():
        print(f"{domanda.replace('_', ' ').title()}: {risposta}")
    
    print("\nGrazie per aver completato il questionario!")

def main():
    """Funzione principale"""
    try:
        risposte = questionario_personalita()
        mostra_risultati(risposte)
        
        # Opzione per salvare i risultati
        salva = input("\nVuoi salvare i risultati in un file? (Sì/No): ")
        if valida_si_no(salva) == 'Sì':
            while True:
                try:
                    percorso_file = input("Inserisci il percorso completo del file (con estensione): ")
                    
                    # Se non ha estensione, aggiungi .txt
                    if not percorso_file.endswith(('.txt', '.csv', '.log')):
                        percorso_file += '.txt'
                    
                    # Crea le directory se non esistono
                    import os
                    directory = os.path.dirname(percorso_file)
                    if directory and not os.path.exists(directory):
                        os.makedirs(directory)
                        print(f"Directory creata: {directory}")
                    
                    # Salva il file
                    with open(percorso_file, 'w', encoding='utf-8') as f:
                        f.write("RISULTATI QUESTIONARIO PERSONALITÀ\n")
                        f.write("="*40 + "\n\n")
                        for domanda, risposta in risposte.items():
                            f.write(f"{domanda.replace('_', ' ').title()}: {risposta}\n")
                    
                    print(f"Risultati salvati in '{percorso_file}'")
                    break
                    
                except PermissionError:
                    print("   Errore: Non hai i permessi per scrivere in questa posizione.")
                    print("   Prova con un percorso diverso.")
                except FileNotFoundError:
                    print("   Errore: Percorso non valido.")
                    print("   Assicurati che il percorso sia corretto.")
                except Exception as e:
                    print(f"   Errore durante il salvataggio: {e}")
                    print("   Prova con un percorso diverso.")
            
    except KeyboardInterrupt:
        print("\n\nQuestionario interrotto dall'utente.")
    except Exception as e:
        print(f"\nSi è verificato un errore: {e}")

if __name__ == "__main__":
    main()