package com.taxisrodoviario.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.taxisrodoviario.app.data.Vehiculo

class VehicleAdapter(
    private var vehicles: List<Vehiculo>,
    private val layoutId: Int, // ID del layout a usar (ej. R.layout.item_vehicle)
    var onVehicleClick: (Vehiculo) -> Unit
) : RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false) // Usa el layoutId proporcionado
        return VehicleViewHolder(view)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        val vehicle = vehicles[position]
        holder.bind(vehicle, onVehicleClick)
    }

    override fun getItemCount() = vehicles.size

    fun updateVehicles(newVehicles: List<Vehiculo>) {
        vehicles = newVehicles
        notifyDataSetChanged()
    }

    class VehicleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPatente: TextView = itemView.findViewById(R.id.tvPatente)

        fun bind(vehicle: Vehiculo, onVehicleClick: (Vehiculo) -> Unit) {
            tvPatente.text = vehicle.patente
            itemView.setOnClickListener { onVehicleClick(vehicle) }
        }
    }
}
