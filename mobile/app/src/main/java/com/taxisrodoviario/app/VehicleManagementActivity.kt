package com.taxisrodoviario.app

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.taxisrodoviario.app.data.Vehiculo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VehicleManagementActivity : AppCompatActivity() {

    private lateinit var rvVehicles: RecyclerView
    private lateinit var vehicleAdapter: VehicleAdapter
    private val apiService by lazy { (application as MainApplication).retrofitClient.apiService }
    private var vehicleList = mutableListOf<Vehiculo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehicle_management)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar_vehicle_management)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rvVehicles = findViewById(R.id.rv_manage_vehicles)
        rvVehicles.layoutManager = LinearLayoutManager(this)

        vehicleAdapter = VehicleAdapter(vehicleList, R.layout.item_vehicle) { vehicle ->
            // Aquí se podría manejar el clic en un vehículo para editarlo
            showCreateOrEditVehicleDialog(vehicle)
        }
        rvVehicles.adapter = vehicleAdapter

        findViewById<FloatingActionButton>(R.id.fab_add_vehicle).setOnClickListener {
            showCreateOrEditVehicleDialog(null)
        }

        fetchVehicles()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun fetchVehicles() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getVehiculos()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val newVehicles = response.body() ?: emptyList()
                        vehicleList.clear()
                        vehicleList.addAll(newVehicles)
                        vehicleAdapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this@VehicleManagementActivity, "Error al cargar vehículos", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VehicleManagementActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showCreateOrEditVehicleDialog(vehicle: Vehiculo?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_vehicle, null)
        val etPatente = dialogView.findViewById<EditText>(R.id.et_patente)
        val etMarca = dialogView.findViewById<EditText>(R.id.et_marca)
        val etModelo = dialogView.findViewById<EditText>(R.id.et_modelo)

        val dialogTitle = if (vehicle == null) "Crear Vehículo" else "Editar Vehículo"
        
        if (vehicle != null) {
            etPatente.setText(vehicle.patente)
            etPatente.isEnabled = false // No permitir editar la patente
            etMarca.setText(vehicle.marca)
            etModelo.setText(vehicle.modelo)
        }

        AlertDialog.Builder(this)
            .setTitle(dialogTitle)
            .setView(dialogView)
            .setPositiveButton("Guardar") { dialog, _ ->
                val patente = etPatente.text.toString()
                val marca = etMarca.text.toString()
                val modelo = etModelo.text.toString()

                if (patente.isBlank() || marca.isBlank() || modelo.isBlank()) {
                    Toast.makeText(this, "Todos los campos son requeridos", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (vehicle == null) {
                    createVehicle(patente, marca, modelo)
                } else {
                    updateVehicle(vehicle.idvehiculo, marca, modelo)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun createVehicle(patente: String, marca: String, modelo: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.crearVehiculo(mapOf("patente" to patente, "marca" to marca, "modelo" to modelo))
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@VehicleManagementActivity, "Vehículo creado", Toast.LENGTH_SHORT).show()
                        fetchVehicles() // Recargar la lista
                    } else {
                        val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                        Toast.makeText(this@VehicleManagementActivity, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VehicleManagementActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateVehicle(vehicleId: Int, marca: String, modelo: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.updateVehiculo(vehicleId, mapOf("marca" to marca, "modelo" to modelo))
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@VehicleManagementActivity, "Vehículo actualizado", Toast.LENGTH_SHORT).show()
                        fetchVehicles() // Recargar la lista
                    } else {
                        val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                        Toast.makeText(this@VehicleManagementActivity, "Error al actualizar: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VehicleManagementActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
