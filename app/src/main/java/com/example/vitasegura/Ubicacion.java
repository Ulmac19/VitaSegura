package com.example.vitasegura;

public class Ubicacion {
    private String direccion;
    private String fechaHora;
    private double latitud;
    private double longitud;

    public Ubicacion(String direccion, String fechaHora, double latitud, double longitud) {
        this.direccion = direccion;
        this.fechaHora = fechaHora;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public String getDireccion() { return direccion; }
    public String getFechaHora() { return fechaHora; }
    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }
}