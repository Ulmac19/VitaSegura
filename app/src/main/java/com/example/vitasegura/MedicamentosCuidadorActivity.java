package com.example.vitasegura;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MedicamentosCuidadorActivity extends AppCompatActivity {

    private RecyclerView rv;
    private MedicamentoCuidadorAdapter adapter;
    private Button btn_agregar_recordatorio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicamentos_cuidador);

        // Vincular vistas
        btn_agregar_recordatorio = findViewById(R.id.btn_agregar_recordatorio);
        rv = findViewById(R.id.rv_medicamentos_cuidador);

        // Configurar el RecyclerView
        rv.setLayoutManager(new LinearLayoutManager(this));

        // DATOS DE PRUEBA
        List<Medicamento> listaPrueba = new ArrayList<>();
        listaPrueba.add(new Medicamento("Metformina", "Cada 2 días", "20:00", "1 tableta", "Notas aquí..."));
        listaPrueba.add(new Medicamento("Insulina", "Diario", "08:00", "10 unidades", "En ayunas"));

        // Inicializar adaptador
        adapter = new MedicamentoCuidadorAdapter(listaPrueba, new MedicamentoCuidadorAdapter.OnMedicamentoClickListener() {
            @Override
            public void onEditarClick(Medicamento m) {
                Intent intent = new Intent(MedicamentosCuidadorActivity.this, FormularioMedicamentoActivity.class);
                intent.putExtra("nombre", m.getNombre());
                intent.putExtra("frecuencia", m.getFrecuencia());
                intent.putExtra("hora", m.getHora());
                intent.putExtra("dosis", m.getDosis());
                intent.putExtra("notas", m.getNotas());
                startActivity(intent);
            }

            @Override
            public void onEliminarClick(Medicamento m) {
                // Ahora llamamos al método que está afuera
                mostrarPopUpEliminar(m);
            }
        });

        rv.setAdapter(adapter);

        // Botón agregar
        btn_agregar_recordatorio.setOnClickListener(v -> {
            startActivity(new Intent(MedicamentosCuidadorActivity.this, FormularioMedicamentoActivity.class));
        });

        // Botón atrás
        findViewById(R.id.iv_back_meds).setOnClickListener(v -> finish());
    }

    // EL MÉTODO DEBE IR AQUÍ, FUERA DEL ONCREATE
    private void mostrarPopUpEliminar(Medicamento m) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MedicamentosCuidadorActivity.this);

        // Inflar diseño personalizado
        View view = getLayoutInflater().inflate(R.layout.dialog_confirmar_eliminar, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();

        // Fondo transparente para los bordes redondeados
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Button btnAceptar = view.findViewById(R.id.btn_aceptar_eliminar);
        btnAceptar.setOnClickListener(v -> {
            // Acción de eliminar (Lógica de Firebase después)
            Toast.makeText(MedicamentosCuidadorActivity.this, "Eliminando: " + m.getNombre(), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }
}