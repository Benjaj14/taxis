package com.taxisrodoviario.app.data

import com.google.gson.annotations.SerializedName

data class RecorridoPaginationResponse(
    @SerializedName("recorridos")
    val recorridos: List<Recorrido>,

    @SerializedName("totalRecorridos")
    val totalRecorridos: Int,

    @SerializedName("paginaActual")
    val paginaActual: Int,

    @SerializedName("totalPaginas")
    val totalPaginas: Int
)
