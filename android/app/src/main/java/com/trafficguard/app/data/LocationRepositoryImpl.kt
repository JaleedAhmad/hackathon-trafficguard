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
                    android.util.Log.i("LocationRepository", "Real-time location updated: ${location.latitude}, ${location.longitude}")
                    _locationFlow.value = MapLatLng(location.latitude, location.longitude)
                    _speedFlow.value = location.speed // in meters per second
                }
            }
        }

        try {
            val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (hasFine || hasCoarse) {
                android.util.Log.i("LocationRepository", "Location permissions verified. Getting last known location...")
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    location?.let {
                        android.util.Log.i("LocationRepository", "Last known location found: ${it.latitude}, ${it.longitude}")
                        _locationFlow.value = MapLatLng(it.latitude, it.longitude)
                        _speedFlow.value = it.speed
                    }
                }
                
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback!!,
                    Looper.getMainLooper()
                )
            } else {
                android.util.Log.w("LocationRepository", "No location permissions, falling back to mock stream")
                mockLocationStream()
            }
        } catch (e: Exception) {
            android.util.Log.e("LocationRepository", "Location updates start failed, using mock: ${e.message}")
            mockLocationStream()
        }
    }

    override val currentLocation: MapLatLng?
        get() = _locationFlow.value

    override fun isLocationEnabled(): Boolean {
        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return false

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        return isGpsEnabled || isNetworkEnabled
    }

    override fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    private fun mockLocationStream() {
        // Mock stream removed. Relying strictly on real GPS location from FusedLocationProviderClient.
    }
}
