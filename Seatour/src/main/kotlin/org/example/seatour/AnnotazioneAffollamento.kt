package org.example.seatour

// AnnotazioneAffollamento.kt
import java.time.LocalDate
import java.time.LocalTime

data class AnnotazioneAffollamento(
    val data: LocalDate,
    val orario: LocalTime,
    val livelloAffollamento: Int, // 1-5
    val direzione: String, // "ANDATA" o "RITORNO"
    val linea: String = "Linea Bus"
)
