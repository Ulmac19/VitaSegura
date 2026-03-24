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

    //Variables para el filtro de fecha y Base de Datos
    private NotificacionesDBHelper dbHelper;
    private String fechaSeleccionada = null; // Guarda la fecha del calendario

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

        // Inicializar BD y limpiar datos de más de 30 días
        dbHelper = new NotificacionesDBHelper(this);
        dbHelper.limpiarHistorialAntiguo();

        // Cargar desde SQLite en lugar de datos de prueba
        cargarDatosDesdeDB();

        //Filtrar por texto mientras escribes
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
            etBuscar.setText(""); // Disparará aplicarFiltros automáticamente
            fechaSeleccionada = null; // Limpiamos también la fecha si tocan la cruz
            ivClearSearch.setVisibility(View.GONE);
            aplicarFiltros("", null);
        });

        //Filtrar por fecha al tocar el calendario
        findViewById(R.id.iv_calendario_filtro).setOnClickListener(v -> mostrarDatePicker());

        // Botón atrás
        findViewById(R.id.iv_back_notis).setOnClickListener(v -> finish());


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void cargarDatosDesdeDB() {
        listaOriginal = new ArrayList<>();

        // Obtenemos las listas directamente de SQLite
        List<Notificacion> listaEmergencias = dbHelper.obtenerNotificaciones(true);
        List<Notificacion> listaInfo = dbHelper.obtenerNotificaciones(false);

        // Juntamos ambas en la lista maestra para que el buscador funcione con todas
        listaOriginal.addAll(listaEmergencias);
        listaOriginal.addAll(listaInfo);

        // Inicializamos adaptadores
        adapterEmergencias = new NotificacionAdapter(listaEmergencias);
        adapterInformacion = new NotificacionAdapter(listaInfo);

        rvEmergencias.setAdapter(adapterEmergencias);
        rvInformacion.setAdapter(adapterInformacion);

        // Ocultar etiquetas si no hay datos
        tvLabelEmergencia.setVisibility(listaEmergencias.isEmpty() ? View.GONE : View.VISIBLE);
        tvLabelInfo.setVisibility(listaInfo.isEmpty() ? View.GONE : View.VISIBLE);
    }

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
            // Formatear la fecha como "dd/MM/yyyy"
            fechaSeleccionada = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, (month + 1), year);
            aplicarFiltros(etBuscar.getText().toString(), fechaSeleccionada);

            // Mostramos la cruz para que puedan quitar el filtro de fecha
            ivClearSearch.setVisibility(View.VISIBLE);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

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