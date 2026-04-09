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
import java.util.Calendar;
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
    private java.util.HashMap<String, Long> historialAlertasMeds = new java.util.HashMap<>();

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
            String accion = intent.getAction();

            if("DATA_PULSERA_REAL".equals(accion)) {
                String rawData = intent.getStringExtra("valor_raw");
                if (rawData != null) procesarYEnviar(rawData);
            } else if ("PULSERA_DESCONECTADA".equals(accion)) {
                //Si se desconecta, lanzamos alerta
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

        // Registrar el receptor para captar datos Y desconexiones
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
                medHandler.postDelayed(this, 10000);
            }
        };
        medHandler.post(medRunnable);
    }

    private void revisarHoraMedicamentos() {
        Calendar ahora = Calendar.getInstance();
        int horaActual = ahora.get(Calendar.HOUR_OF_DAY); // Formato 0-23 horas
        int minutoActual = ahora.get(Calendar.MINUTE);
        long tiempoActualMs = System.currentTimeMillis();

        // Convertimos la hora actual a los minutos totales del día (Ej: 01:00 AM = 60 min)
        int minutosActualesTotales = (horaActual * 60) + minutoActual;

        for (Medicamento med : listaMeds) {
            String freq = med.getFrecuencia();

            // Ignorar los de dolor
            if (freq == null || freq.equals("Solo si hay dolor")) continue;

            try {
                int frecuenciaHoras = Integer.parseInt(freq.replaceAll("[^0-9]", ""));
                if (frecuenciaHoras <= 0) continue; // Protección contra divisiones entre cero
                int frecuenciaEnMinutos = frecuenciaHoras * 60;

                String[] partesHora = med.getHora().split(":");
                int horaInicio = Integer.parseInt(partesHora[0].trim());
                int minutoInicio = Integer.parseInt(partesHora[1].trim());

                int minutosInicioTotales = (horaInicio * 60) + minutoInicio;

                boolean tocaDosis = false;

                // Generamos todas las tomas exactas que le tocan en un ciclo de 24 horas
                int maxTomasAlDia = (24 * 60) / frecuenciaEnMinutos;

                for (int i = 0; i <= maxTomasAlDia; i++) {
                    // Calculamos a qué minuto del día cae esta dosis
                    int minutoDeTomaEsperada = (minutosInicioTotales + (i * frecuenciaEnMinutos)) % 1440; // 1440 min = 24h

                    // Comparamos el reloj de tu celular con la toma esperada
                    int diferencia = minutosActualesTotales - minutoDeTomaEsperada;

                    // Tolerancia estricta: si es el minuto exacto (0) o el sistema se retrasó un minuto (1)
                    // El -1439 cubre el caso extremo de que toque a las 23:59 y sean las 00:00
                    if (diferencia == 0 || diferencia == 1 || diferencia == -1439) {
                        tocaDosis = true;
                        break; // Ya encontramos que sí le toca la dosis, dejamos de buscar
                    }
                }

                // SI SÍ LE TOCA LA DOSIS, REVISAMOS LA MEMORIA ANTISPAM
                if (tocaDosis) {
                    // Buscamos si ya le avisamos de esta pastilla hace un momento
                    long ultimaVezQueSono = historialAlertasMeds.containsKey(med.getId()) ? historialAlertasMeds.get(med.getId()) : 0;

                    // Bloqueo de 2 minutos: Garantiza que suene exactamente UNA VEZ y no haga spam
                    if (tiempoActualMs - ultimaVezQueSono > 120000) {
                        dispararNotificacionMed(med);
                        // Guardamos en la memoria que ya sonó
                        historialAlertasMeds.put(med.getId(), tiempoActualMs);
                    }
                }

            } catch (Exception e) {
                // Ignorar si el texto está mal escrito y pasar al siguiente medicamento
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

    //Metodo de notificacion en caso de desconexion de la pulsera
    private void lanzarAlertaDesconexion(){
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String canalId = "canal_desconexion";

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(canalId, "Alertas de Pulsera", NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            if(manager != null) manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, canalId)
                .setSmallIcon(R.drawable.pulsera)
                .setContentTitle("!Pulsera desconectada!")
                .setContentText("Tu pulsera VitaSegura ha perdido conexión. Por favor, acércala a tu celular.")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setAutoCancel(true);

        if(manager != null){
            //ID fijo anti spam para que no se repita la notificacion, sino que se actualice
            manager.notify(999, builder.build());
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