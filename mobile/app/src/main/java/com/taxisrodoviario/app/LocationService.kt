package com.taxisrodoviario.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import com.taxisrodoviario.app.data.Posicion
import com.taxisrodoviario.app.data.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.*

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var currentRecorridoId: Int? = null
    private val recorridoMutex = Mutex()
    private var lastLocation: Location? = null
    
    private val stopHandler = Handler(Looper.getMainLooper())
    private lateinit var stopRunnable: Runnable
    private val STOP_DELAY_MS = 1 * 60 * 1000L // 1 minuto para la finalización automática

    private val apiService by lazy { (application as MainApplication).retrofitClient.apiService }
    private val sessionManager by lazy { (application as MainApplication).sessionManager }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: El servicio está siendo creado.")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        stopRunnable = Runnable {
            Log.d(TAG, "TEMPORIZADOR: Detención prolongada detectada. Finalizando recorrido.")
            finalizarRecorridoActual()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: El servicio está siendo iniciado.")

        // Verificar si el turno está activo. Si no, detener el servicio inmediatamente.
        if (!sessionManager.isShiftActive()) {
            Log.d(TAG, "onStartCommand: Turno inactivo detectado en SessionManager. Deteniendo servicio de ubicación.")
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val notificationIntent = Intent(this, WorkerMapActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Taxis Rodoviario - Turno Activo")
            .setContentText("Detectando viajes y enviando ubicación...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
        
        currentRecorridoId = null
        lastLocation = null
        
        startLocationUpdates()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "Location Service Channel", NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(serviceChannel)
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(10000)
            .setMaxUpdateDelayMillis(20000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    if (location.accuracy < MIN_ACCURACY_METERS) {
                        sendLocationToServer(location)
                    } else {
                        Log.w(TAG, "Ubicación descartada por baja precisión. Precisión: ${location.accuracy}m")
                    }
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e(TAG, "No se tienen permisos de ubicación.", e)
            stopSelf()
        }
    }
    
    private fun hasMoved(newLocation: Location): Boolean {
        val last = lastLocation
        if (last == null) {
            return false
        }
        val distance = last.distanceTo(newLocation)
        Log.d(TAG, "hasMoved: Distancia calculada: $distance metros.")
        return distance > 10 // Umbral de movimiento
    }

    private fun sendLocationToServer(newLocation: Location) {
        coroutineScope.launch {
            recorridoMutex.withLock {
                val moved = hasMoved(newLocation)
                
                if (currentRecorridoId == null) {
                    if (moved) {
                        try {
                            val vehiculoId = sessionManager.fetchVehicleId()
                            if (vehiculoId == null) {
                                Log.e(TAG, "No se puede crear recorrido. ID de vehículo no encontrado en sesión.")
                                lastLocation = newLocation // Actualizamos para el próximo cálculo de movimiento
                                return@withLock
                            }
                            
                            val response = apiService.crearRecorrido(mapOf("idvehiculo" to vehiculoId))
                            if (response.isSuccessful && response.body() != null) {
                                currentRecorridoId = response.body()!!.idrecorrido
                                Log.d(TAG, "ÉXITO. Nuevo recorrido creado con ID: $currentRecorridoId")
                                resetStopTimer()
                                enviarPosicion(newLocation, currentRecorridoId!!)
                                broadcastLocation(newLocation)
                            } else {
                                Log.e(TAG, "FALLO al crear recorrido: ${response.errorBody()?.string()}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "EXCEPCIÓN al crear recorrido.", e)
                        }
                    }
                } else { // Recorrido ya existente
                    if (moved) {
                        resetStopTimer()
                    }
                    enviarPosicion(newLocation, currentRecorridoId!!)
                    broadcastLocation(newLocation)
                }
                lastLocation = newLocation
            }
        }
    }

    private fun enviarPosicion(location: Location, recorridoId: Int) {
        coroutineScope.launch {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val timestamp = sdf.format(Date())
                val posicion = Posicion(recorridoId, location.latitude, location.longitude, timestamp)
                
                val response = apiService.insertPosicion(posicion)
                if (response.isSuccessful) {
                    Log.d(TAG, "enviarPosicion: Ubicación enviada para el recorrido ID: $recorridoId")
                } else {
                    Log.e(TAG, "enviarPosicion: Error al enviar la ubicación: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "enviarPosicion: Excepción al preparar la ubicación.", e)
            }
        }
    }

    private fun broadcastLocation(location: Location) {
        val intent = Intent(ACTION_LOCATION_UPDATE).apply {
            putExtra(EXTRA_LATITUDE, location.latitude)
            putExtra(EXTRA_LONGITUDE, location.longitude)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun finalizarRecorridoActual() {
        if (currentRecorridoId == null) return

        coroutineScope.launch {
            recorridoMutex.withLock {
                val tripId = currentRecorridoId ?: return@withLock
                
                try {
                    val kmRecorridos = 0.0f 
                    val response = apiService.cerrarRecorrido(tripId, mapOf("km" to kmRecorridos))
                    if (response.isSuccessful) {
                        Log.d(TAG, "finalizarRecorrido: Recorrido ID: $tripId finalizado correctamente.")
                    } else {
                        Log.e(TAG, "finalizarRecorrido: Error al finalizar: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "finalizarRecorrido: Excepción al finalizar.", e)
                } finally {
                    currentRecorridoId = null
                    stopHandler.removeCallbacks(stopRunnable)
                    // Enviar broadcast para notificar a la UI que el viaje terminó
                    val intent = Intent(ACTION_TRIP_FINALIZED)
                    LocalBroadcastManager.getInstance(this@LocationService).sendBroadcast(intent)
                    Log.d(TAG, "finalizarRecorrido: Enviada broadcast de finalización de recorrido.")
                }
            }
        }
    }

    private fun resetStopTimer() {
        stopHandler.removeCallbacks(stopRunnable)
        if (currentRecorridoId != null) {
            Log.d(TAG, "resetStopTimer: Reiniciando temporizador de detención.")
            stopHandler.postDelayed(stopRunnable, STOP_DELAY_MS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: El servicio está siendo destruido.")
        fusedLocationClient.removeLocationUpdates(locationCallback)
        finalizarRecorridoActual() // Intenta finalizar cualquier recorrido abierto
        stopHandler.removeCallbacks(stopRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "LocationService"
        private const val CHANNEL_ID = "LocationServiceChannel"
        private const val MIN_ACCURACY_METERS = 50f

        const val ACTION_LOCATION_UPDATE = "com.taxisrodoviario.app.LOCATION_UPDATE"
        const val ACTION_TRIP_FINALIZED = "com.taxisrodoviario.app.TRIP_FINALIZED"
        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"
    }
}
