package com.example.vitasegura;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class UsuariosActivity extends AppCompatActivity {

    private TextView tvTitulo, tvNombre, tvCorreo, tvTelefono;
    private Button btnCambiarPass, btnEliminar, btnAnterior, btnSiguiente;
    private ImageView ivAgregar;

    private List<Usuario> listaUsuarios = new ArrayList<>();
    private int indiceActual = 0;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String miUid;
    private String uidAbueloActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usuarios);

        // Vincular vistas
        tvTitulo = findViewById(R.id.tv_titulo_pantalla);
        tvNombre = findViewById(R.id.tv_nombre);
        tvCorreo = findViewById(R.id.tv_correo);
        tvTelefono = findViewById(R.id.tv_telefono);
        btnCambiarPass = findViewById(R.id.btn_cambiar_pass);
        btnEliminar = findViewById(R.id.btn_eliminar_usuario);
        btnAnterior = findViewById(R.id.btn_anterior);
        btnSiguiente = findViewById(R.id.btn_siguiente);
        ivAgregar = findViewById(R.id.iv_agregar_usuario);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        if (mAuth.getCurrentUser() != null) {
            miUid = mAuth.getCurrentUser().getUid();
            obtenerIdAbueloYCuidadores();
        }

        // Navegación carrusel
        btnSiguiente.setOnClickListener(v -> {
            if (indiceActual < listaUsuarios.size() - 1) {
                indiceActual++;
                actualizarPantalla();
            }
        });

        btnAnterior.setOnClickListener(v -> {
            if (indiceActual > 0) {
                indiceActual--;
                actualizarPantalla();
            }
        });

        // Lógica de botones
        btnCambiarPass.setOnClickListener(v -> {
            Intent intent = new Intent(this, CambiarPassActivity.class);
            startActivity(intent);
        });

        btnEliminar.setOnClickListener(v -> mostrarDialogEliminar());

        //Abrimos un diálogo para agregar por correo
        ivAgregar.setOnClickListener(v -> mostrarDialogAgregarCuidador());

        findViewById(R.id.iv_back_usuarios).setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // --- LÓGICA DE FIREBASE ---

    private void obtenerIdAbueloYCuidadores() {
        //Saber a qué abuelo estoy cuidando
        mDatabase.child("Vinculos").child(miUid).child("id_adulto_vinculado").get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        //Se tiene un abuelo vinculado: cargamos toda la red
                        uidAbueloActual = snapshot.getValue(String.class);
                        cargarRedDeCuidadores(uidAbueloActual);
                    }else{
                        //No se tiene un abuelo vinculado: solo mi información
                        cargarSoloMiInformacion();
                    }
                });
    }

    private void cargarSoloMiInformacion() {
        mDatabase.child("Usuarios").child(miUid).get().addOnSuccessListener(userSnap -> {
            if (userSnap.exists()) {
                listaUsuarios.clear();
                Usuario u = userSnap.getValue(Usuario.class);
                // Si el getValue(Usuario.class) te da problemas, usa el mapeo manual que ya tenías:
                if (u != null) {
                    u.setUid(miUid); // Aseguramos que tenga el UID
                    listaUsuarios.add(u);
                    indiceActual = 0;
                    actualizarPantalla();
                }
            }
        });
    }

    private void cargarRedDeCuidadores(String idAbuelo) {
        //Buscar en "Vinculos" a todos los que cuiden a este mismo abuelo
        mDatabase.child("Vinculos").orderByChild("id_adulto_vinculado").equalTo(idAbuelo)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        listaUsuarios.clear();
                        indiceActual = 0;

                        List<String> uidsCuidadores = new ArrayList<>();
                        for (DataSnapshot vinculo : snapshot.getChildren()) {
                            uidsCuidadores.add(vinculo.getKey()); // Obtenemos el UID de cada cuidador
                        }

                        // 3. Descargar la información de cada cuidador encontrado
                        for (String uid : uidsCuidadores) {
                            mDatabase.child("Usuarios").child(uid).get().addOnSuccessListener(userSnap -> {
                                if (userSnap.exists()) {
                                    String nombre = userSnap.child("nombre").getValue(String.class);
                                    String correo = userSnap.child("correo").getValue(String.class);
                                    String telefono = userSnap.child("telefono").getValue(String.class);
                                    Boolean esPrincipal = userSnap.child("esPrincipal").getValue(Boolean.class);

                                    Usuario u = new Usuario(uid, nombre, correo, telefono, esPrincipal != null ? esPrincipal : true);

                                    // Me aseguro de que "Mi Información" siempre quede en la posición 0
                                    if (uid.equals(miUid)) {
                                        listaUsuarios.add(0, u);
                                    } else {
                                        listaUsuarios.add(u);
                                    }
                                    actualizarPantalla();
                                }
                            });
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void mostrarDialogAgregarCuidador() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Agregar Cuidador de Apoyo");
        builder.setMessage("Ingresa el correo del cuidador que deseas asociar. Debe tener una cuenta creada en VitaSegura.");

        final EditText input = new EditText(this);
        input.setHint("correo@ejemplo.com");

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        builder.setView(input);

        builder.setPositiveButton("Vincular", (dialog, which) -> {
            String correoIngresado = input.getText().toString().trim();
            if (!correoIngresado.isEmpty() && uidAbueloActual != null) {
                buscarYVincularCuidadorPorCorreo(correoIngresado);
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void buscarYVincularCuidadorPorCorreo(String correo) {
        mDatabase.child("Usuarios").orderByChild("correo").equalTo(correo).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        for (DataSnapshot userSnap : snapshot.getChildren()) {
                            String nuevoCuidadorUid = userSnap.getKey();

                            // Leemos tu campo esPrincipal desde Firebase
                            Boolean esPrincipal = userSnap.child("esPrincipal").getValue(Boolean.class);

                            // Si es true, es un Cuidador. Si es false o null, es Abuelo.
                            if (esPrincipal != null && esPrincipal) {
                                mDatabase.child("Vinculos").child(nuevoCuidadorUid).child("id_adulto_vinculado")
                                        .setValue(uidAbueloActual)
                                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "Cuidador asociado con éxito", Toast.LENGTH_SHORT).show());
                            } else {
                                Toast.makeText(this, "Este correo pertenece a un Adulto Mayor, no a un cuidador.", Toast.LENGTH_LONG).show();
                            }
                            return; // Terminamos porque ya lo encontramos
                        }
                    } else {
                        Toast.makeText(this, "No se encontró ningún usuario con ese correo", Toast.LENGTH_LONG).show();
                    }
                });
    }

    // --- INTERFAZ VISUAL ---

    private void actualizarPantalla() {
        if (listaUsuarios.isEmpty()) return;

        Usuario user = listaUsuarios.get(indiceActual);

        tvNombre.setText(user.getNombre() != null ? user.getNombre() : "Sin nombre");
        tvCorreo.setText(user.getCorreo() != null ? user.getCorreo() : "Sin correo");
        tvTelefono.setText(user.getTelefono() != null ? user.getTelefono() : "Sin teléfono");

        //Comparamos si este usuario soy yo
        boolean esMiPerfil = user.getUid().equals(miUid);

        if (esMiPerfil) {
            tvTitulo.setText("Mi Información");
            btnCambiarPass.setVisibility(View.VISIBLE);
            btnEliminar.setVisibility(View.GONE);
            ivAgregar.setVisibility(listaUsuarios.size() < 3 ? View.VISIBLE : View.GONE);
        } else {
            tvTitulo.setText("Cuidador de Apoyo");
            btnCambiarPass.setVisibility(View.GONE);
            btnEliminar.setVisibility(View.VISIBLE);
            ivAgregar.setVisibility(View.GONE);
        }

        btnAnterior.setEnabled(indiceActual > 0);
        btnAnterior.setAlpha(indiceActual > 0 ? 1.0f : 0.5f);

        btnSiguiente.setEnabled(indiceActual < listaUsuarios.size() - 1);
        btnSiguiente.setAlpha(indiceActual < listaUsuarios.size() - 1 ? 1.0f : 0.5f);
    }

    private void mostrarDialogEliminar() {
        // En lugar de un layout personalizado, usamos el AlertDialog nativo por rapidez y limpieza
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Cuidador")
                .setMessage("¿Estás seguro de que deseas desvincular a este cuidador? Ya no podrá ver la información del adulto mayor.")
                .setPositiveButton("Sí, eliminar", (dialog, which) -> {
                    Usuario userAEliminar = listaUsuarios.get(indiceActual);
                    // Borramos su nodo en Vinculos
                    mDatabase.child("Vinculos").child(userAEliminar.getUid()).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Cuidador desvinculado", Toast.LENGTH_SHORT).show();
                                // La lista se actualizará sola gracias al addValueEventListener
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}