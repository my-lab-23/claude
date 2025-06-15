package com.example

import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class LocalDateTimeAdapter : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_DATE_TIME

    override fun serialize(src: LocalDateTime?, typeOfSrc: java.lang.reflect.Type?, context: com.google.gson.JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.format(formatter))
    }

    override fun deserialize(json: JsonElement?, typeOfT: java.lang.reflect.Type?, context: com.google.gson.JsonDeserializationContext?): LocalDateTime {
        try {
            return LocalDateTime.parse(json?.asString, formatter)
        } catch (e: DateTimeParseException) {
            println("Errore nel parsing della data: ${json?.asString}")
            println("Formato atteso: yyyy-MM-ddTHH:mm:ss")
            throw e
        }
    }
} 