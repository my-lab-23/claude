import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

# Leggi il file CSV
file_path = "/home/ema/Scrivania/archive/personality_datasert.csv"
df = pd.read_csv(file_path)

# Pulisci i dati: converti valori strani in NaN e poi rimuovili
df['Friends_circle_size'] = pd.to_numeric(df['Friends_circle_size'], errors='coerce')
df_clean = df.dropna(subset=['Friends_circle_size'])

# Arrotonda i valori decimali a numeri interi (potrebbero essere errori di lettura)
df_clean['Friends_circle_size'] = df_clean['Friends_circle_size'].round().astype(int)

# Conta i livelli di Friends_circle_size
friends_counts = df_clean['Friends_circle_size'].value_counts().sort_index()

# Calcola il totale
total = len(df_clean)
total_original = len(df)

# Stampa i risultati
print(f"Totale persone nel dataset originale: {total_original}")
print(f"Persone con dati validi per Friends_circle_size: {total}")
if total_original != total:
    print(f"Righe scartate per dati invalidi: {total_original - total}")

print("\nDistribuzione Friends_circle_size:")
print("-" * 40)

total_percentage = 0
for size, count in friends_counts.items():
    percentage = (count / total) * 100
    total_percentage += percentage
    print(f"Dimensione {size}: {count} persone ({percentage:.1f}%)")

print("-" * 40)
print(f"Verifica totale: {total_percentage:.1f}%")

# Crea il grafico
plt.figure(figsize=(10, 6))

# Istogramma dei dati
plt.hist(df_clean['Friends_circle_size'], bins=range(int(df_clean['Friends_circle_size'].min()), 
                                                   int(df_clean['Friends_circle_size'].max()) + 2), 
         density=True, alpha=0.7, color='skyblue', edgecolor='black', label='Dati osservati')

# Curva gaussiana di best fit
mu = df_clean['Friends_circle_size'].mean()
sigma = df_clean['Friends_circle_size'].std()
x = np.linspace(df_clean['Friends_circle_size'].min(), df_clean['Friends_circle_size'].max(), 100)
y = (1/(sigma * np.sqrt(2 * np.pi))) * np.exp(-0.5 * ((x - mu) / sigma) ** 2)

plt.plot(x, y, 'r-', linewidth=2, label=f'Curva gaussiana (μ={mu:.1f}, σ={sigma:.1f})')

# Personalizzazione del grafico
plt.xlabel('Dimensione cerchia amici')
plt.ylabel('Densità')
plt.title('Distribuzione Friends_circle_size con curva gaussiana')
plt.legend()
plt.grid(True, alpha=0.3)

# Mostra statistiche sul grafico
plt.text(0.02, 0.98, f'Media: {mu:.1f}\nDeviazione std: {sigma:.1f}\nCampioni: {total}', 
         transform=plt.gca().transAxes, verticalalignment='top', 
         bbox=dict(boxstyle='round', facecolor='white', alpha=0.8))

plt.tight_layout()
plt.show()
