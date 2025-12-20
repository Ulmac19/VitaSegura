package com.example.vitasegura;

public class Ubicacion {
    private String direccion;
    private String fechaHora;

    public Ubicacion(String direccion, String fechaHora) {
        this.direccion = direccion;
        this.fechaHora = fechaHora;
    }

    public String getDireccion() { return direccion; }
    public String getFechaHora() { return fechaHora; }
}