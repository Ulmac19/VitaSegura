package com.example.vitasegura;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Pantalla principal del cuidador.
 *
 * Centraliza el acceso a las funciones de monitoreo (información del abuelo,
 * medicamentos, ubicación, notificaciones, usuarios y configuración) e inicia el
 * servicio de vigilancia de alertas. Refleja en tiempo real el estado del
 * vínculo con el adulto mayor y gestiona el banner de "sin conexión".
 */
public class MainFamiliarActivity extends AppCompatActivity {
    private LinearLayout btnMonitoreo, btnMedicamentos, btnUbicacion, btn_notificaciones, btn_usuarios,
    btn_configuracion;

    // Estado de conectividad
    private TextView tvSinConexion;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

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

                // Muestra solo el primer nombre en el saludo
                String primerNombre = "";
                if (nombreCompleto != null && !nombreCompleto.isEmpty()) {
                    String[] partes = nombreCompleto.split(" ");
                    primerNombre = partes[0];
                }

                tvNombre.setText("Bienvenido,\n" + primerNombre);
            }
        });


        // Conectividad: estado inicial y monitoreo en tiempo real
        tvSinConexion = findViewById(R.id.tv_sin_conexion);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        verificarConexionInicial();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(() -> tvSinConexion.setVisibility(View.GONE));
            }

            @Override
            public void onLost(@NonNull Network network) {
                // Solo muestra el banner si no queda otra red activa (evita falsos avisos al cambiar de red)
                Network redActiva = connectivityManager.getActiveNetwork();
                NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(redActiva);
                boolean aunConectado = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                if (!aunConectado) {
                    runOnUiThread(() -> tvSinConexion.setVisibility(View.VISIBLE));
                }
            }
        };

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(request, networkCallback);


        // Botones de navegación de la pantalla
        btnMonitoreo = findViewById(R.id.btn_monitoreo);
        btn_agregar_abuelo = findViewById(R.id.btn_agregar_abuelo);
        btnMedicamentos = findViewById(R.id.btn_medicamentos);
        btnUbicacion = findViewById(R.id.btn_ubicacion);
        btn_notificaciones = findViewById(R.id.btn_notificaciones);
        btn_usuarios = findViewById(R.id.btn_usuarios);
        btn_configuracion = findViewById(R.id.btn_configuracion);
        btn_cerrar_sesion = findViewById(R.id.btn_cerrar_sesion);


        if (btnMonitoreo != null) {
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
           new AlertDialog.Builder(this)
                   .setTitle("Cerrar Sesión")
                   .setMessage("¿Estás seguro de que deseas salir de tu cuenta?")
                   .setPositiveButton("Sí, salir", (dialog, which) -> {
                       FirebaseAuth.getInstance().signOut(); // cierra la sesión de Firebase
                       Intent intent = new Intent(this, LoginActivity.class);
                       // Limpia la pila de actividades para impedir volver atrás
                       intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                       startActivity(intent);
                       finish();
                   })
                   .setNegativeButton("Cancelar", null)
                   .show();
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


        // Tarjeta de estado del vínculo con el adulto mayor
        cardEstadoConexion = findViewById(R.id.card_estado_conexion);
        tvTituloConexion = findViewById(R.id.tv_titulo_conexion);
        tvSubtituloConexion = findViewById(R.id.tv_subtitulo_conexion);
        ivIconoVinculo = findViewById(R.id.iv_icono_vinculo);
        viewIndicadorLed = findViewById(R.id.view_indicador_led);

        // Observa el vínculo en tiempo real para alternar entre la tarjeta y el botón de agregar
        mDatabase.child("Vinculos").child(uidCuidador).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot){
                if(snapshot.exists()){
                    // Hay vínculo: muestra la tarjeta con los datos del abuelo
                    String idAbuelo = snapshot.child("id_adulto_vinculado").getValue(String.class);
                    actualizarInterfazVinculo(idAbuelo);
                    cardEstadoConexion.setVisibility(View.VISIBLE);
                    btn_agregar_abuelo.setVisibility(View.GONE);
                } else{
                    // No hay vínculo: muestra el botón para agregar un adulto mayor
                    btn_agregar_abuelo.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        solicitarPermisoNotificaciones();

        Intent servicioIntent = new Intent(this, ServicioAlertasCuidador.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(servicioIntent);
        } else {
            startService(servicioIntent);
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /** Solicita el permiso de notificaciones en Android 13+ (Tiramisu). */
    private void solicitarPermisoNotificaciones() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }

    /** Muestra el banner de "sin conexión" si al abrir la app no hay internet. */
    private void verificarConexionInicial() {
        if(connectivityManager != null){
            Network activeNetwork = connectivityManager.getActiveNetwork();
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            boolean isConnected = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

            if(!isConnected){
                tvSinConexion.setVisibility(View.VISIBLE);
            }
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        // Libera el callback de red para evitar fugas de memoria
        if(connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }

    /** Rellena la tarjeta de vínculo con el nombre del adulto mayor y el estado activo. */
    private void actualizarInterfazVinculo(String uidAbuelo) {
        mDatabase.child("Usuarios").child(uidAbuelo).child("nombre").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String nombreAbuelo = task.getResult().getValue(String.class);

                tvTituloConexion.setText("Cuidando a: " + nombreAbuelo);
                tvSubtituloConexion.setText("Conexión activa");

                // Colorea el icono y el LED para indicar conexión activa
                ivIconoVinculo.setImageTintList(ColorStateList.valueOf(Color.parseColor("#80C7DE")));
                viewIndicadorLed.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            }
        });
    }
}