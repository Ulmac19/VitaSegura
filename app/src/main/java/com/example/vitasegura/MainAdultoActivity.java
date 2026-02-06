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

        // Recuperar nombre y extraer solo el primer nombre
        mDatabase.child("Usuarios").child(uidAbuelo).child("nombre").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().getValue() != null) {
                String nombreCompleto = task.getResult().getValue(String.class);

                // Lógica para obtener el primer nombre
                String primerNombre = "";
                if (nombreCompleto != null && !nombreCompleto.isEmpty()) {
                    String[] partes = nombreCompleto.split(" ");
                    primerNombre = partes[0]; // Tomamos la primera posición
                }

                tvNombre.setText("Bienvenido,\n" + primerNombre);
            }
        });

        btnGenerarCodigo = findViewById(R.id.btn_generar_codigo);
        btnGenerarCodigo.setOnClickListener(v -> gestionarCodigoVinculacion());

        btnSalud = findViewById(R.id.btn_salud);
        btnMeds = findViewById(R.id.btn_medicamentos);
        btnEmergencia = findViewById(R.id.btn_emergencia);
        btnInfo = findViewById(R.id.btn_info_personal);
        btn_vincular_pulsera = findViewById(R.id.btn_vincular_pulsera);

        btnSalud.setOnClickListener(v -> {
            Intent intent = new Intent(MainAdultoActivity.this, MonitoreoSaludActivity.class);
            startActivity(intent);
        });

        btnMeds.setOnClickListener(v -> {
            Intent intent = new Intent(MainAdultoActivity.this, MedicamentosAbueloActivity.class);
            startActivity(intent);
        });

        btnInfo.setOnClickListener(v -> {
            Intent intent = new Intent(MainAdultoActivity.this, MiInformacionActivity.class);
            startActivity(intent);
        });

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
        String caracteres = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder codigo = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int index = (int) (Math.random() * caracteres.length());
            codigo.append(caracteres.charAt(index));
        }
        String codigoFinal = codigo.toString();

        mDatabase.child("Codigos_Temporales").child(codigoFinal).setValue(uidAbuelo)
                .addOnSuccessListener(aVoid -> {
                    mostrarDialogoCodigo(codigoFinal);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al generar código", Toast.LENGTH_SHORT).show();
                });
    }

    private void mostrarDialogoCodigo(String codigo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Código para tu familiar");

        TextView tvCodigo = new TextView(this);
        tvCodigo.setText(codigo);
        tvCodigo.setTextSize(60);
        tvCodigo.setPadding(0, 50, 0, 50);
        tvCodigo.setGravity(Gravity.CENTER);
        tvCodigo.setTextColor(Color.parseColor("#23608C"));
        tvCodigo.setTypeface(null, Typeface.BOLD);

        builder.setView(tvCodigo);
        builder.setPositiveButton("Listo", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}