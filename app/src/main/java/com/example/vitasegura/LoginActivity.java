package com.example.vitasegura;

import android.annotation.SuppressLint;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Pantalla de inicio de sesión con correo y contraseña (Firebase Auth).
 *
 * Tras autenticar, consulta el rol del usuario en la base de datos y lo dirige
 * a la pantalla principal del cuidador o del adulto mayor. Incluye el acceso al
 * flujo de recuperación de contraseña.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etCorreo, etPassword;
    private Button btnEntrar;
    private TextView tvOlvidaste;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        mAuth= FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();


        etCorreo = findViewById(R.id.et_login_correo);
        etPassword = findViewById(R.id.et_login_password);
        btnEntrar = findViewById(R.id.btn_login_entrar);
        tvOlvidaste = findViewById(R.id.tv_olvidaste_pass);

        btnEntrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUsuario();
            }
        });


        tvOlvidaste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RecuperarActivity.class);
                startActivity(intent);
            }
        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /** Autentica con Firebase y redirige al usuario según su rol. */
    private void loginUsuario() {
        String correo = etCorreo.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (correo.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Ingresa correo y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(correo, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();

                        // Consulta el rol del usuario en la Realtime Database
                        mDatabase.child("Usuarios").child(uid).get().addOnCompleteListener(dbTask -> {
                            if (dbTask.isSuccessful()) {
                                // esPrincipal: true = cuidador, false = adulto mayor
                                Boolean esCuidador = dbTask.getResult().child("esPrincipal").getValue(Boolean.class);

                                Intent intent;
                                if (esCuidador != null && esCuidador) {
                                    intent = new Intent(LoginActivity.this, MainFamiliarActivity.class);
                                } else {
                                    intent = new Intent(LoginActivity.this, MainAdultoActivity.class);
                                }

                                startActivity(intent);
                                finish(); // evita volver al login con el botón atrás
                            } else {
                                Toast.makeText(LoginActivity.this, "Error al obtener perfil", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(LoginActivity.this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    protected void onStart() {
        super.onStart();
        // Si ya hay una sesión activa, redirige sin volver a pedir credenciales
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
                    intent = new Intent(LoginActivity.this, MainFamiliarActivity.class);
                } else {
                    intent = new Intent(LoginActivity.this, MainAdultoActivity.class);
                }

                startActivity(intent);
                finish(); // evita volver al login con el botón atrás
            } else {
                Toast.makeText(this, "Error al recuperar perfil del usuario", Toast.LENGTH_SHORT).show();
            }
        });
    }
}