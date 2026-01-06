package com.taxisrodoviario.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.taxisrodoviario.app.data.Trabajador

class WorkerAdapter(
    private var workers: List<Trabajador>,
    val onWorkerClick: (Trabajador) -> Unit
) : RecyclerView.Adapter<WorkerAdapter.WorkerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_worker, parent, false)
        return WorkerViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkerViewHolder, position: Int) {
        holder.bind(workers[position], onWorkerClick)
    }

    override fun getItemCount() = workers.size

    fun updateWorkers(newWorkers: List<Trabajador>) {
        workers = newWorkers
        notifyDataSetChanged()
    }

    class WorkerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_worker_name)
        private val tvEmail: TextView = itemView.findViewById(R.id.tv_worker_email)

        fun bind(worker: Trabajador, onWorkerClick: (Trabajador) -> Unit) {
            tvName.text = worker.nombre
            tvEmail.text = worker.email
            itemView.setOnClickListener { onWorkerClick(worker) }
        }
    }
}
