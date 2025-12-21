package com.example.vitasegura;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Calendar;
import java.util.Locale;

public class FormularioMedicamentoActivity extends AppCompatActivity {

    private EditText etNombre, etFrecuencia, etHora, etDosis, etNotas;
    private TextView tvTitulo;
    private Button btnAccion;
    private boolean esEdicion = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_formulario_medicamento);

        // Vincular vistas
        tvTitulo = findViewById(R.id.tv_titulo_formulario);
        btnAccion = findViewById(R.id.btn_accion);
        etNombre = findViewById(R.id.et_nombre);
        etFrecuencia = findViewById(R.id.et_frecuencia);
        etHora = findViewById(R.id.et_hora);
        etDosis = findViewById(R.id.et_dosis);
        etNotas = findViewById(R.id.et_notas);

        // Listeners para selectores
        etFrecuencia.setOnClickListener(v -> configurarFrecuencia());
        etHora.setOnClickListener(v -> mostrarReloj());

        // Manejo de edición o nuevo
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("nombre")) {
            esEdicion = true;
            tvTitulo.setText("Editar\nRecordatorio");
            btnAccion.setText("Aceptar");
            etNombre.setText(intent.getStringExtra("nombre"));
            etFrecuencia.setText(intent.getStringExtra("frecuencia"));
            etHora.setText(intent.getStringExtra("hora"));
            etDosis.setText(intent.getStringExtra("dosis"));
            etNotas.setText(intent.getStringExtra("notas"));
        }

        // Botón atrás
        findViewById(R.id.iv_back_form).setOnClickListener(v -> finish());

        // Botón guardar
        btnAccion.setOnClickListener(v -> {
            String mensaje = esEdicion ? "Cambios guardados" : "Nuevo recordatorio agregado";
            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
            finish();
        });

        // Responsividad
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void configurarFrecuencia() {
        final String[] opciones = {
                "Una vez al día",
                "Dos veces al día (Cada 12 horas)",
                "Tres veces al día (Cada 8 horas)",
                "Cuatro veces al día (Cada 6 horas)",
                "Cada 2 días",
                "Una vez a la semana"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Frecuencia de toma");
        builder.setItems(opciones, (dialog, which) -> {
            etFrecuencia.setText(opciones[which]);
        });
        builder.show();
    }

    private void mostrarReloj() {
        final Calendar c = Calendar.getInstance();
        int h = c.get(Calendar.HOUR_OF_DAY);
        int m = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    String horaSelected = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    etHora.setText(horaSelected);
                }, h, m, true);
        timePickerDialog.show();
    }
}