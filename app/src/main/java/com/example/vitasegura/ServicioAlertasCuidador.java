package com.example.vitasegura;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import com.google.firebase.database.ValueEventListener;

/**
 * Servicio en primer plano que vigila las emergencias del adulto mayor vinculado
 * y avisa al cuidador.
 *
 * Resuelve dinámicamente a qué adulto escuchar mediante el nodo Vinculos y, por
 * cada nueva alerta, la guarda siempre en el historial local (SQLite). La
 * notificación push solo se lanza para alertas de los últimos 10 minutos y
 * cuando el cuidador no las ha desactivado en su configuración.
 */
public class ServicioAlertasCuidador extends Service {

    private static final String CANAL_SERVICIO_ID = "Canal_Vigilancia_Cuidador";
    private DatabaseReference mDatabase;
    private String uidCuidador;
    private String idAbueloEscuchando = null;
    private DatabaseReference refEmergenciasActual = null;
    private ChildEventListener listenerEmergencias = null;

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Canal de notificación del servicio persistente
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CANAL_SERVICIO_ID, "Vigilancia Activa", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(canal);
        }

        // 2. Notificación persistente que mantiene vivo el foreground service
        Notification notificacionPersistente = new NotificationCompat.Builder(this, CANAL_SERVICIO_ID)
                .setContentTitle("Modo Cuidador Activo")
                .setContentText("Vigilando signos vitales del abuelo...")
                .setSmallIcon(R.drawable.pulsera)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true) // no se puede descartar deslizando
                .build();

        startForeground(2, notificacionPersistente);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uidCuidador = FirebaseAuth.getInstance().getCurrentUser().getUid();
            mDatabase = FirebaseDatabase.getInstance().getReference();
            // Mantiene la sincronización aunque la app esté en segundo plano
            mDatabase.keepSynced(true);
            buscarVinculoYEscuchar();
        }
    }

    /**
     * Observa el vínculo del cuidador y (re)suscribe el listener de emergencias
     * cuando cambia el adulto mayor asociado.
     */
    private void buscarVinculoYEscuchar() {
        mDatabase.child("Vinculos").child(uidCuidador).child("id_adulto_vinculado")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String nuevoId = snapshot.getValue(String.class);
                            if (nuevoId != null && !nuevoId.equals(idAbueloEscuchando)) {
                                if (refEmergenciasActual != null && listenerEmergencias != null) {
                                    refEmergenciasActual.removeEventListener(listenerEmergencias);
                                }
                                idAbueloEscuchando = nuevoId;
                                escucharEmergencias(nuevoId);
                            }
                        } else {
                            if (refEmergenciasActual != null && listenerEmergencias != null) {
                                refEmergenciasActual.removeEventListener(listenerEmergencias);
                                listenerEmergencias = null;
                                refEmergenciasActual = null;
                                idAbueloEscuchando = null;
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    /**
     * Suscribe el listener de EmergenciasPendientes del adulto indicado, guarda
     * cada alerta en el historial y dispara la notificación cuando corresponde.
     */
    private void escucharEmergencias(String idAbuelo) {
        refEmergenciasActual = mDatabase.child("Usuarios").child(idAbuelo).child("EmergenciasPendientes");

        listenerEmergencias = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (snapshot.exists()) {
                    String mensaje = snapshot.child("mensaje").getValue(String.class);
                    Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                    String tipo = snapshot.child("tipo").getValue(String.class);


                    if (mensaje != null && timestamp != null) {
                        long tiempoActual = System.currentTimeMillis();

                        // Deduplicación: se omite si esta alerta ya fue procesada
                        android.content.SharedPreferences prefs = getSharedPreferences("HistorialAlertas", Context.MODE_PRIVATE);
                        String idAlerta = snapshot.getKey();

                        if (!prefs.getBoolean(idAlerta, false)) {

                            boolean esEmergencia = tipo == null || !tipo.startsWith("INFO_");

                            // 1. Se guarda siempre en el historial local, sin importar la antigüedad
                            NotificacionesDBHelper db = NotificacionesDBHelper.getInstance(getApplicationContext());
                            String fechaActual = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date());
                            String horaActual = new java.text.SimpleDateFormat("HH:mm a", java.util.Locale.getDefault()).format(new java.util.Date());
                            db.insertarNotificacion(mensaje, horaActual, fechaActual, esEmergencia, tiempoActual);
                            prefs.edit().putBoolean(idAlerta, true).apply();

                            // 2. La notificación push solo se lanza para alertas recientes (10 min)
                            if (tiempoActual - timestamp <= 600000) {
                                android.content.SharedPreferences configPrefs = getSharedPreferences("VitaConfig", Context.MODE_PRIVATE);
                                if (!configPrefs.getBoolean("notificaciones_desactivadas", false)) {
                                    if (esEmergencia)
                                        lanzarAlarmaRoja(mensaje);
                                    else
                                        lanzarAlertaInformativa(mensaje, tipo);
                                }
                            }
                        }

                    }
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        refEmergenciasActual.addChildEventListener(listenerEmergencias);
    }

    /** Lanza la notificación de emergencia (S.O.S.) con máxima prioridad y vibración. */
    private void lanzarAlarmaRoja(String mensaje) {
        // Enciende la pantalla para asegurar que la alerta sea visible
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                    PowerManager.ACQUIRE_CAUSES_WAKEUP |
                    PowerManager.ON_AFTER_RELEASE, "VitaSegura:AlertaSOS");
            wl.acquire(10000); // mantiene el sistema despierto 10 segundos
        }

        String canalAlarmaId = "Canal_Emergencia_Roja";
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    canalAlarmaId, "Emergencias S.O.S", NotificationManager.IMPORTANCE_HIGH);

            // Permite mostrar la notificación sobre la pantalla de bloqueo
            canal.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            canal.enableVibration(true);
            canal.setVibrationPattern(new long[]{0, 1000, 500, 1000, 500, 1000});

            if (manager != null) manager.createNotificationChannel(canal);
        }

        Intent intentMapa = new Intent(this, UbicacionAdultoActivity.class);
        intentMapa.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                intentMapa, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, canalAlarmaId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("🚨 ¡EMERGENCIA S.O.S! 🚨")
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM) // categoría alarma para sortear el modo "No molestar"
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setVibrate(new long[]{0, 1000, 500, 1000, 500, 1000})
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    /**
     * Lanza una notificación informativa (no crítica) y la enlaza con la pantalla
     * pertinente según el tipo de aviso (signos vitales, medicación, etc.).
     */
    private void lanzarAlertaInformativa(String mensaje, String tipo){
        String canalInfoId = "Canal_Informacion_Salud";
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel canal = new NotificationChannel(
                    canalInfoId, "Avisos de Salud", NotificationManager.IMPORTANCE_HIGH);
            if (manager != null) manager.createNotificationChannel(canal);
        }

        Intent intent;
        if(tipo != null){
            if(tipo.equals("INFO_BPM") || tipo.equals("INFO_OXI")){
                intent = new Intent(this, InformacionAbueloActivity.class);
            } else if (tipo.equals("INFO_MED")) {
                intent = new Intent(this, MedicamentosCuidadorActivity.class);
            } else{
                intent = new Intent(this, NotificacionesActivity.class);
            }
        } else {
            intent = new Intent(this, MainFamiliarActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, canalInfoId)
                .setSmallIcon(R.drawable.notificacion)
                .setContentTitle("Aviso de Monitoreo")
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        if (manager != null){
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // el sistema reinicia el servicio si lo detiene
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (refEmergenciasActual != null && listenerEmergencias != null) {
            refEmergenciasActual.removeEventListener(listenerEmergencias);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}