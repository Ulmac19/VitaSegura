package com.example.vitasegura;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
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
        holder.tvDosis.setText(med.getDosis());
        holder.tvNotas.setText(med.getNotas());

        String freq = med.getFrecuencia();

        //Validacion LOGICA: Es para dolor o normal
        if(freq != null && freq.equals("Solo si hay dolor")){
            //Textos adaptados para el dolor
            holder.tvFrecuencia.setText("Solo en caso de dolor");
            holder.tvHora.setText("Libre");

            //Aparecer el boton de tomar
            holder.btnTomarDolor.setVisibility(View.VISIBLE);

            // Acción al presionar el botón
            holder.btnTomarDolor.setOnClickListener(v -> {
                enviarAvisoTomaDolor(med.getNombre(), v.getContext());
            });
        }else {
            // Es un medicamento normal con horario
            holder.tvFrecuencia.setText(freq); // Mostrará "Cada 8 horas" etc.
            holder.tvHora.setText(med.getHora());

            // Ocultamos el botón para que no lo presionen por error
            holder.btnTomarDolor.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    //Metodo para enviar aviso de tomar medicamento
    private void enviarAvisoTomaDolor(String nombreMed, Context context) {
        if(FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String miUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference refAlertas = FirebaseDatabase.getInstance().getReference()
                .child("Usuarios").child(miUid).child("EmergenciasPendientes");

        HashMap<String, Object> aviso = new HashMap<>();
        aviso.put("mensaje", "Tu familiar acaba de tomar: " + nombreMed + " (Para el dolor)");
        aviso.put("tipo", "INFO_MED");
        aviso.put("timestamp", System.currentTimeMillis());

        refAlertas.push().setValue(aviso).addOnCompleteListener(task -> {
            Toast.makeText(context, "Aviso enviado a tu cuidador", Toast.LENGTH_SHORT).show();
        });
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvFrecuencia, tvHora, tvDosis, tvNotas;
        Button btnTomarDolor;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tv_nombre_med);
            tvFrecuencia = itemView.findViewById(R.id.tv_frecuencia_med);
            tvHora = itemView.findViewById(R.id.tv_hora_med);
            tvDosis = itemView.findViewById(R.id.tv_dosis_med);
            tvNotas = itemView.findViewById(R.id.tv_notas_med);
            btnTomarDolor = itemView.findViewById(R.id.btn_tomar_dolor);
        }
    }
}