package com.example.vitasegura;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * Pantalla de alerta de emergencia del adulto mayor.
 *
 * Muestra una cuenta regresiva con vibración que el usuario puede cancelar. Si
 * llega a cero, envía la alerta de pánico al cuidador: la publica en Firebase si
 * hay conexión o la encola en SQLite para reenviarla cuando vuelva la red.
 */
public class AlertaEmergenciaActivity extends AppCompatActivity {

    private TextView tvContador;
    private Button btnCancelar;
    private CountDownTimer countDownTimer;
    private Vibrator vibrator;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_alerta_emergencia);

        tvContador = findViewById(R.id.tv_contador_emergencia);
        btnCancelar = findViewById(R.id.btn_cancelar_alerta);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Cuenta regresiva de 6 segundos antes de enviar la alerta
        iniciarContador(6000);

        btnCancelar.setOnClickListener(v -> {
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            if (vibrator != null) {
                vibrator.cancel();
            }
            Toast.makeText(this, "Alerta cancelada", Toast.LENGTH_SHORT).show();
            finish();
        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /** Lanza la cuenta regresiva, actualizando el contador y vibrando cada segundo. */
    private void iniciarContador(long tiempo) {
        countDownTimer = new CountDownTimer(tiempo, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int segundos = (int) (millisUntilFinished / 1000);
                tvContador.setText(String.valueOf(segundos));

                // Vibra en cada segundo del conteo
                if (vibrator != null) {
                    vibrator.vibrate(200);
                }
            }
            @Override
            public void onFinish() {
                tvContador.setText("0");
                enviarAlertaFirebase();
            }
        }.start();
    }

    /** Envía la alerta de pánico al cuidador, o la encola si no hay conexión. */
    private void enviarAlertaFirebase() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String miUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        long timestampActual = System.currentTimeMillis();
        String tipo = "SOS_BOTON";
        String mensaje = "¡Botón de pánico presionado! Entra a la app para ver la ubicación.";

        if(hayConexionInternet()){
            DatabaseReference refAlertas = FirebaseDatabase.getInstance().getReference()
                    .child("Usuarios").child(miUid).child("EmergenciasPendientes");

            Map<String, Object> alerta = new HashMap<>();
            alerta.put("tipo", tipo);
            alerta.put("mensaje", mensaje);
            alerta.put("timestamp", timestampActual);

            refAlertas.removeValue(); // descarta alertas anteriores no atendidas
            refAlertas.push().setValue(alerta);

            Toast.makeText(AlertaEmergenciaActivity.this, "¡AYUDA SOLICITADA AL CUIDADOR!", Toast.LENGTH_LONG).show();
        }else {
            // Sin internet: la alerta se encola en SQLite para reenviarse al reconectar
            AlertasOfflineDBHelper dbHelper = AlertasOfflineDBHelper.getInstance(this);
            dbHelper.insertarAlerta(tipo, mensaje, timestampActual);

            Toast.makeText(this, "Sin red: Alerta guardada para reenvío", Toast.LENGTH_LONG).show();
        }

        finish();
    }

    /** Indica si el dispositivo tiene actualmente una red con acceso a internet. */
    private boolean hayConexionInternet(){
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cm != null){
            android.net.Network activeNetwork = cm.getActiveNetwork();
            if(activeNetwork != null){
                android.net.NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
                return capabilities != null && capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);            }
        }
        return false;
    }
}