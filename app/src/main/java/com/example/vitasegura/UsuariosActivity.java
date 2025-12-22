package com.example.vitasegura;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class UsuariosActivity extends AppCompatActivity {

    private TextView tvTitulo, tvNombre, tvCorreo, tvTelefono;
    private Button btnCambiarPass, btnEliminar, btnAnterior, btnSiguiente;
    private ImageView ivAgregar;
    private List<Usuario> listaUsuarios = new ArrayList<>();
    private int indiceActual = 0;

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

        // Cargar datos (Simulando que el 0 es el principal)
        listaUsuarios.add(new Usuario("Daniel Eduardo Pelayo Gómez", "a22300954@ceti.mx", "33-12-34-56-78", true));
        listaUsuarios.add(new Usuario("Familiar de Apoyo", "familiar1@mail.com", "33-98-76-54-32", false));

        actualizarPantalla();

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
            // Reutilizamos la activity de cambio de pass del abuelo
            Intent intent = new Intent(this, CambiarPassActivity.class);
            startActivity(intent);
        });

        btnEliminar.setOnClickListener(v -> mostrarDialogEliminar());

        ivAgregar.setOnClickListener(v -> {
            Intent intent = new Intent(this, FormularioMedicamentoActivity.class);
            intent.putExtra("modo", "AGREGAR_USUARIO");
            startActivity(intent);
        });

        findViewById(R.id.iv_back_usuarios).setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void actualizarPantalla() {
        Usuario user = listaUsuarios.get(indiceActual);

        tvNombre.setText(user.getNombre());
        tvCorreo.setText(user.getCorreo());
        tvTelefono.setText(user.getTelefono());

        if (user.isEsPrincipal()) {
            tvTitulo.setText("Mi Información");
            btnCambiarPass.setVisibility(View.VISIBLE);
            btnEliminar.setVisibility(View.GONE);
            ivAgregar.setVisibility(listaUsuarios.size() < 3 ? View.VISIBLE : View.GONE);
        } else {
            tvTitulo.setText("Usuarios");
            btnCambiarPass.setVisibility(View.GONE);
            btnEliminar.setVisibility(View.VISIBLE);
            ivAgregar.setVisibility(View.GONE);
        }

        // Desactivar botones si no hay más elementos
        btnAnterior.setEnabled(indiceActual > 0);
        btnSiguiente.setEnabled(indiceActual < listaUsuarios.size() - 1);
    }

    private void mostrarDialogEliminar() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirmar_eliminar, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView tvMsj = dialogView.findViewById(R.id.tv_mensaje_confirmacion);
        tvMsj.setText("¿Estás seguro de eliminar este usuario?");

        dialogView.findViewById(R.id.btn_aceptar_eliminar).setOnClickListener(v -> {
            listaUsuarios.remove(indiceActual);
            indiceActual = 0; // Regresar al principal
            actualizarPantalla();
            dialog.dismiss();
            Toast.makeText(this, "Usuario eliminado", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }
}