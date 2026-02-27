package com.example.vitasegura;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

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

        // 1. Crear el canal para el servicio continuo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CANAL_SERVICIO_ID, "Vigilancia Activa", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(canal);
        }

        // 2. Iniciar el Foreground Service para que Android no lo cierre
        Notification notificacionPersistente = new NotificationCompat.Builder(this, CANAL_SERVICIO_ID)
                .setContentTitle("Modo Cuidador Activo")
                .setContentText("Atento a cualquier emergencia...")
                .setSmallIcon(R.drawable.ubicacion) // Cambia esto por tu ícono
                .build();
        startForeground(2, notificacionPersistente);

        // 3. Buscar a qué abuelo estamos vinculados y empezar a escuchar
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uidCuidador = FirebaseAuth.getInstance().getCurrentUser().getUid();
            mDatabase = FirebaseDatabase.getInstance().getReference();
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
                    if (mensaje != null) {
                        lanzarAlarmaRoja(mensaje);
                        snapshot.getRef().removeValue(); // Borramos la alerta para que no suene doble
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
        String canalAlarmaId = "Canal_Emergencia_Roja";
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Le damos IMPORTANCE_HIGH para que haga ruido y muestre un banner emergente
            NotificationChannel canal = new NotificationChannel(
                    canalAlarmaId, "Emergencias S.O.S", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(canal);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, canalAlarmaId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert) // Ícono de alerta nativo
                .setContentTitle("🚨 ¡EMERGENCIA S.O.S! 🚨")
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVibrate(new long[]{0, 1000, 500, 1000, 500, 1000}) // Patrón de vibración fuerte
                .setAutoCancel(true);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}