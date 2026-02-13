package com.example.vitasegura;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InformacionAbueloActivity extends AppCompatActivity {

    private TextView tvBpm, tvOxi;
    private DatabaseReference mDatabase;
    private String uidCuidador;

    private HealthDBHelper dbHelper;
    private final Handler hideHandler = new Handler(Looper.getMainLooper());
    private LineChart chartPulso, chartOxi;
    private List<String> fechasEjeX = new ArrayList<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_informacion_abuelo);

        tvBpm = findViewById(R.id.tv_bpm_actual);
        tvOxi = findViewById(R.id.tv_oxigenacion_actual);
        chartPulso = findViewById(R.id.chart_pulso);
        chartOxi = findViewById(R.id.chart_oxigeno);
        findViewById(R.id.iv_back_salud).setOnClickListener(v -> finish());

        mDatabase = FirebaseDatabase.getInstance().getReference();
        uidCuidador = FirebaseAuth.getInstance().getCurrentUser().getUid();

        dbHelper = new HealthDBHelper(this);
        dbHelper.limpiarDatosAntiguos();

        //Configuracion de Graficas
        if(chartPulso != null){
            setupGrafica(chartPulso, "#23608C", "BPM", 10f,
                    150f, "Historial de pulso cardiaco del familiar");
        }
        if (chartOxi != null) {
            setupGrafica(chartOxi, "#7DB9A9", "%", 5f,
                    100f, "Historial de oxigenación del familiar");
        }

        //Cargar los datos previos existentes en el dispositivo
        cargarHistorialGraficas();

        //Iniciar el proceso de busqueda del abuelo y escuchar Firebase
        obtenerIdAbueloYMonitorear();


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void obtenerIdAbueloYMonitorear(){
        //Buscar al abuelo vinculado
        mDatabase.child("Vinculos").child(uidCuidador).child("id_adulto_vinculado")
                .get().addOnCompleteListener(task ->{
                    if(task.isSuccessful() && task.getResult().exists()){
                        String uidABuelo = task.getResult().getValue(String.class);
                        iniciarEscuchaRealTime(uidABuelo);
                    }
                });
    }

    private void iniciarEscuchaRealTime(String uidAbuelo) {
        mDatabase.child("Usuarios").child(uidAbuelo).child("MonitoreoActual")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()) {
                            //Extraer BPM y Oxi
                            Float bpm = snapshot.child("bpm").getValue(Float.class);
                            Float oxi = snapshot.child("oxi").getValue(Float.class);

                            if (bpm != null && oxi != null) {
                                // GUARDAR EN EL SQLITE LOCAL DEL CUIDADOR
                                // Esto guarda el dato que acaba de llegar de Firebase
                                dbHelper.insertarLecturaTemporal(bpm, oxi);
                                dbHelper.consolidarDiasAnteriores();

                                // ACTUALIZAR LA INTERFAZ
                                tvBpm.setText("Actual: " + bpm.intValue() + " BPM");
                                tvOxi.setText("Actual: " + oxi.intValue() + " %");

                                // REFRESCAR LAS GRÁFICAS CON LOS DATOS DEL SQLITE
                                cargarHistorialGraficas();
                            }
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void cargarHistorialGraficas() {
        fechasEjeX.clear();
        List<Entry> entriesBpm = dbHelper.getPromediosGrafica("bpm", fechasEjeX);
        List<Entry> entriesOxi = dbHelper.getPromediosGrafica("oxi", null);

        actualizarGraficaDePromedios(chartPulso, entriesBpm, "#23608C");
        actualizarGraficaDePromedios(chartOxi, entriesOxi, "#7DB9A9");

        if (chartPulso != null) chartPulso.setVisibleXRangeMaximum(5);
        if (chartOxi != null) chartOxi.setVisibleXRangeMaximum(5);

        if (chartPulso != null) chartPulso.moveViewToX(fechasEjeX.size());
        if (chartOxi != null) chartOxi.moveViewToX(fechasEjeX.size());
    }

    private void actualizarGraficaDePromedios(LineChart chart, List<Entry> entries, String colorHex) {
        if (chart == null || entries.isEmpty()) return;
        LineDataSet dataSet = new LineDataSet(entries, "Historial");
        dataSet.setHighlightEnabled(true);
        dataSet.setDrawHighlightIndicators(false);
        dataSet.setColor(Color.parseColor(colorHex));
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(Color.parseColor(colorHex));
        dataSet.setCircleRadius(8f);
        dataSet.setCircleHoleRadius(4f);
        dataSet.setLineWidth(3f);
        dataSet.setFillColor(Color.parseColor(colorHex));
        dataSet.setFillAlpha(50);
        dataSet.setDrawValues(false);

        chart.setData(new LineData(dataSet));
        chart.setMaxHighlightDistance(35f);
        chart.invalidate();
    }

    private void setupGrafica(LineChart chart, String colorHex, String unidadY, float granularidadY, float maxValY, String titulo) {
        if (chart == null) return;

        chart.setExtraOffsets(5, 45, 12, 35);

        Description desc = new Description();
        desc.setText(titulo);
        desc.setTextSize(13f);
        desc.setTextColor(Color.parseColor("#23608C"));
        desc.setTypeface(Typeface.DEFAULT_BOLD);
        desc.setTextAlign(Paint.Align.CENTER);

        chart.post(() -> {
            desc.setPosition(chart.getWidth() / 2f, 40f);
            chart.setDescription(desc);
            chart.invalidate();
        });

        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setDoubleTapToZoomEnabled(false);

        final SaludMarkerView mv = new SaludMarkerView(this, R.layout.marker_view, unidadY);
        mv.setChartView(chart);
        chart.setMarker(mv);

        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                hideHandler.removeCallbacksAndMessages(null);
                mv.setCustomAlpha(255);
                chart.invalidate();
                hideHandler.postDelayed(() -> {
                    ValueAnimator fadeAnim = ValueAnimator.ofInt(255, 0);
                    fadeAnim.setDuration(800);
                    fadeAnim.addUpdateListener(animation -> {
                        int alphaValue = (int) animation.getAnimatedValue();
                        mv.setCustomAlpha(alphaValue);
                        chart.invalidate();
                    });
                    fadeAnim.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            chart.highlightValue(null);
                            mv.setCustomAlpha(255);
                        }
                    });
                    fadeAnim.start();
                }, 3000);
            }
            @Override
            public void onNothingSelected() {
                hideHandler.removeCallbacksAndMessages(null);
            }
        });

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(11f);
        xAxis.setLabelRotationAngle(-45);
        xAxis.setTextColor(Color.GRAY);
        xAxis.setAvoidFirstLastClipping(true);

        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                return (index >= 0 && index < fechasEjeX.size()) ? fechasEjeX.get(index) : "";
            }
        });

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setTextColor(Color.parseColor(colorHex));
        leftAxis.setTextSize(12f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(maxValY);
        leftAxis.setGranularity(granularidadY);
        leftAxis.setGranularityEnabled(true);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + " " + unidadY;
            }
        });
    }

    private static class HealthDBHelper extends SQLiteOpenHelper {
        private static final String DB_NAME = "VitaSalud.db";
        public HealthDBHelper(Context context) { super(context, DB_NAME, null, 2); }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE temp_readings (id PRIMARY KEY, bpm REAL, oxi REAL, date TEXT)");
            db.execSQL("CREATE TABLE daily_averages (id PRIMARY KEY, bpm_avg REAL, oxi_avg REAL, date TEXT UNIQUE)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int old, int n) {
            db.execSQL("DROP TABLE IF EXISTS temp_readings");
            db.execSQL("DROP TABLE IF EXISTS daily_averages");
            onCreate(db);
        }

        public void insertarLecturaTemporal(float bpm, float oxi) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues v = new ContentValues();
            v.put("bpm", bpm);
            v.put("oxi", oxi);
            v.put("date", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
            db.insert("temp_readings", null, v);
        }

        public void consolidarDiasAnteriores() {
            SQLiteDatabase db = this.getWritableDatabase();
            String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            Cursor c = db.rawQuery("SELECT DISTINCT date FROM temp_readings WHERE date != ?", new String[]{hoy});
            if (c.moveToFirst()) {
                do {
                    String fechaPasada = c.getString(0);
                    Cursor avgC = db.rawQuery("SELECT AVG(bpm), AVG(oxi) FROM temp_readings WHERE date = ?", new String[]{fechaPasada});
                    if (avgC.moveToFirst()) {
                        ContentValues v = new ContentValues();
                        v.put("bpm_avg", avgC.getFloat(0)); v.put("oxi_avg", avgC.getFloat(1)); v.put("date", fechaPasada);
                        db.insertWithOnConflict("daily_averages", null, v, SQLiteDatabase.CONFLICT_IGNORE);
                        db.delete("temp_readings", "date = ?", new String[]{fechaPasada});
                    }
                    avgC.close();
                } while (c.moveToNext());
            }
            c.close();
        }

        public List<Entry> getPromediosGrafica(String tipo, List<String> outFechas) {
            List<Entry> entries = new ArrayList<>();
            SQLiteDatabase db = this.getReadableDatabase();
            String col = tipo.equals("bpm") ? "bpm_avg" : "oxi_avg";
            String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            Cursor c = db.rawQuery("SELECT " + col + ", date FROM daily_averages ORDER BY date ASC LIMIT 30", null);
            int idx = 0;
            while (c.moveToNext()) {
                entries.add(new Entry(idx++, c.getFloat(0)));
                if (outFechas != null) outFechas.add(formatearFechaEje(c.getString(1)));
            }
            c.close();

            String colTemp = tipo.equals("bpm") ? "AVG(bpm)" : "AVG(oxi)";
            Cursor cToday = db.rawQuery("SELECT " + colTemp + " FROM temp_readings WHERE date = ?", new String[]{hoy});
            if (cToday.moveToFirst() && !cToday.isNull(0)) {
                entries.add(new Entry(idx, cToday.getFloat(0)));
                if (outFechas != null) outFechas.add(formatearFechaEje(hoy));
            }
            cToday.close();
            return entries;
        }

        private String formatearFechaEje(String fechaSql) {
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(fechaSql);
                return new SimpleDateFormat("dd/MM", Locale.getDefault()).format(date);
            } catch (Exception e) { return fechaSql; }
        }

        public void limpiarDatosAntiguos() {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete("daily_averages", "date < date('now','-30 days')", null);
        }
    }
}