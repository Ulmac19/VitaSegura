package com.example.vitasegura;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ServicioAlertasCuidador extends Service {

    private static final String CANAL_SERVICIO_ID = "Canal_Vigilancia_Cuidador";
    private DatabaseReference mDatabase;
    private String uidCuidador;

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Crear el canal para el servicio persistente
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CANAL_SERVICIO_ID, "Vigilancia Activa", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(canal);
        }

        // 2. Iniciar el Foreground Service (Obligatorio para que no muera al apagar pantalla)
        Notification notificacionPersistente = new NotificationCompat.Builder(this, CANAL_SERVICIO_ID)
                .setContentTitle("Modo Cuidador Activo")
                .setContentText("Vigilando signos vitales del abuelo...")
                .setSmallIcon(R.drawable.pulsera)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true) // Impide que el usuario la deslice para borrarla
                .build();

        startForeground(2, notificacionPersistente);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uidCuidador = FirebaseAuth.getInstance().getCurrentUser().getUid();
            mDatabase = FirebaseDatabase.getInstance().getReference();
            // Activamos persistencia para que Firebase no se desconecte al apagar pantalla
            mDatabase.keepSynced(true);
            buscarVinculoYEscuchar();
        }
    }

    private void buscarVinculoYEscuchar() {
        mDatabase.child("Vinculos").child(uidCuidador).child("id_adulto_vinculado").get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String idAbuelo = snapshot.getValue(String.class);
                        escucharEmergencias(idAbuelo);
                    }
                });
    }

    private void escucharEmergencias(String idAbuelo) {
        DatabaseReference refEmergencias = mDatabase.child("Usuarios").child(idAbuelo).child("EmergenciasPendientes");

        refEmergencias.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (snapshot.exists()) {
                    String mensaje = snapshot.child("mensaje").getValue(String.class);
                    Long timestamp = snapshot.child("timestamp").getValue(Long.class);

                    if (mensaje != null && timestamp != null) {
                        long tiempoActual = System.currentTimeMillis();

                        //Filtro de Tiempo: Solo alertas de los últimos 10 minutos (600,000 milisegundos)
                        if (tiempoActual - timestamp <= 600000) {

                            //Filtro de Memoria: Verificamos si esta alerta exacta ya la hicimos sonar
                            android.content.SharedPreferences prefs = getSharedPreferences("HistorialAlertas", Context.MODE_PRIVATE);
                            String idAlerta = snapshot.getKey(); // El ID único que genera Firebase

                            if (!prefs.getBoolean(idAlerta, false)) {
                                lanzarAlarmaRoja(mensaje);

                                // Codigo para guardar en el historial local de alertas
                                NotificacionesDBHelper db = new NotificacionesDBHelper(getApplicationContext());

                                // Generamos la fecha y hora actuales en formato texto
                                String fechaActual = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date());
                                String horaActual = new java.text.SimpleDateFormat("HH:mm a", java.util.Locale.getDefault()).format(new java.util.Date());

                                // Insertamos la alerta como EMERGENCIA (true)
                                db.insertarNotificacion(mensaje, horaActual, fechaActual, true, tiempoActual);

                                // Guardamos que ya sonó para no repetirla si cerramos y abrimos la app
                                prefs.edit().putBoolean(idAlerta, true).apply();
                            }
                        }

                    }
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void lanzarAlarmaRoja(String mensaje) {
        // --- CÓDIGO PARA DESPERTAR EL CELULAR ---
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                    PowerManager.ACQUIRE_CAUSES_WAKEUP |
                    PowerManager.ON_AFTER_RELEASE, "VitaSegura:AlertaSOS");
            wl.acquire(10000); // Mantiene el sistema despierto 10 segundos para asegurar que suene
        }

        String canalAlarmaId = "Canal_Emergencia_Roja";
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    canalAlarmaId, "Emergencias S.O.S", NotificationManager.IMPORTANCE_HIGH);

            // Esto permite que la notificación se vea sobre la pantalla de bloqueo
            canal.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            canal.enableVibration(true);
            canal.setVibrationPattern(new long[]{0, 1000, 500, 1000, 500, 1000});

            if (manager != null) manager.createNotificationChannel(canal);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, canalAlarmaId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("🚨 ¡EMERGENCIA S.O.S! 🚨")
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM) // Categoría Alarma para bypass de "No molestar"
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setVibrate(new long[]{0, 1000, 500, 1000, 500, 1000})
                .setAutoCancel(true);

        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Asegura que el servicio se reinicie si el sistema lo mata
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}