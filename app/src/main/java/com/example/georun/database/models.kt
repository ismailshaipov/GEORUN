package com.example.georun.database

import java.time.LocalDateTime

data class Coordinates(
    val trackId: Long = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class TrackSession(
    val sessionId: Long = 0,
    val startTime: LocalDateTime = LocalDateTime.now(),
    val endTime: LocalDateTime = LocalDateTime.now(),
    val distance: String = ""
)