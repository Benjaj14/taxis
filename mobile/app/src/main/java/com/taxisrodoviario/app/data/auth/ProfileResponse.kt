package com.taxisrodoviario.app.data.auth

import com.google.gson.annotations.SerializedName

data class ProfileResponse(
    @SerializedName("idtrabajador")
    val id: Int?,

    @SerializedName("nombre")
    val nombre: String?,
    
    @SerializedName("email")
    val email: String,

    @SerializedName("role")
    val role: String,

    @SerializedName("idvehiculo")
    val idvehiculo: Int?
)
