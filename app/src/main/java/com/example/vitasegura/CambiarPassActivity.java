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

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class CambiarPassActivity extends AppCompatActivity {

    private EditText etPassActual,etNuevaPass, etConfirmarPass;
    private Button btnConfirmar;
    private ImageView ivBack;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cambiar_pass);

        etPassActual = findViewById(R.id.et_pass_actual);
        etNuevaPass = findViewById(R.id.et_nueva_pass);
        etConfirmarPass = findViewById(R.id.et_confirmar_nueva_pass);
        btnConfirmar = findViewById(R.id.btn_confirmar_cambio);
        ivBack = findViewById(R.id.iv_back_cambiar);

        ivBack.setOnClickListener(v -> finish());
        btnConfirmar.setOnClickListener(v -> reautenticarYCambiarPassword());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void reautenticarYCambiarPassword() {
        String passActual = etPassActual.getText().toString().trim();
        String nuevaPass = etNuevaPass.getText().toString().trim();
        String confirmarPass = etConfirmarPass.getText().toString().trim();

        if (passActual.isEmpty() || nuevaPass.isEmpty() || confirmarPass.isEmpty()) {
            Toast.makeText(this, "Por favor llena todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!nuevaPass.equals(confirmarPass)) {
            Toast.makeText(this, "Las contraseñas nuevas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!esPasswordSegura(nuevaPass)) {
            Toast.makeText(this, "La nueva contraseña no cumple con los requisitos de seguridad", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && user.getEmail() != null) {
            // 1. Creamos la credencial con el correo del usuario y su contraseña actual
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), passActual);

            // 2. Re-autenticamos al usuario
            user.reauthenticate(credential).addOnCompleteListener(authTask -> {
                if (authTask.isSuccessful()) {
                    // 3. Si la re-autenticación es exitosa, ahora sí cambiamos la contraseña
                    user.updatePassword(nuevaPass).addOnCompleteListener(updateTask -> {
                        if (updateTask.isSuccessful()) {
                            Toast.makeText(CambiarPassActivity.this, "Contraseña actualizada con éxito", Toast.LENGTH_SHORT).show();
                            finish(); // Regresa a la pantalla anterior
                        } else {
                            Toast.makeText(CambiarPassActivity.this, "Error al actualizar: " + updateTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    // Si falla aquí, es porque escribieron mal su contraseña actual
                    Toast.makeText(CambiarPassActivity.this, "La contraseña actual es incorrecta", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Error: Ningún usuario ha iniciado sesión", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean esPasswordSegura(String password) {
        // Expresión regular que valida:
        // (?=.*[0-9])       - Al menos un número
        // (?=.*[a-z])       - Al menos una minúscula
        // (?=.*[A-Z])       - Al menos una mayúscula
        // (?=.*[@#$%^&+=!]) - Al menos un carácter especial
        // .{6,}             - Mínimo 6 caracteres de longitud
        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_\\-]).{6,}$";
        return password.matches(regex);
    }
}