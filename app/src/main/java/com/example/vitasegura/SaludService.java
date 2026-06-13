package com.example.vitasegura;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Servicio en primer plano que centraliza el monitoreo de salud del adulto mayor.
 *
 * Recibe por broadcast los datos crudos que la pulsera envía vía Bluetooth Low
 * Energy (signos vitales, ubicación y alertas), los procesa y los escribe en
 * Firebase. Además revisa periódicamente los horarios de medicación y lanza los
 * recordatorios locales correspondientes. Las alertas generadas sin conexión se
 * encolan en SQLite mediante AlertasOfflineDBHelper.
 */
public class SaludService extends Service {
    private DatabaseReference mDatabase;
    private String uidAdulto;

    // --- Estado de medicamentos ---
    private List<Medicamento> listaMeds = new ArrayList<>();
    private Handler medHandler = new Handler(Looper.getMainLooper());
    private Runnable medRunnable;
    private java.util.HashMap<String, Long> historialAlertasMeds = new java.util.HashMap<>();

    // --- Antispam de alertas de la pulsera ---
    private long ultimaAlertaEnviada = 0;
    private static final long INTERVALO_MINIMO_ALERTA = 15000; // 15 segundos

    // --- Antispam de alertas de signos vitales ---
    private long ultimaAlertaBpm = 0;
    private long ultimaAlertaOxi = 0;
    private static final long COOLDOWN_VITALES = 600000; // 10 minutos en milisegundos

    private final BroadcastReceiver receptorBluetooth = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String accion = intent.getAction();

            if("DATA_PULSERA_REAL".equals(accion)) {
                String rawData = intent.getStringExtra("valor_raw");
                if (rawData != null) procesarYEnviar(rawData);
            } else if ("PULSERA_DESCONECTADA".equals(accion)) {
                lanzarAlertaDesconexion();
            }
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

        // Registra el receptor para captar tanto los datos como las desconexiones de la pulsera
        IntentFilter filter = new IntentFilter();
        filter.addAction("DATA_PULSERA_REAL");
        filter.addAction("PULSERA_DESCONECTADA");

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

    /**
     * Mantiene en memoria la lista de medicamentos y programa la revisión de
     * horarios cada 10 segundos.
     */
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
                medHandler.postDelayed(this, 10000);
            }
        };
        medHandler.post(medRunnable);
    }

    /**
     * Revisa todos los medicamentos con horario y lanza el recordatorio cuando el
     * minuto actual coincide con alguna de las tomas del ciclo de 24 horas.
     * Aplica un antispam de 2 minutos por medicamento para no repetir el aviso.
     */
    private void revisarHoraMedicamentos() {
        Calendar ahora = Calendar.getInstance();
        int horaActual = ahora.get(Calendar.HOUR_OF_DAY); // formato 0-23
        int minutoActual = ahora.get(Calendar.MINUTE);
        long tiempoActualMs = System.currentTimeMillis();

        // Hora actual expresada en minutos totales del día (ej. 01:00 = 60)
        int minutosActualesTotales = (horaActual * 60) + minutoActual;

        for (Medicamento med : listaMeds) {
            String freq = med.getFrecuencia();

            // Los medicamentos "solo si hay dolor" no tienen horario
            if (freq == null || freq.equals("Solo si hay dolor")) continue;

            try {
                int frecuenciaHoras = Integer.parseInt(freq.replaceAll("[^0-9]", ""));
                if (frecuenciaHoras <= 0) continue; // evita divisiones entre cero
                int frecuenciaEnMinutos = frecuenciaHoras * 60;

                String[] partesHora = med.getHora().split(":");
                int horaInicio = Integer.parseInt(partesHora[0].trim());
                int minutoInicio = Integer.parseInt(partesHora[1].trim());

                int minutosInicioTotales = (horaInicio * 60) + minutoInicio;

                boolean tocaDosis = false;

                // Recorre todas las tomas del ciclo de 24 horas
                int maxTomasAlDia = (24 * 60) / frecuenciaEnMinutos;

                for (int i = 0; i <= maxTomasAlDia; i++) {
                    // Minuto del día en el que cae esta dosis (1440 min = 24 h)
                    int minutoDeTomaEsperada = (minutosInicioTotales + (i * frecuenciaEnMinutos)) % 1440;

                    int diferencia = minutosActualesTotales - minutoDeTomaEsperada;

                    // Coincide en el minuto exacto (0), con un retraso de un minuto (1)
                    // o en el cruce de medianoche de 23:59 a 00:00 (-1439)
                    if (diferencia == 0 || diferencia == 1 || diferencia == -1439) {
                        tocaDosis = true;
                        break;
                    }
                }

                if (tocaDosis) {
                    // Última vez que se notificó este medicamento
                    long ultimaVezQueSono = historialAlertasMeds.containsKey(med.getId()) ? historialAlertasMeds.get(med.getId()) : 0;

                    // Antispam de 2 minutos: garantiza un único aviso por toma
                    if (tiempoActualMs - ultimaVezQueSono > 120000) {
                        dispararNotificacionMed(med);
                        historialAlertasMeds.put(med.getId(), tiempoActualMs);
                    }
                }

            } catch (Exception e) {
                // Frecuencia u hora con formato inválido: se omite este medicamento
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

        Intent intentMeds = new Intent(this, MedicamentosAbueloActivity.class);
        intentMeds.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntentMeds = PendingIntent.getActivity(this, 0,
                intentMeds, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, canalMedId)
                .setSmallIcon(R.drawable.medicamentos_logo)
                .setContentTitle("¡HORA DE MEDICAMENTO!")
                .setContentText("Toma tu: " + med.getNombre())
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setContentIntent(pendingIntentMeds)
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

    /** Lanza una notificación cuando la pulsera pierde la conexión Bluetooth. */
    private void lanzarAlertaDesconexion(){
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String canalId = "canal_desconexion";

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(canalId, "Alertas de Pulsera", NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            if(manager != null) manager.createNotificationChannel(channel);
        }

        Intent intentPulsera = new Intent(this, PulseraActivity.class);
        intentPulsera.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntentPulsera = PendingIntent.getActivity(this, 0,
                intentPulsera, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, canalId)
                .setSmallIcon(R.drawable.pulsera)
                .setContentTitle("!Pulsera desconectada!")
                .setContentText("Tu pulsera VitaSegura ha perdido conexión. Por favor, acércala a tu celular.")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setContentIntent(pendingIntentPulsera)
                .setAutoCancel(true);

        if(manager != null){
            // ID fijo para que la notificación se actualice en lugar de duplicarse
            manager.notify(999, builder.build());
        }
    }

    /**
     * Interpreta la trama de texto recibida de la pulsera y deriva cada bloque a
     * su destino: signos vitales y ubicación se escriben en Firebase, y las
     * alertas se envían (o encolan si no hay conexión).
     */
    private void procesarYEnviar(String rawData) {
        String cleanData = rawData.trim();

        // --- 1. Signos vitales ---
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

        // --- 2. Ubicación GPS ---
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

        // --- 3. Alertas (SOS, caída...) con antispam ---
        if (cleanData.contains("ALERTA:")) {
            long tiempoActual = System.currentTimeMillis();

            // Solo se procesa si pasaron más de 15 segundos desde la última alerta
            if (tiempoActual - ultimaAlertaEnviada > INTERVALO_MINIMO_ALERTA) {
                try {
                    String tipoAlerta = cleanData.split("ALERTA:")[1].trim().split(" ")[0];
                    HashMap<String, Object> emergencia = new HashMap<>();

                    if (tipoAlerta.equalsIgnoreCase("SOS")) {
                        emergencia.put("mensaje", "¡Botón de pánico presionado! Entra a la app para ver la ubicación.");
                    } else if (tipoAlerta.equalsIgnoreCase("CAIDA")) {
                        emergencia.put("mensaje", "¡Se ha detectado una caída!");
                    } else {
                        emergencia.put("mensaje", "Alerta: " + tipoAlerta);
                    }

                    emergencia.put("tipo", tipoAlerta);

                    if (hayConexionInternet()) {
                        emergencia.put("timestamp", ServerValue.TIMESTAMP);
                        DatabaseReference refEmergencias = mDatabase.child("Usuarios").child(uidAdulto).child("EmergenciasPendientes");
                        refEmergencias.setValue(null).addOnCompleteListener(task -> {
                            refEmergencias.push().setValue(emergencia);
                        });
                    } else {
                        String mensajeOffline = (String) emergencia.get("mensaje");
                        AlertasOfflineDBHelper.getInstance(this)
                                .insertarAlerta(tipoAlerta, mensajeOffline, tiempoActual);
                    }

                    ultimaAlertaEnviada = tiempoActual;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Evalúa los signos vitales y emite un aviso informativo al cuidador cuando
     * salen del rango normal. Aplica un cooldown de 10 minutos por tipo de signo.
     */
    private void evaluarSignosVitales(float bpm, float oxi) {
        long tiempoActual = System.currentTimeMillis();

        // 1. Frecuencia cardíaca (rango normal: 60-100 lpm)
        if(tiempoActual - ultimaAlertaBpm > COOLDOWN_VITALES) {
            if(bpm > 100){
                enviarAlertaInfo("Ritmo cardíaco elevado (Taquicardia): " + Math.round(bpm) + " lpm", "INFO_BPM");
                ultimaAlertaBpm = tiempoActual;
            } else if (bpm < 60 && bpm > 30) { // > 30 descarta lecturas falsas al retirar la pulsera
                enviarAlertaInfo("Ritmo cardíaco bajo (Bradycardia): " + Math.round(bpm) + " lpm", "INFO_BPM");
                ultimaAlertaBpm = tiempoActual;
            }
        }

        // 2. Saturación de oxígeno (rango normal: > 95%)
        if(tiempoActual - ultimaAlertaOxi > COOLDOWN_VITALES) {
            if(oxi <= 94 && oxi > 50){ // > 50 descarta falsos positivos
                String gravedad = (oxi <= 90) ? "moderada/grave" : "leve";
                enviarAlertaInfo("Hipoxemia " + gravedad + ". Oxigeno al " + Math.round(oxi) + "%", "INFO_OXI");
                ultimaAlertaOxi = tiempoActual;
            }
        }
    }

    /**
     * Publica un aviso informativo en EmergenciasPendientes, o lo encola en
     * SQLite si no hay conexión a internet.
     */
    private void enviarAlertaInfo(String mensaje, String tipo) {
        if (hayConexionInternet()) {
            HashMap<String, Object> info = new HashMap<>();
            info.put("mensaje", mensaje);
            info.put("tipo", tipo); // distingue entre aviso informativo y emergencia
            info.put("timestamp", ServerValue.TIMESTAMP);

            mDatabase.child("Usuarios").child(uidAdulto).child("EmergenciasPendientes")
                    .push().setValue(info);
        } else {
            AlertasOfflineDBHelper.getInstance(this)
                    .insertarAlerta(tipo, mensaje, System.currentTimeMillis());
        }
    }

    /** Indica si el dispositivo tiene actualmente una red con acceso a internet. */
    private boolean hayConexionInternet() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            android.net.Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork != null) {
                android.net.NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
                return capabilities != null && capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
            }
        }
        return false;
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