package com.example.vitasegura;

public class Usuario {
    private String nombre, correo, telefono;
    private boolean esPrincipal;

    public Usuario(String nombre, String correo, String telefono, boolean esPrincipal) {
        this.nombre = nombre;
        this.correo = correo;
        this.telefono = telefono;
        this.esPrincipal = esPrincipal;
    }

    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getTelefono() { return telefono; }
    public boolean isEsPrincipal() { return esPrincipal; }
}