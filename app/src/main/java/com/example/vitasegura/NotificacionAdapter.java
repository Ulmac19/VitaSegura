package com.example.vitasegura;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class NotificacionAdapter extends RecyclerView.Adapter<NotificacionAdapter.ViewHolder> {

    private List<Notificacion> lista;

    public NotificacionAdapter(List<Notificacion> lista) {
        this.lista = lista;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notificacion, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notificacion n = lista.get(position);

        // Sincronización total de datos
        holder.tvMensaje.setText(n.getMensaje());
        holder.tvHora.setText(n.getHora());
        holder.tvFecha.setText(n.getFecha());

        // Color azul oscuro para el cuerpo del mensaje
        holder.tvMensaje.setTextColor(android.graphics.Color.parseColor("#23608C"));
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public void actualizarLista(List<Notificacion> nuevaLista) {
        this.lista = nuevaLista;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // Se agregó tvFecha para que coincida con el XML
        TextView tvMensaje, tvHora, tvFecha;
        LinearLayout contenedor;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMensaje = itemView.findViewById(R.id.tv_mensaje_notificacion);
            tvHora = itemView.findViewById(R.id.tv_hora_notificacion);
            tvFecha = itemView.findViewById(R.id.tv_fecha_notificacion);
            contenedor = itemView.findViewById(R.id.ll_contenedor_notificacion);
        }
    }
}