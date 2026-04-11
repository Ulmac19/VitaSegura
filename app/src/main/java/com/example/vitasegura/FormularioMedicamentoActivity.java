package com.example.vitasegura;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FormularioMedicamentoActivity extends AppCompatActivity {

    private EditText etNombre, etFrecuencia, etHora, etDosis, etNotas;
    private Button btnAccion;
    private TextView tvTitulo;

    private DatabaseReference mDatabase;
    private String uidAbuelo;
    private String medicamentoIdEditar = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_formulario_medicamento);

        // Vincular vistas
        tvTitulo = findViewById(R.id.tv_titulo_formulario);
        etNombre = findViewById(R.id.et_nombre);
        etFrecuencia = findViewById(R.id.et_frecuencia);
        etHora = findViewById(R.id.et_hora);
        etDosis = findViewById(R.id.et_dosis);
        etNotas = findViewById(R.id.et_notas);
        btnAccion = findViewById(R.id.btn_accion);

        findViewById(R.id.iv_back_form).setOnClickListener(v -> finish());
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Recibir datos del Intent
        uidAbuelo = getIntent().getStringExtra("UID_ABUELO");
        medicamentoIdEditar = getIntent().getStringExtra("MEDICAMENTO_ID");

        // Configurar Selector de Hora
        etHora.setOnClickListener(v -> mostrarTimePicker());

        // Configurar Selector de Frecuencia
        etFrecuencia.setOnClickListener(v -> mostrarDialogoFrecuencia());

        // Si es Modo Edición, rellenar los campos
        if (medicamentoIdEditar != null) {
            tvTitulo.setText("Editar\nRecordatorio");
            btnAccion.setText("Guardar Cambios");
            etNombre.setText(getIntent().getStringExtra("NOMBRE"));
            etFrecuencia.setText(getIntent().getStringExtra("FRECUENCIA"));
            etHora.setText(getIntent().getStringExtra("HORA"));
            etDosis.setText(getIntent().getStringExtra("DOSIS"));
            etNotas.setText(getIntent().getStringExtra("NOTAS"));
        }

        btnAccion.setOnClickListener(v -> guardarMedicamento());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void mostrarTimePicker() {
        Calendar calendario = Calendar.getInstance();
        int horaActual = calendario.get(Calendar.HOUR_OF_DAY);
        int minutoActual = calendario.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    String horaFormateada = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    etHora.setText(horaFormateada);
                }, horaActual, minutoActual, true);
        timePickerDialog.show();
    }

    private void mostrarDialogoFrecuencia() {
        String[] opciones = {"Cada 8 horas", "Cada 12 horas", "Cada 24 horas", "Solo si hay dolor"};
        new AlertDialog.Builder(this)
                .setTitle("Selecciona Frecuencia")
                .setItems(opciones, (dialog, which) -> etFrecuencia.setText(opciones[which]))
                .show();
    }

    private void guardarMedicamento() {
        String nombre = etNombre.getText().toString().trim();
        String frecuencia = etFrecuencia.getText().toString().trim();
        String hora = etHora.getText().toString().trim();
        String dosis = etDosis.getText().toString().trim();
        String notas = etNotas.getText().toString().trim();

        if (nombre.isEmpty() || frecuencia.isEmpty() || hora.isEmpty() || dosis.isEmpty()) {
            Toast.makeText(this, "Llena todos los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        //Configurar los textos del dialogo dependiendo de si es edicion o nuevo medicamento
        String tituloDialogo = (medicamentoIdEditar != null) ? "Confirmar Cambios" : "Nuevo Medicamento";
        String mensajeDialogo = (medicamentoIdEditar != null) ?
                "¿Estás seguro de que deseas actualizar la información de " + nombre + "?" :
                "¿Estás seguro de que deseas guardar " + nombre + " para el abuelo?";

        new AlertDialog.Builder(this)
                .setTitle(tituloDialogo)
                .setMessage(mensajeDialogo)
                .setPositiveButton("Guardar", (dialog, which) -> {

                    // Si es nuevo, generamos un ID. Si es edición, usamos el existente.
                    String medId = (medicamentoIdEditar != null) ? medicamentoIdEditar : mDatabase.push().getKey();

                    Medicamento nuevoMedicamento = new Medicamento(medId, nombre, frecuencia, hora, dosis, notas);

                    // Guardar en Firebase
                    mDatabase.child("Usuarios").child(uidAbuelo).child("Medicamentos").child(medId).setValue(nuevoMedicamento)
                            .addOnSuccessListener(aVoid -> {

                                if (medicamentoIdEditar == null) {
                                    Map<String, Object> notif = new HashMap<>();
                                    notif.put("titulo", "Nuevo Medicamento Asignado");
                                    notif.put("mensaje", "Debes tomar " + nombre + " (" + dosis + ")");
                                    notif.put("timestamp", System.currentTimeMillis());

                                    mDatabase.child("Usuarios").child(uidAbuelo).child("NotificacionesPendientes").push().setValue(notif);
                                }

                                Toast.makeText(this, "Medicamento guardado", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

}