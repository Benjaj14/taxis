package com.taxisrodoviario.app.network

import com.taxisrodoviario.app.data.*
import com.taxisrodoviario.app.data.auth.LoginRequest
import com.taxisrodoviario.app.data.auth.LoginResponse
import com.taxisrodoviario.app.data.auth.ProfileResponse
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/profile")
    suspend fun getProfile(@Header("Cookie") token: String): Response<ProfileResponse>

    // Vehiculos
    @GET("vehiculos")
    suspend fun getVehiculos(): Response<List<Vehiculo>>

    @POST("vehiculos")
    suspend fun crearVehiculo(@Body vehicleData: Map<String, String>): Response<Vehiculo>

    @PUT("vehiculos/{id}")
    suspend fun updateVehiculo(@Path("id") vehicleId: Int, @Body vehicleData: Map<String, String>): Response<Vehiculo>

    // Recorridos
    @GET("recorridos")
    suspend fun getRecorridos(
        @Query("idvehiculo") idVehiculo: Int,
        @Query("fecha") fecha: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): Response<RecorridoPaginationResponse>

    @POST("recorridos")
    suspend fun crearRecorrido(@Body body: Map<String, Int>): Response<Recorrido>

    @PUT("recorridos/{id}")
    suspend fun cerrarRecorrido(@Path("id") recorridoId: Int, @Body body: Map<String, Float>): Response<Recorrido>
    
    // Posiciones
    @POST("posiciones")
    suspend fun insertPosicion(@Body posicion: Posicion): Response<Unit>

    @GET("posiciones/recorrido/{id}")
    suspend fun getPosicionesPorRecorrido(@Path("id") recorridoId: Int): Response<List<PosicionSimple>>

    // Trabajadores (Admin)
    @GET("api/trabajadores")
    suspend fun getTrabajadores(): Response<List<Trabajador>>

    @POST("api/register")
    suspend fun createTrabajador(@Body workerData: Map<String, String>): Response<Trabajador>

    @PUT("api/trabajadores/{id}")
    suspend fun updateTrabajador(@Path("id") workerId: Int, @Body workerData: Map<String, String>): Response<Trabajador>

    @DELETE("api/trabajadores/{id}")
    suspend fun deleteTrabajador(@Path("id") workerId: Int): Response<Unit>
}
