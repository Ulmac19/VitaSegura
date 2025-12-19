package com.example.vitasegura;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MedicamentoAdapter extends RecyclerView.Adapter<MedicamentoAdapter.ViewHolder> {
    private List<Medicamento> lista;

    public MedicamentoAdapter(List<Medicamento> lista) { this.lista = lista; }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medicamento_abuelo, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Medicamento m = lista.get(position);
        holder.nombre.setText(m.getNombre());
        holder.frecuencia.setText(m.getFrecuencia());
        holder.hora.setText(m.getHora());
        holder.dosis.setText(m.getDosis());
        holder.notas.setText(m.getNotas());
    }

    @Override
    public int getItemCount() { return lista.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nombre, frecuencia, hora, dosis, notas;
        public ViewHolder(View itemView) {
            super(itemView);
            nombre = itemView.findViewById(R.id.tv_nombre_med);
            frecuencia = itemView.findViewById(R.id.tv_frecuencia_med);
            hora = itemView.findViewById(R.id.tv_hora_med);
            dosis = itemView.findViewById(R.id.tv_dosis_med);
            notas = itemView.findViewById(R.id.tv_notas_med);
        }
    }
}