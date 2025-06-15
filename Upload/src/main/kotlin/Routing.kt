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
import java.io.File

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        
        // Endpoint per ottenere la lista dei file in /home/ema/upload con numerazione
        get("/files") {
            try {
                val uploadDir = File("/home/ema/upload")
                
                if (!uploadDir.exists() || !uploadDir.isDirectory) {
                    call.respond(HttpStatusCode.NotFound, "Directory not found")
                    return@get
                }
                
                val files = uploadDir.listFiles()?.filter { it.isFile }?.map { it.name } ?: emptyList()
                val indexedFiles = files.mapIndexed { index, filename ->
                    mapOf("index" to index, "filename" to filename)
                }
                call.respond(indexedFiles)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error reading directory: ${e.message}")
            }
        }
        
        // Endpoint per scaricare un file specifico (per nome o per indice)
        get("/download/{identifier}") {
            val identifier = call.parameters["identifier"]
            
            if (identifier.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "File identifier is required")
                return@get
            }
            
            try {
                val uploadDir = File("/home/ema/upload")
                
                if (!uploadDir.exists() || !uploadDir.isDirectory) {
                    call.respond(HttpStatusCode.NotFound, "Upload directory not found")
                    return@get
                }
                
                val files = uploadDir.listFiles()?.filter { it.isFile } ?: emptyList()
                
                val targetFile = if (identifier.toIntOrNull() != null) {
                    // L'identifier è un numero - cerca per indice
                    val index = identifier.toInt()
                    if (index < 0 || index >= files.size) {
                        call.respond(HttpStatusCode.NotFound, "File index out of range")
                        return@get
                    }
                    files[index]
                } else {
                    // L'identifier è un nome file - cerca per nome
                    files.find { it.name == identifier }
                }
                
                if (targetFile == null || !targetFile.exists()) {
                    call.respond(HttpStatusCode.NotFound, "File not found")
                    return@get
                }
                
                // Verifica che il file sia effettivamente nella directory upload (sicurezza)
                if (!targetFile.canonicalPath.startsWith("/home/ema/upload")) {
                    call.respond(HttpStatusCode.Forbidden, "Access denied")
                    return@get
                }
                
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, targetFile.name).toString()
                )
                
                call.respondFile(targetFile)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error downloading file: ${e.message}")
            }
        }
        
        // Static plugin. Try to access `/static/index.html`
        staticResources("/static", "static")
    }
}