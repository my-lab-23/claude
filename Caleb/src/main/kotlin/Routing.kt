package com.example

import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.event.*

fun Application.configureRouting() {
    val appointmentService = AppointmentService()
    
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        
        post("/appointments") {
            try {
                val appointment = call.receive<Appointment>()
                val savedAppointment = appointmentService.saveAppointment(appointment)
                call.respond(HttpStatusCode.Created, savedAppointment)
            } catch (e: Exception) {
                println("Errore dettagliato: ${e.message}")
                println("Stack trace: ${e.stackTraceToString()}")
                call.respond(HttpStatusCode.BadRequest, "Errore nella gestione della richiesta: ${e.message}")
            }
        }
        
        // Static plugin. Try to access `/static/index.html`
        staticResources("/static", "static")
    }
}
