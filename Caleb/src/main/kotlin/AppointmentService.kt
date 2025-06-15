package com.example

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.io.File
import java.time.LocalDateTime

class AppointmentService {
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .create()
    private val dataDirectory = createDataDirectory()
    private val appointmentsFile = File(dataDirectory, "appointments.json")

    private fun createDataDirectory(): File {
        val directory = File(System.getProperty("user.home"), "Caleb")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    private fun loadAppointments(): MutableList<Appointment> {
        if (!appointmentsFile.exists()) {
            return mutableListOf()
        }
        try {
            val type = object : TypeToken<MutableList<Appointment>>() {}.type
            return gson.fromJson(appointmentsFile.readText(), type) ?: mutableListOf()
        } catch (e: JsonSyntaxException) {
            println("Errore nel parsing del file JSON: ${e.message}")
            println("Dettaglio errore: ${e.cause?.message}")
            return mutableListOf()
        }
    }

    fun saveAppointment(appointment: Appointment): Appointment {
        try {
            // Validazione dei campi
            validateAppointment(appointment)

            // Genera un ID univoco se non presente
            val appointmentWithId = if (appointment.id == null) {
                appointment.copy(id = java.util.UUID.randomUUID().toString())
            } else {
                appointment
            }

            // Ricarica gli appuntamenti dal file
            val appointments = loadAppointments()

            // Verifica se l'appuntamento esiste già
            val existingIndex = appointments.indexOfFirst { it.id == appointmentWithId.id }
            if (existingIndex != -1) {
                appointments[existingIndex] = appointmentWithId
            } else {
                appointments.add(appointmentWithId)
            }

            // Salva su file
            appointmentsFile.writeText(gson.toJson(appointments))
            
            return appointmentWithId
        } catch (e: Exception) {
            println("Errore durante il salvataggio dell'appuntamento: ${e.message}")
            println("Stack trace: ${e.stackTraceToString()}")
            throw e
        }
    }

    private fun validateAppointment(appointment: Appointment) {
        val errors = mutableListOf<String>()

        if (appointment.startTime != null && appointment.endTime != null) {
            if (appointment.startTime.isAfter(appointment.endTime)) {
                errors.add("La data di inizio non può essere successiva alla data di fine")
            }
        }

        if (appointment.participants != null && appointment.participants.isEmpty()) {
            errors.add("La lista dei partecipanti non può essere vuota")
        }

        if (errors.isNotEmpty()) {
            throw IllegalArgumentException("Errori di validazione: ${errors.joinToString(", ")}")
        }
    }

    fun getAppointment(id: String): Appointment? {
        return loadAppointments().find { it.id == id }
    }

    fun getAllAppointments(): List<Appointment> {
        return loadAppointments().toList()
    }

    fun deleteAppointment(id: String): Boolean {
        val appointments = loadAppointments()
        val removed = appointments.removeIf { it.id == id }
        if (removed) {
            appointmentsFile.writeText(gson.toJson(appointments))
        }
        return removed
    }
} 