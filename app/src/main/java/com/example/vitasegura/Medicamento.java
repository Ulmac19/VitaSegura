package com.example.vitasegura;

/**
 * Modelo de datos que representa un medicamento y su recordatorio asociado.
 *
 * Se almacena bajo Usuarios/[uidAbuelo]/Medicamentos/[id] en Firebase. El campo
 * frecuencia admite los valores "Cada 8 horas", "Cada 12 horas", "Cada 24 horas"
 * o "Solo si hay dolor"; en este último caso hora queda vacío porque la toma es
 * libre.
 */
public class Medicamento {
    private String id;
    private String nombre;
    private String frecuencia;
    private String hora;
    private String dosis;
    private String notas;

    // Constructor vacío requerido por Firebase
    public Medicamento() {}

    public Medicamento(String id, String nombre, String frecuencia, String hora, String dosis, String notas) {
        this.id = id;
        this.nombre = nombre;
        this.frecuencia = frecuencia;
        this.hora = hora;
        this.dosis = dosis;
        this.notas = notas;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public String getFrecuencia() { return frecuencia; }
    public String getHora() { return hora; }
    public String getDosis() { return dosis; }
    public String getNotas() { return notas; }
}