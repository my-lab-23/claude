import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

# Leggi il file CSV
file_path = "/home/ema/Scrivania/archive/personality_datasert.csv"
df = pd.read_csv(file_path)

# Pulisci i dati
df['Friends_circle_size'] = pd.to_numeric(df['Friends_circle_size'], errors='coerce')
df_clean = df.dropna(subset=['Friends_circle_size', 'Personality'])
df_clean['Friends_circle_size'] = df_clean['Friends_circle_size'].round().astype(int)

# Separa introversi ed estroversi
introverts = df_clean[df_clean['Personality'] == 'Introvert']['Friends_circle_size']
extroverts = df_clean[df_clean['Personality'] == 'Extrovert']['Friends_circle_size']

# Statistiche
print("=" * 50)
print("ANALISI FRIENDS_CIRCLE_SIZE PER PERSONALITÀ")
print("=" * 50)

print(f"Totale persone analizzate: {len(df_clean)}")
print(f"Introversi: {len(introverts)} persone")
print(f"Estroversi: {len(extroverts)} persone")

print("\nSTATISTICHE INTROVERSI:")
print(f"Media: {introverts.mean():.1f}")
print(f"Deviazione standard: {introverts.std():.1f}")
print(f"Mediana: {introverts.median():.1f}")
print(f"Min: {introverts.min()}, Max: {introverts.max()}")

print("\nSTATISTICHE ESTROVERSI:")
print(f"Media: {extroverts.mean():.1f}")
print(f"Deviazione standard: {extroverts.std():.1f}")
print(f"Mediana: {extroverts.median():.1f}")
print(f"Min: {extroverts.min()}, Max: {extroverts.max()}")

# Crea il grafico
plt.figure(figsize=(12, 8))

# Range comune per i bins
min_val = min(introverts.min(), extroverts.min())
max_val = max(introverts.max(), extroverts.max())
bins = range(int(min_val), int(max_val) + 2)

# Istogrammi sovrapposti
plt.hist(introverts, bins=bins, density=True, alpha=0.6, color='blue', 
         edgecolor='black', label=f'Introversi (n={len(introverts)})')
plt.hist(extroverts, bins=bins, density=True, alpha=0.6, color='red', 
         edgecolor='black', label=f'Estroversi (n={len(extroverts)})')

# Curve gaussiane
x = np.linspace(min_val, max_val, 100)

# Curva per introversi
if len(introverts) > 1:
    mu_i = introverts.mean()
    sigma_i = introverts.std()
    y_i = (1/(sigma_i * np.sqrt(2 * np.pi))) * np.exp(-0.5 * ((x - mu_i) / sigma_i) ** 2)
    plt.plot(x, y_i, 'b--', linewidth=2, label=f'Gaussiana Introversi (μ={mu_i:.1f}, σ={sigma_i:.1f})')

# Curva per estroversi
if len(extroverts) > 1:
    mu_e = extroverts.mean()
    sigma_e = extroverts.std()
    y_e = (1/(sigma_e * np.sqrt(2 * np.pi))) * np.exp(-0.5 * ((x - mu_e) / sigma_e) ** 2)
    plt.plot(x, y_e, 'r--', linewidth=2, label=f'Gaussiana Estroversi (μ={mu_e:.1f}, σ={sigma_e:.1f})')

# Personalizzazione del grafico
plt.xlabel('Dimensione cerchia amici')
plt.ylabel('Densità')
plt.title('Distribuzione Friends_circle_size: Introversi vs Estroversi')
plt.legend()
plt.grid(True, alpha=0.3)

# Linee verticali per le medie
if len(introverts) > 0:
    plt.axvline(introverts.mean(), color='blue', linestyle=':', alpha=0.8, label=f'Media Introversi: {introverts.mean():.1f}')
if len(extroverts) > 0:
    plt.axvline(extroverts.mean(), color='red', linestyle=':', alpha=0.8, label=f'Media Estroversi: {extroverts.mean():.1f}')

plt.tight_layout()
plt.show()

# Test statistico (se ci sono abbastanza dati)
if len(introverts) > 10 and len(extroverts) > 10:
    from scipy import stats
    t_stat, p_value = stats.ttest_ind(introverts, extroverts)
    print(f"\nTEST T-STUDENT:")
    print(f"T-statistic: {t_stat:.3f}")
    print(f"P-value: {p_value:.3f}")
    if p_value < 0.05:
        print("Differenza statisticamente significativa (p < 0.05)")
    else:
        print("Differenza NON statisticamente significativa (p >= 0.05)")
else:
    print("\nDati insufficienti per il test statistico (serve scipy: pip install scipy)")