package org.example.caleb

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import javafx.application.Application
import javafx.concurrent.Task
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.stage.Stage
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.jasypt.util.text.BasicTextEncryptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

// Data classes
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

// API Interfaces
interface GroqService {
    @POST("openai/v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: GroqRequest
    ): Response<GroqResponse>
}

interface AppointmentService {
    @POST("appointments")
    suspend fun createAppointment(@Body appointment: Appointment): Response<Any>
}

// API Key Manager per Linux
class ApiKeyManager {
    private val configDir = File(System.getProperty("user.home"), ".caleb")
    private val keyFile = File(configDir, "api.key")
    private val encryptor = BasicTextEncryptor().apply {
        setPassword("caleb-linux-${System.getProperty("user.name")}")
    }

    init {
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
    }

    fun saveApiKey(apiKey: String) {
        try {
            val encrypted = encryptor.encrypt(apiKey)
            keyFile.writeText(encrypted)
        } catch (e: Exception) {
            println("Errore salvando API key: ${e.message}")
        }
    }

    fun getApiKey(): String? {
        return try {
            if (keyFile.exists()) {
                val encrypted = keyFile.readText()
                encryptor.decrypt(encrypted)
            } else null
        } catch (e: Exception) {
            println("Errore leggendo API key: ${e.message}")
            null
        }
    }

    fun hasApiKey(): Boolean = getApiKey() != null

    fun clearApiKey() {
        if (keyFile.exists()) {
            keyFile.delete()
        }
    }
}

// Main Application
class CalebApp : Application() {
    private val apiKeyManager = ApiKeyManager()
    private lateinit var primaryStage: Stage

    override fun start(primaryStage: Stage) {
        this.primaryStage = primaryStage
        primaryStage.title = "🚀 Caleb - AI Appointments (Linux)"

        // Controlla se esiste API key
        if (apiKeyManager.hasApiKey()) {
            showMainScreen()
        } else {
            showApiKeyScreen()
        }

        primaryStage.show()
    }

    private fun showApiKeyScreen() {
        val root = VBox(20.0).apply {
            padding = Insets(30.0)
            alignment = Pos.CENTER
            style = "-fx-background-color: linear-gradient(to bottom, #667eea 0%, #764ba2 100%);"
        }

        // Titolo
        val title = Label("🔑 Configurazione API Key").apply {
            style = "-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;"
        }

        val subtitle = Label("Per utilizzare Caleb, inserisci la tua API Key di Groq").apply {
            style = "-fx-font-size: 14px; -fx-text-fill: #e0e0e0; -fx-text-alignment: center;"
            isWrapText = true
        }

        // Campo input
        val apiKeyField = TextField().apply {
            promptText = "gsk..."
            style = "-fx-font-size: 12px; -fx-pref-width: 400px; -fx-pref-height: 30px;"
        }

        val errorLabel = Label().apply {
            style = "-fx-text-fill: #ff6b6b; -fx-font-size: 12px;"
            isVisible = false
        }

        val infoLabel = Label("La chiave verrà salvata in modo sicuro in ~/.caleb/").apply {
            style = "-fx-text-fill: #b0b0b0; -fx-font-size: 11px;"
        }

        // Bottone salva
        val saveButton = Button("💾 Salva e Continua").apply {
            style = """
                -fx-background-color: #4CAF50;
                -fx-text-fill: white;
                -fx-font-size: 14px;
                -fx-font-weight: bold;
                -fx-padding: 12px 24px;
                -fx-background-radius: 8px;
            """.trimIndent()

            setOnAction {
                val apiKey = apiKeyField.text?.trim() ?: ""

                when {
                    apiKey.isBlank() -> {
                        errorLabel.text = "Inserisci una API key"
                        errorLabel.isVisible = true
                    }
                    !apiKey.startsWith("gsk") -> {
                        errorLabel.text = "La API key deve iniziare con 'gsk'"
                        errorLabel.isVisible = true
                    }
                    apiKey.length < 20 -> {
                        errorLabel.text = "La API key sembra troppo corta"
                        errorLabel.isVisible = true
                    }
                    else -> {
                        apiKeyManager.saveApiKey(apiKey)
                        showMainScreen()
                    }
                }
            }
        }

        val linkLabel = Label("Ottieni la tua API key gratuita su console.groq.com").apply {
            style = "-fx-text-fill: #d0d0d0; -fx-font-size: 11px;"
        }

        root.children.addAll(title, subtitle, VBox(10.0).apply {
            alignment = Pos.CENTER
            children.addAll(apiKeyField, errorLabel, infoLabel)
        }, saveButton, linkLabel)

        primaryStage.scene = Scene(root, 600.0, 500.0)
    }

    private fun showMainScreen() {
        val apiKey = apiKeyManager.getApiKey() ?: return

        val root = BorderPane().apply {
            style = "-fx-background-color: #f5f7fa;"
        }

        // Header
        val header = HBox(20.0).apply {
            padding = Insets(20.0)
            alignment = Pos.CENTER_LEFT
            style = "-fx-background-color: linear-gradient(to right, #667eea 0%, #764ba2 100%);"

            children.add(Label("🚀 Caleb - AI Appointments").apply {
                style = "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;"
            })

            // Bottone reset API key
            val resetButton = Button("🔧 Cambia API Key").apply {
                style = """
                    -fx-background-color: rgba(255,255,255,0.2);
                    -fx-text-fill: white;
                    -fx-background-radius: 6px;
                    -fx-font-size: 11px;
                """.trimIndent()

                setOnAction {
                    apiKeyManager.clearApiKey()
                    showApiKeyScreen()
                }
            }

            val spacer = Region()
            HBox.setHgrow(spacer, Priority.ALWAYS)
            children.add(spacer)
            children.add(resetButton)
        }

        // Content area
        val content = VBox(20.0).apply {
            padding = Insets(30.0)
            alignment = Pos.TOP_CENTER
        }

        // Input area
        val promptArea = TextArea().apply {
            promptText = "Descrivi il tuo appuntamento (es: Riunione con Mario domani alle 15 in ufficio)"
            prefRowCount = 4
            prefWidth = 600.0
            prefHeight = 100.0
            style = "-fx-font-size: 26px; -fx-background-radius: 8px; -fx-border-radius: 8px;"
        }

        val createButton = Button("🚀 Crea Appuntamento con AI").apply {
            prefWidth = 300.0
            prefHeight = 40.0
            style = """
                -fx-background-color: linear-gradient(to right, #667eea 0%, #764ba2 100%);
                -fx-text-fill: white;
                -fx-font-size: 14px;
                -fx-font-weight: bold;
                -fx-padding: 12px 24px;
                -fx-background-radius: 8px;
            """.trimIndent()
        }

        val progressBar = ProgressIndicator().apply {
            prefWidth = 30.0
            prefHeight = 30.0
            isVisible = false
        }

        // Response area
        val responseArea = TextArea().apply {
            isEditable = false
            prefRowCount = 12
            prefWidth = 600.0
            prefHeight = 300.0
            style = """
                -fx-font-size: 12px; 
                -fx-background-radius: 8px; 
                -fx-border-radius: 8px;
                -fx-background-color: #ffffff;
            """.trimIndent()
        }

        val scrollPane = ScrollPane(responseArea).apply {
            isFitToWidth = true
            style = "-fx-background-radius: 8px;"
        }

        // Setup networking
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            println("HTTP: $message")
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val groqService = Retrofit.Builder()
            .baseUrl("https://api.groq.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GroqService::class.java)

        val appointmentService = Retrofit.Builder()
            .baseUrl("http://192.168.168.93:8079/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AppointmentService::class.java)

        // Button action
        createButton.setOnAction {
            val promptText = promptArea.text?.trim()

            if (promptText.isNullOrBlank()) {
                responseArea.text = "⚠️ Inserisci la descrizione dell'appuntamento"
                return@setOnAction
            }

            // Crea task per operazione asincrona
            val task = object : Task<String>() {
                override fun call(): String {
                    return runBlocking {
                        processAppointment(promptText, apiKey, groqService, appointmentService)
                    }
                }
            }

            task.setOnRunning {
                createButton.isDisable = true
                progressBar.isVisible = true
                responseArea.text = "🔄 Elaborazione con AI in corso..."
            }

            task.setOnSucceeded {
                createButton.isDisable = false
                progressBar.isVisible = false
                responseArea.text = task.value
            }

            task.setOnFailed {
                createButton.isDisable = false
                progressBar.isVisible = false
                responseArea.text = "❌ Errore: ${task.exception?.message}"
                task.exception?.printStackTrace()
            }

            Thread(task).start()
        }

        content.children.addAll(
            Label("Descrivi il tuo appuntamento:").apply {
                style = "-fx-font-size: 14px; -fx-font-weight: bold;"
            },
            promptArea,
            HBox(10.0).apply {
                alignment = Pos.CENTER
                children.addAll(createButton, progressBar)
            },
            Label("Risposta:").apply {
                style = "-fx-font-size: 14px; -fx-font-weight: bold;"
            },
            scrollPane
        )

        root.top = header
        root.center = content

        primaryStage.scene = Scene(root, 900.0, 700.0)
    }

    private suspend fun processAppointment(
        promptText: String,
        apiKey: String,
        groqService: GroqService,
        appointmentService: AppointmentService
    ): String {
        return try {
            println("Inizio elaborazione appuntamento")

            // Genera ID univoco per l'appuntamento
            val appointmentId = "APP-${(100..999).random()}-${(100..999).random()}-${(100..999).random()}-${(1..9).random()}"

            // Crea system prompt con data corrente e ID
            val currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            val systemPrompt = createSystemPrompt(currentDate, appointmentId)

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
                return "❌ Errore Groq API!\nCodice: ${groqResponse.code()}\nErrore: $errorBody"
            }

            val aiContent = groqResponse.body()?.choices?.firstOrNull()?.message?.content
            if (aiContent.isNullOrBlank()) {
                return "❌ Risposta AI vuota"
            }

            println("Risposta AI: $aiContent")

            // Pulisci e parsa JSON
            val cleanedJson = aiContent.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            println("JSON pulito: $cleanedJson")

            // Converte JSON in oggetto Appointment
            val appointment = try {
                Gson().fromJson(cleanedJson, Appointment::class.java)
            } catch (e: Exception) {
                return "❌ Errore parsing JSON: ${e.message}\n\nJSON ricevuto:\n$cleanedJson"
            }

            println("Appuntamento creato: $appointment")

            // Invia al server
            val serverResponse = appointmentService.createAppointment(appointment)

            if (serverResponse.isSuccessful) {
                """
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
            } else {
                val errorBody = serverResponse.errorBody()?.string()
                """
                    ⚠️ AI OK, ma errore server:
                    
                    📅 Appuntamento generato:
                    ${appointment.title}
                    • Stato: ${appointment.status}
                    
                    ❌ Errore server:
                    Codice: ${serverResponse.code()}
                    Errore: $errorBody
                """.trimIndent()
            }

        } catch (e: Exception) {
            """
                🔥 Errore generale!
                Tipo: ${e.javaClass.simpleName}
                Messaggio: ${e.message ?: "Errore sconosciuto"}
                
                Stack trace:
                ${e.stackTraceToString()}
            """.trimIndent()
        }
    }

    private fun createSystemPrompt(currentDate: String, appointmentId: String): String {
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
}

// Main function
fun main() {
    Application.launch(CalebApp::class.java)
}
