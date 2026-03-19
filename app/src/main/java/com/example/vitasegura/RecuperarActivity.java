package com.example.vitasegura;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
import java.util.Map;

public class RecuperarActivity extends AppCompatActivity {

    private EditText etCorreo;
    private Button btnEnviar;

    private FirebaseFunctions mFunctions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recuperar);

        etCorreo = findViewById(R.id.et_recuperar_correo);
        btnEnviar = findViewById(R.id.btn_enviar_codigo);

        // Inicializamos Cloud Functions
        mFunctions = FirebaseFunctions.getInstance();

        btnEnviar.setOnClickListener(v -> enviarCodigo());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void enviarCodigo() {
        String correo = etCorreo.getText().toString().trim();

        if (correo.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            Toast.makeText(this, "Por favor ingresa un correo electrónico válido", Toast.LENGTH_SHORT).show();
            return;
        }

        // Deshabilitar el botón para evitar múltiples clics
        btnEnviar.setEnabled(false);
        btnEnviar.setText("Enviando...");

        // Preparamos los datos que le enviaremos a la nube
        Map<String, Object> data = new HashMap<>();
        data.put("email", correo);

        // Llamamos a la función "enviarCodigoRecuperacion" que creaste en index.js
        mFunctions.getHttpsCallable("enviarCodigoRecuperacion")
                .call(data)
                .addOnCompleteListener(task -> {
                    btnEnviar.setEnabled(true);
                    btnEnviar.setText("Enviar Código");

                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Código enviado con éxito a tu correo", Toast.LENGTH_LONG).show();

                        // Pasamos a la siguiente pantalla y le mandamos el correo para validarlo allá
                        Intent intent = new Intent(RecuperarActivity.this, IngresarCodigoActivity.class);
                        intent.putExtra("correoUsuario", correo);
                        startActivity(intent);
                    } else {
                        Exception e = task.getException();
                        Toast.makeText(this, "Error al enviar el código: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}