package org.example.perso

// PersonalityApp.kt
import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.stage.FileChooser
import javafx.stage.Stage

// Modello dati per le risposte del questionario
data class QuestionnaireResponse(
    var oreTrascorseDaSolo: Int = 0,
    var pauraDelPalcoscenico: Boolean = false,
    var partecipazioneEventiSociali: Int = 0,
    var usciteSettimanali: Int = 0,
    var stanchezzaDopoSocializzazione: Boolean = false,
    var numeroAmiciStretti: Int = 0,
    var frequenzaPostSocial: Int = 0
)

class PersonalityApp : Application() {
    private lateinit var stage: Stage
    private val responses = QuestionnaireResponse()
    private val predictor = PersonalityPredictor()
    private var currentQuestionIndex = 0
    
    // Domande del questionario
    private val questions = listOf(
        Question("Quante ore passi da solo/a ogni giorno?", QuestionType.SLIDER, 0, 11),
        Question("Hai paura del palcoscenico/di parlare in pubblico?", QuestionType.YES_NO),
        Question("Con che frequenza partecipi ad eventi sociali?", QuestionType.SLIDER, 0, 10, "mai", "sempre"),
        Question("Quante volte esci di casa a settimana?", QuestionType.SLIDER, 0, 7),
        Question("Ti senti scarico/a dopo aver socializzato?", QuestionType.YES_NO),
        Question("Quanti amici stretti hai?", QuestionType.SLIDER, 0, 15),
        Question("Con che frequenza pubblichi sui social media?", QuestionType.SLIDER, 0, 10, "mai", "continuamente")
    )
    
    override fun start(primaryStage: Stage) {
        stage = primaryStage
        stage.title = "Sistema di Previsione Personalità"
        stage.isResizable = false
        stage.width = 900.0
        stage.height = 900.0
        stage.minWidth = 900.0
        stage.minHeight = 900.0
        stage.maxWidth = 900.0
        stage.maxHeight = 900.0
        showWelcomeScreen()
        stage.show()
    }
    
    private fun showWelcomeScreen() {
        val root = VBox(30.0).apply {
            alignment = Pos.CENTER
            padding = Insets(60.0)
            style = "-fx-background-color: linear-gradient(to bottom, #f8fafc, #e0e7ef);"
            minWidth = 900.0
            minHeight = 900.0
            prefWidth = 900.0
            prefHeight = 900.0
            maxWidth = 900.0
            maxHeight = 900.0
        }
        
        val title = Label("🧠 Sistema di Previsione Personalità").apply {
            font = Font.font("Arial", FontWeight.BOLD, 42.0)
            textFill = Color.web("#22223b")
        }
        
        val subtitle = Label("Scopri se sei introverso o estroverso attraverso l'intelligenza artificiale").apply {
            font = Font.font("Arial", 24.0)
            textFill = Color.web("#4a4e69")
            isWrapText = true
            maxWidth = 600.0
        }
        
        val description = Label("""
            Questo questionario utilizza algoritmi di machine learning per analizzare 
            le tue abitudini sociali e predire il tuo tipo di personalità.
            
            Ti faremo 7 domande e poi ti mostreremo i risultati con le probabilità.
        """.trimIndent()).apply {
            font = Font.font("Arial", 21.0)
            textFill = Color.web("#22223b")
            isWrapText = true
            maxWidth = 700.0
        }
        
        val startButton = Button("Inizia Questionario").apply {
            font = Font.font("Arial", FontWeight.BOLD, 24.0)
            prefWidth = 300.0
            prefHeight = 75.0
            style = """
                -fx-background-color: #a3cef1;
                -fx-text-fill: #22223b;
                -fx-background-radius: 25;
                -fx-cursor: hand;
            """.trimIndent()
            setOnAction { showQuestionScreen() }
        }
        
        root.children.addAll(title, subtitle, description, startButton)
        
        val scene = Scene(root, 900.0, 900.0)
        stage.scene = scene
    }
    
    private fun showQuestionScreen() {
        if (currentQuestionIndex >= questions.size) {
            showResultsScreen()
            return
        }
        
        val question = questions[currentQuestionIndex]
        val root = VBox(45.0).apply {
            alignment = Pos.CENTER
            padding = Insets(60.0)
            style = "-fx-background-color: linear-gradient(to bottom, #f8fafc, #e0e7ef);"
            minWidth = 900.0
            minHeight = 900.0
            prefWidth = 900.0
            prefHeight = 900.0
            maxWidth = 900.0
            maxHeight = 900.0
        }
        
        // Progress bar
        val progressBar = ProgressBar().apply {
            progress = (currentQuestionIndex + 1).toDouble() / questions.size
            prefWidth = 600.0
            prefHeight = 15.0
            style = "-fx-accent: #a3cef1;"
        }
        
        val progressLabel = Label("Domanda ${currentQuestionIndex + 1} di ${questions.size}").apply {
            font = Font.font("Arial", FontWeight.BOLD, 21.0)
            textFill = Color.web("#22223b")
        }
        
        val questionTitle = Label(question.text).apply {
            font = Font.font("Arial", FontWeight.BOLD, 30.0)
            textFill = Color.web("#22223b")
            isWrapText = true
            maxWidth = 700.0
        }
        
        val inputContainer = createInputForQuestion(question)
        
        val navigationBox = HBox(30.0).apply {
            alignment = Pos.CENTER
        }
        
        val backButton = Button("Indietro").apply {
            prefWidth = 180.0
            prefHeight = 60.0
            style = """
                -fx-background-color: #bdbdbd;
                -fx-text-fill: #22223b;
                -fx-background-radius: 20;
                -fx-cursor: hand;
            """.trimIndent()
            font = Font.font("Arial", 21.0)
            isDisable = currentQuestionIndex == 0
            setOnAction { 
                currentQuestionIndex--
                showQuestionScreen()
            }
        }
        
        val nextButton = Button(if (currentQuestionIndex == questions.size - 1) "Vedi Risultati" else "Avanti").apply {
            prefWidth = 180.0
            prefHeight = 60.0
            style = """
                -fx-background-color: #a3cef1;
                -fx-text-fill: #22223b;
                -fx-background-radius: 20;
                -fx-cursor: hand;
            """.trimIndent()
            font = Font.font("Arial", 21.0)
            setOnAction { 
                if (saveCurrentAnswer(question, inputContainer)) {
                    currentQuestionIndex++
                    showQuestionScreen()
                }
            }
        }
        
        navigationBox.children.addAll(backButton, nextButton)
        
        root.children.addAll(progressLabel, progressBar, questionTitle, inputContainer, navigationBox)
        
        val scrollPane = ScrollPane(root).apply {
            isFitToWidth = true
            style = "-fx-background: transparent; -fx-background-color: transparent;"
            minWidth = 900.0
            minHeight = 900.0
            prefWidth = 900.0
            prefHeight = 900.0
            maxWidth = 900.0
            maxHeight = 900.0
        }
        val scene = Scene(scrollPane, 900.0, 900.0)
        stage.scene = scene
        Platform.runLater { scrollPane.vvalue = 0.0 }
    }
    
    private fun createInputForQuestion(question: Question): VBox {
        val container = VBox(22.0).apply {
            alignment = Pos.CENTER
        }
        
        when (question.type) {
            QuestionType.SLIDER -> {
                val slider = Slider(question.min.toDouble(), question.max.toDouble(), 0.0).apply {
                    prefWidth = 450.0
                    isShowTickLabels = true
                    isShowTickMarks = true
                    majorTickUnit = if (question.max <= 7) 1.0 else 2.0
                    isSnapToTicks = true
                    style = "-fx-control-inner-background: white;"
                }
                
                val valueLabel = Label("0").apply {
                    font = Font.font("Arial", FontWeight.BOLD, 27.0)
                    textFill = Color.web("#22223b")
                }
                
                slider.valueProperty().addListener { _, _, newValue ->
                    valueLabel.text = newValue.toInt().toString()
                }
                
                val labelsBox = HBox().apply {
                    alignment = Pos.CENTER_LEFT
                    prefWidth = 450.0
                }
                
                if (question.minLabel.isNotEmpty() && question.maxLabel.isNotEmpty()) {
                    val minLabel = Label(question.minLabel).apply {
                        font = Font.font("Arial", 18.0)
                        textFill = Color.web("#4a4e69")
                    }
                    val maxLabel = Label(question.maxLabel).apply {
                        font = Font.font("Arial", 18.0)
                        textFill = Color.web("#4a4e69")
                    }
                    
                    labelsBox.children.addAll(minLabel)
                    val spacer = Region()
                    HBox.setHgrow(spacer, Priority.ALWAYS)
                    labelsBox.children.addAll(spacer, maxLabel)
                }
                
                container.children.addAll(slider, valueLabel, labelsBox)
                container.userData = slider
            }
            
            QuestionType.YES_NO -> {
                val toggleGroup = ToggleGroup()
                
                val yesButton = RadioButton("Sì").apply {
                    font = Font.font("Arial", 24.0)
                    textFill = Color.web("#22223b")
                    style = "-fx-text-fill: #22223b;"
                }
                val noButton = RadioButton("No").apply {
                    font = Font.font("Arial", 24.0)
                    textFill = Color.web("#22223b")
                    style = "-fx-text-fill: #22223b;"
                }
                yesButton.toggleGroup = toggleGroup
                noButton.toggleGroup = toggleGroup
                
                val buttonBox = HBox(45.0).apply {
                    alignment = Pos.CENTER
                }
                buttonBox.children.addAll(yesButton, noButton)
                
                container.children.add(buttonBox)
                container.userData = toggleGroup
            }
        }
        
        return container
    }
    
    private fun saveCurrentAnswer(question: Question, inputContainer: VBox): Boolean {
        return when (question.type) {
            QuestionType.SLIDER -> {
                val slider = inputContainer.userData as Slider
                val value = slider.value.toInt()
                
                when (currentQuestionIndex) {
                    0 -> responses.oreTrascorseDaSolo = value
                    2 -> responses.partecipazioneEventiSociali = value
                    3 -> responses.usciteSettimanali = value
                    5 -> responses.numeroAmiciStretti = value
                    6 -> responses.frequenzaPostSocial = value
                }
                true
            }
            
            QuestionType.YES_NO -> {
                val toggleGroup = inputContainer.userData as ToggleGroup
                val selected = toggleGroup.selectedToggle
                
                if (selected == null) {
                    showAlert("Attenzione", "Per favore seleziona una risposta prima di continuare.")
                    return false
                }
                
                val isYes = (selected as RadioButton).text == "Sì"
                
                when (currentQuestionIndex) {
                    1 -> responses.pauraDelPalcoscenico = isYes
                    4 -> responses.stanchezzaDopoSocializzazione = isYes
                }
                true
            }
        }
    }
    
private fun showResultsScreen() {
    val (prediction, probabilities) = predictor.predict(responses)
    
    val root = VBox(40.0).apply {
        alignment = Pos.TOP_CENTER
        padding = Insets(60.0, 0.0, 40.0, 0.0)
        style = """
            -fx-background-color: linear-gradient(to bottom, #f8fafc, #e0e7ef);
        """.trimIndent()
        prefWidth = 900.0
    }
    
    val title = Label("🔮 Risultati della Previsione").apply {
        font = Font.font("Arial", FontWeight.BOLD, 44.0)
        textFill = Color.web("#22223b")
        style = "-fx-effect: dropshadow(gaussian, #bde0fe, 8, 0.2, 0, 2);"
        maxWidth = Double.MAX_VALUE
    }
    
    val resultCard = VBox(18.0).apply {
        alignment = Pos.CENTER
        padding = Insets(32.0, 40.0, 32.0, 40.0)
        style = """
            -fx-background-color: #ffffffcc;
            -fx-background-radius: 24;
            -fx-effect: dropshadow(gaussian, #bde0fe, 18, 0.2, 0, 4);
            -fx-border-color: #a3cef1;
            -fx-border-width: 2;
            -fx-border-radius: 24;
        """.trimIndent()
        maxWidth = 600.0
    }
    
    val predictionLabel = Label("Il tuo tipo di personalità previsto è:").apply {
        font = Font.font("Arial", 25.0)
        textFill = Color.web("#4a4e69")
        maxWidth = Double.MAX_VALUE
    }
    
    val resultLabel = Label(prediction).apply {
        font = Font.font("Arial", FontWeight.EXTRA_BOLD, 60.0)
        textFill = Color.web("#4361ee")
        style = "-fx-effect: dropshadow(gaussian, #f9c74f, 12, 0.3, 0, 2);"
        maxWidth = Double.MAX_VALUE
    }
    
    resultCard.children.addAll(predictionLabel, resultLabel)
    
    val probabilitiesTitle = Label("Probabilità:").apply {
        font = Font.font("Arial", FontWeight.BOLD, 28.0)
        textFill = Color.web("#22223b")
        padding = Insets(18.0, 0.0, 0.0, 0.0)
        maxWidth = Double.MAX_VALUE
    }
    
    val probabilitiesBox = VBox(18.0).apply {
        alignment = Pos.CENTER
        maxWidth = 600.0
        style = "-fx-background-color: #f1f6fb; -fx-background-radius: 18; -fx-padding: 24;"
    }
    
    probabilities.forEach { (type, probability) ->
        val perc = (probability * 100).coerceIn(0.0, 100.0)
        val row = HBox(18.0).apply {
            alignment = Pos.CENTER_LEFT
            maxWidth = 520.0
        }
        val percentageLabel = Label("$type: ${String.format("%.1f", perc)}%").apply {
            font = Font.font("Arial", FontWeight.BOLD, 24.0)
            textFill = if (type == prediction) Color.web("#4361ee") else Color.web("#22223b")
            minWidth = 200.0
            maxWidth = 200.0
        }
        val progressBar = ProgressBar(probability.coerceIn(0.0, 1.0)).apply {
            prefWidth = 260.0
            prefHeight = 36.0
            style = if (type == prediction) "-fx-accent: #4361ee;" else "-fx-accent: #bde0fe;"
            maxWidth = 260.0
        }
        row.children.addAll(percentageLabel, progressBar)
        probabilitiesBox.children.add(row)
    }
    
    val summaryBox = createSummaryBox()
    
    val buttonsBox = HBox(30.0).apply {
        alignment = Pos.CENTER
        padding = Insets(18.0, 0.0, 0.0, 0.0)
        maxWidth = 600.0
    }
    
    val restartButton = Button("Nuovo Questionario").apply {
        prefWidth = 225.0
        prefHeight = 60.0
        style = """
            -fx-background-color: #a3cef1;
            -fx-text-fill: #22223b;
            -fx-background-radius: 20;
            -fx-cursor: hand;
        """.trimIndent()
        font = Font.font("Arial", 21.0)
        setOnAction { 
            currentQuestionIndex = 0
            showWelcomeScreen()
        }
    }
    
    val exitButton = Button("Esci").apply {
        prefWidth = 150.0
        prefHeight = 60.0
        style = """
            -fx-background-color: #f08080;
            -fx-text-fill: #22223b;
            -fx-background-radius: 20;
            -fx-cursor: hand;
        """.trimIndent()
        font = Font.font("Arial", 21.0)
        setOnAction { Platform.exit() }
    }
    
    buttonsBox.children.addAll(restartButton, exitButton)
    
    root.children.addAll(title, resultCard, probabilitiesTitle, probabilitiesBox, summaryBox, buttonsBox)
    
    val scrollPane = ScrollPane(root).apply {
        isFitToWidth = true
        isFitToHeight = false
        hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
        style = "-fx-background: transparent; -fx-background-color: transparent;"
        prefWidth = 900.0
        prefHeight = 900.0
        // Imposta immediatamente la posizione iniziale
        vvalue = 0.0
    }
    
    val scene = Scene(scrollPane, 900.0, 900.0)
    stage.scene = scene
    
    // Assicurati che lo scroll rimanga in cima dopo il rendering completo
    Platform.runLater {
        Platform.runLater { // Doppio runLater per assicurarsi che sia dopo il layout completo
            scrollPane.vvalue = 0.0
        }
    }
}
    
    private fun createSummaryBox(): VBox {
        val summaryBox = VBox(18.0).apply {
            alignment = Pos.CENTER
            style = """
                -fx-background-color: #f8fafc;
                -fx-background-radius: 18;
                -fx-border-color: #bde0fe;
                -fx-border-width: 2;
                -fx-border-radius: 18;
                -fx-padding: 32;
            """.trimIndent()
            maxWidth = 600.0
        }
        
        val summaryTitle = Label("Riepilogo delle tue risposte:").apply {
            font = Font.font("Arial", FontWeight.BOLD, 25.0)
            textFill = Color.web("#22223b")
            padding = Insets(0.0, 0.0, 10.0, 0.0)
            maxWidth = Double.MAX_VALUE
        }
        
        val summaryItems = listOf(
            "Ore trascorse da solo al giorno: ${responses.oreTrascorseDaSolo}",
            "Paura del palcoscenico: ${if (responses.pauraDelPalcoscenico) "Sì" else "No"}",
            "Partecipazione eventi sociali: ${responses.partecipazioneEventiSociali}/10",
            "Uscite settimanali: ${responses.usciteSettimanali}",
            "Stanchezza dopo socializzazione: ${if (responses.stanchezzaDopoSocializzazione) "Sì" else "No"}",
            "Numero amici stretti: ${responses.numeroAmiciStretti}",
            "Frequenza post social: ${responses.frequenzaPostSocial}/10"
        )
        
        summaryBox.children.add(summaryTitle)
        
        summaryItems.forEach { item ->
            val itemLabel = Label("• $item").apply {
                font = Font.font("Arial", 22.0)
                textFill = Color.web("#4a4e69")
                padding = Insets(2.0, 0.0, 2.0, 0.0)
                maxWidth = Double.MAX_VALUE
            }
            summaryBox.children.add(itemLabel)
        }
        
        return summaryBox
    }
    
    private fun showAlert(title: String, message: String) {
        val alert = Alert(Alert.AlertType.INFORMATION).apply {
            this.title = title
            headerText = null
            contentText = message
        }
        alert.showAndWait()
    }
}

// Classi di supporto
data class Question(
    val text: String,
    val type: QuestionType,
    val min: Int = 0,
    val max: Int = 10,
    val minLabel: String = "",
    val maxLabel: String = ""
)

enum class QuestionType {
    SLIDER, YES_NO
}

// Funzione main
fun main() {
    Application.launch(PersonalityApp::class.java)
}