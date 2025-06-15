import pandas as pd
import numpy as np

# Leggi il file CSV
df = pd.read_csv('/home/ema/Scrivania/archive/personality_datasert.csv')

# Visualizza informazioni di base sul dataset
print("=== INFORMAZIONI SUL DATASET ===")
print(f"Numero totale di righe: {len(df)}")
print(f"Colonne: {list(df.columns)}")
print("\nPrime righe del dataset:")
print(df.head())

# Conta il numero di estroversi e introversi
print("\n=== DISTRIBUZIONE PERSONALITÀ ===")
personality_counts = df['Personality'].value_counts()
print(personality_counts)

# Separa i dati per tipo di personalità
extroverts = df[df['Personality'] == 'Extrovert']
introverts = df[df['Personality'] == 'Introvert']

print(f"\nNumero di estroversi: {len(extroverts)}")
print(f"Numero di introversi: {len(introverts)}")

# Calcola le medie per le colonne numeriche
numeric_columns = ['Time_spent_Alone', 'Social_event_attendance', 'Going_outside', 
                   'Friends_circle_size', 'Post_frequency']

print("\n=== MEDIE PER ESTROVERSI ===")
extrovert_means = {}
for col in numeric_columns:
    if col in extroverts.columns:
        mean_val = extroverts[col].mean()
        extrovert_means[col] = mean_val
        print(f"{col}: {mean_val:.2f}")

print("\n=== MEDIE PER INTROVERSI ===")
introvert_means = {}
for col in numeric_columns:
    if col in introverts.columns:
        mean_val = introverts[col].mean()
        introvert_means[col] = mean_val
        print(f"{col}: {mean_val:.2f}")

# Calcola le percentuali per le colonne categoriche (Yes/No)
categorical_columns = ['Stage_fear', 'Drained_after_socializing']

print("\n=== PERCENTUALI PER ESTROVERSI ===")
for col in categorical_columns:
    if col in extroverts.columns:
        yes_count = len(extroverts[extroverts[col] == 'Yes'])
        total_count = len(extroverts)
        percentage = (yes_count / total_count) * 100 if total_count > 0 else 0
        print(f"{col} (Yes): {percentage:.1f}% ({yes_count}/{total_count})")

print("\n=== PERCENTUALI PER INTROVERSI ===")
for col in categorical_columns:
    if col in introverts.columns:
        yes_count = len(introverts[introverts[col] == 'Yes'])
        total_count = len(introverts)
        percentage = (yes_count / total_count) * 100 if total_count > 0 else 0
        print(f"{col} (Yes): {percentage:.1f}% ({yes_count}/{total_count})")

# Confronto diretto
print("\n=== CONFRONTO DIRETTO ===")
print(f"{'Caratteristica':<25} {'Estroversi':<12} {'Introversi':<12} {'Differenza':<12}")
print("-" * 65)

for col in numeric_columns:
    if col in extrovert_means and col in introvert_means:
        ext_mean = extrovert_means[col]
        int_mean = introvert_means[col]
        diff = ext_mean - int_mean
        print(f"{col:<25} {ext_mean:<12.2f} {int_mean:<12.2f} {diff:<+12.2f}")
