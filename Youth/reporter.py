#!/usr/bin/env python3
"""
Script per analizzare una cartella di file di trascrizione e generare un file con gli argomenti delle lezioni.
"""

import sys
import json
import re
from collections import Counter, defaultdict
from pathlib import Path

class FolderReportGenerator:
    def __init__(self):
        # Dizionario specializzato per Probabilità 
        self.concetti_probabilita = {
            # Concetti base
            'probabilità', 'evento', 'spazio campionario', 'omega', 'esperimento',
            
            # Eventi e operazioni
            'unione', 'intersezione', 'complementare', 'incompatibili', 'disgiunti',
            'partizione', 'sigma algebra', 'boreliano', 'misurabile',
            
            # Probabilità condizionata
            'condizionata', 'indipendenza', 'indipendenti', 'bayes', 'totale',
            'posteriore', 'priori', 'likelihood', 'verosimiglianza',
            
            # Variabili aleatorie
            'variabile aleatoria', 'discreta', 'continua', 'distribuzione',
            'funzione di massa', 'densità', 'ripartizione', 'cumulativa',
            
            # Distribuzioni comuni
            'bernoulli', 'binomiale', 'poisson', 'geometrica', 'ipergeometrica',
            'uniforme', 'normale', 'gaussiana', 'esponenziale', 'gamma', 'beta',
            'chi quadrato', 'student', 'fisher',
            
            # Momenti e statistiche
            'valore atteso', 'media', 'varianza', 'deviazione standard', 'momento',
            'covarianza', 'correlazione', 'standardizzata', 'centrata',
            
            # Teoremi fondamentali
            'legge debole', 'legge forte', 'grandi numeri', 'limite centrale',
            'chebyshev', 'markov', 'jensen', 'slutsky', 'convergenza',
            
            # Processi e avanzato
            'processo stocastico', 'catena markov', 'stazionario', 'ergodico',
            'martingala', 'browniano', 'wiener', 'levy'
        }
        
        # Pattern per formule matematiche
        self.pattern_formule = [
            r'P\([^)]+\)',  # P(A), P(A|B), etc.
            r'E\[[^\]]+\]',  # E[X], E[X|Y], etc.
            r'Var\([^)]+\)', # Var(X)
            r'Cov\([^)]+\)', # Cov(X,Y)
            r'\b[A-Z]\s*~\s*[A-Za-z]+', # X ~ Normale, etc.
        ]
        
        # Indicatori di sezioni importanti
        self.marcatori_sezione = [
            'definizione', 'teorema', 'proposizione', 'lemma', 'corollario',
            'dimostrazione', 'esempio', 'esercizio', 'applicazione',
            'osservazione', 'nota bene', 'attenzione', 'ricorda'
        ]

    def analizza_file_singolo(self, file_path):
        """Analizza un singolo file di trascrizione e restituisce l'argomento."""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                result = json.load(f)
        except (FileNotFoundError, json.JSONDecodeError):
            return None

        testo_completo = result.get('text', '')
        if not testo_completo:
            return None

        # Analisi del contenuto
        concetti_trovati = self.identifica_concetti_probabilita(testo_completo)
        contenuto_categorizzato = self.categorizza_contenuto(concetti_trovati)
        argomento_principale = self.determina_argomento_principale(contenuto_categorizzato, testo_completo)
        
        return argomento_principale

    def identifica_concetti_probabilita(self, testo):
        """Trova concetti specifici di probabilità nel testo."""
        testo_lower = testo.lower()
        concetti_trovati = Counter()
        
        for concetto in self.concetti_probabilita:
            count = len(re.findall(r'\b' + re.escape(concetto) + r'\b', testo_lower))
            if count > 0:
                concetti_trovati[concetto] = count
                
        return concetti_trovati.most_common(10)

    def categorizza_contenuto(self, concetti_trovati):
        """Organizza i concetti trovati in categorie."""
        categorie = {
            'Concetti Base': ['probabilità', 'evento', 'spazio campionario', 'omega', 'esperimento'],
            'Eventi e Operazioni': ['unione', 'intersezione', 'complementare', 'incompatibili', 'disgiunti', 'partizione'],
            'Probabilità Condizionata': ['condizionata', 'indipendenza', 'bayes', 'totale', 'posteriore', 'priori'],
            'Variabili Aleatorie': ['variabile aleatoria', 'discreta', 'continua', 'distribuzione', 'densità', 'ripartizione'],
            'Distribuzioni': ['bernoulli', 'binomiale', 'poisson', 'normale', 'uniforme', 'gaussiana', 'esponenziale'],
            'Momenti e Statistiche': ['valore atteso', 'media', 'varianza', 'covarianza', 'correlazione', 'deviazione standard'],
            'Teoremi Limite': ['grandi numeri', 'limite centrale', 'chebyshev', 'convergenza'],
            'Processi Stocastici': ['processo stocastico', 'catena markov', 'stazionario', 'martingala', 'browniano']
        }
        
        contenuto_categorizzato = defaultdict(list)
        for concetto, freq in concetti_trovati:
            categorizzato = False
            for categoria, termini in categorie.items():
                if any(termine in concetto for termine in termini):
                    contenuto_categorizzato[categoria].append((concetto, freq))
                    categorizzato = True
                    break
            if not categorizzato:
                contenuto_categorizzato['Altro'].append((concetto, freq))
                
        return dict(contenuto_categorizzato)

    def determina_argomento_principale(self, contenuto_categorizzato, testo_completo):
        """Determina l'argomento principale della lezione basandosi sui concetti più frequenti."""
        if not contenuto_categorizzato:
            return "Argomento non identificato"
        
        # Trova la categoria con più concetti e frequenze più alte
        categoria_principale = ""
        max_peso = 0
        
        for categoria, concetti in contenuto_categorizzato.items():
            if not concetti:
                continue
                
            # Calcola peso: numero di concetti * frequenza totale
            peso = len(concetti) * sum(freq for _, freq in concetti)
            
            if peso > max_peso:
                max_peso = peso
                categoria_principale = categoria
        
        # Crea descrizione dettagliata
        if categoria_principale and categoria_principale in contenuto_categorizzato:
            concetti_principali = contenuto_categorizzato[categoria_principale]
            
            # Prendi i 3 concetti più frequenti
            top_concetti = sorted(concetti_principali, key=lambda x: x[1], reverse=True)[:3]
            nomi_concetti = [concetto.title() for concetto, _ in top_concetti]
            
            if categoria_principale == "Distribuzioni":
                return f"Distribuzioni di Probabilità: {', '.join(nomi_concetti)}"
            elif categoria_principale == "Probabilità Condizionata":
                return f"Probabilità Condizionata e Indipendenza: {', '.join(nomi_concetti)}"
            elif categoria_principale == "Variabili Aleatorie":
                return f"Variabili Aleatorie: {', '.join(nomi_concetti)}"
            elif categoria_principale == "Teoremi Limite":
                return f"Teoremi Limite: {', '.join(nomi_concetti)}"
            elif categoria_principale == "Processi Stocastici":
                return f"Processi Stocastici: {', '.join(nomi_concetti)}"
            else:
                return f"{categoria_principale}: {', '.join(nomi_concetti)}"
        
        return "Contenuti Vari di Probabilità"

    def processa_cartella(self, cartella_path, output_file="argomenti_lezioni.txt"):
        """Processa tutti i file JSON in una cartella e crea il file output."""
        cartella = Path(cartella_path)
        
        if not cartella.exists() or not cartella.is_dir():
            print(f"❌ Errore: La cartella '{cartella_path}' non esiste o non è una directory.")
            return
        
        # Trova tutti i file JSON nella cartella
        file_json = list(cartella.glob("*.json"))
        
        if not file_json:
            print(f"❌ Nessun file JSON trovato nella cartella '{cartella_path}'.")
            return
        
        print(f"📁 Trovati {len(file_json)} file JSON da processare...")
        
        risultati = []
        
        for file_path in sorted(file_json):
            print(f"🔍 Analizzando: {file_path.name}")
            
            argomento = self.analizza_file_singolo(file_path)
            
            if argomento:
                risultati.append(f"{file_path.stem}: {argomento}")
            else:
                risultati.append(f"{file_path.stem}: Errore nel processamento del file")
        
        # Scrivi il file di output
        output_path = Path(output_file)
        try:
            with open(output_path, 'w', encoding='utf-8') as f:
                f.write("ARGOMENTI DELLE LEZIONI DI PROBABILITÀ\n")
                f.write("=" * 50 + "\n\n")
                
                for risultato in risultati:
                    f.write(risultato + "\n")
                
                f.write(f"\n\nTotale lezioni processate: {len(risultati)}")
            
            print(f"\n✅ File creato con successo: {output_path.absolute()}")
            print(f"📊 Processate {len(risultati)} lezioni")
            
        except Exception as e:
            print(f"❌ Errore nella scrittura del file: {e}")

def main():
    print("=" * 60)
    print("     GENERATORE ARGOMENTI LEZIONI DI PROBABILITÀ")
    print("=" * 60 + "\n")

    if len(sys.argv) > 1:
        cartella_path = sys.argv[1]
        output_file = sys.argv[2] if len(sys.argv) > 2 else "argomenti_lezioni.txt"
    else:
        cartella_path = input("📁 Inserisci il percorso della cartella con i file JSON: ").strip()
        output_file = input("📄 Nome del file di output (default: argomenti_lezioni.txt): ").strip()
        if not output_file:
            output_file = "argomenti_lezioni.txt"

    if not cartella_path:
        print("❌ Il percorso della cartella è obbligatorio!")
        return

    reporter = FolderReportGenerator()
    reporter.processa_cartella(cartella_path, output_file)

if __name__ == "__main__":
    main()