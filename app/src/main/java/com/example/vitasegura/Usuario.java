package com.example.vitasegura;

public class Usuario {
    private String uid;
    private String nombre;
    private String correo;
    private String telefono;
    private boolean esPrincipal;

    // CONSTRUCTOR VACÍO
    public Usuario() {
    }

    // 2. Constructor con todos los parámetros
    public Usuario(String uid, String nombre, String correo, String telefono, boolean esPrincipal) {
        this.uid = uid;
        this.nombre = nombre;
        this.correo = correo;
        this.telefono = telefono;
        this.esPrincipal = esPrincipal;
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
}