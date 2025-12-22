package com.example.vitasegura;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class NotificacionesActivity extends AppCompatActivity {

    private RecyclerView rvEmergencias, rvInformacion;
    private NotificacionAdapter adapterEmergencias, adapterInformacion;
    private TextView tvLabelEmergencia, tvLabelInfo;
    private List<Notificacion> listaOriginal;
    private EditText etBuscar;
    private ImageView ivClearSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notificaciones);

        // Vincular vistas
        etBuscar = findViewById(R.id.et_buscar_notificacion);
        ivClearSearch = findViewById(R.id.iv_clear_search_notis);
        tvLabelEmergencia = findViewById(R.id.tv_label_emergencia);
        tvLabelInfo = findViewById(R.id.tv_label_info);

        // Configurar RecyclerView Emergencias
        rvEmergencias = findViewById(R.id.rv_emergencias);
        rvEmergencias.setLayoutManager(new LinearLayoutManager(this));

        // Configurar RecyclerView Información
        rvInformacion = findViewById(R.id.rv_informacion);
        rvInformacion.setLayoutManager(new LinearLayoutManager(this));

        cargarDatosPrueba();

        // 1. Filtrar por texto mientras escribes
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String texto = s.toString();

                // Lógica de visibilidad de la cruz
                if (texto.isEmpty()) {
                    ivClearSearch.setVisibility(View.GONE);
                } else {
                    ivClearSearch.setVisibility(View.VISIBLE);
                }

                aplicarFiltros(texto, null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        ivClearSearch.setOnClickListener(v -> {
            etBuscar.setText(""); // Esto disparará automáticamente aplicarFiltros("")
            ivClearSearch.setVisibility(View.GONE);
        });

        // 2. Filtrar por fecha al tocar el calendario
        findViewById(R.id.iv_calendario_filtro).setOnClickListener(v -> mostrarDatePicker());

        // Botón atrás
        findViewById(R.id.iv_back_notis).setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void cargarDatosPrueba() {
        listaOriginal = new ArrayList<>();
        listaOriginal.add(new Notificacion("[Nombre] ha activado el botón de emergencia", "18:02 P.M.", "20/12/2025", true));
        listaOriginal.add(new Notificacion("La pulsera ha detectado una caída", "18:00 P.M.", "20/12/2025", true));
        listaOriginal.add(new Notificacion("Se ha notificado la toma de medicamento", "09:30 A.M.", "20/12/2025", false));
        listaOriginal.add(new Notificacion("La batería de la pulsera es baja", "11:33 A.M.", "19/12/2025", false));

        // Inicializamos los adaptadores con las listas filtradas por tipo
        adapterEmergencias = new NotificacionAdapter(obtenerPorTipo(listaOriginal, true));
        adapterInformacion = new NotificacionAdapter(obtenerPorTipo(listaOriginal, false));

        rvEmergencias.setAdapter(adapterEmergencias);
        rvInformacion.setAdapter(adapterInformacion);
    }

    // Método auxiliar para separar la lista por tipo
    private List<Notificacion> obtenerPorTipo(List<Notificacion> lista, boolean buscarEmergencia) {
        List<Notificacion> resultado = new ArrayList<>();
        for (Notificacion n : lista) {
            if (n.isEsEmergencia() == buscarEmergencia) {
                resultado.add(n);
            }
        }
        return resultado;
    }

    private void mostrarDatePicker() {
        final Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String fecha = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, (month + 1), year);
            aplicarFiltros(null, fecha);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * Aplica filtros de texto y fecha simultáneamente
     */
    private void aplicarFiltros(String texto, String fecha) {
        List<Notificacion> filtrada = new ArrayList<>();
        String busqueda = (texto != null) ? texto.toLowerCase() : etBuscar.getText().toString().toLowerCase();

        for (Notificacion n : listaOriginal) {
            boolean coincideTexto = n.getMensaje().toLowerCase().contains(busqueda);
            boolean coincideFecha = (fecha == null) || n.getFecha().equals(fecha);

            if (coincideTexto && coincideFecha) {
                filtrada.add(n);
            }
        }

        // Dividir la lista filtrada en los dos adaptadores
        List<Notificacion> emergencias = obtenerPorTipo(filtrada, true);
        List<Notificacion> informacion = obtenerPorTipo(filtrada, false);

        adapterEmergencias.actualizarLista(emergencias);
        adapterInformacion.actualizarLista(informacion);

        // Mostrar u ocultar etiquetas según si hay resultados
        tvLabelEmergencia.setVisibility(emergencias.isEmpty() ? View.GONE : View.VISIBLE);
        tvLabelInfo.setVisibility(informacion.isEmpty() ? View.GONE : View.VISIBLE);
    }
}