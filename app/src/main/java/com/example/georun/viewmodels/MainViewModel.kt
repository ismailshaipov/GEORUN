package com.example.georun.viewmodels
import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.georun.database.Coordinates
import com.example.georun.database.DBHelper
import com.example.georun.database.TrackSession
import com.example.georun.locating.Locator
import com.example.georun.locating.Locator.locationCallback
import com.example.georun.locating.Locator.locationRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val dbHelper = DBHelper(app)

    var trackSessionsWithCoordinates: List<Pair<TrackSession, List<Coordinates>>> by mutableStateOf(emptyList())

    var showRequestDialog: Boolean by mutableStateOf(true)
    var updJob: Job? = null

    private val fusedLocationClient = LocationServices
        .getFusedLocationProviderClient(app.applicationContext)

    var requestLocationUpdates by mutableStateOf(true)

    private val _location: MutableStateFlow<Location?> = MutableStateFlow(null)
    val location: StateFlow<Location?> = _location

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (isPermissionsGranted(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION, context = getApplication<Application>().applicationContext)) {
            fusedLocationClient.lastLocation.addOnCompleteListener {
                viewModelScope.launch {
                    _location.emit(it.result)
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    )
                }
            }

            updJob = viewModelScope.launch {
                Locator.location.collect {
                    _location.emit(it)
                }
            }

        }
    }

    fun stopLocationUpdates() {
        updJob?.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    fun isPermissionsGranted(vararg permissions: String, context: Context) =
        permissions.fold(true) { acc, perm ->
            acc && context.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED
        }

    fun addNewSession(session: TrackSession) {
        dbHelper.insertTrackSession(session)
    }

    fun addCoordinatesToSession(coordinates: Coordinates) {
        dbHelper.insertCoordinates(coordinates)
    }

    fun deleteSession(sessionId: Int) {
        dbHelper.deleteTrackSession(sessionId)
    }

    fun getAllSessions(): List<TrackSession> {
        return dbHelper.getTrackSessions()
    }

    fun getCoordinatesForSession(sessionId: Int): List<Coordinates> {
        return dbHelper.getCoordinatesForSession(sessionId)
    }

    fun loadTrackSessionsWithCoordinates() {
        val sessions = dbHelper.getTrackSessions()
        trackSessionsWithCoordinates = sessions.map { session ->
            val coordinates = dbHelper.getCoordinatesForSession(session.sessionId)
            Pair(session, coordinates)
        }
    }
}
