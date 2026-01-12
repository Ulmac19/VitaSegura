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
                        // 1. Obtener el UID del usuario que acaba de entrar
                        String uid = mAuth.getCurrentUser().getUid();

                        // 2. Consultar su rol en la Realtime Database
                        mDatabase.child("Usuarios").child(uid).get().addOnCompleteListener(dbTask -> {
                            if (dbTask.isSuccessful()) {
                                // Extraemos el valor de esPrincipal (tu clase Usuario)
                                Boolean esCuidador = dbTask.getResult().child("esPrincipal").getValue(Boolean.class);

                                Intent intent;
                                if (esCuidador != null && esCuidador) {
                                    // Es Cuidador / Familiar
                                    intent = new Intent(LoginActivity.this, MainFamiliarActivity.class);
                                } else {
                                    // Es Adulto Mayor
                                    intent = new Intent(LoginActivity.this, MainAdultoActivity.class);
                                }

                                startActivity(intent);
                                finish(); // Cerramos el Login para que no puedan regresar con el botón atrás
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
                    intent = new Intent(LoginActivity.this, MainFamiliarActivity.class);
                } else {
                    // Si es false, va a la pantalla del Adulto Mayor
                    intent = new Intent(LoginActivity.this, MainAdultoActivity.class);
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