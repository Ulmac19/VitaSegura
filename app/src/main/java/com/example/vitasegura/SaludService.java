package com.example.vitasegura;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import java.util.HashMap;

public class SaludService extends Service {
    private DatabaseReference mDatabase;
    private String uidAdulto;

    private final BroadcastReceiver receptorBluetooth = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String rawData = intent.getStringExtra("valor_raw");
            if (rawData != null) procesarYEnviar(rawData);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uidAdulto = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // Registrar el receptor para captar datos del BluetoothServiceManager
        IntentFilter filter = new IntentFilter("DATA_PULSERA_REAL");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receptorBluetooth, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receptorBluetooth, filter);
        }

        mostrarNotificacion();
    }

    private void mostrarNotificacion() {
        String canalId = "salud_service_channel";
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(canalId, "VitaSegura Activo", NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, canalId)
                .setContentTitle("VitaSegura: Monitoreo Activo")
                .setContentText("Tu pulsera está enviando datos de salud en tiempo real")
                .setSmallIcon(R.drawable.atras) // Asegúrate de que este icono exista
                .setOngoing(true)
                .build();

        startForeground(1, notification);
    }

    private void procesarYEnviar(String rawData) {
        String cleanData = rawData.trim();

        // Envío de Salud (BPM y SpO2)
        if (cleanData.contains("BPM:") && cleanData.contains("SpO2:")) {
            try {
                String bpmStr = cleanData.split("BPM:")[1].trim().split(" ")[0];
                String oxiStr = cleanData.split("SpO2:")[1].trim().split("%")[0];

                HashMap<String, Object> salud = new HashMap<>();
                salud.put("bpm", Float.parseFloat(bpmStr));
                salud.put("oxi", Float.parseFloat(oxiStr));
                salud.put("timestamp", ServerValue.TIMESTAMP);
                mDatabase.child("Usuarios").child(uidAdulto).child("MonitoreoActual").setValue(salud);
            } catch (Exception e) {}
        }

        // Envío de Ubicación (LAT y LON)
        if (cleanData.contains("LAT:") && cleanData.contains("LON:")) {
            try {
                String[] partes = cleanData.split(" ");
                String latStr = "", lonStr = "";
                for(String p : partes) {
                    if(p.startsWith("LAT:")) latStr = p.replace("LAT:", "");
                    if(p.startsWith("LON:")) lonStr = p.replace("LON:", "");
                }
                if(!latStr.isEmpty() && !lonStr.isEmpty()) {
                    HashMap<String, Object> gps = new HashMap<>();
                    gps.put("latitud", Double.parseDouble(latStr));
                    gps.put("longitud", Double.parseDouble(lonStr));
                    gps.put("ultima_actualizacion", ServerValue.TIMESTAMP);
                    mDatabase.child("Usuarios").child(uidAdulto).child("Ubicacion").setValue(gps);
                }
            } catch (Exception e) {}
        }

        // --- ENVÍO DE ALERTAS (CAÍDAS Y SOS) ---
        if (cleanData.contains("ALERTA:")) {
            try {
                // Extraemos si es SOS o CAIDA
                String tipoAlerta = cleanData.split("ALERTA:")[1].trim().split(" ")[0];

                HashMap<String, Object> alerta = new HashMap<>();
                alerta.put("tipo", tipoAlerta);
                alerta.put("timestamp", ServerValue.TIMESTAMP);
                alerta.put("leida", false); // Útil para que el familiar sepa si es nueva

                // Guardamos en una rama especial de Alertas
                mDatabase.child("Usuarios").child(uidAdulto).child("Alertas").setValue(alerta);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Opcional: Si no hay alerta en el mensaje actual, podemos limpiar la alerta en Firebase
            // después de cierto tiempo o dejar que el familiar la borre manualmente.
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // El servicio se reinicia automáticamente si el sistema lo cierra
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receptorBluetooth);
    }
}