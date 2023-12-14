package com.example.georun.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.Instant
import java.time.ZoneId

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
                        "end_time  DATE" +
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
    fun insertTrackSession(trackSession: TrackSession) {
        with(writableDatabase) {
            beginTransaction()
            val values = ContentValues(2)
                values.put("start_time", trackSession.startTime.toString())
                values.put("end_time", trackSession.endTime.toString())
            insert("tracks", null, values)
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

    fun deleteTrackSession(sessionId: Int) {
        with(writableDatabase) {
            beginTransaction()
            delete("tracks", "_id = ?", arrayOf(sessionId.toString()))
            delete("coordinates","track_id = ?", arrayOf(sessionId.toString()))
            setTransactionSuccessful()
            endTransaction()
        }
    }

    fun getTrackSessions(): List<TrackSession> {
        val sessions = mutableListOf<TrackSession>()
        with(readableDatabase) {
            query(
                "tracks",
                arrayOf("_id","start_time", "end_time"),
                null,
                null,
                null,
                null,
                null
            ).let {
                while (it.moveToNext()) {
                    val sessionId = it.getInt(it.getColumnIndexOrThrow("_id"))
                    val startTime = Instant.parse(it.getString(it.getColumnIndexOrThrow("start_time")))
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
                    val endTime = Instant.parse(it.getString(it.getColumnIndexOrThrow("end_time")))
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
                    sessions.add(TrackSession(sessionId, startTime, endTime))
                }
                it.close()
            }
        }
        return sessions
    }

    fun getCoordinatesForSession(sessionId: Int): List<Coordinates> {
        val coordinates = mutableListOf<Coordinates>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM coordinates WHERE track_id = ?", arrayOf(sessionId.toString()))
        with(cursor) {
            while (moveToNext()) {
                val latitude = getDouble(getColumnIndexOrThrow("latitude"))
                val longitude = getDouble(getColumnIndexOrThrow("longitude"))
                val timestamp = Instant.parse(getString(getColumnIndexOrThrow(("timestamp"))))
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                coordinates.add(Coordinates(sessionId, latitude, longitude, timestamp))
            }
        }
        return coordinates
    }

}
