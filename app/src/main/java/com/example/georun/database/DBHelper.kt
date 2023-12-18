package com.example.georun.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DBHelper(context: Context) : SQLiteOpenHelper(
    context,
    "GeoDB",
    null,
    1
) {
    override fun onCreate(db: SQLiteDatabase?) {
        db?.let { database ->
            database.beginTransaction()
            database.execSQL(
                "CREATE TABLE tracks (" +
                        "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "start_time  DATE," +
                        "end_time  DATE," +
                        "distance TEXT"+
                        ")"
            )

            database.execSQL(
                "CREATE TABLE coordinates (" +
                        "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "track_id INTEGER," +
                        "latitude REAL," +
                        "longitude REAL," +
                        "timestamp DATE," +
                        "FOREIGN KEY(track_id) REFERENCES tracks(_id)" +
                        ")"
            )
            database.setTransactionSuccessful()
            database.endTransaction()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS tracks")
        db.execSQL("DROP TABLE IF EXISTS coordinates")
        onCreate(db)
    }

    @SuppressLint("SuspiciousIndentation")
    fun insertTrackSessionAndGetId(startTime: LocalDateTime): Long {
        with(writableDatabase) {
            beginTransaction()
            val values = ContentValues(1)
            values.put("start_time", startTime.toString())
            val id = insert("tracks", null, values)
            setTransactionSuccessful()
            endTransaction()
            return id
        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun updateTrackSessionEndTime(sessionId: Long, endTime: LocalDateTime, distance: String) {
        with(writableDatabase) {
            beginTransaction()
            val values = ContentValues(2)
                values.put("end_time", endTime.toString())
                values.put("distance", distance)
            update("tracks", values, "_id = ?", arrayOf(sessionId.toString()))
            setTransactionSuccessful()
            endTransaction()
        }
    }

    fun deleteTrackSession(sessionId: Long) {
        with(writableDatabase) {
            beginTransaction()
            delete("tracks", "_id = ?", arrayOf(sessionId.toString()))
            delete("coordinates","track_id = ?", arrayOf(sessionId.toString()))
            setTransactionSuccessful()
            endTransaction()
        }
    }
    fun insertCoordinates(coordinates: Coordinates) {
        with(writableDatabase) {
            beginTransaction()
            val values = ContentValues(4)
            values.put("track_id", coordinates.trackId)
            values.put("latitude", coordinates.latitude)
            values.put("longitude", coordinates.longitude)
            values.put("timestamp", coordinates.timestamp.toString())
            insert("coordinates", null, values)
            setTransactionSuccessful()
            endTransaction()
        }
    }

    fun getTrackSessions(): List<TrackSession> {
        val sessions = mutableListOf<TrackSession>()
        with(readableDatabase) {
            query(
                "tracks",
                arrayOf("_id","start_time", "end_time","distance"),
                null,
                null,
                null,
                null,
                null
            ).let {
                while (it.moveToNext()) {
                    val sessionId = it.getLong(it.getColumnIndexOrThrow("_id"))
                    val startTimeString = it.getString(it.getColumnIndexOrThrow("start_time"))
                    val endTimeString = it.getString(it.getColumnIndexOrThrow("end_time"))

                    // Проверка на null перед парсингом
                    if (startTimeString != null && endTimeString != null) {
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")

                        val zone = ZoneId.systemDefault()

                        val startTime = LocalDateTime.parse(startTimeString, formatter)
                            .atZone(zone)
                            .toLocalDateTime()

                        val endTime = LocalDateTime.parse(endTimeString, formatter)
                            .atZone(zone)
                            .toLocalDateTime()

                        val distance = it.getString(it.getColumnIndexOrThrow("distance"))
                        sessions.add(TrackSession(sessionId, startTime, endTime, distance))
                    } else {
                        Log.d("MyTag", "NullSes")
                    }
                }
                it.close()
            }

        }

        return sessions
    }


    fun getCoordinatesForSession(trackSessionId: Long): List<Coordinates> {
        val coordinates = mutableListOf<Coordinates>()

        with(readableDatabase) {
            query(
                "coordinates",
                arrayOf("_id","track_id", "latitude","longitude","timestamp"),
                "track_id = ?",
                arrayOf(trackSessionId.toString()),
                null,
                null,
                null
            ).let {
                while (it.moveToNext()) {
                val latitude = it.getDouble(it.getColumnIndexOrThrow("latitude"))
                val longitude = it.getDouble(it.getColumnIndexOrThrow("longitude"))
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
                val starttimestamp = it.getString(it.getColumnIndexOrThrow("timestamp"))
                val timestamp = LocalDateTime.parse(starttimestamp, formatter)
                coordinates.add(Coordinates(trackSessionId, latitude, longitude, timestamp))
            }
            it.close()
            }
        }

        return coordinates
    }
}