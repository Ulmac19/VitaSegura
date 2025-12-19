package com.example.vitasegura;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MedicamentosAbueloActivity extends AppCompatActivity {

    private RecyclerView rv;
    private MedicamentoAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_medicamentos_abuelo);

        findViewById(R.id.iv_back_meds).setOnClickListener(v -> finish());

        rv = findViewById(R.id.rv_medicamentos);
        rv.setLayoutManager(new LinearLayoutManager(this));

        // Datos de prueba según tu diseño
        List<Medicamento> listaPrueba = new ArrayList<>();
        listaPrueba.add(new Medicamento("Metformina", "Cada 2 días", "20:00", "1 tableta", "Tomar después de cenar"));
        listaPrueba.add(new Medicamento("Aspirina", "Diario", "08:00", "500mg", "No en ayunas"));

        adapter = new MedicamentoAdapter(listaPrueba);
        rv.setAdapter(adapter);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}