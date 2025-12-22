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

import java.util.Calendar;
import java.util.Locale;

public class FormularioMedicamentoActivity extends AppCompatActivity {

    private EditText etNombre, etFrecuencia, etHora, etDosis, etNotas;
    private TextView tvTitulo;
    private Button btnAccion;
    private boolean esEdicion = false;
    private boolean modoAgregarUsuario = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_formulario_medicamento);

        // 1. Vincular vistas
        tvTitulo = findViewById(R.id.tv_titulo_formulario);
        btnAccion = findViewById(R.id.btn_accion);
        etNombre = findViewById(R.id.et_nombre);
        etFrecuencia = findViewById(R.id.et_frecuencia);
        etHora = findViewById(R.id.et_hora);
        etDosis = findViewById(R.id.et_dosis);
        etNotas = findViewById(R.id.et_notas);

        // Acceder a la flecha del RelativeLayout de frecuencia
        ImageView ivFlecha = (ImageView) ((RelativeLayout) etFrecuencia.getParent()).getChildAt(1);

        // 2. Revisar si venimos de "Agregar Usuario" o de "Medicamentos"
        Intent intent = getIntent();
        if (intent != null && "AGREGAR_USUARIO".equals(intent.getStringExtra("modo"))) {
            configurarModoUsuario(ivFlecha);
        } else {
            configurarModoMedicamento();
            // Lógica de edición (solo para medicamentos)
            if (intent != null && intent.hasExtra("nombre")) {
                esEdicion = true;
                tvTitulo.setText("Editar\nRecordatorio");
                btnAccion.setText("Aceptar");
                rellenarDatosEdicion(intent);
            }
        }

        // Botón atrás común
        findViewById(R.id.iv_back_form).setOnClickListener(v -> finish());

        // Botón acción (Agregar/Aceptar)
        btnAccion.setOnClickListener(v -> {
            if (modoAgregarUsuario) {
                validarYAgregarUsuario();
            } else {
                String mensaje = esEdicion ? "Cambios guardados" : "Nuevo recordatorio agregado";
                Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void configurarModoUsuario(ImageView ivFlecha) {
        modoAgregarUsuario = true;
        tvTitulo.setText("Agregar Nuevo\nUsuario");
        btnAccion.setText("Agregar");

        // 1. Extraer la tipografía y el tamaño del campo Nombre (que es el original)
        android.graphics.Typeface tipografíaOriginal = etNombre.getTypeface();
        float tamanoOriginal = etNombre.getTextSize(); // Esto obtiene el tamaño en píxeles

        // Campo Nombre (Ya tiene el estilo correcto)
        etNombre.setHint("Nombre completo");

        // Campo Correo electrónico
        etFrecuencia.setHint("Correo electrónico");
        etFrecuencia.setOnClickListener(null);
        etFrecuencia.setFocusableInTouchMode(true);
        etFrecuencia.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        // Forzar estilo original
        etFrecuencia.setTypeface(tipografíaOriginal);
        etFrecuencia.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, tamanoOriginal);
        ivFlecha.setVisibility(View.GONE);

        // Campo Contraseña
        etHora.setHint("Contraseña");
        etHora.setOnClickListener(null);
        etHora.setFocusableInTouchMode(true);
        etHora.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        // Forzar estilo original (Evita que se ponga en negrita o monospace)
        etHora.setTypeface(tipografíaOriginal);
        etHora.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, tamanoOriginal);

        // Campo Confirmar contraseña
        etDosis.setHint("Confirmar contraseña");
        etDosis.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        // Forzar estilo original
        etDosis.setTypeface(tipografíaOriginal);
        etDosis.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, tamanoOriginal);

        // Campo Teléfono
        etNotas.setHint("Teléfono");
        etNotas.setInputType(InputType.TYPE_CLASS_PHONE);
        // Forzar estilo original
        etNotas.setTypeface(tipografíaOriginal);
        etNotas.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, tamanoOriginal);
    }

    private void configurarModoMedicamento() {
        etFrecuencia.setOnClickListener(v -> configurarFrecuencia());
        etHora.setOnClickListener(v -> mostrarReloj());
    }

    private void rellenarDatosEdicion(Intent intent) {
        etNombre.setText(intent.getStringExtra("nombre"));
        etFrecuencia.setText(intent.getStringExtra("frecuencia"));
        etHora.setText(intent.getStringExtra("hora"));
        etDosis.setText(intent.getStringExtra("dosis"));
        etNotas.setText(intent.getStringExtra("notas"));
    }

    private void validarYAgregarUsuario() {
        String pass = etHora.getText().toString();
        String confirmPass = etDosis.getText().toString();

        if (pass.isEmpty() || !pass.equals(confirmPass)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Nuevo usuario agregado", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void configurarFrecuencia() {
        final String[] opciones = {"Una vez al día", "Dos veces al día (Cada 12 horas)",
                "Tres veces al día (Cada 8 horas)", "Cuatro veces al día (Cada 6 horas)",
                "Cada 2 días", "Una vez a la semana"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Frecuencia de toma");
        builder.setItems(opciones, (dialog, which) -> etFrecuencia.setText(opciones[which]));
        builder.show();
    }

    private void mostrarReloj() {
        final Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            etHora.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }
}