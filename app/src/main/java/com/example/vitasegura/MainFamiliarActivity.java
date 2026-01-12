package com.example.vitasegura;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class MainFamiliarActivity extends AppCompatActivity {
    private LinearLayout btnMonitoreo, btnMedicamentos, btnUbicacion, btn_notificaciones, btn_usuarios,
    btn_configuracion;
    private ImageView btn_agregar_abuelo, btn_cerrar_sesion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_familiar);

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}