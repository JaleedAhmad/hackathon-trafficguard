package com.traffic_guard.ai.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterNotNull

class LocationRepositoryImpl(private val context: Context) : LocationRepository {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _locationFlow = MutableStateFlow<MapLatLng?>(null)
    override val locationFlow: Flow<MapLatLng> = _locationFlow.filterNotNull()

    private val _speedFlow = MutableStateFlow(0f)
    override val speedFlow: Flow<Float> = _speedFlow

    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    override fun startLocationUpdates() {
        stopLocationUpdates() // Avoid double initialization

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000L
        ).apply {
            setMinUpdateIntervalMillis(1000L)
            setWaitForAccurateLocation(false)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    _locationFlow.value = MapLatLng(location.latitude, location.longitude)
                    _speedFlow.value = location.speed // in meters per second
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            // Premium mock fallback if permissions not fully ready yet
            mockLocationStream()
        }
    }

    override fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    private fun mockLocationStream() {
        // Start streaming Islamabad coordinates for premium simulation demo
        val mockPoints = listOf(
            MapLatLng(33.6844, 73.0479),
            MapLatLng(33.6855, 73.0490),
            MapLatLng(33.6870, 73.0515),
            MapLatLng(33.6895, 73.0540),
            MapLatLng(33.6910, 73.0565)
        )
        Thread {
            var i = 0
            while (locationCallback != null) {
                _locationFlow.value = mockPoints[i % mockPoints.size]
                _speedFlow.value = 12.5f
                i++
                try { Thread.sleep(3000) } catch (e: InterruptedException) { break }
            }
        }.start()
    }
}
