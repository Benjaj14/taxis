package com.taxisrodoviario.app

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.taxisrodoviario.app.data.Trabajador
import com.taxisrodoviario.app.data.Vehiculo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkerManagementActivity : AppCompatActivity() {

    private lateinit var rvWorkers: RecyclerView
    private lateinit var workerAdapter: WorkerAdapter
    private val apiService by lazy { (application as MainApplication).retrofitClient.apiService }
    private var workerList = mutableListOf<Trabajador>()
    private var vehicleList = listOf<Vehiculo>() // Cache for the spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_worker_management)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar_worker_management)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rvWorkers = findViewById(R.id.rv_manage_workers)
        rvWorkers.layoutManager = LinearLayoutManager(this)

        workerAdapter = WorkerAdapter(workerList) { worker ->
            showCreateOrEditWorkerDialog(worker)
        }
        rvWorkers.adapter = workerAdapter

        findViewById<FloatingActionButton>(R.id.fab_add_worker).setOnClickListener {
            showCreateOrEditWorkerDialog(null)
        }

        fetchAllData()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun fetchAllData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Fetch both workers and vehicles
                val workersResponse = apiService.getTrabajadores()
                val vehiclesResponse = apiService.getVehiculos()

                withContext(Dispatchers.Main) {
                    if (workersResponse.isSuccessful) {
                        workerList.clear()
                        workerList.addAll(workersResponse.body() ?: emptyList())
                        workerAdapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this@WorkerManagementActivity, "Error al cargar trabajadores", Toast.LENGTH_SHORT).show()
                    }

                    if (vehiclesResponse.isSuccessful) {
                        vehicleList = vehiclesResponse.body() ?: emptyList()
                    } else {
                        Toast.makeText(this@WorkerManagementActivity, "Error al cargar vehículos para el selector", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@WorkerManagementActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showCreateOrEditWorkerDialog(worker: Trabajador?) {
        if (vehicleList.isEmpty()) {
            Toast.makeText(this, "No hay vehículos para asignar. Crea un vehículo primero.", Toast.LENGTH_LONG).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_create_worker, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_worker_name)
        val etEmail = dialogView.findViewById<EditText>(R.id.et_worker_email)
        val etPassword = dialogView.findViewById<EditText>(R.id.et_worker_password)
        val spinnerVehicles = dialogView.findViewById<Spinner>(R.id.spinner_assign_vehicle)

        // Setup Spinner
        val vehiclePatents = vehicleList.map { it.patente }
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, vehiclePatents)
        spinnerVehicles.adapter = spinnerAdapter

        val dialogTitle = if (worker == null) "Crear Trabajador" else "Editar Trabajador"
        
        if (worker != null) {
            etName.setText(worker.nombre)
            etEmail.setText(worker.email)
            // etPassword.hint = "Dejar en blanco para no cambiar" // Removido a petición del usuario
            // Select current vehicle in spinner
            val vehiclePosition = vehicleList.indexOfFirst { it.idvehiculo == worker.idvehiculo }
            if (vehiclePosition != -1) {
                spinnerVehicles.setSelection(vehiclePosition)
            }
        }

        AlertDialog.Builder(this)
            .setTitle(dialogTitle)
            .setView(dialogView)
            .setPositiveButton("Guardar") { dialog, _ ->
                val name = etName.text.toString()
                val email = etEmail.text.toString()
                val password = etPassword.text.toString()
                
                if (name.isBlank() || email.isBlank() || (password.isBlank() && worker == null)) {
                    Toast.makeText(this, "Nombre, email y contraseña son requeridos para crear.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val selectedVehicle = vehicleList[spinnerVehicles.selectedItemPosition]
                val vehicleId = selectedVehicle.idvehiculo.toString()
                
                if (worker == null) {
                    createWorker(name, email, password, vehicleId)
                } else {
                    val dataToUpdate = mutableMapOf(
                        "nombre" to name,
                        "email" to email,
                        "idvehiculo" to vehicleId
                    )
                    if (password.isNotBlank()) {
                        dataToUpdate["password"] = password
                    }
                    updateWorker(worker.idtrabajador, dataToUpdate)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun createWorker(name: String, email: String, pass: String, vehicleId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val workerData = mapOf("nombre" to name, "email" to email, "password" to pass, "idvehiculo" to vehicleId)
                val response = apiService.createTrabajador(workerData)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@WorkerManagementActivity, "Trabajador creado", Toast.LENGTH_SHORT).show()
                        fetchAllData() // Recargar la lista
                    } else {
                        val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                        Toast.makeText(this@WorkerManagementActivity, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@WorkerManagementActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateWorker(workerId: Int, workerData: Map<String, String>) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.updateTrabajador(workerId, workerData)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@WorkerManagementActivity, "Trabajador actualizado", Toast.LENGTH_SHORT).show()
                        fetchAllData() // Recargar la lista
                    } else {
                        val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                        Toast.makeText(this@WorkerManagementActivity, "Error al actualizar: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@WorkerManagementActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
