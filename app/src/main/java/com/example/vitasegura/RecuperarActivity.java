package com.example.vitasegura;

import android.content.Intent;
import android.os.Bundle;
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

public class RecuperarActivity extends AppCompatActivity {

    private EditText etCorreo;
    private Button btnEnviar;
    private TextView tvContador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recuperar);

        etCorreo = findViewById(R.id.et_recuperar_correo);
        btnEnviar = findViewById(R.id.btn_enviar_codigo);
        tvContador = findViewById(R.id.tv_contador_codigo);

        btnEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String correo = etCorreo.getText().toString().trim();
                if (correo.isEmpty()) {
                    Toast.makeText(RecuperarActivity.this, "Ingresa un correo válido", Toast.LENGTH_SHORT).show();
                } else {
                    // Aquí integraremos el envío con Firebase después
                    Toast.makeText(RecuperarActivity.this, "Código enviado a " + correo, Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ... (validación de correo)
                Intent intent = new Intent(RecuperarActivity.this, IngresarCodigoActivity.class);
                startActivity(intent);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}