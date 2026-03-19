package com.example.vitasegura;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class IngresarCodigoActivity extends AppCompatActivity {

    private EditText etCodigo;
    private Button btnVerificar;
    private DatabaseReference mDatabase;
    private String correoUsuario;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ingresar_codigo);

        etCodigo = findViewById(R.id.et_ingresar_codigo);
        etCodigo = findViewById(R.id.et_ingresar_codigo);
        btnVerificar = findViewById(R.id.btn_verificar_codigo);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Recibimos el correo de la pantalla anterior
        correoUsuario = getIntent().getStringExtra("correoUsuario");

        btnVerificar.setOnClickListener(v -> verificarCodigo());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void verificarCodigo() {
        String codigoIngresado = etCodigo.getText().toString().trim();

        if (codigoIngresado.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa el código", Toast.LENGTH_SHORT).show();
            return;
        }

        // En Node.js guardamos el correo cambiando los puntos por guiones bajos
        String emailKey = correoUsuario.replace(".", "_");

        mDatabase.child("CodigosRecuperacion").child(emailKey).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String codigoGuardado = snapshot.child("codigo").getValue(String.class);
                        Long expiracion = snapshot.child("expiresAt").getValue(Long.class);

                        // 1. Validar que no esté expirado
                        if (expiracion != null && System.currentTimeMillis() > expiracion) {
                            Toast.makeText(this, "El código ha expirado. Solicita uno nuevo.", Toast.LENGTH_LONG).show();
                        }
                        // 2. Validar que sea correcto
                        else if (codigoGuardado != null && codigoGuardado.equals(codigoIngresado)) {
                            Toast.makeText(this, "Código verificado correctamente", Toast.LENGTH_SHORT).show();

                            // Pasamos a la pantalla de Cambiar Contraseña
                            Intent intent = new Intent(this, CambiarPassActivity.class);
                            intent.putExtra("correoUsuario", correoUsuario);
                            intent.putExtra("flujoRecuperacion", true); // Bandera para saber de dónde venimos
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, "Código incorrecto", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "No se encontró ningún código vigente para este correo", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al verificar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

}