package com.taxisrodoviario.app.data

import com.google.gson.annotations.SerializedName

data class Vehiculo(
    @SerializedName("idvehiculo")
    val idvehiculo: Int,
    @SerializedName("patente")
    val patente: String,
    @SerializedName("marca")
    val marca: String?,
    @SerializedName("modelo")
    val modelo: String?
)