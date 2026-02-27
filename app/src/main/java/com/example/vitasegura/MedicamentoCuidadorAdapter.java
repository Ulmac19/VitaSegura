package com.example.vitasegura;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class MedicamentoCuidadorAdapter extends RecyclerView.Adapter<MedicamentoCuidadorAdapter.ViewHolder> {

    private List<Medicamento> lista;
    private Context context;
    private String uidAbuelo; // Necesario para saber de qué base de datos borrar

    public MedicamentoCuidadorAdapter(List<Medicamento> lista, Context context) {
        this.lista = lista;
        this.context = context;
    }

    public void setUidAbuelo(String uidAbuelo) {
        this.uidAbuelo = uidAbuelo;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medicamento_cuidador, parent, false);
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

        // Botón Eliminar
        holder.btnEliminar.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Eliminar Medicamento")
                    .setMessage("¿Estás seguro de que deseas eliminar " + med.getNombre() + "?")
                    .setPositiveButton("Eliminar", (dialog, which) -> {
                        if (uidAbuelo != null && med.getId() != null) {
                            DatabaseReference db = FirebaseDatabase.getInstance().getReference();
                            db.child("Usuarios").child(uidAbuelo).child("Medicamentos").child(med.getId()).removeValue()
                                    .addOnSuccessListener(aVoid -> Toast.makeText(context, "Eliminado", Toast.LENGTH_SHORT).show());
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        // Botón Editar
        holder.btnEditar.setOnClickListener(v -> {
            Intent intent = new Intent(context, FormularioMedicamentoActivity.class);
            intent.putExtra("UID_ABUELO", uidAbuelo);
            intent.putExtra("MEDICAMENTO_ID", med.getId());
            intent.putExtra("NOMBRE", med.getNombre());
            intent.putExtra("FRECUENCIA", med.getFrecuencia());
            intent.putExtra("HORA", med.getHora());
            intent.putExtra("DOSIS", med.getDosis());
            intent.putExtra("NOTAS", med.getNotas());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return lista.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvFrecuencia, tvHora, tvDosis, tvNotas;
        Button btnEditar, btnEliminar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tv_nombre_med);
            tvFrecuencia = itemView.findViewById(R.id.tv_frecuencia_med);
            tvHora = itemView.findViewById(R.id.tv_hora_med);
            tvDosis = itemView.findViewById(R.id.tv_dosis_med);
            tvNotas = itemView.findViewById(R.id.tv_notas_med);
            btnEditar = itemView.findViewById(R.id.btn_editar);
            btnEliminar = itemView.findViewById(R.id.btn_eliminar);
        }
    }
}