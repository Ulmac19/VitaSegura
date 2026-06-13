package com.example.vitasegura;

/**
 * Modelo de datos que representa a un usuario de la aplicación.
 *
 * Se corresponde con los nodos Usuarios/[uid] de Firebase Realtime Database.
 * El campo esPrincipal determina el rol: true para el cuidador (Familiar) y
 * false para el adulto mayor.
 */
public class Usuario {
    private String uid;
    private String nombre;
    private String correo;
    private String telefono;
    private boolean esPrincipal;
    private String fotoPerfil;

    /** Constructor vacío requerido por Firebase para la deserialización automática. */
    public Usuario() {
    }

    /** Construye un usuario con todos sus atributos. */
    public Usuario(String uid, String nombre, String correo, String telefono, boolean esPrincipal, String fotoPerfil) {
        this.uid = uid;
        this.nombre = nombre;
        this.correo = correo;
        this.telefono = telefono;
        this.esPrincipal = esPrincipal;
        this.fotoPerfil = fotoPerfil;
    }

    // Getters y Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public boolean isEsPrincipal() { return esPrincipal; }
    public void setEsPrincipal(boolean esPrincipal) { this.esPrincipal = esPrincipal; }

    public String getFotoPerfil() { return fotoPerfil; }
    public void setFotoPerfil(String fotoPerfil) { this.fotoPerfil = fotoPerfil; }
}