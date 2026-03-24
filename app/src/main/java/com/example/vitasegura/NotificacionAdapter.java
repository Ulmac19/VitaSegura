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

        // Colores según el tipo (Emergencia = fondo rojo clarito, Info = fondo blanco)
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

    // Actualiza la lista de notificaciones
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