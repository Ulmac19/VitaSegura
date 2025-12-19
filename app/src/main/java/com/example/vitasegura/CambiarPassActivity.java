package com.example.vitasegura;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class CambiarPassActivity extends AppCompatActivity {

    private EditText etPass, etConfirmPass;
    private Button btnConfirmar;
    private ImageView ivBack;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cambiar_pass);

        etPass = findViewById(R.id.et_nueva_pass);
        etConfirmPass = findViewById(R.id.et_confirmar_nueva_pass);
        btnConfirmar = findViewById(R.id.btn_confirmar_cambio);
        ivBack = findViewById(R.id.iv_back_cambiar);

        // Volver a la pantalla anterior
        ivBack.setOnClickListener(v -> finish());

        btnConfirmar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pass = etPass.getText().toString();
                String confirm = etConfirmPass.getText().toString();

                if (pass.isEmpty() || confirm.isEmpty()) {
                    Toast.makeText(CambiarPassActivity.this, "Complete ambos campos", Toast.LENGTH_SHORT).show();
                } else if (!pass.equals(confirm)) {
                    Toast.makeText(CambiarPassActivity.this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                } else {
                    // Lógica para actualizar en base de datos
                    Toast.makeText(CambiarPassActivity.this, "Contraseña actualizada con éxito", Toast.LENGTH_SHORT).show();
                    // Regresar al login
                    Intent intent = new Intent(CambiarPassActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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