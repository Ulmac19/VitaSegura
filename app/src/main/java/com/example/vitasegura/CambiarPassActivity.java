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

/**
 * Pantalla de cambio de contraseña, reutilizada en dos flujos:
 *
 * - Recuperación (flujoRecuperacion = true): el usuario olvidó su contraseña, por
 *   lo que se omite la contraseña actual y el cambio se delega en la Cloud
 *   Function cambiarPasswordOlvidada.
 * - Cambio normal: el usuario autenticado se reautentica con su contraseña actual
 *   antes de actualizarla.
 */
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

        // Determina el flujo de origen (recuperación o cambio normal)
        if (getIntent() != null) {
            vieneDeRecuperacion = getIntent().getBooleanExtra("flujoRecuperacion", false);
            correoRecuperacion = getIntent().getStringExtra("correoUsuario");
        }

        // En el flujo de recuperación no se solicita la contraseña actual
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

    /** Cambia la contraseña a través de la Cloud Function (flujo de recuperación). */
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

        // Datos enviados a la Cloud Function
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
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** Reautentica al usuario con su contraseña actual y luego la actualiza. */
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
            // 1. Construye la credencial con el correo y la contraseña actual
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), passActual);

            // 2. Reautentica al usuario (requisito de Firebase para operaciones sensibles)
            user.reauthenticate(credential).addOnCompleteListener(authTask -> {
                if (authTask.isSuccessful()) {
                    // 3. Una vez reautenticado, actualiza la contraseña
                    user.updatePassword(nuevaPass).addOnCompleteListener(updateTask -> {
                        if (updateTask.isSuccessful()) {
                            Toast.makeText(CambiarPassActivity.this, "Contraseña actualizada con éxito", Toast.LENGTH_SHORT).show();
                            FirebaseAuth.getInstance().signOut();
                            Intent intent = new Intent(CambiarPassActivity.this, LoginActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(CambiarPassActivity.this, "Error al actualizar: " + updateTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    // El fallo en la reautenticación indica que la contraseña actual es incorrecta
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