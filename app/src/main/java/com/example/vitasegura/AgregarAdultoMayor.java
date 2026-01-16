package com.example.vitasegura;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AgregarAdultoMayor extends AppCompatActivity {

    private EditText etCodigo;
    private Button btnVincular;
    private ImageView btnAtras;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private String uidCuidador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_agregar_adulto_mayor);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        uidCuidador = mAuth.getCurrentUser().getUid();

        etCodigo = findViewById(R.id.et_codigo_vinculacion);
        btnVincular = findViewById(R.id.btn_vincular);
        btnAtras = findViewById(R.id.btn_atras);

        btnAtras.setOnClickListener(v -> finish());
        btnVincular.setOnClickListener(v -> buscarCodigo());


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void buscarCodigo() {
        String codigoIngresado = etCodigo.getText().toString().trim().toUpperCase();

        if (codigoIngresado.isEmpty()) {
            Toast.makeText(this, "Ingresa el código del Adulto Mayor", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Buscamos el código en la rama temporal
        mDatabase.child("Codigos_Temporales").child(codigoIngresado).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        // El valor guardado bajo el código es el UID del abuelo
                        String uidAbuelo = task.getResult().getValue(String.class);
                        procederAVincular(uidAbuelo, codigoIngresado);
                    } else {
                        Toast.makeText(this, "Código no válido o ya expiró", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void procederAVincular(String uidAbuelo, String codigoUsado) {
        // 2. Creamos el vínculo permanente: Vinculos -> UID_Cuidador -> UID_Abuelo
        mDatabase.child("Vinculos").child(uidCuidador).child("id_adulto_vinculado").setValue(uidAbuelo)
                .addOnSuccessListener(aVoid -> {

                    // 3. Borramos el código temporal para que no se use de nuevo
                    mDatabase.child("Codigos_Temporales").child(codigoUsado).removeValue();

                    Toast.makeText(this, "¡Vinculación Exitosa!", Toast.LENGTH_SHORT).show();

                    // Regresamos al panel principal del familiar
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al crear el vínculo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}