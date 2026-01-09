package com.example.vitasegura;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegistroActivity extends AppCompatActivity {

    private EditText etNombre, etCorreo, etPassword, etConfirmPassword, etTelefono;
    private RadioGroup rgRol;
    private Button btnRegistar;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registro);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        etNombre = findViewById(R.id.et_nombre);
        etCorreo = findViewById(R.id.et_correo);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etTelefono = findViewById(R.id.et_telefono);
        rgRol = findViewById(R.id.rg_rol);
        btnRegistar = findViewById(R.id.btn_crear_cuenta_final);

        btnRegistar.setOnClickListener(v -> registrarUsuario());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    private void registrarUsuario() {
        String nombre = etNombre.getText().toString().trim();
        String correo = etCorreo.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();
        if (!pass.equals(confirmPass)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }
        String telefono = etTelefono.getText().toString().trim();


        int selectedId = rgRol.getCheckedRadioButtonId();

        if (nombre.isEmpty() || correo.isEmpty() || pass.isEmpty() || telefono.isEmpty() || selectedId == -1) {
            Toast.makeText(this, "Por favor, completa todos los campos y selecciona un rol", Toast.LENGTH_SHORT).show();
            return;
        }


        boolean esPrincipal = (selectedId == R.id.rb_cuidador);

        mAuth.createUserWithEmailAndPassword(correo, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // 3. Guardar los datos adicionales en la base de datos
                        String uid = mAuth.getCurrentUser().getUid();
                        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

                        Usuario nuevoUsuario = new Usuario(nombre, correo, telefono, esPrincipal);

                        mDatabase.child("Usuarios").child(uid).setValue(nuevoUsuario)
                                .addOnCompleteListener(dbTask -> {
                                    if (dbTask.isSuccessful()) {
                                        Toast.makeText(RegistroActivity.this, "Usuario registrado con éxito", Toast.LENGTH_SHORT).show();
                                        finish(); // Regresa al Login
                                    } else {
                                        Toast.makeText(RegistroActivity.this, "Error al guardar datos", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(RegistroActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}