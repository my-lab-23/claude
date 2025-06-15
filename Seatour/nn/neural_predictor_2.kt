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
    val target: Double,
    val originalRecord: BusRecord
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
    val finalTrainingError: Double,
    val finalTestError: Double,
    val trainingSamples: Int,
    val testSamples: Int,
    val bestEpoch: Int,
    val bestTestError: Double
)

data class DataSplit(
    val trainData: List<ProcessedSample>,
    val testData: List<ProcessedSample>
)

data class EpochResult(
    val epoch: Int,
    val trainingError: Double,
    val testError: Double,
    val isImprovement: Boolean
)

// Rete Neurale con Early Stopping
class SimpleNeuralNetwork(
    private val inputSize: Int = 4,
    private val hiddenSize: Int = 8,
    private val outputSize: Int = 1
) {
    private var weightsIH = randomMatrix(hiddenSize, inputSize)
    private var weightsHO = randomMatrix(outputSize, hiddenSize)
    private var biasH = DoubleArray(hiddenSize) { Random.nextDouble(-0.1, 0.1) }
    private var biasO = DoubleArray(outputSize) { Random.nextDouble(-0.1, 0.1) }
    
    // Best weights per early stopping
    private var bestWeightsIH = weightsIH.map { it.clone() }.toTypedArray()
    private var bestWeightsHO = weightsHO.map { it.clone() }.toTypedArray()
    private var bestBiasH = biasH.clone()
    private var bestBiasO = biasO.clone()
    
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
    
    fun evaluate(data: List<ProcessedSample>): Double {
        return data.map { sample ->
            val prediction = predict(sample.inputs)
            abs(prediction.outputs[0] - sample.target)
        }.average()
    }
    
    fun saveBestWeights() {
        bestWeightsIH = weightsIH.map { it.clone() }.toTypedArray()
        bestWeightsHO = weightsHO.map { it.clone() }.toTypedArray()
        bestBiasH = biasH.clone()
        bestBiasO = biasO.clone()
    }
    
    fun restoreBestWeights() {
        weightsIH = bestWeightsIH.map { it.clone() }.toTypedArray()
        weightsHO = bestWeightsHO.map { it.clone() }.toTypedArray()
        biasH = bestBiasH.clone()
        biasO = bestBiasO.clone()
    }
}

// Preprocessore dati migliorato
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
                target = normalizeTarget(record.livelloAffollamento),
                originalRecord = record
            )
        }
    }
    
    fun splitData(data: List<ProcessedSample>, testRatio: Double = 0.2, stratify: Boolean = true): DataSplit {
        println("   🔄 Divisione dati: ${(1-testRatio)*100}% training, ${testRatio*100}% test")
        
        return if (stratify) {
            stratifiedSplit(data, testRatio)
        } else {
            randomSplit(data, testRatio)
        }
    }
    
    private fun stratifiedSplit(data: List<ProcessedSample>, testRatio: Double): DataSplit {
        // Raggruppa per direzione e livello di affollamento
        val grouped = data.groupBy { 
            "${it.originalRecord.direzione}_${it.originalRecord.livelloAffollamento.toInt()}" 
        }
        
        val trainData = mutableListOf<ProcessedSample>()
        val testData = mutableListOf<ProcessedSample>()
        
        grouped.forEach { (group, samples) ->
            val shuffled = samples.shuffled()
            val testSize = maxOf(1, (samples.size * testRatio).toInt())
            
            testData.addAll(shuffled.take(testSize))
            trainData.addAll(shuffled.drop(testSize))
        }
        
        println("     📊 Split stratificato per direzione e livello affollamento")
        println("     📈 Training: ${trainData.size} campioni")
        println("     🧪 Test: ${testData.size} campioni")
        
        return DataSplit(trainData.shuffled(), testData.shuffled())
    }
    
    private fun randomSplit(data: List<ProcessedSample>, testRatio: Double): DataSplit {
        val shuffled = data.shuffled()
        val testSize = (data.size * testRatio).toInt()
        
        val testData = shuffled.take(testSize)
        val trainData = shuffled.drop(testSize)
        
        return DataSplit(trainData, testData)
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

// Sistema di previsione con validazione
class BusPredictionSystem {
    private val network = SimpleNeuralNetwork(4, 8, 1)
    private val processor = DataProcessor()
    private var trained = false
    private var trainingStats: TrainingStats? = null
    private var dataSplit: DataSplit? = null
    
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
        
        // Distribuzione livelli affollamento
        val levels = data.groupBy { it.livelloAffollamento.toInt() }
        println("   Distribuzione affollamento:")
        levels.toSortedMap().forEach { (level, records) ->
            println("     Livello $level: ${records.size} campioni (${"%.1f".format(records.size * 100.0 / data.size)}%)")
        }
    }
    
    fun trainNetwork(data: List<BusRecord>, epochs: Int = 1000, testRatio: Double = 0.2, patience: Int = 100) {
        println("\n🧠 TRAINING RETE NEURALE CON VALIDAZIONE:")
        println("   Preprocessing di ${data.size} campioni...")
        
        val processedData = processor.normalizeData(data)
        dataSplit = processor.splitData(processedData, testRatio, stratify = true)
        
        val trainData = dataSplit!!.trainData
        val testData = dataSplit!!.testData
        
        println("   Inizio training per $epochs epoche (early stopping: $patience)...")
        val startTime = System.currentTimeMillis()
        
        var bestTestError = Double.MAX_VALUE
        var bestEpoch = 0
        var patienceCounter = 0
        
        val epochResults = mutableListOf<EpochResult>()
        
        for (epoch in 0 until epochs) {
            // Training epoch
            val shuffledTrain = trainData.shuffled()
            val trainingError = shuffledTrain.map { sample ->
                network.train(sample.inputs, doubleArrayOf(sample.target))
            }.average()
            
            // Valutazione su test set
            val testError = network.evaluate(testData)
            
            val isImprovement = testError < bestTestError
            if (isImprovement) {
                bestTestError = testError
                bestEpoch = epoch
                network.saveBestWeights()
                patienceCounter = 0
            } else {
                patienceCounter++
            }
            
            epochResults.add(EpochResult(epoch, trainingError, testError, isImprovement))
            
            if (epoch % 200 == 0 || epoch == epochs - 1 || isImprovement) {
                val marker = if (isImprovement) " ✨" else ""
                println("   Epoca ${epoch + 1}/$epochs - Train: ${"%.4f".format(trainingError)}, Test: ${"%.4f".format(testError)}$marker")
            }
            
            // Early stopping
            if (patienceCounter >= patience) {
                println("   🛑 Early stopping alla epoca ${epoch + 1} (nessun miglioramento per $patience epoche)")
                break
            }
        }
        
        // Ripristina i pesi migliori
        network.restoreBestWeights()
        val finalTestError = network.evaluate(testData)
        val finalTrainError = network.evaluate(trainData)
        
        val trainingTime = System.currentTimeMillis() - startTime
        
        trainingStats = TrainingStats(
            epochs = epochResults.size,
            trainingTime = trainingTime,
            finalTrainingError = finalTrainError,
            finalTestError = finalTestError,
            trainingSamples = trainData.size,
            testSamples = testData.size,
            bestEpoch = bestEpoch + 1,
            bestTestError = bestTestError
        )
        
        showTrainingResults(epochResults)
        trained = true
    }
    
    private fun showTrainingResults(epochResults: List<EpochResult>) {
        val stats = trainingStats!!
        
        println("\n✅ TRAINING COMPLETATO:")
        println("   ⏱️  Tempo: ${stats.trainingTime}ms (${stats.epochs} epoche)")
        println("   🏆 Miglior epoca: ${stats.bestEpoch}")
        println("   📊 Errore finale - Train: ${"%.4f".format(stats.finalTrainingError)}, Test: ${"%.4f".format(stats.finalTestError)}")
        
        // Calcola overfitting ratio
        val overfittingRatio = stats.finalTrainingError / stats.finalTestError
        val overfittingStatus = when {
            overfittingRatio > 0.8 -> "✅ Basso overfitting"
            overfittingRatio > 0.6 -> "⚠️ Overfitting moderato"
            else -> "🚨 Alto overfitting"
        }
        println("   🎯 Rapporto Train/Test: ${"%.3f".format(overfittingRatio)} - $overfittingStatus")
        
        // Mostra accuratezza su test set
        val testAccuracy = calculateAccuracy(dataSplit!!.testData)
        println("   🎯 Accuratezza test set: ${"%.1f".format(testAccuracy)}%")
    }
    
    private fun calculateAccuracy(testData: List<ProcessedSample>): Double {
        val correctPredictions = testData.count { sample ->
            val prediction = network.predict(sample.inputs)
            val denormalizedPred = processor.denormalizeTarget(prediction.outputs[0])
            val denormalizedActual = processor.denormalizeTarget(sample.target)
            
            // Considera corretta se la predizione è entro 0.5 livelli dal valore reale
            abs(denormalizedPred - denormalizedActual) <= 0.5
        }
        
        return correctPredictions * 100.0 / testData.size
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
        // Calcola fiducia basata sulla precisione del valore predetto
        val nearestHalf = (prediction * 2).roundToInt() / 2.0
        val distance = abs(prediction - nearestHalf)
        val valueConfidence = maxOf(0.0, 100.0 - (distance * 100))
        
        // Incorpora l'accuratezza del modello dal test set
        val modelAccuracy = if (dataSplit != null) {
            calculateAccuracy(dataSplit!!.testData)
        } else {
            50.0 // Default se non abbiamo dati di test
        }
        
        // Combina la fiducia del valore con l'accuratezza del modello
        // Formula: peso maggiore all'accuratezza del modello (70%) vs precisione del valore (30%)
        val combinedConfidence = (modelAccuracy * 0.7) + (valueConfidence * 0.3)
        
        // Aggiunge penalità se l'accuratezza è molto bassa
        val accuracyPenalty = if (modelAccuracy < 50.0) {
            (50.0 - modelAccuracy) * 0.5 // Penalità proporzionale alla scarsa accuratezza
        } else {
            0.0
        }
        
        return maxOf(25.0, combinedConfidence - accuracyPenalty)
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
        
        val directionEmoji = if (direzione == "ANDATA") "🚌" else "🚐"
        val date = LocalDate.parse(targetDate)
        
        println("\n🔮 PREVISIONE:")
        println("   $directionEmoji $direzione per ${date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))} (${result.dayName})")
        println("   🌡️ Temperatura: ${temperatura.toInt()}°C")
        println("   📊 Livello previsto: ${result.level} - ${levelDescriptions[result.level]}")
        println("   📈 Affidabilità: ${"%.1f".format(result.confidence)}% (basata su accuratezza modello)")
        println("   🔧 Output rete: ${"%.3f".format(result.rawOutput)} (normalizzato: ${"%.3f".format(result.networkOutput)})")
        
        trainingStats?.let { stats ->
            val modelAccuracy = calculateAccuracy(dataSplit!!.testData)
            println("\n⚙️ QUALITÀ MODELLO:")
            println("   🎯 Accuratezza test: ${"%.1f".format(modelAccuracy)}%")
            println("   📊 Errore test: ${"%.4f".format(stats.finalTestError)}")
            println("   🏆 Miglior epoca: ${stats.bestEpoch}/${stats.epochs}")
            println("   📈 Campioni: ${stats.trainingSamples} training, ${stats.testSamples} test")
            
            val overfittingRatio = stats.finalTrainingError / stats.finalTestError
            println("   🎯 Overfitting: ${"%.3f".format(overfittingRatio)} " + 
                   if (overfittingRatio > 0.8) "✅" else if (overfittingRatio > 0.6) "⚠️" else "🚨")
            
            // Spiegazione dell'affidabilità
            println("\n💡 NOTA AFFIDABILITÀ:")
            when {
                result.confidence >= 70 -> println("   ✅ Predizione affidabile (modello accurato + valore netto)")
                result.confidence >= 50 -> println("   ⚠️ Predizione moderata (accuratezza modello limitata)")
                else -> println("   🚨 Predizione poco affidabile (bassa accuratezza modello)")
            }
        }
    }
    
    fun runTestEvaluation() {
        if (!trained || dataSplit == null) {
            println("❌ Modello non addestrato o dati non disponibili")
            return
        }
        
        println("\n🧪 VALUTAZIONE DETTAGLIATA SU TEST SET:")
        val testData = dataSplit!!.testData
        
        val predictions = testData.map { sample ->
            val prediction = network.predict(sample.inputs)
            val denormalizedPred = processor.denormalizeTarget(prediction.outputs[0])
            val denormalizedActual = processor.denormalizeTarget(sample.target)
            
            Triple(sample.originalRecord, denormalizedActual, denormalizedPred)
        }
        
        // Raggruppa per direzione
        val byDirection = predictions.groupBy { it.first.direzione }
        
        byDirection.forEach { (direzione, preds) ->
            val errors = preds.map { abs(it.second - it.third) }
            val mae = errors.average()
            val accuracy = preds.count { abs(it.second - it.third) <= 0.5 } * 100.0 / preds.size
            
            println("   $direzione:")
            println("     📊 Campioni: ${preds.size}")
            println("     📈 MAE: ${"%.3f".format(mae)}")
            println("     🎯 Accuracità (±0.5): ${"%.1f".format(accuracy)}%")
        }
        
        // Mostra alcuni esempi di predizioni
        println("\n   🔍 ESEMPI PREDIZIONI:")
        predictions.take(5).forEach { (record, actual, predicted) ->
            val date = LocalDate.parse(record.data).format(DateTimeFormatter.ofPattern("dd/MM"))
            val error = abs(actual - predicted)
            val status = if (error <= 0.5) "✅" else "❌"
            println("     $status $date ${record.direzione}: Reale ${"%.1f".format(actual)}, Previsto ${"%.1f".format(predicted)} (err: ${"%.2f".format(error)})")
        }
    }
}

fun main(args: Array<String>) {
    if (args.size < 4) {
        println("📋 USO:")
        println("  kotlin NeuralPredictorKt <file.json> <data> <temperatura> <direzione> [epoche] [test_ratio]")
        println("")
        println("🔧 PARAMETRI:")
        println("  file.json     - File JSON con dati storici")
        println("  data          - Data target (YYYY-MM-DD)")
        println("  temperatura   - Temperatura prevista (°C)")
        println("  direzione     - ANDATA o RITORNO")
        println("  epoche        - Numero epoche training (default: 1000)")
        println("  test_ratio    - Percentuale test set (default: 0.2)")
        println("")
        println("📌 ESEMPIO:")
        println("  kotlin NeuralPredictorKt dati.json 2025-06-10 25 ANDATA 1500 0.2")
        return
    }
    
    val jsonFile = args[0]
    val targetDate = args[1]
    val temperatura = args[2].toDoubleOrNull()
    val direzione = args[3].uppercase()
    val epochs = args.getOrNull(4)?.toIntOrNull() ?: 1000
    val testRatio = args.getOrNull(5)?.toDoubleOrNull() ?: 0.2
    
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
    
    if (testRatio <= 0 || testRatio >= 1) {
        println("❌ Test ratio deve essere tra 0 e 1")
        return
    }
    
    try {
        LocalDate.parse(targetDate)
    } catch (e: Exception) {
        println("❌ Data non valida. Usa formato YYYY-MM-DD")
        return
    }
    
    println("🚀 SISTEMA PREVISIONALE AUTOBUS - RETE NEURALE CON VALIDAZIONE")
    println("===============================================================")
    
    val system = BusPredictionSystem()
    
    try {
        val data = system.loadData(jsonFile)
        system.trainNetwork(data, epochs, testRatio, patience = 100)
        
        val result = system.predict(targetDate, temperatura, direzione)
        result?.let { 
            system.showPrediction(it, targetDate, temperatura, direzione)
        }
        
        // Mostra valutazione dettagliata
        system.runTestEvaluation()
        
        println("\n✨ Analisi completata!")
        
    } catch (e: Exception) {
        println("❌ Errore durante l'esecuzione: ${e.message}")
        e.printStackTrace()
    }
}