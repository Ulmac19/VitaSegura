package com.example.vitasegura;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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

public class ServicioNotificacionesAbuelo extends Service {

    private static final String CANAL_ID = "Canal_Servicio_Vita";
    private DatabaseReference mDatabase;
    private String miUid;

    @Override
    public void onCreate() {
        super.onCreate();

        // Crear el canal para el Foreground Service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CANAL_ID, "Servicio en Segundo Plano", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(canal);
        }

        //Iniciar el Foreground Service (Obligatorio en Android para que no lo maten)
        Notification notificacionPersistente = new NotificationCompat.Builder(this, CANAL_ID)
                .setContentTitle("VitaSegura Activa")
                .setContentText("Escuchando notificaciones...")
                .setSmallIcon(R.drawable.medicamentos_logo) // Tu ícono
                .build();
        startForeground(1, notificacionPersistente);

        //Empezar a escuchar Firebase
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            miUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            mDatabase = FirebaseDatabase.getInstance().getReference().child("Usuarios").child(miUid).child("NotificacionesPendientes");
            escucharNotificaciones();
        }
    }

    private void escucharNotificaciones() {
        mDatabase.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (snapshot.exists()) {
                    String titulo = snapshot.child("titulo").getValue(String.class);
                    String mensaje = snapshot.child("mensaje").getValue(String.class);

                    if (titulo != null && mensaje != null) {
                        lanzarAlarmaVisual(titulo, mensaje);
                        snapshot.getRef().removeValue(); // Borramos para no repetir
                    }
                }
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void lanzarAlarmaVisual(String titulo, String mensaje) {
        String canalAlarma = "Canal_Alarmas_Meds";
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    canalAlarma, "Alertas de Medicamentos", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(canal);
        }

        Intent intentMenu = new Intent(this, MedicamentosAbueloActivity.class);
        intentMenu.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                intentMenu, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, canalAlarma)
                .setSmallIcon(R.drawable.medicamentos_logo)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Importancia ALTA para que suene
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Si el sistema lo mata, lo reinicia automáticamente
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}