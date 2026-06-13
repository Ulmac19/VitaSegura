package com.example.vitasegura;

/**
 * Modelo de datos que representa una ubicación GPS del adulto mayor.
 *
 * Combina las coordenadas (latitud, longitud) con una dirección legible y la
 * fecha/hora de registro. Se emplea para el historial de ubicaciones que el
 * cuidador consulta en UbicacionAdultoActivity.
 */
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