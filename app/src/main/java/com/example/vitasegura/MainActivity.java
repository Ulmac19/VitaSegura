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

/**
 * Pantalla de entrada de la aplicación.
 *
 * Si ya existe una sesión activa, redirige automáticamente al usuario a su
 * pantalla principal según su rol (cuidador o adulto mayor). En caso contrario,
 * ofrece las opciones de iniciar sesión o registrarse.
 */
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
                // Acceso directo a la interfaz del cuidador
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
        // Si ya hay una sesión activa, redirige sin pasar por el login
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            verificarRolYRedirigir(user.getUid());
        }
    }

    /**
     * Consulta el rol del usuario en Firebase y lo redirige a la pantalla
     * principal correspondiente.
     */
    private void verificarRolYRedirigir(String uid) {
        mDatabase.child("Usuarios").child(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                // esPrincipal: true = cuidador, false = adulto mayor
                Boolean esCuidador = task.getResult().child("esPrincipal").getValue(Boolean.class);

                Intent intent;
                if (esCuidador != null && esCuidador) {
                    intent = new Intent(MainActivity.this, MainFamiliarActivity.class);
                } else {
                    intent = new Intent(MainActivity.this, MainAdultoActivity.class);
                }

                startActivity(intent);
                finish(); // evita volver a esta pantalla con el botón atrás
            } else {
                Toast.makeText(this, "Error al recuperar perfil del usuario", Toast.LENGTH_SHORT).show();
            }
        });
    }

}