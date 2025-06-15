package com.example

import java.time.LocalDateTime

data class Appointment(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val startTime: LocalDateTime? = null,
    val endTime: LocalDateTime? = null,
    val location: String? = null,
    val participants: List<String>? = null,
    val status: String? = null,
    val notes: String? = null
) 