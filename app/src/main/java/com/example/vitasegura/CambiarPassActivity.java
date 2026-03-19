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
import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
import java.util.Map;

public class CambiarPassActivity extends AppCompatActivity {

    private EditText etPassActual, etNuevaPass, etConfirmarPass;
    private Button btnConfirmar;
    private ImageView ivBack;

    private boolean vieneDeRecuperacion = false;
    private String correoRecuperacion = "";
    private FirebaseFunctions mFunctions;
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

        mFunctions = FirebaseFunctions.getInstance();

        // Verificamos de dónde viene el usuario
        if (getIntent() != null) {
            vieneDeRecuperacion = getIntent().getBooleanExtra("flujoRecuperacion", false);
            correoRecuperacion = getIntent().getStringExtra("correoUsuario");
        }

        // Si viene de recuperar contraseña, no le pedimos la actual porque no se la sabe
        if (vieneDeRecuperacion) {
            etPassActual.setVisibility(View.GONE);
        }



        ivBack.setOnClickListener(v -> finish());
        btnConfirmar.setOnClickListener(v -> {
            if (vieneDeRecuperacion) {
                cambiarPassDesdeNube();
            } else {
                reautenticarYCambiarPassword();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void cambiarPassDesdeNube() {
        String nuevaPass = etNuevaPass.getText().toString().trim();
        String confirmarPass = etConfirmarPass.getText().toString().trim();

        if (nuevaPass.isEmpty() || confirmarPass.isEmpty()) {
            Toast.makeText(this, "Por favor llena ambos campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!nuevaPass.equals(confirmarPass)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!esPasswordSegura(nuevaPass)) {
            Toast.makeText(this, "La contraseña no cumple con los requisitos de seguridad", Toast.LENGTH_LONG).show();
            return;
        }

        btnConfirmar.setEnabled(false);
        btnConfirmar.setText("Cambiando...");

        // Preparamos los datos para Node.js
        Map<String, Object> data = new HashMap<>();
        data.put("email", correoRecuperacion);
        data.put("newPassword", nuevaPass);

        mFunctions.getHttpsCallable("cambiarPasswordOlvidada")
                .call(data)
                .addOnCompleteListener(task -> {
                    btnConfirmar.setEnabled(true);
                    btnConfirmar.setText("Confirmar");

                    if (task.isSuccessful()) {
                        Toast.makeText(this, "¡Contraseña recuperada con éxito!", Toast.LENGTH_LONG).show();
                        // Terminamos y el usuario ya puede ir al Login a probar su nueva clave
                        finish();
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
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