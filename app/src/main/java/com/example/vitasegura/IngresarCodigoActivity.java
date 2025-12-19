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

public class IngresarCodigoActivity extends AppCompatActivity {

    private EditText etCodigo;
    private Button btnVerificar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ingresar_codigo);

        etCodigo = findViewById(R.id.et_ingresar_codigo);
        btnVerificar = findViewById(R.id.btn_verificar_codigo);

        btnVerificar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String codigo = etCodigo.getText().toString().trim();
                if (codigo.isEmpty()) {
                    Toast.makeText(IngresarCodigoActivity.this, "Por favor, ingresa el código", Toast.LENGTH_SHORT).show();
                } else {
                    // Simulación de verificación exitosa
                    Intent intent = new Intent(IngresarCodigoActivity.this, CambiarPassActivity.class);
                    startActivity(intent);
                }
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}