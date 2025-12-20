package com.example.vitasegura;

import android.os.Bundle;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class UbicacionAdultoActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ubicacion_adulto);

        RecyclerView rvHistorial = findViewById(R.id.rv_historial_ubicacion);
        rvHistorial.setLayoutManager(new LinearLayoutManager(this));

        List<Ubicacion> listaPrueba = new ArrayList<>();
        listaPrueba.add(new Ubicacion("Av. Chapultepec #45, Guadalajara", "Hoy - 12:00 PM"));
        listaPrueba.add(new Ubicacion("Centro Comercial Andares", "Ayer - 05:30 PM"));
        listaPrueba.add(new Ubicacion("Parque Metropolitano", "18 Dic - 10:15 AM"));

        HistorialUbicacionAdapter adapter = new HistorialUbicacionAdapter(listaPrueba);
        rvHistorial.setAdapter(adapter);

        // Inicializar el fragmento del mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        findViewById(R.id.iv_back_ubicacion).setOnClickListener(v -> finish());

        // Aquí después inicializarás el RecyclerView del historial

        // Asegúrate de que este ID (R.id.main) coincida con el android:id del layout raíz en tu XML
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Marcador de prueba (esto se actualizará con Firebase después)
        // Ejemplo: Guadalajara, México
        LatLng ubicacionAdulto = new LatLng(20.659698, -103.349609);

        // Añadir marcador y mover la cámara
        mMap.addMarker(new MarkerOptions()
                .position(ubicacionAdulto)
                .title("Ubicación del Adulto Mayor"));

        // Zoom nivel 15 (calle)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacionAdulto, 15f));
    }
}