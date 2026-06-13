package com.example.vitasegura;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla donde el cuidador gestiona los medicamentos del adulto mayor vinculado.
 *
 * Resuelve el adulto asociado a partir del nodo Vinculos, muestra su lista de
 * medicamentos en tiempo real y permite agregar nuevos recordatorios mediante
 * FormularioMedicamentoActivity. La edición y el borrado se delegan en el adapter.
 */
public class MedicamentosCuidadorActivity extends AppCompatActivity {

    private RecyclerView rvMedicamentos;
    private MedicamentoCuidadorAdapter adapter;
    private List<Medicamento> listaMedicamentos;

    private DatabaseReference mDatabase;
    private String uidCuidador;
    private String uidAbueloActual;

    private TextView tvTitulo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_medicamentos_cuidador);

        tvTitulo = findViewById(R.id.tv_titulo);
        rvMedicamentos = findViewById(R.id.rv_medicamentos_cuidador);
        rvMedicamentos.setLayoutManager(new LinearLayoutManager(this));

        listaMedicamentos = new ArrayList<>();
        // El adapter requiere el UID del adulto mayor para editar y eliminar
        adapter = new MedicamentoCuidadorAdapter(listaMedicamentos, this);
        rvMedicamentos.setAdapter(adapter);

        findViewById(R.id.iv_back_meds).setOnClickListener(v -> finish());

        findViewById(R.id.btn_agregar_recordatorio).setOnClickListener(v -> {
            if (uidAbueloActual != null) {
                Intent intent = new Intent(this, FormularioMedicamentoActivity.class);
                intent.putExtra("UID_ABUELO", uidAbueloActual);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Cargando datos del familiar, espera un momento", Toast.LENGTH_SHORT).show();
            }
        });

        mDatabase = FirebaseDatabase.getInstance().getReference();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uidCuidador = FirebaseAuth.getInstance().getCurrentUser().getUid();
            obtenerIdAbueloYMedicamentos();
        }




        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /** Obtiene el adulto vinculado, configura el título y carga sus medicamentos. */
    private void obtenerIdAbueloYMedicamentos() {
        mDatabase.child("Vinculos").child(uidCuidador).child("id_adulto_vinculado")
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        uidAbueloActual = task.getResult().getValue(String.class);
                        adapter.setUidAbuelo(uidAbueloActual); // habilita editar/eliminar en el adapter

                        // Usa el primer nombre del adulto para el título de la pantalla
                        mDatabase.child("Usuarios").child(uidAbueloActual).child("nombre").get().addOnCompleteListener(t -> {
                            if (t.isSuccessful() && t.getResult().exists()) {
                                String nombreCompleto = t.getResult().getValue(String.class);
                                String primerNombre = "";
                                if (nombreCompleto != null && !nombreCompleto.isEmpty()) {
                                    primerNombre = nombreCompleto.split(" ")[0];
                                }
                                tvTitulo.setText("Medicamentos de\n" + primerNombre);
                            }
                        });

                        cargarMedicamentos(uidAbueloActual);
                    }
                });
    }

    /** Escucha en tiempo real la lista de medicamentos del adulto y refresca el adapter. */
    private void cargarMedicamentos(String uidAbuelo) {
        mDatabase.child("Usuarios").child(uidAbuelo).child("Medicamentos")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        listaMedicamentos.clear();
                        for (DataSnapshot dato : snapshot.getChildren()) {
                            Medicamento med = dato.getValue(Medicamento.class);
                            if (med != null) {
                                listaMedicamentos.add(med);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MedicamentosCuidadorActivity.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
                    }
                });
    }

}