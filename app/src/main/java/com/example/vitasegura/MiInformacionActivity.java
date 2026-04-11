package com.example.vitasegura;

import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class MiInformacionActivity extends AppCompatActivity {

    private ImageView ivBack, ivPerfil;
    private TextView tvNombre, tvCorreo, tvTelefono;

    //Variables para foto de perfil
    private Uri imagenUri;
    private ActivityResultLauncher<String> galeriaLauncher;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mi_informacion);

        ivBack = findViewById(R.id.iv_back_info);
        ivPerfil = findViewById(R.id.iv_perfil_info);
        tvNombre = findViewById(R.id.tv_info_nombre);
        tvCorreo = findViewById(R.id.tv_info_correo);
        tvTelefono = findViewById(R.id.tv_info_telefono);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        ivBack.setOnClickListener(v -> finish());

        galeriaLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if(uri != null){//Si se seleccionó una imagen
                        imagenUri = uri;
                        //Mostramos la foto localmente de inmediato
                        Glide.with(this).load(imagenUri).circleCrop().into(ivPerfil);
                        //Subimos la foto a Firebase Storage
                        subirFotoAFirebase();

                    }
                }
        );

        //AL tocar la foto se abre la galeria
        ivPerfil.setOnClickListener(v ->{
            galeriaLauncher.launch("image/*"); //Para cualquier tipo de imagen
        });


        cargarInformacionAbuelo();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void cargarInformacionAbuelo() {
        if (mAuth.getCurrentUser() != null) {
            String uidAbuelo = mAuth.getCurrentUser().getUid();

            mDatabase.child("Usuarios").child(uidAbuelo).get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            // Si el abuelo existe, obtenemos su información
                            Usuario abuelo = snapshot.getValue(Usuario.class);

                            if (abuelo != null) {
                                tvNombre.setText(abuelo.getNombre() != null ? abuelo.getNombre() : "Sin nombre");
                                tvCorreo.setText(abuelo.getCorreo() != null ? abuelo.getCorreo() : "Sin correo");
                                tvTelefono.setText(abuelo.getTelefono() != null ? abuelo.getTelefono() : "Sin teléfono");

                                String urlFoto = abuelo.getFotoPerfil();
                                if(urlFoto != null && !urlFoto.isEmpty()){
                                    Glide.with(this)
                                            .load(urlFoto)
                                            .circleCrop()
                                            .placeholder(R.drawable.usuario)
                                            .into(ivPerfil);
                                }else {
                                    ivPerfil.setImageResource(R.drawable.usuario);
                                }
                            }
                        } else {
                            Toast.makeText(this, "No se encontró la información del perfil", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error de conexión al cargar datos", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    //Metodo para subir la foto a Firebase Storage
    private void subirFotoAFirebase() {
        if (mAuth.getCurrentUser() == null || imagenUri == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        Toast.makeText(this, "Subiendo foto, por favor espera...", Toast.LENGTH_SHORT).show();

        //Crear la referencia en Firebase Storage (Carpeta: FotosPerfil / Nombre: el UID del usuario)
        com.google.firebase.storage.StorageReference storageRef = com.google.firebase.storage.FirebaseStorage.getInstance()
                .getReference().child("FotosPerfil").child(uid + ".jpg");

        //Subir la imagen
        storageRef.putFile(imagenUri)
                .addOnSuccessListener(taskSnapshot -> {
                    //Le pedimos la URL directamente al archivo que acaba de llegar a la nube
                    taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                        String urlDescarga = uri.toString();

                        //Guardamos ese enlace en nuestro Usuario en la base de datos de texto
                        mDatabase.child("Usuarios").child(uid).child("fotoPerfil").setValue(urlDescarga)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(MiInformacionActivity.this, "¡Foto actualizada con éxito!", Toast.LENGTH_SHORT).show();
                                });
                    }).addOnFailureListener(e -> {
                        Toast.makeText(MiInformacionActivity.this, "Error al obtener URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MiInformacionActivity.this, "Error al subir la foto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}