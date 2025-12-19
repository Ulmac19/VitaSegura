package com.example.vitasegura;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainAdultoActivity extends AppCompatActivity {

    private LinearLayout btnSalud, btnMeds, btnEmergencia, btnInfo;
    private TextView tvBienvenida;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_adulto);

        tvBienvenida = findViewById(R.id.tv_bienvenida_nombre);
        btnSalud = findViewById(R.id.btn_salud);
        btnMeds = findViewById(R.id.btn_medicamentos);
        btnEmergencia = findViewById(R.id.btn_emergencia);
        btnInfo = findViewById(R.id.btn_info_personal);

        // Ejemplo de navegación al módulo de salud
        btnSalud.setOnClickListener(v -> {
            Intent intent = new Intent(MainAdultoActivity.this, MonitoreoSaludActivity.class);
            startActivity(intent);

        });

        // Ejemplo de navegación al módulo de medicamentos
        btnMeds.setOnClickListener(v -> {
            Intent intent = new Intent(MainAdultoActivity.this, MedicamentosAbueloActivity.class);
            startActivity(intent);
        });

        // Ejemplo de navegación al módulo de información personal
        btnInfo.setOnClickListener(v -> {
            Intent intent = new Intent(MainAdultoActivity.this, MiInformacionActivity.class);
            startActivity(intent);
        });


        // El botón de emergencia podría disparar una alerta directa
        btnEmergencia.setOnClickListener(v -> {
            // Lógica de alerta inmediata
        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}