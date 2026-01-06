package com.taxisrodoviario.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.taxisrodoviario.app.data.Recorrido
import java.text.SimpleDateFormat
import java.util.Locale

class RecorridoAdapter(
    private var recorridos: MutableList<Recorrido>, // Cambiado a MutableList
    private val onRecorridoClick: (Recorrido) -> Unit
) : RecyclerView.Adapter<RecorridoAdapter.RecorridoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecorridoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recorrido, parent, false)
        return RecorridoViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecorridoViewHolder, position: Int) {
        holder.bind(recorridos[position], onRecorridoClick)
    }

    override fun getItemCount() = recorridos.size

    // Reemplaza la lista completa (usado al cambiar de vehículo o fecha)
    fun updateRecorridos(newRecorridos: List<Recorrido>) {
        this.recorridos.clear()
        this.recorridos.addAll(newRecorridos)
        notifyDataSetChanged()
    }

    // Añade recorridos al final de la lista (usado para paginación)
    fun addRecorridos(newRecorridos: List<Recorrido>) {
        val startPosition = this.recorridos.size
        this.recorridos.addAll(newRecorridos)
        notifyItemRangeInserted(startPosition, newRecorridos.size)
    }

    class RecorridoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvInicio: TextView = itemView.findViewById(R.id.tv_recorrido_inicio)
        private val tvDuracion: TextView = itemView.findViewById(R.id.tv_recorrido_duracion)
        private val tvKm: TextView = itemView.findViewById(R.id.tv_recorrido_km)

        fun bind(recorrido: Recorrido, onRecorridoClick: (Recorrido) -> Unit) {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            
            try {
                val date = inputFormat.parse(recorrido.inicio)
                tvInicio.text = "Inicio: ${outputFormat.format(date)}"
            } catch (e: Exception) {
                tvInicio.text = "Inicio: N/A"
            }

            tvDuracion.text = "Duración: ${recorrido.duracion ?: 0} min"
            tvKm.text = "Distancia: ${recorrido.km ?: 0.0f} km"
            
            itemView.setOnClickListener { onRecorridoClick(recorrido) }
        }
    }
}
