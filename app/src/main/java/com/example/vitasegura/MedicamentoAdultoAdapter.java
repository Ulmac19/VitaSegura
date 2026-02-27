package com.example.vitasegura;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MedicamentoAdultoAdapter extends RecyclerView.Adapter<MedicamentoAdultoAdapter.ViewHolder> {

    private List<Medicamento> lista;

    public MedicamentoAdultoAdapter(List<Medicamento> lista) {
        this.lista = lista;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflamos tu diseño sin botones
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medicamento_abuelo, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Medicamento med = lista.get(position);

        holder.tvNombre.setText(med.getNombre());
        holder.tvFrecuencia.setText(med.getFrecuencia());
        holder.tvHora.setText(med.getHora());
        holder.tvDosis.setText(med.getDosis());
        holder.tvNotas.setText(med.getNotas());
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvFrecuencia, tvHora, tvDosis, tvNotas;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tv_nombre_med);
            tvFrecuencia = itemView.findViewById(R.id.tv_frecuencia_med);
            tvHora = itemView.findViewById(R.id.tv_hora_med);
            tvDosis = itemView.findViewById(R.id.tv_dosis_med);
            tvNotas = itemView.findViewById(R.id.tv_notas_med);
        }
    }
}