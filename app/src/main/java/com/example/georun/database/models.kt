package com.example.georun.database

import java.time.LocalDateTime

data class Coordinates(
    val trackId: Int,
    val latitude: Double,
    val longitude: Double,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class TrackSession(
    val sessionId: Int,
    val startTime: LocalDateTime = LocalDateTime.now(),
    val endTime: LocalDateTime = LocalDateTime.now()
)