package com.example.vitasegura;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * Adaptador de RecyclerView que muestra el historial de ubicaciones del adulto
 * mayor.
 *
 * Cada elemento es pulsable: al hacer clic notifica al listener para que
 * UbicacionAdultoActivity centre el mapa en esa ubicación.
 */
public class HistorialUbicacionAdapter extends RecyclerView.Adapter<HistorialUbicacionAdapter.ViewHolder> {

    private List<Ubicacion> historial;
    private OnItemClickListener listener;

    /** Callback para notificar la selección de una ubicación del historial. */
    public interface OnItemClickListener {
        void onItemClick(Ubicacion ubicacion);
    }

    public HistorialUbicacionAdapter(List<Ubicacion> historial, OnItemClickListener listener) {
        this.historial = historial;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_historial_ubicacion, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Ubicacion u = historial.get(position);
        holder.tvDireccion.setText(u.getDireccion());
        holder.tvFecha.setText(u.getFechaHora());

        // Evento clic para mover el mapa
        holder.itemView.setOnClickListener(v -> listener.onItemClick(u));
    }

    @Override
    public int getItemCount() { return historial.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDireccion, tvFecha;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDireccion = itemView.findViewById(R.id.tv_direccion_historial);
            tvFecha = itemView.findViewById(R.id.tv_fecha_historial);
        }
    }
}