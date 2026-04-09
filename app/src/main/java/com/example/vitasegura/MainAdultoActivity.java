package com.example.vitasegura;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class MainAdultoActivity extends AppCompatActivity {

    //Variables para verificar conexión
    private TextView tvSinConexion;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

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

        //Declarar botón de generar código
        btnGenerarCodigo = findViewById(R.id.btn_generar_codigo);
        btnGenerarCodigo.setOnClickListener(v -> gestionarCodigoVinculacion());

        //Verificar conexión
        tvSinConexion = findViewById(R.id.tv_sin_conexion);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        //Verificar el estado inicial al abrir la app
        verificarConexionInicial();

        //Crear el oyente que reacciona en tiempo real
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                // Hay internet -> Ocultar banner
                runOnUiThread(() -> tvSinConexion.setVisibility(View.GONE));
            }

            @Override
            public void onLost(@NonNull Network network) {
                // Se fue el internet -> Mostrar banner
                runOnUiThread(() -> tvSinConexion.setVisibility(View.VISIBLE));
            }
        };

        //Registrar el oyente
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(request, networkCallback);

        //Declarar botones de la pantalla
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

        Intent serviceIntent = new Intent(this, SaludService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        Intent servicioIntent = new Intent(this, ServicioNotificacionesAbuelo.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(servicioIntent);
        } else {
            startService(servicioIntent);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    //Metodo para verificar si hay conexión
    private void verificarConexionInicial() {
        if(connectivityManager != null){
            Network activeNetwork = connectivityManager.getActiveNetwork();
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork); //Verifica si hay internet
            boolean isConnected = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

            if(!isConnected){
                tvSinConexion.setVisibility(View.VISIBLE);
            }
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        //Destruir el oyente para no saturar la memoria
        if(connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }

    private void gestionarCodigoVinculacion() {
        String caracteres = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder codigo = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int index = (int) (Math.random() * caracteres.length());
            codigo.append(caracteres.charAt(index));
        }
        String codigoFinal = codigo.toString();

        // Generar código temporal
        long tiempoExpiracion = System.currentTimeMillis() + (15*60*1000);

        Map<String, Object> datosCodigo = new HashMap<>();
        datosCodigo.put("id_adulto_vinculado", uidAbuelo);
        datosCodigo.put("expiresAt", tiempoExpiracion);

        mDatabase.child("Codigos_Temporales").child(codigoFinal).setValue(datosCodigo)
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