import pandas as pd

# Leggi il file CSV
file_path = "/home/ema/Scrivania/archive/personality_datasert.csv"
df = pd.read_csv(file_path)

# Conta i tipi di personalità
personality_counts = df['Personality'].value_counts()

# Calcola il totale
total = len(df)

# Calcola le percentuali
introvert_count = personality_counts.get('Introvert', 0)
extrovert_count = personality_counts.get('Extrovert', 0)

introvert_percentage = (introvert_count / total) * 100
extrovert_percentage = (extrovert_count / total) * 100

# Analisi persone con zero amici
zero_friends = df[df['Friends_circle_size'] == 0.0]

# Conta introversi ed estroversi con zero amici
introvert_zero_friends = len(zero_friends[zero_friends['Personality'] == 'Introvert'])
extrovert_zero_friends = len(zero_friends[zero_friends['Personality'] == 'Extrovert'])

# Calcola le percentuali tra introversi ed estroversi con zero amici
total_zero_friends = introvert_zero_friends + extrovert_zero_friends
introvert_zero_percentage = (introvert_zero_friends / total_zero_friends * 100) if total_zero_friends > 0 else 0
extrovert_zero_percentage = (extrovert_zero_friends / total_zero_friends * 100) if total_zero_friends > 0 else 0

# Stampa i risultati
print(f"Totale persone analizzate: {total}")
print(f"Introversi: {introvert_count} ({introvert_percentage:.1f}%)")
print(f"Estroversi: {extrovert_count} ({extrovert_percentage:.1f}%)")
print(f"Verifica: {introvert_percentage + extrovert_percentage:.1f}%")
print()
print("ANALISI PERSONE CON ZERO AMICI:")
print(f"Totale persone con 0 amici: {total_zero_friends}")
print(f"Introversi con 0 amici: {introvert_zero_friends} ({introvert_zero_percentage:.1f}%)")
print(f"Estroversi con 0 amici: {extrovert_zero_friends} ({extrovert_zero_percentage:.1f}%)")
print(f"Verifica: {introvert_zero_percentage + extrovert_zero_percentage:.1f}%")