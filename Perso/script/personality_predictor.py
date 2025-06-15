import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.svm import SVC
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix
from sklearn.preprocessing import LabelEncoder
import joblib
import os

# Aggiunta per ONNX
try:
    import skl2onnx
    from skl2onnx import convert_sklearn
    from skl2onnx.common.data_types import FloatTensorType
    import onnxruntime as ort
    ONNX_AVAILABLE = True
except ImportError:
    ONNX_AVAILABLE = False
    print("[ATTENZIONE] skl2onnx o onnxruntime non installati. L'esportazione ONNX non sarà disponibile.")

def carica_dati_da_file():
    """Carica i dati da un file CSV"""
    while True:
        try:
            percorso_file = input("Inserisci il percorso del file CSV: ")
            
            # Verifica se il file esiste
            if not os.path.exists(percorso_file):
                print(f"   Errore: Il file '{percorso_file}' non esiste.")
                continue
            
            # Carica il CSV
            df = pd.read_csv(percorso_file)
            print(f"Dataset caricato con successo: {df.shape[0]} righe, {df.shape[1]} colonne")
            
            # Mostra le prime righe per verifica
            print("\nPrime 3 righe del dataset:")
            print(df.head(3))
            
            # Verifica che abbia la colonna 'Personality'
            if 'Personality' not in df.columns:
                print("   Attenzione: Il file deve contenere una colonna 'Personality'")
                print("   Colonne trovate:", list(df.columns))
                continue
            
            return df
            
        except pd.errors.EmptyDataError:
            print("   Errore: Il file CSV è vuoto.")
        except pd.errors.ParserError as e:
            print(f"   Errore nel parsing del CSV: {e}")
        except Exception as e:
            print(f"   Errore nel caricamento del file: {e}")
        
        # Chiedi se vuole riprovare
        riprova = input("Vuoi riprovare con un altro file? (Sì/No): ")
        if riprova.lower() not in ['si', 'sì', 's', 'yes', 'y']:
            return None

def preprocessa_dati(df):
    """Preprocessa i dati per il machine learning"""
    # Copia del dataframe
    df_processed = df.copy()
    
    # Converti le colonne Yes/No in 1/0
    yes_no_columns = ['Stage_fear', 'Drained_after_socializing']
    le = LabelEncoder()
    
    for col in yes_no_columns:
        if col in df_processed.columns:
            # Gestisci diversi formati di Yes/No
            df_processed[col] = df_processed[col].astype(str).str.lower()
            df_processed[col] = df_processed[col].map({
                'yes': 1, 'no': 0, 'sì': 1, 'si': 1,
                'true': 1, 'false': 0, '1': 1, '0': 0
            })
            # Se ci sono valori non mappati, usa LabelEncoder
            if df_processed[col].isnull().any():
                df_processed[col] = le.fit_transform(df_processed[col].fillna('no'))
    
    # Separa features e target
    X = df_processed.drop('Personality', axis=1)
    y = df_processed['Personality']
    
    print("Caratteristiche utilizzate:", list(X.columns))
    print("Classi target:", y.unique())
    print("Distribuzione classi:")
    print(y.value_counts())
    
    return X, y

def addestra_modelli(X, y):
    """Addestra diversi modelli di machine learning"""
    # Dividi i dati in training e test
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)
    
    # Definisci i modelli
    modelli = {
        'Random Forest': RandomForestClassifier(n_estimators=100, random_state=42),
        'Logistic Regression': LogisticRegression(random_state=42, max_iter=1000),
        'SVM': SVC(random_state=42, probability=True)
    }
    
    risultati = {}
    
    print("\n=== ADDESTRAMENTO MODELLI ===")
    for nome, modello in modelli.items():
        print(f"\nAddestrando {nome}...")
        
        # Addestra il modello
        modello.fit(X_train, y_train)
        
        # Fai previsioni
        y_pred = modello.predict(X_test)
        
        # Calcola l'accuratezza
        accuratezza = accuracy_score(y_test, y_pred)
        
        risultati[nome] = {
            'modello': modello,
            'accuratezza': accuratezza,
            'y_test': y_test,
            'y_pred': y_pred
        }
        
        print(f"Accuratezza: {accuratezza:.4f}")
        
        # Mostra il classification report
        print("Classification Report:")
        print(classification_report(y_test, y_pred))
    
    # Trova il modello migliore
    miglior_modello_nome = max(risultati.keys(), key=lambda x: risultati[x]['accuratezza'])
    miglior_modello = risultati[miglior_modello_nome]['modello']
    
    print(f"\n🏆 Miglior modello: {miglior_modello_nome} (Accuratezza: {risultati[miglior_modello_nome]['accuratezza']:.4f})")
    
    return miglior_modello, risultati, X_train, X_test, y_train, y_test

def esporta_modello_onnx(modello, X, nome_file="modello_personalita.onnx"):
    """Esporta il modello in formato ONNX con opzioni ottimizzate per Kotlin"""
    if not ONNX_AVAILABLE:
        print("[ERRORE] Librerie ONNX non disponibili")
        return False, None  # CORREZIONE: Restituisce sempre una tupla
    
    try:
        print(f"\n📦 Esportazione modello ONNX...")
        
        # Determina il numero di feature
        n_features = X.shape[1]
        initial_type = [("float_input", FloatTensorType([None, n_features]))]
        
        # Opzioni per l'esportazione ONNX - CRUCIALI per compatibilità con Kotlin
        options = {
            id(modello): {
                "zipmap": False,  # Disabilita ZipMap per avere array raw di probabilità
                "output_class_labels": True,  # Mantieni le etichette delle classi
                "nocl": False  # Includi le classi nell'output
            }
        }
        
        # Converti il modello
        onnx_model = convert_sklearn(
            modello, 
            initial_types=initial_type,
            options=options,
            target_opset={'': 15, 'ai.onnx.ml': 2}  # Versioni compatibili
        )
        
        # Salva il file
        with open(nome_file, "wb") as f:
            f.write(onnx_model.SerializeToString())
        
        print(f"✅ Modello esportato correttamente: {nome_file}")
        
        # Mostra informazioni sul modello
        print("Informazioni modello ONNX:")
        print(f"  - Input: {[inp.name for inp in onnx_model.graph.input]}")
        print(f"  - Output: {[out.name for out in onnx_model.graph.output]}")
        
        # Ottieni le classi del modello
        classi_modello = modello.classes_
        print(f"  - Classi: {list(classi_modello)}")
        print(f"  - Numero classi: {len(classi_modello)}")
        
        return True, classi_modello
        
    except Exception as e:
        print(f"[ERRORE ONNX] Impossibile esportare il modello: {e}")
        import traceback
        traceback.print_exc()
        return False, None

def testa_modello_onnx(nome_file="modello_personalita.onnx", classi_modello=None):
    """Testa il modello ONNX per verificare l'output"""
    if not ONNX_AVAILABLE:
        print("[ERRORE] ONNX Runtime non disponibile")
        return False
    
    try:
        print(f"\n🧪 Test modello ONNX: {nome_file}")
        
        # Carica il modello ONNX
        session = ort.InferenceSession(nome_file)
        
        # Mostra input e output
        print(f"Input modello: {[inp.name for inp in session.get_inputs()]}")
        print(f"Output modello: {[out.name for out in session.get_outputs()]}")
        
        # Test con dati fittizi
        test_input = np.array([[5.0, 1.0, 3.0, 2.0, 1.0, 4.0, 2.0]], dtype=np.float32)
        
        # Esegui predizione
        input_name = session.get_inputs()[0].name
        result = session.run(None, {input_name: test_input})
        
        print(f"\n📊 Test con input: {test_input[0]}")
        print(f"Numero di output: {len(result)}")
        
        for i, output in enumerate(result):
            output_name = session.get_outputs()[i].name
            print(f"Output {i} ({output_name}):")
            print(f"  - Tipo: {type(output)}")
            print(f"  - Shape: {output.shape if hasattr(output, 'shape') else 'N/A'}")
            print(f"  - Contenuto: {output}")
            
            if output_name == "output_label" and len(output) > 0:
                predicted_class = output[0] if isinstance(output[0], str) else str(output[0])
                print(f"  - Classe predetta: {predicted_class}")
            
            elif output_name == "output_probability" and len(output) > 0:
                probs = output[0] if len(output.shape) > 1 else output
                print(f"  - Probabilità: {probs}")
                
                if classi_modello is not None:
                    print("  - Mappatura probabilità-classi:")
                    for j, (classe, prob) in enumerate(zip(classi_modello, probs)):
                        print(f"    {classe}: {prob:.4f} ({prob*100:.2f}%)")
        
        return True
        
    except Exception as e:
        print(f"[ERRORE TEST ONNX] {e}")
        import traceback
        traceback.print_exc()
        return False

def confronta_predizioni_sklearn_onnx(modello_sklearn, nome_file_onnx="modello_personalita.onnx"):
    """Confronta le predizioni tra il modello sklearn e ONNX"""
    if not ONNX_AVAILABLE:
        print("[ERRORE] ONNX Runtime non disponibile per il confronto")
        return
    
    try:
        print(f"\n🔍 Confronto predizioni sklearn vs ONNX")
        
        # Dati di test
        test_data = np.array([[5.0, 1.0, 3.0, 2.0, 1.0, 4.0, 2.0]], dtype=np.float32)
        
        # Predizione sklearn
        pred_sklearn = modello_sklearn.predict(test_data)[0]
        prob_sklearn = modello_sklearn.predict_proba(test_data)[0]
        classi_sklearn = modello_sklearn.classes_
        
        print(f"📈 Sklearn:")
        print(f"  - Classe predetta: {pred_sklearn}")
        print(f"  - Probabilità: {prob_sklearn}")
        
        # Predizione ONNX
        session = ort.InferenceSession(nome_file_onnx)
        input_name = session.get_inputs()[0].name
        result_onnx = session.run(None, {input_name: test_data})
        
        # Estrai risultati ONNX
        label_onnx = result_onnx[0][0] if len(result_onnx) > 0 else "N/A"
        prob_onnx = result_onnx[1][0] if len(result_onnx) > 1 else []
        
        print(f"📊 ONNX:")
        print(f"  - Classe predetta: {label_onnx}")
        print(f"  - Probabilità: {prob_onnx}")
        
        # Confronto
        classi_match = str(pred_sklearn) == str(label_onnx)
        prob_match = np.allclose(prob_sklearn, prob_onnx, rtol=1e-4) if len(prob_onnx) > 0 else False
        
        print(f"\n✅ Risultati confronto:")
        print(f"  - Classi corrispondenti: {classi_match}")
        print(f"  - Probabilità corrispondenti: {prob_match}")
        
        if not classi_match or not prob_match:
            print("⚠️  Le predizioni non corrispondono perfettamente!")
            
        return classi_match and prob_match
        
    except Exception as e:
        print(f"[ERRORE CONFRONTO] {e}")
        return False

def converti_risposta_questionario(risposte):
    """Converte le risposte del questionario in formato per il modello"""
    # Mappa le chiavi del questionario alle colonne del dataset
    mapping = {
        'Ore_trascorse_da_solo': 'Time_spent_Alone',
        'Paura_del_palcoscenico': 'Stage_fear',
        'Partecipazione_eventi_sociali': 'Social_event_attendance',
        'Uscite_settimanali': 'Going_outside',
        'Stanchezza_dopo_socializzazione': 'Drained_after_socializing',
        'Numero_amici_stretti': 'Friends_circle_size',
        'Frequenza_post_social': 'Post_frequency'
    }
    
    dati_convertiti = {}
    
    for chiave_questionario, chiave_dataset in mapping.items():
        if chiave_questionario in risposte:
            valore = risposte[chiave_questionario]
            
            # Converti Sì/No in 1/0
            if valore == 'Sì':
                dati_convertiti[chiave_dataset] = 1
            elif valore == 'No':
                dati_convertiti[chiave_dataset] = 0
            else:
                dati_convertiti[chiave_dataset] = valore
    
    return dati_convertiti

def prevedi_personalita(modello, dati_utente):
    """Fai una previsione sulla personalità dell'utente"""
    # Converti in DataFrame
    df_utente = pd.DataFrame([dati_utente])
    
    # Fai la previsione
    previsione = modello.predict(df_utente)[0]
    probabilita = modello.predict_proba(df_utente)[0]
    
    # Ottieni le classi
    classi = modello.classes_
    
    print(f"\n🔮 PREVISIONE PERSONALITÀ:")
    print(f"Tipo di personalità previsto: {previsione}")
    print(f"\nProbabilità:")
    for classe, prob in zip(classi, probabilita):
        print(f"  {classe}: {prob:.4f} ({prob*100:.2f}%)")
    
    return previsione, probabilita

def questionario_e_previsione():
    """Funzione integrata questionario + previsione"""
    try:
        from personality_questionnaire import questionario_personalita
        
        print("=== QUESTIONARIO PERSONALITÀ CON PREVISIONE AI ===\n")
        
        # Raccogli i dati tramite questionario
        risposte = questionario_personalita()
        
        # Converti per il modello
        dati_modello = converti_risposta_questionario(risposte)
        
        return dati_modello
    except ImportError:
        print("[ERRORE] Impossibile importare personality_questionnaire.py")
        return None

def input_dati_manuali():
    """Permette l'inserimento manuale dei dati"""
    print("\n📝 Inserimento dati manuali:")
    dati_manuali = {}
    
    campi = {
        'Time_spent_Alone': 'Ore trascorse da solo (0-24)',
        'Stage_fear': 'Paura del palcoscenico (0=No, 1=Sì)',
        'Social_event_attendance': 'Partecipazione eventi sociali (scala 1-10)',
        'Going_outside': 'Uscite settimanali (numero)',
        'Drained_after_socializing': 'Stanchezza dopo socializzazione (0=No, 1=Sì)',
        'Friends_circle_size': 'Numero amici stretti',
        'Post_frequency': 'Frequenza post social (per settimana)'
    }
    
    for col, descrizione in campi.items():
        while True:
            try:
                if col in ['Stage_fear', 'Drained_after_socializing']:
                    val = input(f"{descrizione}: ")
                    if val in ['0', '1']:
                        dati_manuali[col] = int(val)
                        break
                    else:
                        print("Inserisci 0 o 1")
                else:
                    val = float(input(f"{descrizione}: "))
                    dati_manuali[col] = val
                    break
            except ValueError:
                print("Inserisci un numero valido")
    
    return dati_manuali

def salva_info_modello(modello, classi_modello, nome_file="info_modello.txt"):
    """Salva informazioni sul modello per riferimento"""
    try:
        with open(nome_file, 'w', encoding='utf-8') as f:
            f.write("=== INFORMAZIONI MODELLO ===\n")
            f.write(f"Tipo modello: {type(modello).__name__}\n")
            f.write(f"Classi: {list(classi_modello)}\n")
            f.write(f"Numero classi: {len(classi_modello)}\n")
            f.write("\n=== CODICE KOTLIN ===\n")
            f.write("// Aggiorna questa lista nel codice Kotlin:\n")
            f.write(f"private var classLabels: List<String> = listOf(\n")
            for i, classe in enumerate(classi_modello):
                virgola = "," if i < len(classi_modello) - 1 else ""
                f.write(f'    "{classe}"{virgola}\n')
            f.write(")\n")
        
        print(f"💾 Informazioni modello salvate in: {nome_file}")
        
    except Exception as e:
        print(f"[ERRORE] Impossibile salvare info modello: {e}")

def main():
    """Funzione principale"""
    print("🧠 SISTEMA DI PREVISIONE PERSONALITÀ")
    print("="*60)
    
    # Carica i dati dal file CSV
    print("Prima di iniziare, devo caricare il dataset di training.")
    df = carica_dati_da_file()
    if df is None:
        print("Impossibile proseguire senza un dataset valido.")
        return
    
    # Preprocessa i dati
    X, y = preprocessa_dati(df)
    
    # Addestra i modelli
    miglior_modello, risultati, X_train, X_test, y_train, y_test = addestra_modelli(X, y)
    
    # Salva il modello
    joblib.dump(miglior_modello, 'modello_personalita.pkl')
    print(f"\n💾 Modello salvato come 'modello_personalita.pkl'")

    # Esporta in ONNX - CORREZIONE: Ora gestisce correttamente la tupla restituita
    onnx_success, classi_modello = esporta_modello_onnx(miglior_modello, X)
    
    if onnx_success and classi_modello is not None:
        # Testa il modello ONNX
        if testa_modello_onnx("modello_personalita.onnx", classi_modello):
            # Confronta predizioni
            confronta_predizioni_sklearn_onnx(miglior_modello)
        
        # Salva informazioni per Kotlin
        salva_info_modello(miglior_modello, classi_modello)
        
        print(f"\n📋 IMPORTANTE PER KOTLIN:")
        print(f"Aggiorna la lista classLabels con: {list(classi_modello)}")
    else:
        # Anche se ONNX non è disponibile, salva comunque le info base del modello
        print("\n📋 ONNX non disponibile, ma salvo le informazioni del modello...")
        salva_info_modello(miglior_modello, miglior_modello.classes_)
    
    # Menu interattivo
    while True:
        print("\n" + "="*60)
        print("OPZIONI:")
        print("1. Compila questionario e ottieni previsione")
        print("2. Inserisci dati manualmente")
        print("3. Test modello ONNX")
        print("4. Confronta predizioni sklearn vs ONNX")
        print("5. Esci")
        
        scelta = input("\nScegli un'opzione (1-5): ")
        
        if scelta == '1':
            dati_utente = questionario_e_previsione()
            if dati_utente:
                prevedi_personalita(miglior_modello, dati_utente)
        
        elif scelta == '2':
            dati_manuali = input_dati_manuali()
            prevedi_personalita(miglior_modello, dati_manuali)
        
        elif scelta == '3':
            if onnx_success:
                testa_modello_onnx("modello_personalita.onnx", classi_modello)
            else:
                print("Modello ONNX non disponibile")
        
        elif scelta == '4':
            if onnx_success:
                confronta_predizioni_sklearn_onnx(miglior_modello)
            else:
                print("Modello ONNX non disponibile")
        
        elif scelta == '5':
            print("👋 Arrivederci!")
            break
        
        else:
            print("❌ Scelta non valida!")

if __name__ == "__main__":
    main()