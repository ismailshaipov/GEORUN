package com.example.georun.viewmodels

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.georun.database.Coordinates
import com.example.georun.database.DatabaseRepository
import com.example.georun.database.TrackSession
import com.example.georun.locating.Locator
import com.example.georun.locating.Locator.locationCallback
import com.example.georun.locating.Locator.locationRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val databaseRepository = DatabaseRepository(app.applicationContext)
    private var activeSessionId: Long? = null

    private val _sessions: MutableStateFlow<List<TrackSession>> = MutableStateFlow(emptyList())
    val sessions: StateFlow<List<TrackSession>> get()  = _sessions

    var showRequestDialog: Boolean by mutableStateOf(true)
    var updJob: Job? = null

    private val fusedLocationClient = LocationServices
        .getFusedLocationProviderClient(app.applicationContext)

    var requestLocationUpdates by mutableStateOf(true)

    private val _location: MutableStateFlow<Location?> = MutableStateFlow(null)
    val location: StateFlow<Location?> = _location

    init {
            loadSessions()
        // При создании ViewModel загружаем сессии
    }
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (isPermissionsGranted(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION, context = getApplication<Application>().applicationContext)) {
            val sessionId = databaseRepository.startNewSession()
            activeSessionId = sessionId
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

                    sessionId.let { sessionId ->
                        if (it != null) {
                            if (databaseRepository.addCoordinatesToSession(sessionId, it)) {
                                Log.d("MyTag", "Dobavlenie")
                            }
                        }
                    }
                }
            }

        }
    }

    fun stopLocationUpdates() {
        updJob?.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)

        // Завершаем текущую активную сессию
        activeSessionId?.let { sessionId ->
            databaseRepository.endSession(sessionId)
        }

        // Сброс активного идентификатора сессии
        activeSessionId = null
        loadSessions()
    }

    fun isPermissionsGranted(vararg permissions: String, context: Context) =
        permissions.fold(true) { acc, perm ->
            acc && context.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED
        }



    private fun loadSessions() {
        viewModelScope.launch {
            _sessions.value = withContext(Dispatchers.IO) {
                databaseRepository.getAllTrackSessions()
            }
        }
    }

    fun getCoordinatesForSession(sessionId: Long): List<Coordinates> {
        return databaseRepository.getCoordinatesForSession(sessionId)
    }

    fun deleteSession(sessionId: Long) {
        databaseRepository.deleteSession(sessionId)
        loadSessions()
    }
}
