package com.example.vitasegura;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MiInformacionActivity extends AppCompatActivity {

    private ImageView ivBack;
    private TextView tvNombre, tvCorreo, tvTelefono;
    private Button btnCambiarPass;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mi_informacion);

        ImageView ivBack = findViewById(R.id.iv_back_info);
        ivBack.setOnClickListener(v -> finish());

        Button btnCambiar = findViewById(R.id.btn_cambiar_pass_perfil);
        btnCambiar.setOnClickListener(v -> {
            Intent intent = new Intent(MiInformacionActivity.this, CambiarPassActivity.class);
            startActivity(intent);
        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}