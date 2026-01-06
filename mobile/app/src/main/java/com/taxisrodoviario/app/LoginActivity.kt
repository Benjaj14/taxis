package com.taxisrodoviario.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.taxisrodoviario.app.data.auth.LoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar

    private val sessionManager by lazy { (application as MainApplication).sessionManager }
    private val apiService by lazy { (application as MainApplication).retrofitClient.apiService }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si ya hay una sesión activa, vamos directo a la actividad correspondiente
        if (sessionManager.fetchAuthToken() != null) {
            navigateToBasedOnRole()
            return
        }

        setContentView(R.layout.activity_login)
        
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        progressBar = findViewById(R.id.progress_bar)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, ingrese correo y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            performLogin(email, password)
        }
    }

    private fun performLogin(email: String, password: String) {
        showLoading(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.login(LoginRequest(email, password))
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        if (loginResponse != null) {
                            sessionManager.saveAuthToken(loginResponse.token)
                            sessionManager.saveUserRole(loginResponse.role)
                            Toast.makeText(this@LoginActivity, "Bienvenido ${loginResponse.email}", Toast.LENGTH_SHORT).show()
                            // Ahora, obtenemos el perfil para guardar el idvehiculo si es trabajador
                            if (loginResponse.role == "trabajador") {
                                fetchProfileAndNavigate()
                            } else {
                                navigateToBasedOnRole()
                            }
                        } else {
                            showError("Respuesta de login vacía")
                            showLoading(false)
                        }
                    } else {
                        showError("Credenciales incorrectas o usuario no encontrado")
                        showLoading(false)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("LoginActivity", "Error de red: ${e.message}", e)
                    showError("Error de conexión. Verifique su red.")
                    showLoading(false)
                }
            }
        }
    }

    private fun fetchProfileAndNavigate() {
        lifecycleScope.launch(Dispatchers.IO) {
            val token = sessionManager.fetchAuthToken()
            if (token == null) {
                showError("Error de autenticación. Intente de nuevo.")
                showLoading(false)
                return@launch
            }

            try {
                val cookie = "token=$token"
                val response = apiService.getProfile(cookie)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val profile = response.body()
                        if (profile?.idvehiculo != null) {
                            sessionManager.saveVehicleId(profile.idvehiculo)
                        }
                        navigateToBasedOnRole()
                    } else {
                        showError("No se pudo obtener el perfil del trabajador.")
                    }
                }
            } catch (e: Exception) {
                 withContext(Dispatchers.Main) {
                    Log.e("LoginActivity", "Error de red en perfil: ${e.message}", e)
                    showError("Error de conexión al obtener perfil.")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                }
            }
        }
    }


    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun navigateToBasedOnRole() {
        val role = sessionManager.fetchUserRole()
        val intent = when (role) {
            "admin" -> Intent(this, MainActivity::class.java)
            "trabajador" -> Intent(this, WorkerMapActivity::class.java)
            else -> {
                showError("Rol de usuario no reconocido. Contacte a soporte.")
                sessionManager.clearSession()
                null
            }
        }
        
        if (intent != null) {
            startActivity(intent)
            finish()
        }
    }
}
