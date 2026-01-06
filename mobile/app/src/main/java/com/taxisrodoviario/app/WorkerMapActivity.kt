package com.taxisrodoviario.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.navigation.NavigationView
import com.taxisrodoviario.app.data.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkerMapActivity : AppCompatActivity(), OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {

    private lateinit var googleMap: GoogleMap
    private var currentMarker: Marker? = null
    private var currentPolyline: Polyline? = null
    private val currentPath = mutableListOf<LatLng>()
    private lateinit var localBroadcastManager: LocalBroadcastManager

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var startShiftButton: Button
    private lateinit var toggle: ActionBarDrawerToggle

    private var isShiftActive = false

    private val sessionManager: SessionManager by lazy { (application as MainApplication).sessionManager }
    private val apiService by lazy { (application as MainApplication).retrofitClient.apiService }

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                when (it.action) {
                    LocationService.ACTION_LOCATION_UPDATE -> {
                        val latitude = it.getDoubleExtra(LocationService.EXTRA_LATITUDE, 0.0)
                        val longitude = it.getDoubleExtra(LocationService.EXTRA_LONGITUDE, 0.0)
                        val location = Location("").apply {
                            this.latitude = latitude
                            this.longitude = longitude
                        }
                        Log.d(TAG, "WorkerMap: Ubicación recibida. Lat: $latitude, Lon: $longitude")
                        updateMap(location)
                    }
                    LocationService.ACTION_TRIP_FINALIZED -> {
                        Toast.makeText(this@WorkerMapActivity, "Viaje finalizado por inactividad.", Toast.LENGTH_SHORT).show()
                        clearMap()
                    }
                }
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                // Permisos recién concedidos. Si el turno está activo, iniciar el servicio.
                if (isShiftActive) {
                    startLocationService()
                }
            } else {
                Toast.makeText(this, "Permisos de ubicación denegados. El mapa no puede funcionar.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_worker_map)
        
        val toolbar: Toolbar = findViewById(R.id.worker_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Mi Ruta"

        if (sessionManager.fetchUserRole() != "trabajador") {
            logout()
            return
        }

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        startShiftButton = findViewById(R.id.start_shift_button)

        toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        navigationView.setNavigationItemSelectedListener(this)

        startShiftButton.setOnClickListener { startShift() }

        setupMap()
        // La verificación de permisos se moverá a onMapReady para asegurar que el mapa esté listo.
        localBroadcastManager = LocalBroadcastManager.getInstance(this)

        // Restaurar estado del turno
        isShiftActive = sessionManager.isShiftActive()
        updateUiForShiftState()
        fetchVehicleDetailsAndSetTitle() // Obtener y mostrar la patente en el título
    }

    private fun fetchVehicleDetailsAndSetTitle() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Para un trabajador, getVehiculos() devuelve una lista con solo su vehículo
                val response = apiService.getVehiculos()
                if (response.isSuccessful && response.body()?.isNotEmpty() == true) {
                    val vehicle = response.body()!!.first()
                    withContext(Dispatchers.Main) {
                        supportActionBar?.title = "Mi Ruta - ${vehicle.patente}"
                    }
                } else {
                    Log.w(TAG, "No se pudo obtener la patente del vehículo para el título.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener detalles del vehículo: ", e)
            }
        }
    }

    private fun startShift() {
        if (isShiftActive) return
        
        isShiftActive = true
        sessionManager.saveShiftState(true)
        updateUiForShiftState()
        Toast.makeText(this, "Turno iniciado. Buscando ubicación...", Toast.LENGTH_SHORT).show()
        checkLocationPermissions() // Esto ahora manejará el inicio del servicio
    }

    private fun endShift() {
        if (!isShiftActive) return

        isShiftActive = false
        sessionManager.saveShiftState(false)
        updateUiForShiftState()
        stopLocationService()
        Toast.makeText(this, "Turno finalizado.", Toast.LENGTH_SHORT).show()
        clearMap() 
    }
    
    private fun updateUiForShiftState() {
        val navMenu = navigationView.menu
        if (isShiftActive) {
            startShiftButton.visibility = View.GONE
            navMenu.findItem(R.id.nav_end_shift).isVisible = true
        } else {
            startShiftButton.visibility = View.VISIBLE
            navMenu.findItem(R.id.nav_end_shift).isVisible = false
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_end_shift -> {
                endShift()
            }
            R.id.action_logout -> {
                if (isShiftActive) {
                    Toast.makeText(this, "Debes finalizar tu turno antes de cerrar sesión.", Toast.LENGTH_LONG).show()
                } else {
                    logout()
                }
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun logout() {
        if (isShiftActive) {
            Toast.makeText(this, "Por favor, finaliza tu turno primero.", Toast.LENGTH_SHORT).show()
            return
        }
        stopLocationService()
        sessionManager.clearSession()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(LocationService.ACTION_LOCATION_UPDATE)
            addAction(LocationService.ACTION_TRIP_FINALIZED)
        }
        localBroadcastManager.registerReceiver(locationReceiver, filter)
    }

    override fun onPause() {
        localBroadcastManager.unregisterReceiver(locationReceiver)
        super.onPause()
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.worker_map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true
        // Ahora que el mapa está listo, verificamos permisos.
        // Si el turno está activo, esto iniciará el servicio.
        checkLocationPermissions()
    }

    private fun updateMap(location: Location) {
        if (!::googleMap.isInitialized) return

        val latLng = LatLng(location.latitude, location.longitude)

        if (currentMarker == null) {
            currentMarker = googleMap.addMarker(MarkerOptions().position(latLng).title("Mi Ubicación"))
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
        } else {
            currentMarker?.position = latLng
        }

        currentPath.add(latLng)
        if (currentPolyline == null) {
            currentPolyline = googleMap.addPolyline(PolylineOptions().addAll(currentPath).width(10f).color(Color.BLUE))
        } else {
            currentPolyline?.points = currentPath
        }

        if (isShiftActive) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng))
        }
    }
    
    private fun clearMap() {
        currentPath.clear()
        currentMarker?.remove()
        currentMarker = null
        currentPolyline?.remove()
        currentPolyline = null
        googleMap.clear() // Limpia marcadores, polilíneas, etc. del mapa
    }


    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            // Si ya tenemos permisos, y el turno está activo, iniciamos el servicio.
            if (isShiftActive) {
                startLocationService()
            }
        }
    }

    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
    
    private fun stopLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        stopService(serviceIntent)
    }
    
    companion object {
        private const val TAG = "WorkerMapActivity"
    }
}
