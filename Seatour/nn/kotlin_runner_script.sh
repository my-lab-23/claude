#!/bin/bash

# Script per eseguire il predittore neurale Kotlin
# Autore: Script generato per neural_predictor.kt
# Versione: 1.0

set -e  # Esce in caso di errore

# Colori per output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Configurazioni
KOTLIN_FILE="neural_predictor.kt"
COMPILED_JAR="neural-predictor.jar"
MAIN_CLASS="Neural_predictorKt"
GSON_VERSION="2.10.1"
GSON_JAR="gson-${GSON_VERSION}.jar"
GSON_URL="https://repo1.maven.org/maven2/com/google/code/gson/gson/${GSON_VERSION}/${GSON_JAR}"

# Funzione per stampare messaggi colorati
print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_header() {
    echo -e "${PURPLE}🚀 $1${NC}"
}

# Funzione per verificare se un comando esiste
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Funzione per scaricare Gson se necessario
download_gson() {
    if [[ ! -f "$GSON_JAR" ]]; then
        print_info "Scaricamento libreria Gson..."
        if command_exists wget; then
            wget -q "$GSON_URL" -O "$GSON_JAR"
        elif command_exists curl; then
            curl -s -L "$GSON_URL" -o "$GSON_JAR"
        else
            print_error "wget o curl necessari per scaricare Gson"
            exit 1
        fi
        print_success "Gson scaricato con successo"
    else
        print_info "Gson già presente"
    fi
}

# Funzione per compilare il file Kotlin
compile_kotlin() {
    print_info "Compilazione di $KOTLIN_FILE..."
    
    if kotlinc "$KOTLIN_FILE" -cp "$GSON_JAR" -include-runtime -d "$COMPILED_JAR"; then
        print_success "Compilazione completata"
    else
        print_error "Errore durante la compilazione"
        exit 1
    fi
}

# Funzione per eseguire il programma
run_program() {
    local json_file="$1"
    local target_date="$2"
    local temperature="$3"
    local direction="$4"
    local epochs="${5:-1000}"
    
    print_info "Esecuzione del predittore..."
    java -cp "$COMPILED_JAR:$GSON_JAR" "$MAIN_CLASS" "$json_file" "$target_date" "$temperature" "$direction" "$epochs"
}

# Funzione per mostrare l'aiuto
show_help() {
    echo "📋 USO:"
    echo "  $0 <file.json> <data> <temperatura> <direzione> [epoche]"
    echo ""
    echo "📝 PARAMETRI:"
    echo "  file.json     - File JSON con dati storici"
    echo "  data          - Data target (YYYY-MM-DD)"
    echo "  temperatura   - Temperatura prevista (°C)"
    echo "  direzione     - ANDATA o RITORNO"
    echo "  epoche        - Numero epoche training (default: 1000)"
    echo ""
    echo "🔧 OPZIONI:"
    echo "  --help, -h    - Mostra questo aiuto"
    echo "  --clean       - Pulisce i file compilati"
    echo "  --recompile   - Forza la ricompilazione"
    echo ""
    echo "📌 ESEMPI:"
    echo "  $0 dati.json 2025-06-10 25 ANDATA"
    echo "  $0 dati.json 2025-06-15 20 RITORNO 1500"
    echo "  $0 --clean"
}

# Funzione per pulire i file compilati
clean_files() {
    print_info "Pulizia file compilati..."
    rm -f "$COMPILED_JAR"
    print_success "File puliti"
}

# Controllo argomenti
case "${1:-}" in
    --help|-h)
        show_help
        exit 0
        ;;
    --clean)
        clean_files
        exit 0
        ;;
    --recompile)
        clean_files
        shift
        ;;
esac

# Verifica argomenti minimi
if [[ $# -lt 4 ]]; then
    print_error "Argomenti insufficienti"
    echo ""
    show_help
    exit 1
fi

# Verifica prerequisiti
print_header "CONTROLLO PREREQUISITI"

if ! command_exists kotlinc; then
    print_error "kotlinc non trovato. Installa Kotlin compiler"
    exit 1
fi
print_success "Kotlin compiler trovato"

if ! command_exists java; then
    print_error "java non trovato. Installa Java Runtime"
    exit 1
fi
print_success "Java Runtime trovato"

if [[ ! -f "$KOTLIN_FILE" ]]; then
    print_error "File $KOTLIN_FILE non trovato nella directory corrente"
    exit 1
fi
print_success "File sorgente trovato"

# Verifica file JSON di input
JSON_FILE="$1"
if [[ ! -f "$JSON_FILE" ]]; then
    print_error "File JSON '$JSON_FILE' non trovato"
    exit 1
fi
print_success "File JSON trovato"

# Validazione data
TARGET_DATE="$2"
if ! date -d "$TARGET_DATE" >/dev/null 2>&1; then
    print_error "Data '$TARGET_DATE' non valida. Usa formato YYYY-MM-DD"
    exit 1
fi
print_success "Data valida"

# Validazione temperatura
TEMPERATURE="$3"
if ! [[ "$TEMPERATURE" =~ ^-?[0-9]+(\.[0-9]+)?$ ]]; then
    print_error "Temperatura '$TEMPERATURE' non valida"
    exit 1
fi
print_success "Temperatura valida"

# Validazione direzione
DIRECTION="${4^^}"  # Converte in maiuscolo
if [[ "$DIRECTION" != "ANDATA" && "$DIRECTION" != "RITORNO" ]]; then
    print_error "Direzione deve essere ANDATA o RITORNO"
    exit 1
fi
print_success "Direzione valida"

# Validazione epoche (opzionale)
EPOCHS="${5:-1000}"
if ! [[ "$EPOCHS" =~ ^[0-9]+$ ]] || [[ "$EPOCHS" -lt 1 ]]; then
    print_error "Numero epoche '$EPOCHS' non valido"
    exit 1
fi
print_success "Parametri validati"

print_header "PREPARAZIONE AMBIENTE"

# Scarica Gson se necessario
download_gson

# Compila se necessario
if [[ ! -f "$COMPILED_JAR" ]] || [[ "$KOTLIN_FILE" -nt "$COMPILED_JAR" ]]; then
    compile_kotlin
else
    print_info "File già compilato e aggiornato"
fi

print_header "ESECUZIONE PREDITTORE"

# Esegui il programma
run_program "$JSON_FILE" "$TARGET_DATE" "$TEMPERATURE" "$DIRECTION" "$EPOCHS"

print_success "Esecuzione completata!"
