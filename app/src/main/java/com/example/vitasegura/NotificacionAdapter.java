package com.example.vitasegura;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * Adaptador de RecyclerView que muestra el historial de notificaciones.
 *
 * Resalta visualmente cada entrada según su tipo: las emergencias se pintan con
 * fondo rojo claro y texto rojo; los avisos informativos, con fondo blanco y
 * texto azul.
 */
public class NotificacionAdapter extends RecyclerView.Adapter<NotificacionAdapter.ViewHolder> {

    private List<Notificacion> listaNotificaciones;

    public NotificacionAdapter(List<Notificacion> listaNotificaciones) {
        this.listaNotificaciones = listaNotificaciones;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notificacion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notificacion noti = listaNotificaciones.get(position);

        holder.tvMensaje.setText(noti.getMensaje());
        holder.tvFecha.setText(noti.getFecha());
        holder.tvHora.setText(noti.getHora());

        // Estilo según el tipo: emergencia (rojo) o aviso informativo (azul)
        if (noti.isEsEmergencia()) {
            holder.llContenedor.setBackgroundColor(Color.parseColor("#FFF0F0"));
            holder.tvMensaje.setTextColor(Color.parseColor("#D32F2F"));
        } else {
            holder.llContenedor.setBackgroundColor(Color.parseColor("#FFFFFF"));
            holder.tvMensaje.setTextColor(Color.parseColor("#23608C"));
        }
    }

    @Override
    public int getItemCount() {
        return listaNotificaciones.size();
    }

    /** Reemplaza la lista de notificaciones y refresca la vista. */
    public void actualizarLista(List<Notificacion> nuevaLista) {
        this.listaNotificaciones = nuevaLista;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMensaje, tvFecha, tvHora;
        LinearLayout llContenedor;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMensaje = itemView.findViewById(R.id.tv_mensaje_notificacion);
            tvFecha = itemView.findViewById(R.id.tv_fecha_notificacion);
            tvHora = itemView.findViewById(R.id.tv_hora_notificacion);
            llContenedor = itemView.findViewById(R.id.ll_contenedor_notificacion);
        }
    }

}