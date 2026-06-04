package com.example.vitasegura;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;

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
        } else {
            // Es un medicamento normal con horario
            holder.tvFrecuencia.setText(freq);
            holder.tvHora.setText(calcularProximaToma(med.getHora(), freq));

            // Ocultamos el botón para que no lo presionen por error
            holder.btnTomarDolor.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    private String calcularProximaToma(String hora, String frecuencia) {
        try {
            int frecHoras = Integer.parseInt(frecuencia.replaceAll("[^0-9]", ""));
            if (frecHoras <= 0) return hora;

            int frecMinutos = frecHoras * 60;
            String[] partes = hora.split(":");
            int minutosTotalesInicio = Integer.parseInt(partes[0].trim()) * 60
                    + Integer.parseInt(partes[1].trim());

            Calendar ahora = Calendar.getInstance();
            int minutosActuales = ahora.get(Calendar.HOUR_OF_DAY) * 60 + ahora.get(Calendar.MINUTE);

            int maxTomas = (24 * 60) / frecMinutos;
            int proximaEnMinutos = -1;
            int primeraToma = Integer.MAX_VALUE;

            for (int i = 0; i <= maxTomas; i++) {
                int minutosToma = (minutosTotalesInicio + i * frecMinutos) % 1440;
                if (minutosToma < primeraToma) primeraToma = minutosToma;
                if (minutosToma >= minutosActuales && (proximaEnMinutos == -1 || minutosToma < proximaEnMinutos)) {
                    proximaEnMinutos = minutosToma;
                }
            }

            // Si ya pasaron todas las tomas del día, mostrar la primera del día siguiente
            int resultado = (proximaEnMinutos != -1) ? proximaEnMinutos : primeraToma;
            return String.format(Locale.getDefault(), "%02d:%02d", resultado / 60, resultado % 60);

        } catch (Exception e) {
            return hora;
        }
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