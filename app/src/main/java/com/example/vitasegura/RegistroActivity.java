package com.example.vitasegura;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RegistroActivity extends AppCompatActivity {

    private EditText etNombre, etCorreo, etPassword, etConfirmPassword, etTelefono;
    private RadioGroup rgRol;
    private Button btnCrear;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registro);

        etNombre = findViewById(R.id.et_nombre);
        etCorreo = findViewById(R.id.et_correo);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etTelefono = findViewById(R.id.et_telefono);
        rgRol = findViewById(R.id.rg_rol);
        btnCrear = findViewById(R.id.btn_crear_cuenta_final);

        btnCrear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Aquí irá la lógica de Firebase después
                validarDatos();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.RegistroActivity), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void validarDatos() {
        String nombre = etNombre.getText().toString();
        if (nombre.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa tu nombre", Toast.LENGTH_SHORT).show();
        }
        // Agregaremos más validaciones cuando conectemos el backend
    }
}