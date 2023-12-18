package com.example.georun.database

import android.content.Context
import android.location.Location
import java.time.LocalDateTime

class DatabaseRepository(context: Context) {

    private val dbHelper = DBHelper(context)

    fun startNewSession(): Long {
        //val sessionstart = System.currentTimeMillis()
        val sessionstart = LocalDateTime.now()
        return dbHelper.insertTrackSessionAndGetId(sessionstart)
    }

    fun endSession(sessionId: Long) {
        val endTime = LocalDateTime.now()
        val coordinates = dbHelper.getCoordinatesForSession(sessionId)
        val distance = calculateDistance(coordinates)
        dbHelper.updateTrackSessionEndTime(sessionId, endTime, distance)
    }

    fun calculateDistance(coordinates: List<Coordinates>): String {
        if (coordinates.size < 2) {
            // Недостаточно координат для расчета расстояния
            return "0"
        }

        var totalDistance = 0.0
        var prevLocation: Location? = null

        for (coordinate in coordinates) {
            val currentLocation = Location("current").apply {
                latitude = coordinate.latitude
                longitude = coordinate.longitude
            }

            if (prevLocation != null) {
                totalDistance += prevLocation.distanceTo(currentLocation).toDouble()
            }

            prevLocation = currentLocation
        }

        // Если расстояние меньше 1000 м, возвращаем его без десятичных знаков
        if (totalDistance < 1000) {
            return "${totalDistance.toInt()} м"
        } else {
            // Иначе возвращаем расстояние в километрах с двумя десятичными знаками
            return String.format("%.2f км", totalDistance / 1000.0)
        }
    }


    fun addCoordinatesToSession(sessionId: Long, location: Location): Boolean {
        val coordinates = Coordinates(
            trackId = sessionId,
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = LocalDateTime.now()
        )
        dbHelper.insertCoordinates(coordinates)
        return true
    }

    fun getAllTrackSessions(): List<TrackSession> {
        return dbHelper.getTrackSessions()
    }

    fun getCoordinatesForSession(sessionId: Long): List<Coordinates> {
        return dbHelper.getCoordinatesForSession(sessionId)
    }

    fun deleteSession(sessionId: Long) {
        dbHelper.deleteTrackSession(sessionId)
    }
}
