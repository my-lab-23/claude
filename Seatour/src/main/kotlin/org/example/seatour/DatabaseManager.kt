package org.example.seatour

// DatabaseManager.kt
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import com.google.gson.*
import java.lang.reflect.Type

class DatabaseManager {
    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
        .registerTypeAdapter(LocalTime::class.java, LocalTimeAdapter())
        .setPrettyPrinting()
        .create()

    private val dataFile = File("/home/ema/annotazioni_bus.json")

    fun salvaAnnotazione(annotazione: AnnotazioneAffollamento): Boolean {
        val annotazioni = caricaAnnotazioni().toMutableList()

        // Controlla se esiste già un'annotazione per oggi nella stessa direzione
        if (annotazioni.any { it.data == annotazione.data && it.direzione == annotazione.direzione }) {
            return false // Duplicato trovato per la stessa direzione
        }

        annotazioni.add(annotazione)

        try {
            dataFile.writeText(gson.toJson(annotazioni))
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun caricaAnnotazioni(): List<AnnotazioneAffollamento> {
        if (!dataFile.exists()) {
            return emptyList()
        }

        return try {
            val json = dataFile.readText()
            val type = object : TypeToken<List<AnnotazioneAffollamento>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun haAnnotazioneOggi(direzione: String): Boolean {
        val oggi = LocalDate.now()
        return caricaAnnotazioni().any { it.data == oggi && it.direzione == direzione }
    }
}

// Adapters per serializzazione JSON

class LocalDateAdapter : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
    override fun serialize(src: LocalDate?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src.toString())
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDate {
        return LocalDate.parse(json!!.asString)
    }
}

class LocalTimeAdapter : JsonSerializer<LocalTime>, JsonDeserializer<LocalTime> {
    override fun serialize(src: LocalTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src.toString())
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalTime {
        return LocalTime.parse(json!!.asString)
    }
}