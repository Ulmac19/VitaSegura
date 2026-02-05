package com.example.vitasegura;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainAdultoActivity extends AppCompatActivity {

    private LinearLayout btnSalud, btnMeds, btnEmergencia, btnInfo, btnGenerarCodigo;
    private TextView tvNombre;
    private ImageView btn_vincular_pulsera;

    private String uidAbuelo;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_adulto);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        uidAbuelo = FirebaseAuth.getInstance().getCurrentUser().getUid();

        tvNombre = findViewById(R.id.tv_bienvenida_adulto);

        mDatabase.child("Usuarios").child(uidAbuelo).child("nombre").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String nombre = task.getResult().getValue(String.class);
                tvNombre.setText("Bienvenido, " + nombre);
            }
        });

        btnGenerarCodigo = findViewById(R.id.btn_generar_codigo);
        btnGenerarCodigo.setOnClickListener(v -> gestionarCodigoVinculacion());




        btnSalud = findViewById(R.id.btn_salud);
        btnMeds = findViewById(R.id.btn_medicamentos);
        btnEmergencia = findViewById(R.id.btn_emergencia);
        btnInfo = findViewById(R.id.btn_info_personal);
        btn_vincular_pulsera = findViewById(R.id.btn_vincular_pulsera);

        // Ejemplo de navegación al módulo de salud
        btnSalud.setOnClickListener(v -> {
            Intent intent = new Intent(MainAdultoActivity.this, MonitoreoSaludActivity.class);
            startActivity(intent);

        });

        // Ejemplo de navegación al módulo de medicamentos
        btnMeds.setOnClickListener(v -> {
            Intent intent = new Intent(MainAdultoActivity.this, MedicamentosAbueloActivity.class);
            startActivity(intent);
        });

        // Ejemplo de navegación al módulo de información personal
        btnInfo.setOnClickListener(v -> {
            Intent intent = new Intent(MainAdultoActivity.this, MiInformacionActivity.class);
            startActivity(intent);
        });


        // El botón de emergencia podría disparar una alerta directa
        btnEmergencia.setOnClickListener(v -> {
            Intent intent = new Intent(MainAdultoActivity.this, AlertaEmergenciaActivity.class);
            startActivity(intent);
        });

        btn_vincular_pulsera.setOnClickListener(v -> {
            Intent intent = new Intent(MainAdultoActivity.this, PulseraActivity.class);
            startActivity(intent);

        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void gestionarCodigoVinculacion() {
        // 1. Generar código de 6 caracteres alfanuméricos
        String caracteres = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Evitamos O, 0, I, 1 para evitar confusión
        StringBuilder codigo = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int index = (int) (Math.random() * caracteres.length());
            codigo.append(caracteres.charAt(index));
        }
        String codigoFinal = codigo.toString();

        // 2. Guardar en Firebase (Rama temporal)
        mDatabase.child("Codigos_Temporales").child(codigoFinal).setValue(uidAbuelo)
                .addOnSuccessListener(aVoid -> {
                    // 3. Mostrar el código al abuelo
                    mostrarDialogoCodigo(codigoFinal);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al generar código", Toast.LENGTH_SHORT).show();
                });
    }

    private void mostrarDialogoCodigo(String codigo) {
        // Creamos un diseño inflado o un simple AlertDialog con letra muy grande
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Código para tu familiar");

        TextView tvCodigo = new TextView(this);
        tvCodigo.setText(codigo);
        tvCodigo.setTextSize(60); // Letra gigante para que la vean bien
        tvCodigo.setPadding(0, 50, 0, 50);
        tvCodigo.setGravity(Gravity.CENTER);
        tvCodigo.setTextColor(Color.parseColor("#23608C")); // El azul oscuro que usas
        tvCodigo.setTypeface(null, Typeface.BOLD);

        builder.setView(tvCodigo);
        builder.setPositiveButton("Listo", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}