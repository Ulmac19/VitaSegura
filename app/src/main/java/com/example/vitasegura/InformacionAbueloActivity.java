package com.example.vitasegura;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InformacionAbueloActivity extends AppCompatActivity {

    private TextView tvBpm, tvOxi;
    private LineChart chartDiario;
    private BarChart chartSemanal; // Cambiado a BarChart

    private DatabaseReference mDatabase;
    private String uidCuidador;
    private HealthDBHelper dbHelper;

    private Handler handlerMonitoreo = new Handler(Looper.getMainLooper());
    private Runnable runnableMonitoreo;
    private int intervaloMilisegundos = 5 * 60 * 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_informacion_abuelo);

        tvBpm = findViewById(R.id.tv_actual_bpm);
        tvOxi = findViewById(R.id.tv_actual_oxigeno);
        chartDiario = findViewById(R.id.chart_diario);
        chartSemanal = findViewById(R.id.chart_semanal);
        findViewById(R.id.iv_back_salud).setOnClickListener(v -> finish());

        mDatabase = FirebaseDatabase.getInstance().getReference();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uidCuidador = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        dbHelper = new HealthDBHelper(this);
        dbHelper.limpiarDatosAntiguos();

        // Configuración visual y controles de Zoom/Desplazamiento
        setupGrafica(chartDiario);
        setupGrafica(chartSemanal);

        cargarHistorialGraficas();
        obtenerIdAbueloYConfiguracion();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void obtenerIdAbueloYConfiguracion() {
        SharedPreferences prefs = getSharedPreferences("VitaConfig", MODE_PRIVATE);
        int minutos = prefs.getInt("frecuencia_salud", 5);
        intervaloMilisegundos = minutos * 60 * 1000;

        mDatabase.child("Vinculos").child(uidCuidador).child("id_adulto_vinculado")
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        iniciarMonitoreoPeriodico(task.getResult().getValue(String.class));
                    }
                });
    }

    private void iniciarMonitoreoPeriodico(String uidAbuelo) {
        runnableMonitoreo = new Runnable() {
            @Override
            public void run() {
                mDatabase.child("Usuarios").child(uidAbuelo).child("MonitoreoActual").get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult().exists()) {
                                Float bpm = task.getResult().child("bpm").getValue(Float.class);
                                Float oxi = task.getResult().child("oxi").getValue(Float.class);

                                if (bpm != null && oxi != null) {
                                    dbHelper.insertarLecturaTemporal(bpm, oxi);
                                    dbHelper.consolidarDiasAnteriores();
                                    tvBpm.setText("Actual: " + bpm.intValue() + " BPM");
                                    tvOxi.setText("Actual: " + oxi.intValue() + " %");
                                    cargarHistorialGraficas();
                                }
                            }
                        });
                handlerMonitoreo.postDelayed(this, intervaloMilisegundos);
            }
        };
        handlerMonitoreo.post(runnableMonitoreo);
    }

    private void cargarHistorialGraficas() {
        List<String> fechasEjeX = new ArrayList<>();
        List<Entry> dailyBpm = dbHelper.getPromediosGrafica("bpm", fechasEjeX);
        List<Entry> dailyOxi = dbHelper.getPromediosGrafica("oxi", null);

        // 1. DIBUJAR GRÁFICA DIARIA (Líneas)
        actualizarGraficaDiaria(chartDiario, dailyBpm, dailyOxi);
        if (chartDiario != null && !fechasEjeX.isEmpty()) {
            chartDiario.getXAxis().setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    int index = (int) value;
                    return (index >= 0 && index < fechasEjeX.size()) ? fechasEjeX.get(index) : "";
                }
            });
            chartDiario.setVisibleXRangeMaximum(7);
            chartDiario.moveViewToX(fechasEjeX.size());
        }

        // 2. DIBUJAR GRÁFICA SEMANAL (Barras Agrupadas)
        calcularGraficaSemanal(dailyBpm, dailyOxi);
    }

    private void actualizarGraficaDiaria(LineChart chart, List<Entry> bpmEntries, List<Entry> oxiEntries) {
        if (chart == null || bpmEntries.isEmpty()) return;

        LineDataSet setBpm = new LineDataSet(bpmEntries, "Pulso (BPM)");
        setBpm.setColor(Color.parseColor("#23608C"));
        setBpm.setCircleColor(Color.parseColor("#23608C"));
        setBpm.setLineWidth(3f);
        setBpm.setCircleRadius(5f);
        setBpm.setDrawValues(false);

        LineDataSet setOxi = new LineDataSet(oxiEntries, "Oxigenación (%)");
        setOxi.setColor(Color.parseColor("#7DB9A9"));
        setOxi.setCircleColor(Color.parseColor("#7DB9A9"));
        setOxi.setLineWidth(3f);
        setOxi.setCircleRadius(5f);
        setOxi.setDrawValues(false);

        LineData data = new LineData(setBpm, setOxi);
        chart.setData(data);
        chart.invalidate();
    }

    private void calcularGraficaSemanal(List<Entry> dailyBpm, List<Entry> dailyOxi) {
        List<BarEntry> weeklyBpm = new ArrayList<>();
        List<BarEntry> weeklyOxi = new ArrayList<>();
        List<String> etiquetasSemanas = new ArrayList<>();

        int totalDias = dailyBpm.size();
        int startIndex = Math.max(0, totalDias - 28);

        int weekIdx = 0;
        float sumBpm = 0, sumOxi = 0;
        int count = 0;

        for (int i = startIndex; i < totalDias; i++) {
            sumBpm += dailyBpm.get(i).getY();
            sumOxi += dailyOxi.get(i).getY();
            count++;

            if (count == 7 || i == totalDias - 1) {
                weeklyBpm.add(new BarEntry(weekIdx, sumBpm / count));
                weeklyOxi.add(new BarEntry(weekIdx, sumOxi / count));
                etiquetasSemanas.add("Sem " + (weekIdx + 1));

                sumBpm = 0; sumOxi = 0; count = 0;
                weekIdx++;
            }
        }

        if (weeklyBpm.isEmpty()) return;

        BarDataSet setBpm = new BarDataSet(weeklyBpm, "Pulso (BPM)");
        setBpm.setColor(Color.parseColor("#23608C"));
        setBpm.setDrawValues(false);

        BarDataSet setOxi = new BarDataSet(weeklyOxi, "Oxigenación (%)");
        setOxi.setColor(Color.parseColor("#7DB9A9"));
        setOxi.setDrawValues(false);

        BarData data = new BarData(setBpm, setOxi);

        // Ajustes para agrupar las barras una junto a la otra
        float groupSpace = 0.2f;
        float barSpace = 0.05f;
        float barWidth = 0.35f;
        // (0.35 + 0.05) * 2 + 0.2 = 1.0 (Obligatorio para que cuadren las barras)

        data.setBarWidth(barWidth);
        chartSemanal.setData(data);
        chartSemanal.groupBars(0f, groupSpace, barSpace);

        XAxis xAxis = chartSemanal.getXAxis();
        xAxis.setCenterAxisLabels(true); // Centrar etiquetas debajo del grupo
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(etiquetasSemanas.size());
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                return (index >= 0 && index < etiquetasSemanas.size()) ? etiquetasSemanas.get(index) : "";
            }
        });

        chartSemanal.invalidate();
    }

    // Usamos BarLineChartBase para que aplique tanto a LineChart como a BarChart
    private void setupGrafica(BarLineChartBase<?> chart) {
        if (chart == null) return;

        chart.getDescription().setEnabled(false);

        // HABILITAR ZOOM Y DESPLAZAMIENTO
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);

        // HABILITAR MARCADOR AL TOCAR
        CustomMarkerView mv = new CustomMarkerView(this, R.layout.marker_view);
        mv.setChartView(chart);
        chart.setMarker(mv);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.GRAY);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(150f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);

        chart.getAxisRight().setEnabled(false);

        Legend legend = chart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setDrawInside(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handlerMonitoreo != null && runnableMonitoreo != null) {
            handlerMonitoreo.removeCallbacks(runnableMonitoreo);
        }
    }


    public static class CustomMarkerView extends MarkerView {
        private TextView tvContent;

        public CustomMarkerView(Context context, int layoutResource) {
            super(context, layoutResource);
            tvContent = findViewById(R.id.tvContent);
        }

        @Override
        public void refreshContent(Entry e, Highlight highlight) {
            int indiceDataset = highlight.getDataSetIndex();
            String unidad = (indiceDataset == 0) ? " BPM" : " %";

            tvContent.setText(String.format(Locale.getDefault(), "%d%s", (int) e.getY(), unidad));
            super.refreshContent(e, highlight);
        }

        @Override
        public MPPointF getOffset() {
            return new MPPointF(-(getWidth() / 2f), -getHeight() - 15f);
        }

        //Dibujar el fondo oscuro debajo del texto
        @Override
        public void draw(android.graphics.Canvas canvas, float posx, float posy) {
            MPPointF offset = getOffsetForDrawingAtPoint(posx, posy);
            int saveId = canvas.save();


            canvas.translate(posx + offset.x, posy + offset.y);

            android.graphics.Paint paint = new android.graphics.Paint();
            paint.setColor(Color.parseColor("#CC000000")); // Negro al 80% de opacidad
            paint.setStyle(android.graphics.Paint.Style.FILL);
            paint.setAntiAlias(true); // Bordes suaves

            //Rectángulo redondeado
            android.graphics.RectF rect = new android.graphics.RectF(0, 0, getWidth(), getHeight());
            canvas.drawRoundRect(rect, 15f, 15f, paint);

            // Dibujar el contenido
            draw(canvas);
            canvas.restoreToCount(saveId);
        }
    }

    // --- BASE DE DATOS LOCAL ---
    private static class HealthDBHelper extends SQLiteOpenHelper {
        private static final String DB_NAME = "VitaSaludFamiliar.db";
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