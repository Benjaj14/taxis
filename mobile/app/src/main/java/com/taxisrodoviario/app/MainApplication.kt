package com.taxisrodoviario.app

import android.app.Application
import com.taxisrodoviario.app.data.SessionManager
import com.taxisrodoviario.app.network.RetrofitClient

class MainApplication : Application() {

    // La inicialización perezosa asegura que se creen solo cuando se necesiten por primera vez
    val sessionManager: SessionManager by lazy {
        SessionManager.getInstance(this)
    }

    val retrofitClient: RetrofitClient by lazy {
        RetrofitClient(sessionManager)
    }
}
