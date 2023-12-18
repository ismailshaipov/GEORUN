package com.example.georun.database

import java.time.LocalDateTime

data class Coordinates(
    val trackId: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class TrackSession(
    val sessionId: Long = 0,
    val startTime: LocalDateTime = LocalDateTime.now(),
    val endTime: LocalDateTime = LocalDateTime.now(),
    val distance: String
)