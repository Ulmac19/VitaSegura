package com.example.vitasegura;

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