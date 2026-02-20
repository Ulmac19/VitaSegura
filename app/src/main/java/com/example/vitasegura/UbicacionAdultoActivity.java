package com.example.vitasegura;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UbicacionAdultoActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DatabaseReference mDatabase;
    private String uidCuidador;
    private Marker marcadorFamiliar;
    private TextView tvTitulo;

    // Componentes del Historial
    private RecyclerView rvHistorial;
    private HistorialUbicacionAdapter adapter;
    private List<Ubicacion> listaUbicaciones = new ArrayList<>();
    private UbicacionDBHelper dbHelper;

    // Temporizador
    private Handler handlerUbicacion = new Handler(Looper.getMainLooper());
    private Runnable runnableUbicacion;
    private int intervaloMilisegundos = 5 * 60 * 1000;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ubicacion_adulto);

        tvTitulo = findViewById(R.id.tv_titulo_ubicacion);
        findViewById(R.id.iv_back_ubicacion).setOnClickListener(v -> finish());
        rvHistorial = findViewById(R.id.rv_historial_ubicacion);

        dbHelper = new UbicacionDBHelper(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uidCuidador = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // Configurar RecyclerView
        rvHistorial.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistorialUbicacionAdapter(listaUbicaciones, ubicacion -> {
            // Animación al hacer clic en el historial
            if (mMap != null) {
                LatLng pos = new LatLng(ubicacion.getLatitud(), ubicacion.getLongitud());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f));
            }
        });

        rvHistorial.setAdapter(adapter);
        cargarHistorialDesdeSQLite();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        obtenerIdYArrancarRastreo();
    }

    private void obtenerIdYArrancarRastreo() {
        SharedPreferences prefs = getSharedPreferences("VitaConfig", MODE_PRIVATE);
        intervaloMilisegundos = prefs.getInt("frecuencia_ubicacion", 5) * 60 * 1000;

        mDatabase.child("Vinculos").child(uidCuidador).child("id_adulto_vinculado")
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        String uidAbuelo = task.getResult().getValue(String.class);

                        mDatabase.child("Usuarios").child(uidAbuelo).child("nombre").get().addOnCompleteListener(t -> {
                            if (t.isSuccessful() && t.getResult().exists() && tvTitulo != null) {
                                String nombreCompleto = t.getResult().getValue(String.class);
                                String primerNombre = "";

                                //Obtener solo el primer nombre
                                if (nombreCompleto != null && !nombreCompleto.isEmpty()) {
                                    String[] partes = nombreCompleto.split(" ");
                                    primerNombre = partes[0];
                                }

                                // Ponemos solo el primer nombre en el título
                                tvTitulo.setText("Ubicación de\n" + primerNombre);
                            }
                        });

                        iniciarRastreoPeriodico(uidAbuelo);
                    }
                });
    }

    private void iniciarRastreoPeriodico(String uidAbuelo) {
        runnableUbicacion = new Runnable() {
            @Override
            public void run() {
                mDatabase.child("Usuarios").child(uidAbuelo).child("Ubicacion").get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult().exists()) {
                                Double lat = task.getResult().child("latitud").getValue(Double.class);
                                Double lon = task.getResult().child("longitud").getValue(Double.class);

                                if (lat != null && lon != null) {
                                    actualizarMapa(lat, lon);
                                    guardarEnHistorial(lat, lon);
                                }
                            }
                        });
                handlerUbicacion.postDelayed(this, intervaloMilisegundos);
            }
        };
        handlerUbicacion.post(runnableUbicacion);
    }

    private void actualizarMapa(double lat, double lon) {
        LatLng posicionActual = new LatLng(lat, lon);

        if (marcadorFamiliar == null) {
            MarkerOptions opciones = new MarkerOptions()
                    .position(posicionActual)
                    .title("Ubicación Actual")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            marcadorFamiliar = mMap.addMarker(opciones);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(posicionActual, 16f));
        } else {
            marcadorFamiliar.setPosition(posicionActual);
            mMap.animateCamera(CameraUpdateFactory.newLatLng(posicionActual));
        }
    }

    //Convierte coordenadas a una dirección en texto
    private String obtenerDireccion(double lat, double lon) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> direcciones = geocoder.getFromLocation(lat, lon, 1);
            if (direcciones != null && !direcciones.isEmpty()) {
                return direcciones.get(0).getAddressLine(0); // Ej: Av. Vallarta #123
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Ubicación desconocida (Lat: " + String.format(Locale.US, "%.3f", lat) + ")";
    }

    private void guardarEnHistorial(double lat, double lon) {
        if (!listaUbicaciones.isEmpty()) {
            Ubicacion ultima = listaUbicaciones.get(0);
            if (ultima.getLatitud() == lat && ultima.getLongitud() == lon) return; // Evitar repetidos
        }

        String direccionReal = obtenerDireccion(lat, lon);

        // Formato para mostrar "Hoy - 14:30 PM"
        String fechaHora = new SimpleDateFormat("dd MMM - hh:mm a", Locale.getDefault()).format(new Date());

        dbHelper.insertarUbicacion(direccionReal, fechaHora, lat, lon);
        cargarHistorialDesdeSQLite();
    }

    private void cargarHistorialDesdeSQLite() {
        listaUbicaciones.clear();
        listaUbicaciones.addAll(dbHelper.obtenerHistorial());
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handlerUbicacion != null && runnableUbicacion != null) {
            handlerUbicacion.removeCallbacks(runnableUbicacion);
        }
    }

    //--- BASE DE DATOS LOCAL PARA UBICACIONES --
    private static class UbicacionDBHelper extends SQLiteOpenHelper {
        private static final String DB_NAME = "VitaUbicaciones.db";
        // Subimos a versión 2 para actualizar la tabla e incluir 'direccion'
        private static final int DB_VERSION = 2;

        public UbicacionDBHelper(Context context) { super(context, DB_NAME, null, DB_VERSION); }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE historial (id INTEGER PRIMARY KEY AUTOINCREMENT, direccion TEXT, fecha TEXT, lat REAL, lon REAL)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS historial");
            onCreate(db);
        }

        public void insertarUbicacion(String direccion, String fecha, double lat, double lon) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues v = new ContentValues();
            v.put("direccion", direccion);
            v.put("fecha", fecha);
            v.put("lat", lat);
            v.put("lon", lon);
            db.insert("historial", null, v);

            // Mantener solo los últimos 20 registros
            db.execSQL("DELETE FROM historial WHERE id NOT IN (SELECT id FROM historial ORDER BY id DESC LIMIT 20)");
        }

        public List<Ubicacion> obtenerHistorial() {
            List<Ubicacion> lista = new ArrayList<>();
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery("SELECT direccion, fecha, lat, lon FROM historial ORDER BY id DESC", null);
            while (c.moveToNext()) {
                lista.add(new Ubicacion(c.getString(0), c.getString(1), c.getDouble(2), c.getDouble(3)));
            }
            c.close();
            return lista;
        }
    }
}