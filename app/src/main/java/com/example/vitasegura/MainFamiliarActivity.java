package com.example.vitasegura;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainFamiliarActivity extends AppCompatActivity {
    private LinearLayout btnMonitoreo, btnMedicamentos, btnUbicacion, btn_notificaciones, btn_usuarios,
    btn_configuracion;
    private ImageView btn_agregar_abuelo, btn_cerrar_sesion, ivIconoVinculo;
    private TextView tvNombre, tvTituloConexion, tvSubtituloConexion;
    private View viewIndicadorLed;
    private CardView cardEstadoConexion;
    private String uidCuidador;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_familiar);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        uidCuidador = mAuth.getCurrentUser().getUid();

        tvNombre = findViewById(R.id.tv_bienvenida_cuidador);

        mDatabase.child("Usuarios").child(uidCuidador).child("nombre").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().getValue() != null) {
                String nombreCompleto = task.getResult().getValue(String.class);

                // Lógica para obtener el primer nombre
                String primerNombre = "";
                if (nombreCompleto != null && !nombreCompleto.isEmpty()) {
                    String[] partes = nombreCompleto.split(" ");
                    primerNombre = partes[0]; // Tomamos la primera posición
                }

                tvNombre.setText("Bienvenido,\n" + primerNombre);
            }
        });



        // 2. IMPORTANTE: Vincular el ID después de setContentView
        btnMonitoreo = findViewById(R.id.btn_monitoreo);
        btn_agregar_abuelo = findViewById(R.id.btn_agregar_abuelo);
        btnMedicamentos = findViewById(R.id.btn_medicamentos);
        btnUbicacion = findViewById(R.id.btn_ubicacion);
        btn_notificaciones = findViewById(R.id.btn_notificaciones);
        btn_usuarios = findViewById(R.id.btn_usuarios);
        btn_configuracion = findViewById(R.id.btn_configuracion);
        btn_cerrar_sesion = findViewById(R.id.btn_cerrar_sesion);


        // 3. Ahora sí, configurar el listener
        if (btnMonitoreo != null) { // Buena práctica para evitar cierres
            btnMonitoreo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainFamiliarActivity.this, InformacionAbueloActivity.class);
                    startActivity(intent);
                }
            });
        }

        btn_agregar_abuelo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainFamiliarActivity.this, AgregarAdultoMayor.class);
                startActivity(intent);
            }
        });

       btn_cerrar_sesion.setOnClickListener(view -> {
           FirebaseAuth.getInstance().signOut(); // Borra el token del celular
           Intent intent = new Intent(this, LoginActivity.class);
           // Borramos el historial de pantallas para que no pueda volver atrás
           intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
           startActivity(intent);
           finish();
       });

        btnMedicamentos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainFamiliarActivity.this, MedicamentosCuidadorActivity.class);
                startActivity(intent);
            }
        });

        btnUbicacion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainFamiliarActivity.this, UbicacionAdultoActivity.class);
                startActivity(intent);
            }
        });

        btn_notificaciones.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainFamiliarActivity.this, NotificacionesActivity.class);
                startActivity(intent);
            }
        });

        btn_usuarios.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainFamiliarActivity.this, UsuariosActivity.class);
                startActivity(intent);
            }
        });

        btn_configuracion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainFamiliarActivity.this, ConfiguracionActivity.class);
                startActivity(intent);
            }
        });


        //Tarjeta de Vinculacion
        cardEstadoConexion = findViewById(R.id.card_estado_conexion);
        tvTituloConexion = findViewById(R.id.tv_titulo_conexion);
        tvSubtituloConexion = findViewById(R.id.tv_subtitulo_conexion);
        ivIconoVinculo = findViewById(R.id.iv_icono_vinculo);
        viewIndicadorLed = findViewById(R.id.view_indicador_led);

        //Verificar si hay vinculo
        mDatabase.child("Vinculos").child(uidCuidador).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot){
                if(snapshot.exists()){
                    // Si hay vínculo, obtenemos el ID del abuelo y actualizamos
                    String idAbuelo = snapshot.child("id_adulto_vinculado").getValue(String.class);
                    actualizarInterfazVinculo(idAbuelo);
                    cardEstadoConexion.setVisibility(View.VISIBLE);
                    btn_agregar_abuelo.setVisibility(View.GONE);
                } else{
                    // Si no hay vínculo, mostramos el botón de agregar
                    btn_agregar_abuelo.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void actualizarInterfazVinculo(String uidAbuelo) {
        mDatabase.child("Usuarios").child(uidAbuelo).child("nombre").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String nombreAbuelo = task.getResult().getValue(String.class);

                tvTituloConexion.setText("Cuidando a: " + nombreAbuelo);
                tvSubtituloConexion.setText("Conexión activa");

                // Cambiamos colores a "Activo"
                ivIconoVinculo.setImageTintList(ColorStateList.valueOf(Color.parseColor("#80C7DE")));
                viewIndicadorLed.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            }
        });
    }
}