package com.taxisrodoviario.app.data

import com.google.gson.annotations.SerializedName

data class PosicionSimple(
    @SerializedName("latitud")
    val latitud: Double,
    @SerializedName("longitud")
    val longitud: Double
)
