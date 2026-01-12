package com.example.vitasegura;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {
    private TextView tvMonitoreoLink;
    private Button btnRegistrarse, btnIniciarSesion;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();


        tvMonitoreoLink = findViewById(R.id.tv_monitoreo_link);
        btnRegistrarse = findViewById(R.id.btn_registrarse);
        btnIniciarSesion = findViewById(R.id.btn_iniciar_sesion);

        tvMonitoreoLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Redirección a la interfaz principal del cuidador/familiar
                Intent intent = new Intent(MainActivity.this, MainFamiliarActivity.class);
                startActivity(intent);
            }
        });

        btnRegistrarse.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegistroActivity.class);
            startActivity(intent);
        });

        btnIniciarSesion.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    protected void onStart() {
        super.onStart();
        // Revisamos si ya hay un usuario logueado en este celular
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Si existe, lo mandamos directo a su pantalla según su rol
            verificarRolYRedirigir(user.getUid());
        }
    }

    private void verificarRolYRedirigir(String uid) {
        // Vamos a la rama "Usuarios" y buscamos el ID del usuario actual
        mDatabase.child("Usuarios").child(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                // Sacamos el valor de "esPrincipal" (Cuidador = true, Abuelo = false)
                Boolean esCuidador = task.getResult().child("esPrincipal").getValue(Boolean.class);

                Intent intent;
                if (esCuidador != null && esCuidador) {
                    // Si es true, va a la pantalla del Familiar
                    intent = new Intent(MainActivity.this, MainFamiliarActivity.class);
                } else {
                    // Si es false, va a la pantalla del Adulto Mayor
                    intent = new Intent(MainActivity.this, MainAdultoActivity.class);
                }

                startActivity(intent);
                finish(); // Importante para que no regrese al login al darle "atrás"
            } else {
                // Si por algo no hay datos en la DB, lo dejamos en el Login
                Toast.makeText(this, "Error al recuperar perfil del usuario", Toast.LENGTH_SHORT).show();
            }
        });
    }

}