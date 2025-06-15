import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek
import kotlin.math.*
import kotlin.random.Random

data class BusRecord(
    val data: String,
    val temperatura: Double,
    val direzione: String,
    val livelloAffollamento: Double
)

data class ProcessedSample(
    val inputs: DoubleArray,
    val target: Double
)

data class NormalizedStats(
    val min: Double,
    val max: Double,
    val range: Double
)

data class PredictionResult(
    val level: Double,
    val confidence: Double,
    val rawOutput: Double,
    val networkOutput: Double,
    val dayName: String,
    val direzione: String
)

data class TrainingStats(
    val epochs: Int,
    val trainingTime: Long,
    val avgError: Double,
    val minError: Double,
    val maxError: Double,
    val samplesUsed: Int
)

// Rete Neurale Semplice - Implementazione Kotlin
class SimpleNeuralNetwork(
    private val inputSize: Int = 4,
    private val hiddenSize: Int = 8,
    private val outputSize: Int = 1
) {
    private var weightsIH = randomMatrix(hiddenSize, inputSize)
    private var weightsHO = randomMatrix(outputSize, hiddenSize)
    private var biasH = DoubleArray(hiddenSize) { Random.nextDouble(-0.1, 0.1) }
    private var biasO = DoubleArray(outputSize) { Random.nextDouble(-0.1, 0.1) }
    
    private val learningRate = 0.1
    
    private fun randomMatrix(rows: Int, cols: Int): Array<DoubleArray> =
        Array(rows) { DoubleArray(cols) { Random.nextDouble(-0.2, 0.2) } }
    
    private fun sigmoid(x: Double): Double =
        1.0 / (1.0 + exp(-x.coerceIn(-500.0, 500.0)))
    
    private fun sigmoidDerivative(x: Double): Double = x * (1.0 - x)
    
    data class ForwardResult(val outputs: DoubleArray, val hidden: DoubleArray)
    
    fun predict(inputs: DoubleArray): ForwardResult {
        // Input -> Hidden
        val hidden = DoubleArray(hiddenSize) { i ->
            val sum = biasH[i] + inputs.mapIndexed { j, input -> 
                input * weightsIH[i][j] 
            }.sum()
            sigmoid(sum)
        }
        
        // Hidden -> Output
        val outputs = DoubleArray(outputSize) { i ->
            val sum = biasO[i] + hidden.mapIndexed { j, h -> 
                h * weightsHO[i][j] 
            }.sum()
            sigmoid(sum)
        }
        
        return ForwardResult(outputs, hidden)
    }
    
    fun train(inputs: DoubleArray, targets: DoubleArray): Double {
        val (outputs, hidden) = predict(inputs)
        
        // Calcola errori output
        val outputErrors = outputs.mapIndexed { i, out -> targets[i] - out }
        val outputGradients = outputs.mapIndexed { i, out ->
            outputErrors[i] * sigmoidDerivative(out) * learningRate
        }
        
        // Aggiorna pesi Hidden -> Output
        for (i in 0 until outputSize) {
            for (j in 0 until hiddenSize) {
                weightsHO[i][j] += outputGradients[i] * hidden[j]
            }
            biasO[i] += outputGradients[i]
        }
        
        // Calcola errori hidden
        val hiddenErrors = DoubleArray(hiddenSize) { i ->
            outputErrors.mapIndexed { j, error -> error * weightsHO[j][i] }.sum()
        }
        
        val hiddenGradients = hidden.mapIndexed { i, h ->
            hiddenErrors[i] * sigmoidDerivative(h) * learningRate
        }
        
        // Aggiorna pesi Input -> Hidden
        for (i in 0 until hiddenSize) {
            for (j in 0 until inputSize) {
                weightsIH[i][j] += hiddenGradients[i] * inputs[j]
            }
            biasH[i] += hiddenGradients[i]
        }
        
        return abs(outputErrors[0])
    }
}

// Preprocessore dati
class DataProcessor {
    private val stats = mutableMapOf<String, NormalizedStats>()
    
    fun normalizeData(data: List<BusRecord>): List<ProcessedSample> {
        val features = listOf("temperatura", "dayOfWeek", "direzione", "month")
        
        // Calcola statistiche per normalizzazione
        features.forEach { feature ->
            val values = when (feature) {
                "direzione" -> data.map { if (it.direzione == "ANDATA") 1.0 else 0.0 }
                "dayOfWeek" -> data.map { LocalDate.parse(it.data).dayOfWeek.value.toDouble() }
                "month" -> data.map { LocalDate.parse(it.data).monthValue.toDouble() }
                else -> data.map { getFeatureValue(it, feature) }
            }
            
            val min = values.minOrNull() ?: 0.0
            val max = values.maxOrNull() ?: 1.0
            val range = if (max - min == 0.0) 1.0 else max - min
            
            stats[feature] = NormalizedStats(min, max, range)
        }
        
        // Normalizza target (livelloAffollamento)
        val targets = data.map { it.livelloAffollamento }
        val targetMin = targets.minOrNull() ?: 1.0
        val targetMax = targets.maxOrNull() ?: 5.0
        val targetRange = if (targetMax - targetMin == 0.0) 1.0 else targetMax - targetMin
        
        stats["target"] = NormalizedStats(targetMin, targetMax, targetRange)
        
        return data.map { record ->
            ProcessedSample(
                inputs = normalizeInput(record),
                target = normalizeTarget(record.livelloAffollamento)
            )
        }
    }
    
    private fun getFeatureValue(record: BusRecord, feature: String): Double =
        when (feature) {
            "temperatura" -> record.temperatura
            else -> 0.0
        }
    
    fun normalizeInput(record: BusRecord): DoubleArray {
        val date = LocalDate.parse(record.data)
        return doubleArrayOf(
            normalize(record.temperatura, "temperatura"),
            normalize(date.dayOfWeek.value.toDouble(), "dayOfWeek"),
            normalize(if (record.direzione == "ANDATA") 1.0 else 0.0, "direzione"),
            normalize(date.monthValue.toDouble(), "month")
        )
    }
    
    fun normalizeTarget(value: Double): Double {
        val stat = stats["target"]!!
        return (value - stat.min) / stat.range
    }
    
    fun denormalizeTarget(normalizedValue: Double): Double {
        val stat = stats["target"]!!
        return normalizedValue * stat.range + stat.min
    }
    
    private fun normalize(value: Double, feature: String): Double {
        val stat = stats[feature]!!
        return (value - stat.min) / stat.range
    }
}

// Sistema di previsione principale
class BusPredictionSystem {
    private val network = SimpleNeuralNetwork(4, 8, 1)
    private val processor = DataProcessor()
    private var trained = false
    private var trainingStats: TrainingStats? = null
    
    fun loadData(jsonPath: String): List<BusRecord> {
        return try {
            val jsonString = File(jsonPath).readText()
            val gson = Gson()
            val listType = object : TypeToken<List<BusRecord>>() {}.type
            val data: List<BusRecord> = gson.fromJson(jsonString, listType)
            println("📊 Caricati ${data.size} record dal file $jsonPath")
            showDataStats(data)
            data
        } catch (e: Exception) {
            println("❌ Errore nel caricamento del file: ${e.message}")
            throw e
        }
    }
    
    private fun showDataStats(data: List<BusRecord>) {
        val andataData = data.filter { it.direzione == "ANDATA" }
        val ritornoData = data.filter { it.direzione == "RITORNO" }
        
        val dates = data.map { LocalDate.parse(it.data) }.sorted()
        val temperatures = data.map { it.temperatura }
        
        println("\n📈 STATISTICHE DATI:")
        println("   Periodo: ${dates.first()} - ${dates.last()}")
        println("   Temperature: ${temperatures.minOrNull()?.toInt()}°C - ${temperatures.maxOrNull()?.toInt()}°C")
        println("   Record totali: ${data.size} (${andataData.size} andata, ${ritornoData.size} ritorno)")
        
        val avgAndata = if (andataData.isNotEmpty()) 
            andataData.map { it.livelloAffollamento }.average() else 0.0
        val avgRitorno = if (ritornoData.isNotEmpty()) 
            ritornoData.map { it.livelloAffollamento }.average() else 0.0
            
        println("   Affollamento medio: Andata ${"%.1f".format(avgAndata)}, Ritorno ${"%.1f".format(avgRitorno)}")
    }
    
    fun trainNetwork(data: List<BusRecord>, epochs: Int = 1000) {
        println("\n🧠 TRAINING RETE NEURALE:")
        println("   Preprocessing di ${data.size} campioni...")
        
        val processedData = processor.normalizeData(data).toMutableList()
        
        println("   Inizio training per $epochs epoche...")
        val startTime = System.currentTimeMillis()
        
        var totalError = 0.0
        var minError = Double.MAX_VALUE
        var maxError = 0.0
        
        repeat(epochs) { epoch ->
            processedData.shuffle()
            
            val epochError = processedData.map { sample ->
                network.train(sample.inputs, doubleArrayOf(sample.target))
            }.average()
            
            totalError += epochError
            minError = minOf(minError, epochError)
            maxError = maxOf(maxError, epochError)
            
            if (epoch % 200 == 0 || epoch == epochs - 1) {
                println("   Epoca ${epoch + 1}/$epochs - Errore: ${"%.4f".format(epochError)}")
            }
        }
        
        val trainingTime = System.currentTimeMillis() - startTime
        
        trainingStats = TrainingStats(
            epochs = epochs,
            trainingTime = trainingTime,
            avgError = totalError / epochs,
            minError = minError,
            maxError = maxError,
            samplesUsed = processedData.size
        )
        
        println("\n✅ Training completato in ${trainingTime}ms")
        println("   Errore medio: ${"%.4f".format(trainingStats!!.avgError)}")
        println("   Errore min/max: ${"%.4f".format(minError)}/${"%.4f".format(maxError)}")
        
        trained = true
    }
    
    fun predict(targetDate: String, temperatura: Double, direzione: String): PredictionResult? {
        if (!trained) {
            println("❌ La rete non è stata ancora addestrata!")
            return null
        }
        
        val date = LocalDate.parse(targetDate)
        val testRecord = BusRecord(targetDate, temperatura, direzione, 0.0)
        
        val normalizedInput = processor.normalizeInput(testRecord)
        val prediction = network.predict(normalizedInput)
        val denormalizedOutput = processor.denormalizeTarget(prediction.outputs[0])
        
        val finalPrediction = denormalizedOutput.coerceIn(1.0, 5.0)
        
        return PredictionResult(
            level = (finalPrediction * 2).roundToInt() / 2.0,
            confidence = calculateConfidence(finalPrediction),
            rawOutput = denormalizedOutput,
            networkOutput = prediction.outputs[0],
            dayName = getDayName(date.dayOfWeek),
            direzione = direzione
        )
    }
    
    private fun calculateConfidence(prediction: Double): Double {
        val nearestInt = prediction.roundToInt().toDouble()
        val distance = abs(prediction - nearestInt)
        return maxOf(50.0, 100.0 - (distance * 50))
    }
    
    private fun getDayName(dayOfWeek: DayOfWeek): String =
        when (dayOfWeek) {
            DayOfWeek.MONDAY -> "Lunedì"
            DayOfWeek.TUESDAY -> "Martedì"
            DayOfWeek.WEDNESDAY -> "Mercoledì"
            DayOfWeek.THURSDAY -> "Giovedì"
            DayOfWeek.FRIDAY -> "Venerdì"
            DayOfWeek.SATURDAY -> "Sabato"
            DayOfWeek.SUNDAY -> "Domenica"
        }
    
    fun showPrediction(result: PredictionResult, targetDate: String, temperatura: Double, direzione: String) {
        val levelDescriptions = mapOf(
            1.0 to "Vuoto", 1.5 to "Quasi vuoto", 2.0 to "Poco affollato",
            2.5 to "Moderatamente affollato", 3.0 to "Normalmente affollato",
            3.5 to "Abbastanza affollato", 4.0 to "Molto affollato",
            4.5 to "Quasi pieno", 5.0 to "Strapieno"
        )
        
        val directionEmoji = if (direzione == "ANDATA") "🚌" else "🏠"
        val date = LocalDate.parse(targetDate)
        
        println("\n🔮 PREVISIONE:")
        println("   $directionEmoji $direzione per ${date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))} (${result.dayName})")
        println("   🌡️  Temperatura: ${temperatura.toInt()}°C")
        println("   📊 Livello previsto: ${result.level} - ${levelDescriptions[result.level]}")
        println("   📈 Affidabilità: ${"%.1f".format(result.confidence)}%")
        println("   🔧 Output rete: ${"%.3f".format(result.rawOutput)} (normalizzato: ${"%.3f".format(result.networkOutput)})")
        
        trainingStats?.let { stats ->
            println("\n⚙️  DETTAGLI MODELLO:")
            println("   Addestrato su ${stats.samplesUsed} campioni")
            println("   ${stats.epochs} epoche in ${stats.trainingTime}ms")
            println("   Errore finale: ${"%.4f".format(stats.avgError)}")
        }
    }
}

fun main(args: Array<String>) {
    if (args.size < 4) {
        println("📋 USO:")
        println("  kotlin NeuralPredictorKt <file.json> <data> <temperatura> <direzione> [epoche]")
        println("")
        println("📝 PARAMETRI:")
        println("  file.json     - File JSON con dati storici")
        println("  data          - Data target (YYYY-MM-DD)")
        println("  temperatura   - Temperatura prevista (°C)")
        println("  direzione     - ANDATA o RITORNO")
        println("  epoche        - Numero epoche training (default: 1000)")
        println("")
        println("📌 ESEMPIO:")
        println("  kotlin NeuralPredictorKt dati.json 2025-06-10 25 ANDATA 1500")
        return
    }
    
    val jsonFile = args[0]
    val targetDate = args[1]
    val temperatura = args[2].toDoubleOrNull()
    val direzione = args[3].uppercase()
    val epochs = args.getOrNull(4)?.toIntOrNull() ?: 1000
    
    // Validazione parametri
    if (!File(jsonFile).exists()) {
        println("❌ File $jsonFile non trovato")
        return
    }
    
    if (temperatura == null) {
        println("❌ Temperatura non valida")
        return
    }
    
    if (direzione !in listOf("ANDATA", "RITORNO")) {
        println("❌ Direzione deve essere ANDATA o RITORNO")
        return
    }
    
    try {
        LocalDate.parse(targetDate)
    } catch (e: Exception) {
        println("❌ Data non valida. Usa formato YYYY-MM-DD")
        return
    }
    
    println("🚀 SISTEMA PREVISIONALE AUTOBUS - RETE NEURALE")
    println("================================================")
    
    val system = BusPredictionSystem()
    
    try {
        val data = system.loadData(jsonFile)
        system.trainNetwork(data, epochs)
        
        val result = system.predict(targetDate, temperatura, direzione)
        result?.let { 
            system.showPrediction(it, targetDate, temperatura, direzione)
        }
        
        println("\n✨ Previsione completata!")
        
    } catch (e: Exception) {
        println("❌ Errore durante l'esecuzione: ${e.message}")
        e.printStackTrace()
    }
}