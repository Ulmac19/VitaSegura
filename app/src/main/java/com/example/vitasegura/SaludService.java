package com.example.vitasegura;

import android.annotation.SuppressLint;
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
import android.os.PowerManager;

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

    // --- Control de Spam de Alertas ---
    private long ultimaAlertaEnviada = 0;
    private static final long INTERVALO_MINIMO_ALERTA = 15000; // 15 segundos

    // --- Control de Spam de Signos Vitales ---
    private long ultimaAlertaBpm = 0;
    private long ultimaAlertaOxi = 0;
    private static final long COOLDOWN_VITALES = 600000; // 10 minutos en milisegundos

    private final BroadcastReceiver receptorBluetooth = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String rawData = intent.getStringExtra("valor_raw");
            if (rawData != null) procesarYEnviar(rawData);
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
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
                .setSmallIcon(R.drawable.pulsera)
                .setOngoing(true)
                .build();

        startForeground(1, notification);
    }

    // --- Lógica de Notificación de Medicamentos ---
    private void iniciarVigilanciaMedicamentos() {
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
            channel.setVibrationPattern(new long[]{0, 500, 200, 500, 200, 500});
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, canalMedId)
                .setSmallIcon(R.drawable.medicamentos_logo)
                .setContentTitle("¡HORA DE MEDICAMENTO!")
                .setContentText("Toma tu: " + med.getNombre())
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setFullScreenIntent(null, true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true);

        if (manager != null) {
            manager.notify(med.getNombre().hashCode(), builder.build());
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isInteractive()) {
                PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "VitaSegura:DespertarPantalla");
                wl.acquire(5000);
            }
        }
    }

    private void procesarYEnviar(String rawData) {
        String cleanData = rawData.trim();

        // --- 1. PROCESAR SIGNOS VITALES ---
        if (cleanData.contains("BPM:") && cleanData.contains("SpO2:")) {
            try {
                String bpmStr = cleanData.split("BPM:")[1].trim().split(" ")[0];
                String oxiStr = cleanData.split("SpO2:")[1].trim().split("%")[0];
                float bpm = Float.parseFloat(bpmStr);
                float oxi = Float.parseFloat(oxiStr);

                HashMap<String, Object> salud = new HashMap<>();
                salud.put("bpm", bpm);
                salud.put("oxi", oxi);
                salud.put("timestamp", ServerValue.TIMESTAMP);
                mDatabase.child("Usuarios").child(uidAdulto).child("MonitoreoActual").setValue(salud);

                evaluarSignosVitales(bpm, oxi);
            } catch (Exception e) {}
        }

        // --- 2. PROCESAR UBICACIÓN ---
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

        // --- 3. PROCESAR ALERTAS (CON FILTRO DE SPAM) ---
        if (cleanData.contains("ALERTA:")) {
            long tiempoActual = System.currentTimeMillis();

            // Solo procesamos si han pasado más de 15 segundos desde la última alerta enviada
            if (tiempoActual - ultimaAlertaEnviada > INTERVALO_MINIMO_ALERTA) {
                try {
                    String tipoAlerta = cleanData.split("ALERTA:")[1].trim().split(" ")[0];
                    HashMap<String, Object> emergencia = new HashMap<>();

                    if (tipoAlerta.equalsIgnoreCase("SOS")) {
                        emergencia.put("mensaje", "¡Botón de pánico presionado! Entra a la app para ver la ubicación.");
                    } else if (tipoAlerta.equalsIgnoreCase("CAIDA")) {
                        emergencia.put("mensaje", "¡Se ha detectado una caída automática!");
                    } else {
                        emergencia.put("mensaje", "Alerta: " + tipoAlerta);
                    }

                    emergencia.put("tipo", tipoAlerta);
                    emergencia.put("timestamp", ServerValue.TIMESTAMP);

                    DatabaseReference refEmergencias = mDatabase.child("Usuarios").child(uidAdulto).child("EmergenciasPendientes");

                    // Limpiamos alertas anteriores y subimos la nueva
                    refEmergencias.setValue(null).addOnCompleteListener(task -> {
                        refEmergencias.push().setValue(emergencia);
                    });

                    ultimaAlertaEnviada = tiempoActual;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void evaluarSignosVitales(float bpm, float oxi) {
        long tiempoActual = System.currentTimeMillis();

        //1.Evaluar Frecuencia Cardiaca (Normal: 60-100 bpm)
        if(tiempoActual - ultimaAlertaBpm > COOLDOWN_VITALES) {
            if(bpm > 100){
                enviarAlertaInfo("Ritmo cardíaco elevado (Taquicardia): " + Math.round(bpm) + " lpm", "INFO_BPM");
                ultimaAlertaBpm = tiempoActual;
            } else if (bpm < 60 && bpm > 30) { //30 para evitar avisos falsos si la pulsera es retirada
                enviarAlertaInfo("Ritmo cardíaco bajo (Bradycardia): " + Math.round(bpm) + " lpm", "INFO_BPM");
                ultimaAlertaBpm = tiempoActual;
            }
        }

        //2.Evaluar Oxigeno (Normal: > 95%)
        if(tiempoActual - ultimaAlertaOxi > COOLDOWN_VITALES) {
            if(oxi <= 94 && oxi > 50){ //50 para evitar falsos positivos
                String gravedad = (oxi <= 90) ? "moderada/grave" : "leve";
                enviarAlertaInfo("Hipoxemia " + gravedad + ". Oxigeno al " + Math.round(oxi) + "%", "INFO_OXI");
                ultimaAlertaOxi = tiempoActual;
            }
        }
    }

    private void enviarAlertaInfo(String mensaje, String tipo) {
        HashMap<String, Object> info = new HashMap<>();
        info.put("mensaje", mensaje);
        info.put("tipo", tipo); //Dice si es info o emergencia
        info.put("timestamp", ServerValue.TIMESTAMP);

        //Nodo de las emergencias pendientes
        mDatabase.child("Usuarios").child(uidAdulto).child("EmergenciasPendientes")
                .push().setValue(info);
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