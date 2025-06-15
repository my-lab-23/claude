package com.example.caleb

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.caleb.ui.theme.CalebTheme
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Header
import retrofit2.Response
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import com.google.gson.annotations.SerializedName
import com.google.gson.Gson
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Data class per l'appuntamento
data class Appointment(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val location: String? = null,
    val status: String? = null,
    val notes: String? = null,
    val participants: List<String>? = null
)

// Data classes per Groq API
data class GroqMessage(
    val role: String,
    val content: String
)

data class GroqRequest(
    val messages: List<GroqMessage>,
    val model: String = "llama3-8b-8192",
    val temperature: Double = 0.1,
    @SerializedName("max_tokens") val maxTokens: Int = 1000,
    @SerializedName("top_p") val topP: Int = 1,
    val stream: Boolean = false
)

data class GroqChoice(
    val message: GroqMessage
)

data class GroqResponse(
    val choices: List<GroqChoice>
)

// Interface per Groq API
interface GroqService {
    @POST("openai/v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: GroqRequest
    ): Response<GroqResponse>
}

// Interface per il server appuntamenti
interface AppointmentService {
    @POST("appointments")
    suspend fun createAppointment(@Body appointment: Appointment): Response<Any>
}

class ApiKeyManager(private val context: Context) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        "caleb_secure_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun saveApiKey(apiKey: String) {
        encryptedPrefs.edit().putString("groq_api_key", apiKey).apply()
    }
    
    fun getApiKey(): String? {
        return encryptedPrefs.getString("groq_api_key", null)
    }
    
    fun hasApiKey(): Boolean {
        return getApiKey() != null
    }
    
    fun clearApiKey() {
        encryptedPrefs.edit().remove("groq_api_key").apply()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalebTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val apiKeyManager = remember { ApiKeyManager(context) }
    var hasApiKey by remember { mutableStateOf(false) }
    var apiKey by remember { mutableStateOf("") }

    // Controlla se c'è una API key salvata
    LaunchedEffect(Unit) {
        val savedKey = apiKeyManager.getApiKey()
        if (savedKey != null) {
            hasApiKey = true
            apiKey = savedKey
        }
    }

    if (hasApiKey) {
        // Schermata principale con AI
        AppointmentScreen(
            apiKey = apiKey,
            onResetApiKey = {
                apiKeyManager.clearApiKey()
                hasApiKey = false
                apiKey = ""
            },
            modifier = modifier
        )
    } else {
        // Schermata configurazione API Key
        ApiKeyScreen(
            onApiKeySaved = { newApiKey ->
                apiKeyManager.saveApiKey(newApiKey)
                hasApiKey = true
                apiKey = newApiKey
            },
            modifier = modifier
        )
    }
}

@Composable
fun ApiKeyScreen(
    onApiKeySaved: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var apiKeyInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🔑 Configurazione API Key",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Per utilizzare Caleb, inserisci la tua API Key di Groq",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        TextField(
            value = apiKeyInput,
            onValueChange = { 
                apiKeyInput = it
                errorMessage = ""
            },
            label = { Text("Groq API Key") },
            placeholder = { Text("gsk...") },
            modifier = Modifier.fillMaxWidth(),
            supportingText = {
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text("La chiave verrà salvata in modo sicuro sul dispositivo")
                }
            },
            isError = errorMessage.isNotEmpty(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                when {
                    apiKeyInput.isBlank() -> {
                        errorMessage = "Inserisci una API key"
                    }
                    !apiKeyInput.startsWith("gsk") -> {
                        errorMessage = "La API key deve iniziare con 'gsk'"
                    }
                    apiKeyInput.length < 20 -> {
                        errorMessage = "La API key sembra troppo corta"
                    }
                    else -> {
                        onApiKeySaved(apiKeyInput)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("💾 Salva e Continua")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Ottieni la tua API key gratuita su console.groq.com",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AppointmentScreen(
    apiKey: String,
    onResetApiKey: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var promptText by remember { mutableStateOf("") }
    var responseText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Configurazione Retrofit - ALLINEATA ALLA VERSIONE DESKTOP
    val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d("HTTP", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Groq API
    val groqRetrofit = Retrofit.Builder()
        .baseUrl("https://api.groq.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val groqService = groqRetrofit.create(GroqService::class.java)

    // Server appuntamenti
    val appointmentRetrofit = Retrofit.Builder()
        .baseUrl("http://192.168.168.93:8079/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val appointmentService = appointmentRetrofit.create(AppointmentService::class.java)

    // ALLINEATO ALLA VERSIONE DESKTOP: Sistema prompt con data corrente e ID
    fun createSystemPrompt(currentDate: String, appointmentId: String): String {
        return """Sei un assistente che estrae informazioni da testi in linguaggio naturale e le converte in formato JSON per appuntamenti.

DATA E ORA CORRENTE: $currentDate
ID APPUNTAMENTO: $appointmentId

IMPORTANTE: Devi restituire un JSON PERFETTAMENTE VALIDO e COMPLETO, con tutte le parentesi graffe di apertura e chiusura.

Estrai dal testo dell'utente i seguenti campi e restituisci SOLO un JSON valido:
{
    "id": "$appointmentId",
    "title": "nome della persona",
    "description": "descrizione dettagliata",
    "startTime": "YYYY-MM-DDTHH:MM:SS",
    "endTime": "YYYY-MM-DDTHH:MM:SS",
    "location": "luogo",
    "status": "CONFIRMED o UNCONFIRMED",
    "notes": "prompt originale"
}

Regole per i campi:
- id: usa esattamente questo ID: $appointmentId
- title: il nome proprio della persona che ha richiesto l'appuntamento
- description: descrizione dettagliata basata sul contesto
- startTime: IMPORTANTE - se non è specificata una data nel testo, usa la data corrente
- endTime: data e ora fine (formato ISO: YYYY-MM-DDTHH:MM:SS, aggiungi durata ragionevole)
- location: luogo dell'appuntamento (se non specificato, usa "Da definire")
- status: usa "UNCONFIRMED" se non è specificata una data nel testo, altrimenti usa "CONFIRMED"
- notes: qui scrivi il prompt originale per intero

Regole per le date (USA LA DATA CORRENTE FORNITA SOPRA):
- Se non specificato il giorno, usa la data corrente
- Se non specificata l'ora, usa orari lavorativi (9:00-18:00)
- Se non specificata la durata, stima in base al tipo di riunione
- Calcola sempre le date relative alla DATA CORRENTE: $currentDate

IMPORTANTE: Assicurati che il JSON sia completo e valido, con tutte le parentesi graffe di apertura e chiusura.

Rispondi ESCLUSIVAMENTE con il JSON valido, senza markdown, backticks o altre spiegazioni."""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "🚀 Caleb - AI Appointments",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth()
        )

        TextField(
            value = promptText,
            onValueChange = { promptText = it },
            label = { Text("Descrivi il tuo appuntamento") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            placeholder = { Text("Es: Riunione con Mario domani alle 15 in ufficio") }
        )

        Button(
            onClick = {
                if (promptText.isBlank()) {
                    responseText = "⚠️ Inserisci la descrizione dell'appuntamento"
                    return@Button
                }
                
                scope.launch {
                    isLoading = true
                    try {
                        responseText = "🔄 Elaborazione con AI in corso..."
                        Log.d("AppDebug", "Inizio elaborazione appuntamento")
                        
                        // Genera ID univoco per l'appuntamento
                        val appointmentId = "APP-${(100..999).random()}-${(100..999).random()}-${(100..999).random()}-${(1..9).random()}"
                        
                        // Crea system prompt con data corrente e ID
                        val currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        val systemPrompt = createSystemPrompt(currentDate, appointmentId)
                        
                        // Prepara richiesta Groq
                        val groqRequest = GroqRequest(
                            messages = listOf(
                                GroqMessage("system", systemPrompt),
                                GroqMessage("user", promptText)
                            )
                        )
                        
                        // Chiamata Groq API
                        val groqResponse = groqService.chatCompletion(
                            authorization = "Bearer $apiKey",
                            request = groqRequest
                        )
                        
                        if (!groqResponse.isSuccessful) {
                            val errorBody = groqResponse.errorBody()?.string()
                            responseText = "❌ Errore Groq API!\nCodice: ${groqResponse.code()}\nErrore: $errorBody"
                            Log.e("AppDebug", "Errore Groq: $errorBody")
                            return@launch
                        }
                        
                        val aiContent = groqResponse.body()?.choices?.firstOrNull()?.message?.content
                        if (aiContent.isNullOrBlank()) {
                            responseText = "❌ Risposta AI vuota"
                            return@launch
                        }
                        
                        Log.d("AppDebug", "Risposta AI: $aiContent")
                        
                        // Pulisci e parsa JSON
                        val cleanedJson = aiContent.trim()
                            .removePrefix("```json")
                            .removePrefix("```")
                            .removeSuffix("```")
                            .trim()
                        
                        Log.d("AppDebug", "JSON pulito: $cleanedJson")
                        
                        // Converte JSON in oggetto Appointment
                        val appointment = try {
                            Gson().fromJson(cleanedJson, Appointment::class.java)
                        } catch (e: Exception) {
                            responseText = "❌ Errore parsing JSON: ${e.message}\n\nJSON ricevuto:\n$cleanedJson"
                            Log.e("AppDebug", "Errore parsing: ", e)
                            return@launch
                        }
                        
                        Log.d("AppDebug", "Appuntamento creato: $appointment")
                        
                        // Invia al server
                        val serverResponse = appointmentService.createAppointment(appointment)
                        
                        if (serverResponse.isSuccessful) {
                            responseText = """
                                ✅ SUCCESSO COMPLETO!
                                
                                📅 Appuntamento creato:
                                • ID: ${appointment.id}
                                • Titolo: ${appointment.title}
                                • Quando: ${appointment.startTime}
                                • Dove: ${appointment.location}
                                • Stato: ${appointment.status}
                                • Partecipanti: ${appointment.participants?.size ?: 0}
                                
                                🚀 Salvato sul server!
                                Codice: ${serverResponse.code()}
                                
                                💾 Data elaborazione: $currentDate
                            """.trimIndent()
                            
                            Log.d("AppDebug", "Successo completo!")
                        } else {
                            val errorBody = serverResponse.errorBody()?.string()
                            responseText = """
                                ⚠️ AI OK, ma errore server:
                                
                                📅 Appuntamento generato:
                                ${appointment.title}
                                • Stato: ${appointment.status}
                                
                                ❌ Errore server:
                                Codice: ${serverResponse.code()}
                                Errore: $errorBody
                            """.trimIndent()
                            
                            Log.e("AppDebug", "Errore server: $errorBody")
                        }
                        
                    } catch (e: Exception) {
                        responseText = """
                            🔥 Errore generale!
                            Tipo: ${e.javaClass.simpleName}
                            Messaggio: ${e.message ?: "Errore sconosciuto"}
                            
                            Stack trace:
                            ${e.stackTraceToString()}
                        """.trimIndent()
                        
                        Log.e("AppDebug", "Eccezione:", e)
                        e.printStackTrace()
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isLoading) "Elaborazione..." else "🚀 Crea Appuntamento con AI")
        }

        if (responseText.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = responseText,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}