package com.taxisrodoviario.app.network

import com.taxisrodoviario.app.data.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitClient(sessionManager: SessionManager) {

    companion object {
        // Usar 10.0.2.2 para que el emulador Android acceda al localhost de la máquina de desarrollo
        private const val BASE_URL = "http://10.0.2.2:3000/"
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // El AuthInterceptor ahora se crea aquí
    private val authInterceptor = AuthInterceptor(sessionManager)

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor) // Añadimos el interceptor de autenticación
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
