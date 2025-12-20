package com.example.vitasegura;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class FormularioMedicamentoActivity extends AppCompatActivity {

    private EditText etNombre, etFrecuencia, etHora, etDosis, etNotas;
    private TextView tvTitulo;
    private Button btnAccion;
    private boolean esEdicion = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_formulario_medicamento);

        // 1. Vincular las vistas
        tvTitulo = findViewById(R.id.tv_titulo_formulario);
        btnAccion = findViewById(R.id.btn_accion);
        etNombre = findViewById(R.id.et_nombre);
        etFrecuencia = findViewById(R.id.et_frecuencia);
        etHora = findViewById(R.id.et_hora);
        etDosis = findViewById(R.id.et_dosis);
        etNotas = findViewById(R.id.et_notas);

        // 2. Revisar si recibimos datos desde el Adaptador
        Intent intent = getIntent();
        if (intent.hasExtra("nombre")) {
            esEdicion = true;

            // Cambiar textos según tu maquetado
            tvTitulo.setText("Editar\nRecordatorio");
            btnAccion.setText("Aceptar");

            // Rellenar los campos con la información recuperada
            etNombre.setText(intent.getStringExtra("nombre"));
            etFrecuencia.setText(intent.getStringExtra("frecuencia"));
            etHora.setText(intent.getStringExtra("hora"));
            etDosis.setText(intent.getStringExtra("dosis"));
            etNotas.setText(intent.getStringExtra("notas"));
        }

        // 3. Configurar el botón de regreso
        findViewById(R.id.iv_back_form).setOnClickListener(v -> finish());

        // 4. Lógica del botón principal (Agregar o Aceptar)
        btnAccion.setOnClickListener(v -> {
            if (esEdicion) {
                // Aquí irá la lógica de Firebase para actualizar
                Toast.makeText(this, "Cambios guardados", Toast.LENGTH_SHORT).show();
            } else {
                // Aquí irá la lógica de Firebase para crear nuevo
                Toast.makeText(this, "Nuevo recordatorio agregado", Toast.LENGTH_SHORT).show();
            }
            finish(); // Regresa a la lista
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}