package com.example.vitasegura;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MiInformacionActivity extends AppCompatActivity {

    private ImageView ivBack;
    private TextView tvNombre, tvCorreo, tvTelefono;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mi_informacion);

        ivBack = findViewById(R.id.iv_back_info);
        tvNombre = findViewById(R.id.tv_info_nombre);
        tvCorreo = findViewById(R.id.tv_info_correo);
        tvTelefono = findViewById(R.id.tv_info_telefono);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        ivBack.setOnClickListener(v -> finish());

        cargarInformacionAbuelo();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void cargarInformacionAbuelo() {
        if (mAuth.getCurrentUser() != null) {
            String uidAbuelo = mAuth.getCurrentUser().getUid();

            mDatabase.child("Usuarios").child(uidAbuelo).get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            // Si el abuelo existe, obtenemos su información
                            Usuario abuelo = snapshot.getValue(Usuario.class);

                            if (abuelo != null) {
                                tvNombre.setText(abuelo.getNombre() != null ? abuelo.getNombre() : "Sin nombre");
                                tvCorreo.setText(abuelo.getCorreo() != null ? abuelo.getCorreo() : "Sin correo");
                                tvTelefono.setText(abuelo.getTelefono() != null ? abuelo.getTelefono() : "Sin teléfono");
                            }
                        } else {
                            Toast.makeText(this, "No se encontró la información del perfil", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error de conexión al cargar datos", Toast.LENGTH_SHORT).show();
                    });
        }
    }
}