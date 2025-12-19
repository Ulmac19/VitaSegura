package com.example.vitasegura;

import android.content.Context;
import android.content.Intent;
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

        // Iniciar cuenta regresiva de 5 segundos
        iniciarContador(6000);

        btnCancelar.setOnClickListener(v -> {
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            if (vibrator != null) {
                vibrator.cancel();
            }
            Toast.makeText(this, "Alerta cancelada", Toast.LENGTH_SHORT).show();
            finish(); // Regresa al menú principal
        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void iniciarContador(long tiempo) {
        countDownTimer = new CountDownTimer(tiempo, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int segundos = (int) (millisUntilFinished / 1000);
                tvContador.setText(String.valueOf(segundos));

                // Vibrar en cada segundo
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

    private void enviarAlertaFirebase() {
        // Aquí irá la lógica para subir la ubicación y signos vitales a Firebase
        Toast.makeText(this, "¡AYUDA SOLICITADA!", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(AlertaEmergenciaActivity.this, MainAdultoActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}