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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class SaludService extends Service {
    private DatabaseReference mDatabase;
    private String uidAdulto;

    // --- Variables para Medicamentos ---
    private List<Medicamento> listaMeds = new ArrayList<>();
    private Handler medHandler = new Handler(Looper.getMainLooper());
    private Runnable medRunnable;
    private String ultimaHoraNotificada = "";

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
            iniciarVigilanciaMedicamentos();
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
                .setSmallIcon(R.drawable.atras)
                .setOngoing(true)
                .build();

        startForeground(1, notification);
    }

    // --- Lógica de Notificación de Medicamentos ---
    private void iniciarVigilanciaMedicamentos() {
        // Escuchar medicamentos en tiempo real desde Firebase
        mDatabase.child("Usuarios").child(uidAdulto).child("Medicamentos")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        listaMeds.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Medicamento m = ds.getValue(Medicamento.class);
                            if (m != null) listaMeds.add(m);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        // Tarea repetitiva cada 60 segundos
        medRunnable = new Runnable() {
            @Override
            public void run() {
                revisarHoraMedicamentos();
                medHandler.postDelayed(this, 60000);
            }
        };
        medHandler.post(medRunnable);
    }

    private void revisarHoraMedicamentos() {
        String horaActual = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        // Evitar múltiples notificaciones en el mismo minuto
        if (horaActual.equals(ultimaHoraNotificada)) return;

        for (Medicamento med : listaMeds) {
            if (med.getHora() != null && med.getHora().equals(horaActual)) {
                dispararNotificacionMed(med);
                ultimaHoraNotificada = horaActual;
            }
        }
    }

    private void dispararNotificacionMed(Medicamento med) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String canalMedId = "canal_medicamentos";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(canalMedId, "Recordatorios de Medicación", NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, canalMedId)
                .setSmallIcon(R.drawable.atras) // Puedes cambiarlo por un icono de pastilla
                .setContentTitle("¡Hora de tu medicamento!")
                .setContentText("Es momento de tomar: " + med.getNombre() + " (" + med.getDosis() + ")")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true);

        if (manager != null) {
            manager.notify(med.getNombre().hashCode(), builder.build());
        }
    }

    private void procesarYEnviar(String rawData) {
        String cleanData = rawData.trim();

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

        if (cleanData.contains("ALERTA:")) {
            try {
                String tipoAlerta = cleanData.split("ALERTA:")[1].trim().split(" ")[0];
                HashMap<String, Object> alerta = new HashMap<>();
                alerta.put("tipo", tipoAlerta);
                alerta.put("timestamp", ServerValue.TIMESTAMP);
                alerta.put("leida", false);
                mDatabase.child("Usuarios").child(uidAdulto).child("Alertas").setValue(alerta);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (medHandler != null && medRunnable != null) medHandler.removeCallbacks(medRunnable);
        unregisterReceiver(receptorBluetooth);
    }
}