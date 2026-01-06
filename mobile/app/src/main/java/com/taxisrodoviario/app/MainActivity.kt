package com.taxisrodoviario.app

import android.Manifest
import android.app.AlertDialog
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
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.CalendarView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.navigation.NavigationView
import com.taxisrodoviario.app.data.Recorrido
import com.taxisrodoviario.app.data.Vehiculo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    // Views
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var topAppBar: MaterialToolbar
    private lateinit var historyNavigationView: NavigationView
    private lateinit var calendarView: CalendarView
    private lateinit var rvHistoryRecorridos: RecyclerView
    private lateinit var recorridoAdapter: RecorridoAdapter
    private lateinit var btnLoadMoreRecorridos: Button
    private lateinit var pbLoadMoreHistory: ProgressBar
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var mainNavigationView: NavigationView
    private lateinit var drawerToggle: ActionBarDrawerToggle

    // State & Data
    private var vehicleList: List<Vehiculo> = emptyList()
    private var selectedVehicle: Vehiculo? = null
    private var isShowingHistoricRoute = false
    private lateinit var selectedCalendar: Calendar

    // Paging State
    private var currentPage = 1
    private var totalPages = 1
    private var isLoadingMore = false
    private val recordsPerPage = 20

    // Map State
    private lateinit var googleMap: GoogleMap
    private var currentMarker: Marker? = null
    private var currentPolyline: Polyline? = null
    private var historicPolyline: Polyline? = null
    private val currentPath = mutableListOf<LatLng>()
    private lateinit var localBroadcastManager: LocalBroadcastManager

    // Services & Managers
    private val sessionManager by lazy { (application as MainApplication).sessionManager }
    private val apiService by lazy { (application as MainApplication).retrofitClient.apiService }

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            if (action == LocationService.ACTION_LOCATION_UPDATE) {
                val latitude = intent.getDoubleExtra(LocationService.EXTRA_LATITUDE, 0.0)
                val longitude = intent.getDoubleExtra(LocationService.EXTRA_LONGITUDE, 0.0)
                val location = Location("").apply {
                    this.latitude = latitude
                    this.longitude = longitude
                }
                updateMap(location)
            }
        }
    }

    private val requestPermissionLauncher = 
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                // startLocationService() // Removed
            } else {
                Toast.makeText(this, "Permisos de ubicación denegados.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isUserAuthenticated()) return

        setContentView(R.layout.activity_main)

        setupUI()
        setupMap()
        checkLocationPermissions()
        fetchVehiculos()

        localBroadcastManager = LocalBroadcastManager.getInstance(this)
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(LocationService.ACTION_LOCATION_UPDATE)
            // addAction(LocationService.ACTION_TRIP_FINALIZED) // Removed
        }
        localBroadcastManager.registerReceiver(locationReceiver, filter)
    }

    override fun onPause() {
        localBroadcastManager.unregisterReceiver(locationReceiver)
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.top_app_bar_menu, menu)
        menu.findItem(R.id.action_close_history)?.isVisible = isShowingHistoricRoute
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        return when (item.itemId) {
            R.id.action_logout -> {
                logout()
                true
            }
            R.id.action_close_history -> {
                isShowingHistoricRoute = false
                historicPolyline?.remove()
                historicPolyline = null
                invalidateOptionsMenu() 
                Toast.makeText(this, "Vista histórica cerrada.", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupUI() {
        topAppBar = findViewById(R.id.topAppBar)
        setSupportActionBar(topAppBar)
        supportActionBar?.title = "Seleccionar Vehículo"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        drawerLayout = findViewById(R.id.drawer_layout)
        historyNavigationView = findViewById(R.id.history_navigation_view)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        mainNavigationView = findViewById(R.id.main_navigation_view)

        // --- Setup Hamburger Menu (Main Drawer) ---
        drawerToggle = ActionBarDrawerToggle(this, drawerLayout, topAppBar, R.string.drawer_open, R.string.drawer_close)
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        mainNavigationView.inflateMenu(R.menu.main_drawer_menu)
        mainNavigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_manage_vehicles -> {
                    startActivity(Intent(this, VehicleManagementActivity::class.java))
                }
                R.id.nav_manage_workers -> {
                    startActivity(Intent(this, WorkerManagementActivity::class.java))
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            item.isChecked = false // Desmarcar el item
            true
        }

        // --- History Drawer ---
        val headerView = LayoutInflater.from(this).inflate(R.layout.drawer_history, historyNavigationView, false)
        historyNavigationView.addHeaderView(headerView)
        calendarView = headerView.findViewById(R.id.calendar_view_history)
        rvHistoryRecorridos = headerView.findViewById(R.id.rv_history_recorridos)
        btnLoadMoreRecorridos = headerView.findViewById(R.id.btn_load_more_recorridos)
        pbLoadMoreHistory = headerView.findViewById(R.id.pb_load_more_history)
        recorridoAdapter = RecorridoAdapter(mutableListOf()) { drawHistoricRecorrido(it) }
        rvHistoryRecorridos.layoutManager = LinearLayoutManager(this)
        rvHistoryRecorridos.adapter = recorridoAdapter
        
        // --- Bottom Navigation ---
        bottomNavigationView.menu.findItem(R.id.nav_history).isEnabled = false
        bottomNavigationView.menu.findItem(R.id.nav_info).isVisible = false
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_vehicles -> showVehicleSelectionDialog()
                R.id.nav_history -> drawerLayout.openDrawer(GravityCompat.END)
                R.id.nav_info -> showVehicleInfoDialog()
            }
            true
        }
        
        // --- Listeners ---
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            resetAndFetchRecorridos(calendar)
        }

        btnLoadMoreRecorridos.setOnClickListener {
            if (currentPage < totalPages && !isLoadingMore) {
                currentPage++
                fetchRecorridosForDate(selectedCalendar, false)
            }
        }
    }

    private fun showVehicleSelectionDialog() {
        val dialog = BottomSheetDialog(this, R.style.ThemeOverlay_App_BottomSheetDialog)
        val dialogView = layoutInflater.inflate(R.layout.bottom_sheet_vehicle_list, null)
        dialog.setContentView(dialogView)

        val rvVehiclesDialog: RecyclerView = dialogView.findViewById(R.id.rvVehicles)
        rvVehiclesDialog.layoutManager = LinearLayoutManager(this)
        
        // Creamos el VehicleAdapter con la lista de vehículos ya cargada y el layout oscuro
        val vehicleAdapter = VehicleAdapter(this.vehicleList, R.layout.item_vehicle_dark) { vehicle ->
            selectedVehicle = vehicle
            supportActionBar?.title = "Vehículo: ${vehicle.patente}"
            bottomNavigationView.menu.findItem(R.id.nav_history).isEnabled = true
            bottomNavigationView.menu.findItem(R.id.nav_info).isVisible = true
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            
            resetAndFetchRecorridos(Calendar.getInstance())
            Toast.makeText(this, "Vehículo ${vehicle.patente} seleccionado.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        rvVehiclesDialog.adapter = vehicleAdapter
        
        dialog.show()
    }
    
    private fun showVehicleInfoDialog() {
        selectedVehicle?.let { vehicle ->
            AlertDialog.Builder(this)
                .setTitle("Información del Vehículo")
                .setMessage("Patente: ${vehicle.patente}\nMarca: ${vehicle.marca}\nModelo: ${vehicle.modelo}")
                .setPositiveButton("Aceptar", null)
                .show()
        } ?: run {
            Toast.makeText(this, "Selecciona un vehículo primero.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchVehiculos() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getVehiculos()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        vehicleList = response.body() ?: emptyList()
                        Log.d(TAG, "${vehicleList.size} vehículos cargados.")
                    } else {
                        Log.e(TAG, "Error en la API de vehículos: ${response.code()}")
                        Toast.makeText(this@MainActivity, "Error al cargar vehículos.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Error de conexión en vehículos: ${e.message}", e)
                    Toast.makeText(this@MainActivity, "Error de conexión al cargar vehículos.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
    }

    private fun updateMap(location: Location) {
        if (isShowingHistoricRoute) return

        val latLng = LatLng(location.latitude, location.longitude)
        
        currentMarker?.remove()
        currentMarker = googleMap.addMarker(MarkerOptions().position(latLng).title("Ubicación actual"))
        
        currentPath.add(latLng)
        currentPolyline?.remove()
        currentPolyline = googleMap.addPolyline(PolylineOptions().addAll(currentPath).color(Color.BLUE))
        
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
    }

    private fun resetAndFetchRecorridos(calendar: Calendar) {
        currentPage = 1
        totalPages = 1
        selectedCalendar = calendar
        recorridoAdapter.updateRecorridos(mutableListOf())
        fetchRecorridosForDate(calendar, true)
    }

    private fun fetchRecorridosForDate(calendar: Calendar, isInitialLoad: Boolean) {
        val vehicleId = selectedVehicle?.idvehiculo ?: return
        if (!isInitialLoad && currentPage >= totalPages) {
            updatePaginationUI()
            return
        }

        isLoadingMore = true
        updatePaginationUI()

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formattedDate = sdf.format(calendar.time)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getRecorridos(vehicleId, formattedDate, currentPage, recordsPerPage)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val paginationResponse = response.body()
                        if (paginationResponse != null && paginationResponse.recorridos.isNotEmpty()) {
                            if (isInitialLoad) recorridoAdapter.updateRecorridos(paginationResponse.recorridos)
                            else recorridoAdapter.addRecorridos(paginationResponse.recorridos)
                            totalPages = paginationResponse.totalPaginas
                        } else if (isInitialLoad) {
                            Toast.makeText(this@MainActivity, "No se encontraron recorridos.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Error al obtener recorridos.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Error de conexión: ${e.message}", e)
                    Toast.makeText(this@MainActivity, "Error de conexión.", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isLoadingMore = false
                    updatePaginationUI()
                }
            }
        }
    }
    
    private fun updatePaginationUI() {
        pbLoadMoreHistory.visibility = if (isLoadingMore) View.VISIBLE else View.GONE
        btnLoadMoreRecorridos.visibility = if (!isLoadingMore && currentPage < totalPages) View.VISIBLE else View.GONE
    }

    private fun drawHistoricRecorrido(recorrido: Recorrido) {
        isShowingHistoricRoute = true
        historicPolyline?.remove()
        invalidateOptionsMenu() 
        drawerLayout.closeDrawer(GravityCompat.END)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getPosicionesPorRecorrido(recorrido.idrecorrido)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val posiciones = response.body()
                        if (posiciones != null && posiciones.size > 1) {
                            val points = posiciones.map { LatLng(it.latitud, it.longitud) }
                            historicPolyline = googleMap.addPolyline(PolylineOptions().addAll(points).width(12f).color(Color.RED))
                            val bounds = LatLngBounds.builder().apply { points.forEach { include(it) } }.build()
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                        } else {
                            Toast.makeText(this@MainActivity, "No hay suficientes puntos para este recorrido.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Error al obtener la ruta.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Excepción al obtener la ruta: ${e.message}", e)
                    Toast.makeText(this@MainActivity, "Error al obtener la ruta.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }


    
    private fun isUserAuthenticated(): Boolean {
        if (sessionManager.fetchAuthToken() == null) {
            navigateToLogin()
            return false
        }
        if (sessionManager.fetchUserRole() != "admin") {
            navigateToWorker()
            return false
        }
        return true
    }

    private fun logout() {
        sessionManager.clearSession()
        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToWorker() {
        val intent = Intent(this, WorkerMapActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}