package com.example.vitasegura;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
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

public class MedicamentosAbueloActivity extends AppCompatActivity {

    private RecyclerView rvMedicamentos;
    private MedicamentoAdultoAdapter adapter;
    private List<Medicamento> listaMedicamentos;

    private DatabaseReference mDatabase;
    private String miUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_medicamentos_abuelo);

        // Botón de retroceso
        findViewById(R.id.iv_back_meds).setOnClickListener(v -> finish());

        // Configurar la lista (RecyclerView)
        rvMedicamentos = findViewById(R.id.rv_medicamentos);
        rvMedicamentos.setLayoutManager(new LinearLayoutManager(this));

        listaMedicamentos = new ArrayList<>();
        adapter = new MedicamentoAdultoAdapter(listaMedicamentos);
        rvMedicamentos.setAdapter(adapter);

        // Inicializar Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            miUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            cargarMisMedicamentos();
        } else {
            Toast.makeText(this, "Error de sesión", Toast.LENGTH_SHORT).show();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void cargarMisMedicamentos() {
        // Escucha en tiempo real SU PROPIA lista de medicamentos
        mDatabase.child("Usuarios").child(miUid).child("Medicamentos")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        listaMedicamentos.clear(); // Limpiamos la lista vieja

                        for (DataSnapshot dato : snapshot.getChildren()) {
                            Medicamento med = dato.getValue(Medicamento.class);
                            if (med != null) {
                                listaMedicamentos.add(med);
                            }
                        }
                        // Avisar al adaptador que dibuje los nuevos datos
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MedicamentosAbueloActivity.this, "Error al cargar medicamentos", Toast.LENGTH_SHORT).show();
                    }
                });
    }

}