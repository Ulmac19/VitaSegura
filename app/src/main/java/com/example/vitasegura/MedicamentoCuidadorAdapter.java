package com.example.vitasegura;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MedicamentoCuidadorAdapter extends RecyclerView.Adapter<MedicamentoCuidadorAdapter.ViewHolder> {

    private List<Medicamento> lista;
    private OnMedicamentoClickListener listener;

    // Interfaz para manejar los clics desde la Activity
    public interface OnMedicamentoClickListener {
        void onEditarClick(Medicamento medicamento);
        void onEliminarClick(Medicamento medicamento);
    }

    public MedicamentoCuidadorAdapter(List<Medicamento> lista, OnMedicamentoClickListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medicamento_cuidador, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Medicamento m = lista.get(position);
        holder.nombre.setText(m.getNombre());
        holder.frecuencia.setText(m.getFrecuencia());
        holder.hora.setText(m.getHora());
        holder.dosis.setText(m.getDosis());
        holder.notas.setText(m.getNotas());

        // Configurar botones de la tarjeta
        holder.btnEditar.setOnClickListener(v -> listener.onEditarClick(m));
        holder.btnEliminar.setOnClickListener(v -> listener.onEliminarClick(m));
    }

    @Override
    public int getItemCount() { return lista.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nombre, frecuencia, hora, dosis, notas;
        Button btnEditar, btnEliminar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nombre = itemView.findViewById(R.id.tv_nombre_med);
            frecuencia = itemView.findViewById(R.id.tv_frecuencia_med);
            hora = itemView.findViewById(R.id.tv_hora_med);
            dosis = itemView.findViewById(R.id.tv_dosis_med);
            notas = itemView.findViewById(R.id.tv_notas_med);
            btnEditar = itemView.findViewById(R.id.btn_editar);
            btnEliminar = itemView.findViewById(R.id.btn_eliminar);
        }
    }
}