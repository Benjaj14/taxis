package com.taxisrodoviario.app.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private var prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "auth_prefs"
        private const val USER_TOKEN = "user_token"
        private const val USER_ROLE = "user_role"
        private const val VEHICLE_ID = "vehicle_id"
        private const val SHIFT_ACTIVE = "shift_active"
        private const val SHIFT_RECORRIDO_ID = "shift_recorrido_id"


        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SessionManager(context)
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Guarda el token de autenticación.
     */
    fun saveAuthToken(token: String) {
        val editor = prefs.edit()
        editor.putString(USER_TOKEN, token)
        editor.apply()
    }

    /**
     * Obtiene el token de autenticación.
     */
    fun fetchAuthToken(): String? {
        return prefs.getString(USER_TOKEN, null)
    }

    /**
     * Guarda el rol del usuario.
     */
    fun saveUserRole(role: String) {
        val editor = prefs.edit()
        editor.putString(USER_ROLE, role)
        editor.apply()
    }

    /**
     * Obtiene el rol del usuario.
     */
    fun fetchUserRole(): String? {
        return prefs.getString(USER_ROLE, null)
    }
    
    /**
     * Guarda el ID del vehículo asociado al trabajador.
     */
    fun saveVehicleId(vehicleId: Int) {
        val editor = prefs.edit()
        editor.putInt(VEHICLE_ID, vehicleId)
        editor.apply()
    }

    /**
     * Obtiene el ID del vehículo asociado al trabajador.
     */
    fun fetchVehicleId(): Int? {
        val id = prefs.getInt(VEHICLE_ID, -1)
        return if (id != -1) id else null
    }

    /**
     * Guarda el estado del turno del conductor.
     */
    fun saveShiftState(isActive: Boolean, recorridoId: Int? = null) {
        val editor = prefs.edit()
        editor.putBoolean(SHIFT_ACTIVE, isActive)
        if (isActive && recorridoId != null) {
            editor.putInt(SHIFT_RECORRIDO_ID, recorridoId)
        } else {
            editor.remove(SHIFT_RECORRIDO_ID)
        }
        editor.apply()
    }

    /**
     * Devuelve true si el turno está activo.
     */
    fun isShiftActive(): Boolean {
        return prefs.getBoolean(SHIFT_ACTIVE, false)
    }

    /**
     * Obtiene el ID del recorrido del turno actual.
     */
    fun getShiftRecorridoId(): Int? {
        val id = prefs.getInt(SHIFT_RECORRIDO_ID, -1)
        return if (id != -1) id else null
    }


    /**
     * Limpia todos los datos de la sesión.
     */
    fun clearSession() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}
