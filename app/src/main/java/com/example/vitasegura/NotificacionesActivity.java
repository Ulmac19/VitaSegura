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

/**
 * Pantalla de historial de notificaciones del cuidador.
 *
 * Carga el historial desde SQLite y lo separa en dos listas: emergencias e
 * información. Permite filtrar en vivo por texto y por fecha (selector de
 * calendario), y purga los registros con más de 30 días al abrirse.
 */
public class NotificacionesActivity extends AppCompatActivity {

    private RecyclerView rvEmergencias, rvInformacion;
    private NotificacionAdapter adapterEmergencias, adapterInformacion;
    private TextView tvLabelEmergencia, tvLabelInfo;
    private List<Notificacion> listaOriginal;
    private EditText etBuscar;
    private ImageView ivClearSearch;

    // Filtro por fecha y acceso a la base de datos local
    private NotificacionesDBHelper dbHelper;
    private String fechaSeleccionada = null; // fecha elegida en el calendario

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

        // Inicializa la BD y purga las notificaciones de más de 30 días
        dbHelper = NotificacionesDBHelper.getInstance(this);
        dbHelper.limpiarHistorialAntiguo();

        cargarDatosDesdeDB();

        // Filtra el historial por texto a medida que se escribe
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String texto = s.toString();

                if (texto.isEmpty()) {
                    ivClearSearch.setVisibility(View.GONE);
                } else {
                    ivClearSearch.setVisibility(View.VISIBLE);
                }

                aplicarFiltros(texto, fechaSeleccionada);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        ivClearSearch.setOnClickListener(v -> {
            etBuscar.setText(""); // el TextWatcher reaplicará los filtros
            fechaSeleccionada = null; // también limpia el filtro de fecha
            ivClearSearch.setVisibility(View.GONE);
            aplicarFiltros("", null);
        });

        // Filtra por fecha mediante el selector de calendario
        findViewById(R.id.iv_calendario_filtro).setOnClickListener(v -> mostrarDatePicker());

        // Botón atrás
        findViewById(R.id.iv_back_notis).setOnClickListener(v -> finish());


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /** Carga el historial desde SQLite, lo separa por tipo y prepara los adapters. */
    private void cargarDatosDesdeDB() {
        listaOriginal = new ArrayList<>();

        List<Notificacion> listaEmergencias = dbHelper.obtenerNotificaciones(true);
        List<Notificacion> listaInfo = dbHelper.obtenerNotificaciones(false);

        // Lista maestra con ambas, para que el buscador filtre sobre el historial
        listaOriginal.addAll(listaEmergencias);
        listaOriginal.addAll(listaInfo);

        adapterEmergencias = new NotificacionAdapter(listaEmergencias);
        adapterInformacion = new NotificacionAdapter(listaInfo);

        rvEmergencias.setAdapter(adapterEmergencias);
        rvInformacion.setAdapter(adapterInformacion);

        // Oculta las etiquetas de sección vacías
        tvLabelEmergencia.setVisibility(listaEmergencias.isEmpty() ? View.GONE : View.VISIBLE);
        tvLabelInfo.setVisibility(listaInfo.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /** Filtra una lista para quedarse solo con emergencias o solo con avisos informativos. */
    private List<Notificacion> obtenerPorTipo(List<Notificacion> lista, boolean buscarEmergencia) {
        List<Notificacion> resultado = new ArrayList<>();
        for (Notificacion n : lista) {
            if (n.isEsEmergencia() == buscarEmergencia) {
                resultado.add(n);
            }
        }
        return resultado;
    }

    /** Abre el calendario y aplica el filtro por la fecha seleccionada. */
    private void mostrarDatePicker() {
        final Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            fechaSeleccionada = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, (month + 1), year);
            aplicarFiltros(etBuscar.getText().toString(), fechaSeleccionada);

            // Muestra la cruz para poder limpiar el filtro de fecha
            ivClearSearch.setVisibility(View.VISIBLE);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    /** Aplica los filtros de texto y fecha y refresca ambas listas de notificaciones. */
    private void aplicarFiltros(String texto, String fecha) {
        List<Notificacion> filtrada = new ArrayList<>();
        String busqueda = (texto != null) ? texto.toLowerCase() : "";

        for (Notificacion n : listaOriginal) {
            boolean coincideTexto = n.getMensaje().toLowerCase().contains(busqueda) ||
                    n.getFecha().contains(busqueda);

            boolean coincideFecha = (fecha == null) || n.getFecha().equals(fecha);

            if (coincideTexto && coincideFecha) {
                filtrada.add(n);
            }
        }

        List<Notificacion> emergencias = obtenerPorTipo(filtrada, true);
        List<Notificacion> informacion = obtenerPorTipo(filtrada, false);

        adapterEmergencias.actualizarLista(emergencias);
        adapterInformacion.actualizarLista(informacion);

        tvLabelEmergencia.setVisibility(emergencias.isEmpty() ? View.GONE : View.VISIBLE);
        tvLabelInfo.setVisibility(informacion.isEmpty() ? View.GONE : View.VISIBLE);
    }


}