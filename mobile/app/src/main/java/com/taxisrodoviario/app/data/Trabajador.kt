package com.taxisrodoviario.app.data

import com.google.gson.annotations.SerializedName

// Representa la información de un trabajador que se obtiene de la API
data class Trabajador(
    @SerializedName("idtrabajador")
    val idtrabajador: Int,
    
    @SerializedName("nombre")
    val nombre: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("idvehiculo")
    val idvehiculo: Int?
)
