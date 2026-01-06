package com.taxisrodoviario.app.data.auth

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("id")
    val id: Int,

    @SerializedName("email")
    val email: String,

    @SerializedName("role")
    val role: String,

    @SerializedName("token")
    val token: String
)
