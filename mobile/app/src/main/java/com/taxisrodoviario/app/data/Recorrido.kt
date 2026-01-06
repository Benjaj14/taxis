package com.taxisrodoviario.app.data

import com.google.gson.annotations.SerializedName

data class Recorrido(
    @SerializedName("idrecorrido")
    val idrecorrido: Int,
    
    @SerializedName("idvehiculo")
    val idvehiculo: Int,
    
    @SerializedName("inicio")
    val inicio: String,
    
    @SerializedName("fin")
    val fin: String?,
    
    @SerializedName("km")
    val km: Float?,
    
    @SerializedName("duracion")
    val duracion: Int?,
    
    @SerializedName("estado")
    val estado: String
)
