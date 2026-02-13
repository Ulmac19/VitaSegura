package com.example.vitasegura;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.SharedPreferences;
import android.widget.RadioGroup;

public class ConfiguracionActivity extends AppCompatActivity {

    private EditText etBuscar;
    private ImageView ivClearSearch;
    private LinearLayout sectionSalud, sectionUbicacion, sectionNotificaciones;
    private RadioGroup rgSalud, rgUbicacion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracion);

        //Vincular RadioGroup
        rgSalud = findViewById(R.id.rg_salud);
        rgUbicacion = findViewById(R.id.rg_ubicacion);

        SharedPreferences prefs = getSharedPreferences("VitaConfig", MODE_PRIVATE);

        int freqSalud = prefs.getInt("frecuencia_salud", 5);
        if(freqSalud == 5) rgSalud.check(R.id.rb_salud_5);
        else if(freqSalud == 10) rgSalud.check(R.id.rb_salud_10);
        else if(freqSalud == 15) rgSalud.check(R.id.rb_salud_15);


        rgSalud.setOnCheckedChangeListener((group, checkedId) -> {
            int minutos = 5;
            if(checkedId == R.id.rb_salud_10) minutos = 10;
            else if(checkedId == R.id.rb_salud_15) minutos = 15;

            prefs.edit().putInt("frecuencia_salud", minutos).apply();
        });
        // Vincular vistas
        etBuscar = findViewById(R.id.et_buscar_config);
        ivClearSearch = findViewById(R.id.iv_clear_search);
        sectionSalud = findViewById(R.id.section_salud);
        sectionUbicacion = findViewById(R.id.section_ubicacion);
        sectionNotificaciones = findViewById(R.id.section_notificaciones);

        // Botón atrás
        findViewById(R.id.iv_back_config).setOnClickListener(v -> finish());

        // Botón de cancelar búsqueda (X)
        ivClearSearch.setOnClickListener(v -> {
            etBuscar.setText(""); // Limpia el texto
            ivClearSearch.setVisibility(View.GONE);
        });

        // Lógica de búsqueda
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase();

                // Mostrar/Ocultar botón X
                if (query.isEmpty()) {
                    ivClearSearch.setVisibility(View.GONE);
                } else {
                    ivClearSearch.setVisibility(View.VISIBLE);
                }

                filtrarConfiguraciones(query);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void filtrarConfiguraciones(String query) {
        // Si la búsqueda está vacía, mostrar todo
        if (query.isEmpty()) {
            sectionSalud.setVisibility(View.VISIBLE);
            sectionUbicacion.setVisibility(View.VISIBLE);
            sectionNotificaciones.setVisibility(View.VISIBLE);
            return;
        }

        // Lógica de búsqueda: si el título de la sección contiene el texto, se muestra
        sectionSalud.setVisibility("actualización de datos de salud".contains(query) ? View.VISIBLE : View.GONE);
        sectionUbicacion.setVisibility("actualización de la ubicación".contains(query) ? View.VISIBLE : View.GONE);
        sectionNotificaciones.setVisibility("notificaciones".contains(query) ? View.VISIBLE : View.GONE);
    }


}