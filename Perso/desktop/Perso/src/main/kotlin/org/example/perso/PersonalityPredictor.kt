package org.example.perso

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.Closeable
import java.nio.FloatBuffer
import java.nio.file.Files
import java.nio.file.Paths

class PersonalityPredictor(
    private val modelPath: String = "/home/ema/Scrivania/Gradle/new_fine/Korso/Perso/script/modello_personalita.onnx"
) : Closeable {
    
    private var ortSession: OrtSession? = null
    private var ortEnv: OrtEnvironment? = null
    private var classLabels: List<String> = listOf("Extrovert", "Introvert", "Ambivert")
    
    @Volatile
    private var isModelLoaded = false

    init {
        loadOnnxModel()
    }

    private fun loadOnnxModel() {
        try {
            // Verifica che il file del modello esista
            if (!Files.exists(Paths.get(modelPath))) {
                throw IllegalArgumentException("Il file del modello non esiste: $modelPath")
            }

            ortEnv = OrtEnvironment.getEnvironment()
            ortSession = ortEnv!!.createSession(modelPath, OrtSession.SessionOptions())
            
            val session = ortSession!!
            
            // Log delle informazioni del modello
            println("Modello ONNX caricato con successo da: $modelPath")
            println("Input del modello: ${session.inputNames}")
            println("Output del modello: ${session.outputNames}")
            
            // Verifica che il modello abbia almeno un input e un output
            if (session.inputNames.isEmpty() || session.outputNames.isEmpty()) {
                throw IllegalStateException("Il modello ONNX non ha input o output validi")
            }
            
            isModelLoaded = true
            
        } catch (e: Exception) {
            println("[ERRORE ONNX] Impossibile caricare il modello: ${e.message}")
            e.printStackTrace()
            close()
            throw e
        }
    }

    fun predict(response: QuestionnaireResponse): Pair<String, Map<String, Double>> {
        if (!isModelLoaded || ortSession == null || ortEnv == null) {
            throw IllegalStateException("Il modello ONNX non è stato caricato correttamente")
        }

        var inputTensor: OnnxTensor? = null
        var results: OrtSession.Result? = null
        
        try {
            val features = extractFeatures(response)
            validateFeatures(features)
            
            val session = ortSession!!
            val inputName = session.inputNames.iterator().next()
            
            // Crea il tensor di input
            val inputBuffer = FloatBuffer.wrap(features)
            inputTensor = OnnxTensor.createTensor(
                ortEnv, 
                inputBuffer, 
                longArrayOf(1, features.size.toLong())
            )
            
            // Esegue la predizione
            results = session.run(mapOf(inputName to inputTensor))
            
            // Debug completo degli output
            debugModelOutputs(results)
            
            // Il modello ha 3 output: [label, probabilities, class_labels]
            val outputNames = session.outputNames.toList()
            println("Output disponibili: $outputNames")
            
            // Accesso agli output per indice (più affidabile)
            val labelOutput = results.get(0) // label
            val probabilityOutput = results.get(1) // probabilities  
            val classLabelsOutput = results.get(2) // class_labels
            
            // Aggiorna le class labels se disponibili dal modello
            updateClassLabelsFromModel(classLabelsOutput)
            
            // Estrai la classe predetta
            val predictedClass = extractPredictedClass(labelOutput)
            
            // Estrai le probabilità
            val probabilities = extractProbabilities(probabilityOutput)
            
            // Crea la mappa delle probabilità
            val probMap = createProbabilityMap(probabilities)
            
            return Pair(predictedClass, probMap)
            
        } catch (e: Exception) {
            println("[ERRORE PREDIZIONE] ${e.message}")
            e.printStackTrace()
            throw e
        } finally {
            // Pulisce le risorse
            inputTensor?.close()
            results?.close()
        }
    }

    private fun debugModelOutputs(results: OrtSession.Result) {
        println("=== DEBUG OUTPUT MODELLO ===")
        val outputNames = ortSession?.outputNames?.toList() ?: emptyList()
        
        outputNames.forEachIndexed { index, name ->
            try {
                val output = results.get(index)
                println("Output $index '$name': ${output?.javaClass} - $output")
                
                // Se è un OnnxTensor, estrai informazioni dettagliate
                if (output is OnnxTensor) {
                    println("  - Tipo: ${output.info.type}")
                    println("  - Shape: ${output.info.shape.contentToString()}")
                    
                    // Prova a estrarre i dati
                    try {
                        when (output.info.type.toString()) {
                            "FLOAT" -> {
                                val buffer = output.floatBuffer
                                val data = FloatArray(buffer.remaining())
                                buffer.get(data)
                                println("  - Dati Float: ${data.contentToString()}")
                            }
                            "STRING" -> {
                                val stringData = output.value as? Array<*>
                                println("  - Dati String: ${stringData?.contentToString()}")
                            }
                            else -> {
                                println("  - Valore generico: ${output.value}")
                            }
                        }
                    } catch (e: Exception) {
                        println("  - Errore nell'estrazione dati: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                println("Errore nell'accesso all'output $index '$name': ${e.message}")
            }
        }
        println("=== FINE DEBUG ===")
    }

    private fun updateClassLabelsFromModel(classLabelsOutput: Any?) {
        try {
            when (classLabelsOutput) {
                is OnnxTensor -> {
                    val value = classLabelsOutput.value
                    when (value) {
                        is Array<*> -> {
                            val labels = value.mapNotNull { it?.toString() }
                            if (labels.isNotEmpty()) {
                                classLabels = labels
                                println("Class labels aggiornate dal modello: $classLabels")
                            }
                        }
                        else -> {
                            println("Valore class labels non riconosciuto: $value")
                        }
                    }
                }
                is Array<*> -> {
                    val labels = classLabelsOutput.mapNotNull { it?.toString() }
                    if (labels.isNotEmpty()) {
                        classLabels = labels
                        println("Class labels aggiornate dal modello: $classLabels")
                    }
                }
                else -> {
                    println("Tipo class labels non gestito: ${classLabelsOutput?.javaClass}")
                    println("Usando class labels predefinite: $classLabels")
                }
            }
        } catch (e: Exception) {
            println("Errore nell'aggiornamento delle class labels: ${e.message}")
            println("Usando class labels predefinite: $classLabels")
        }
    }

    private fun extractPredictedClass(labelOutput: Any?): String {
        return try {
            when (labelOutput) {
                is OnnxTensor -> {
                    val value = labelOutput.value
                    when (value) {
                        is Array<*> -> {
                            val firstElement = value.firstOrNull()
                            when (firstElement) {
                                is String -> firstElement
                                is Array<*> -> firstElement.firstOrNull()?.toString() ?: "Classe_Sconosciuta"
                                else -> firstElement?.toString() ?: "Classe_Sconosciuta"
                            }
                        }
                        is String -> value
                        else -> value?.toString() ?: "Classe_Sconosciuta"
                    }
                }
                is Array<*> -> {
                    val firstElement = labelOutput.firstOrNull()
                    when (firstElement) {
                        is String -> firstElement
                        is Array<*> -> firstElement.firstOrNull()?.toString() ?: "Classe_Sconosciuta"
                        else -> firstElement?.toString() ?: "Classe_Sconosciuta"
                    }
                }
                is String -> labelOutput
                else -> labelOutput?.toString() ?: "Classe_Sconosciuta"
            }
        } catch (e: Exception) {
            println("[ERRORE] Impossibile estrarre classe predetta: ${e.message}")
            "Classe_Sconosciuta"
        }
    }

    private fun extractProbabilities(probabilityOutput: Any?): FloatArray {
        println("[DEBUG] Tipo output probabilità: ${probabilityOutput?.javaClass}")
        println("[DEBUG] Contenuto output probabilità: $probabilityOutput")
        
        return try {
            when (probabilityOutput) {
                is OnnxTensor -> {
                    // Gestione diretta di OnnxTensor
                    try {
                        val buffer = probabilityOutput.floatBuffer
                        if (buffer.hasRemaining()) {
                            val probArray = FloatArray(buffer.remaining())
                            buffer.get(probArray)
                            println("[DEBUG] Probabilità estratte da OnnxTensor: ${probArray.contentToString()}")
                            probArray
                        } else {
                            println("[DEBUG] Buffer vuoto, usando valore predefinito")
                            createDefaultProbabilities()
                        }
                    } catch (e: Exception) {
                        println("[ERRORE] Impossibile estrarre FloatBuffer: ${e.message}")
                        // Prova con il valore generico
                        extractProbabilitiesFromValue(probabilityOutput.value)
                    }
                }
                is Array<*> -> {
                    println("[DEBUG] Array di dimensione: ${probabilityOutput.size}")
                    
                    if (probabilityOutput.isEmpty()) {
                        println("[DEBUG] Array vuoto, usando default")
                        createDefaultProbabilities()
                    } else {
                        when (val firstRow = probabilityOutput[0]) {
                            is FloatArray -> {
                                println("[DEBUG] FloatArray trovato: ${firstRow.contentToString()}")
                                firstRow
                            }
                            is DoubleArray -> {
                                println("[DEBUG] DoubleArray trovato: ${firstRow.contentToString()}")
                                firstRow.map { it.toFloat() }.toFloatArray()
                            }
                            is Array<*> -> {
                                println("[DEBUG] Array nested di dimensione: ${firstRow.size}")
                                extractProbabilitiesFromNestedArray(firstRow)
                            }
                            else -> {
                                println("[ERRORE] Formato prima riga non supportato: ${firstRow?.javaClass}")
                                createDefaultProbabilities()
                            }
                        }
                    }
                }
                is FloatArray -> {
                    println("[DEBUG] FloatArray diretto: ${probabilityOutput.contentToString()}")
                    probabilityOutput
                }
                is DoubleArray -> {
                    println("[DEBUG] DoubleArray diretto: ${probabilityOutput.contentToString()}")
                    probabilityOutput.map { it.toFloat() }.toFloatArray()
                }
                null -> {
                    println("[ERRORE] Output probabilità è null")
                    createDefaultProbabilities()
                }
                else -> {
                    println("[ERRORE] Tipo di output probabilità non gestito: ${probabilityOutput.javaClass}")
                    println("[DEBUG] Contenuto: $probabilityOutput")
                    createDefaultProbabilities()
                }
            }
        } catch (e: Exception) {
            println("[ERRORE CRITICO] Errore nell'estrazione probabilità: ${e.message}")
            e.printStackTrace()
            createDefaultProbabilities()
        }
    }

    private fun extractProbabilitiesFromValue(value: Any?): FloatArray {
        return when (value) {
            is Array<*> -> {
                if (value.isEmpty()) {
                    createDefaultProbabilities()
                } else {
                    val firstRow = value[0]
                    when (firstRow) {
                        is FloatArray -> firstRow
                        is DoubleArray -> firstRow.map { it.toFloat() }.toFloatArray()
                        is Array<*> -> extractProbabilitiesFromNestedArray(firstRow)
                        else -> createDefaultProbabilities()
                    }
                }
            }
            is FloatArray -> value
            is DoubleArray -> value.map { it.toFloat() }.toFloatArray()
            else -> createDefaultProbabilities()
        }
    }

    private fun extractProbabilitiesFromNestedArray(array: Array<*>): FloatArray {
        return try {
            val result = array.mapNotNull { 
                when (it) {
                    is Float -> it
                    is Double -> it.toFloat()
                    is Number -> it.toFloat()
                    else -> {
                        println("[DEBUG] Elemento non numerico: $it (${it?.javaClass})")
                        null
                    }
                }
            }.toFloatArray()
            println("[DEBUG] Probabilità estratte da array nested: ${result.contentToString()}")
            if (result.isEmpty()) createDefaultProbabilities() else result
        } catch (e: Exception) {
            println("[ERRORE] Impossibile estrarre probabilità da array nested: ${e.message}")
            createDefaultProbabilities()
        }
    }

    private fun createDefaultProbabilities(): FloatArray {
        // Crea probabilità uniformi per tutte le classi
        val defaultProb = 1.0f / classLabels.size
        return FloatArray(classLabels.size) { defaultProb }
    }

    private fun createProbabilityMap(probabilities: FloatArray): Map<String, Double> {
        return if (probabilities.size == classLabels.size) {
            // Mappa diretta probabilità -> classi
            classLabels.mapIndexed { idx, label -> 
                label to probabilities[idx].toDouble()
            }.toMap()
        } else {
            // Se il numero di probabilità non corrisponde, gestisci il mismatch
            println("[AVVISO] Numero probabilità (${probabilities.size}) diverso da numero classi (${classLabels.size})")
            
            val map = mutableMapOf<String, Double>()
            
            // Strategia: usa il minimo tra le due dimensioni
            val minSize = minOf(probabilities.size, classLabels.size)
            
            // Aggiungi le probabilità disponibili
            for (i in 0 until minSize) {
                map[classLabels[i]] = probabilities[i].toDouble()
            }
            
            // Se ci sono più classi che probabilità, assegna 0.0 alle rimanenti
            if (classLabels.size > probabilities.size) {
                for (i in probabilities.size until classLabels.size) {
                    map[classLabels[i]] = 0.0
                }
            }
            
            // Se ci sono più probabilità che classi, crea etichette generiche
            if (probabilities.size > classLabels.size) {
                for (i in classLabels.size until probabilities.size) {
                    map["Classe_$i"] = probabilities[i].toDouble()
                }
            }
            
            map
        }
    }

    private fun extractFeatures(response: QuestionnaireResponse): FloatArray {
        return floatArrayOf(
            response.oreTrascorseDaSolo.toFloat(),
            if (response.pauraDelPalcoscenico) 1.0f else 0.0f,
            response.partecipazioneEventiSociali.toFloat(),
            response.usciteSettimanali.toFloat(),
            if (response.stanchezzaDopoSocializzazione) 1.0f else 0.0f,
            response.numeroAmiciStretti.toFloat(),
            response.frequenzaPostSocial.toFloat()
        )
    }

    private fun validateFeatures(features: FloatArray) {
        if (features.isEmpty()) {
            throw IllegalArgumentException("Le features non possono essere vuote")
        }
        
        // Verifica che non ci siano valori NaN o infiniti
        if (features.any { it.isNaN() || it.isInfinite() }) {
            throw IllegalArgumentException("Le features contengono valori non validi (NaN o Infinito)")
        }
        
        // Log per debug
        println("Features estratte: ${features.contentToString()}")
    }

    fun isModelReady(): Boolean = isModelLoaded

    fun getModelPath(): String = modelPath

    fun getClassLabels(): List<String> = classLabels.toList()

    fun updateClassLabels(newLabels: List<String>) {
        if (newLabels.isEmpty()) {
            throw IllegalArgumentException("Le etichette delle classi non possono essere vuote")
        }
        classLabels = newLabels.toList()
        println("Etichette classi aggiornate manualmente: $classLabels")
    }

    fun getModelInfo(): Map<String, Any> {
        val session = ortSession
        return if (session != null && isModelLoaded) {
            mapOf(
                "modelPath" to modelPath,
                "isLoaded" to isModelLoaded,
                "inputNames" to session.inputNames.toList(),
                "outputNames" to session.outputNames.toList(),
                "classLabels" to classLabels
            )
        } else {
            mapOf(
                "modelPath" to modelPath,
                "isLoaded" to false,
                "error" to "Modello non caricato"
            )
        }
    }

    override fun close() {
        try {
            ortSession?.close()
            ortSession = null
            
            ortEnv?.close()
            ortEnv = null
            
            isModelLoaded = false
            println("Risorse ONNX rilasciate correttamente")
        } catch (e: Exception) {
            println("[ERRORE CHIUSURA] Errore durante il rilascio delle risorse: ${e.message}")
        }
    }
}